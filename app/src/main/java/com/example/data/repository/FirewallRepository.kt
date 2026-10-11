package com.example.data.repository

import com.example.data.db.CommandLogDao
import com.example.data.model.BlockedRule
import com.example.data.model.CommandLog
import com.example.data.model.FirewallMode
import com.example.engine.CommandResult
import com.example.engine.NativeFirewallEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class FirewallRepository(
    private val commandLogDao: CommandLogDao,
    private val firewallEngine: NativeFirewallEngine
) {

    val recentLogs: Flow<List<CommandLog>> = commandLogDao.getRecentLogs()

    suspend fun checkRoot(): Boolean {
        return firewallEngine.isRootAvailable()
    }

    suspend fun applyRules(
        rules: List<BlockedRule>,
        mode: FirewallMode
    ): CommandResult = withContext(Dispatchers.IO) {
        when (mode) {
            FirewallMode.ROOT -> {
                val script = firewallEngine.buildIptablesScript(rules)
                val result = firewallEngine.executeCommand(script, asRoot = true)
                commandLogDao.insertLog(
                    CommandLog(
                        command = "Apply ${rules.count { it.isBlocked }} blocked rules via iptables",
                        exitCode = result.exitCode,
                        output = result.output.ifEmpty { "Rules applied successfully to $CHAIN_NAME chain." },
                        mode = "ROOT"
                    )
                )
                result
            }

            FirewallMode.ADB_SHELL -> {
                val script = firewallEngine.buildAdbShellScript(rules)
                // In ADB Shell mode, if root is present we can run cmd netpolicy directly, or user runs it via adb
                val result = if (firewallEngine.isRootAvailable()) {
                    firewallEngine.executeCommand(script, asRoot = true)
                } else {
                    CommandResult(
                        exitCode = 0,
                        output = "Script generated for ADB execution. ${rules.count { it.isBlocked }} policies ready."
                    )
                }
                commandLogDao.insertLog(
                    CommandLog(
                        command = "Export / Run ADB netpolicy commands for ${rules.count { it.isBlocked }} apps",
                        exitCode = result.exitCode,
                        output = result.output,
                        mode = "ADB_SHELL"
                    )
                )
                result
            }

            FirewallMode.AUTO -> {
                val isRoot = firewallEngine.isRootAvailable()
                if (isRoot) {
                    applyRules(rules, FirewallMode.ROOT)
                } else {
                    applyRules(rules, FirewallMode.ADB_SHELL)
                }
            }

            FirewallMode.SIMULATION -> {
                val script = firewallEngine.buildIptablesScript(rules)
                val simulatedResult = CommandResult(
                    exitCode = 0,
                    output = "[SIMULATION] Validated rules for ${rules.count { it.isBlocked }} apps.\nSample generated command:\n${script.take(200)}..."
                )
                commandLogDao.insertLog(
                    CommandLog(
                        command = "[SIMULATION] Validate ${rules.count { it.isBlocked }} rules",
                        exitCode = 0,
                        output = simulatedResult.output,
                        mode = "SIMULATION"
                    )
                )
                simulatedResult
            }
        }
    }

    suspend fun flushRules(asRoot: Boolean): CommandResult = withContext(Dispatchers.IO) {
        val script = firewallEngine.buildFlushScript()
        val result = if (asRoot) {
            firewallEngine.executeCommand(script, asRoot = true)
        } else {
            CommandResult(0, "Rules reset in memory.")
        }
        commandLogDao.insertLog(
            CommandLog(
                command = "Flush firewall chain ($CHAIN_NAME)",
                exitCode = result.exitCode,
                output = result.output.ifEmpty { "Chain flushed. All traffic allowed." },
                mode = if (asRoot) "ROOT" else "NON_ROOT"
            )
        )
        result
    }

    /**
     * Checks if internet connectivity is reachable using a low-latency socket ping to DNS.
     */
    suspend fun testGlobalConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("1.1.1.1", 53), 1500)
                true
            }
        } catch (_: Exception) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    fun getIptablesScript(rules: List<BlockedRule>): String =
        firewallEngine.buildIptablesScript(rules)

    fun getAdbScript(rules: List<BlockedRule>): String =
        firewallEngine.buildAdbShellScript(rules)

    fun getWindowsBatScript(rules: List<BlockedRule>): String =
        firewallEngine.buildWindowsBatScript(rules)

    fun getSingleAppAdbCommand(rule: BlockedRule): String =
        firewallEngine.getSingleAppAdbCommand(rule)

    fun getSingleAppRootCommand(rule: BlockedRule): String =
        firewallEngine.getSingleAppRootCommand(rule)

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        commandLogDao.clearLogs()
    }

    companion object {
        const val CHAIN_NAME = NativeFirewallEngine.CHAIN_NAME
    }
}
