package com.example.data.repository

import com.example.data.db.CommandLogDao
import com.example.data.model.AppItem
import com.example.data.model.BlockedRule
import com.example.data.model.CommandLog
import com.example.data.model.FirewallMode
import com.example.engine.BlockingProbe
import com.example.engine.BlockingStrategy
import com.example.engine.CapabilityLevel
import com.example.engine.NativeFirewallEngine
import com.example.engine.ProbeResult
import com.example.executor.AdbHelperExecutor
import com.example.executor.ExecResult
import com.example.executor.PrivilegedExecutor
import com.example.executor.RootExecutor
import com.example.executor.ShizukuExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class FirewallRepository(
    private val commandLogDao: CommandLogDao,
    private val firewallEngine: NativeFirewallEngine,
    val rootExecutor: RootExecutor,
    val shizukuExecutor: ShizukuExecutor,
    val adbHelperExecutor: AdbHelperExecutor
) {

    val recentLogs: Flow<List<CommandLog>> = commandLogDao.getRecentLogs()

    private val _currentProbe = MutableStateFlow(
        ProbeResult(
            strategy = BlockingStrategy.Unsupported,
            capabilityLevel = CapabilityLevel.UNSUPPORTED,
            description = "Probing device capability..."
        )
    )
    val currentProbe = _currentProbe.asStateFlow()

    private val _activeExecutorName = MutableStateFlow("None")
    val activeExecutorName = _activeExecutorName.asStateFlow()

    /**
     * Finds the active privileged executor according to the selected FirewallMode.
     * AUTO prioritizes: Root > Shizuku > AdbHelper.
     */
    suspend fun resolveExecutor(mode: FirewallMode): PrivilegedExecutor? = withContext(Dispatchers.IO) {
        val executor = when (mode) {
            FirewallMode.ROOT -> if (rootExecutor.isAvailable()) rootExecutor else null
            FirewallMode.SHIZUKU -> if (shizukuExecutor.isAvailable()) shizukuExecutor else null
            FirewallMode.ADB_HELPER -> if (adbHelperExecutor.isAvailable()) adbHelperExecutor else null
            FirewallMode.AUTO -> {
                when {
                    rootExecutor.isAvailable() -> rootExecutor
                    shizukuExecutor.isAvailable() -> shizukuExecutor
                    adbHelperExecutor.isAvailable() -> adbHelperExecutor
                    else -> null
                }
            }
            FirewallMode.SIMULATION -> null
        }

        _activeExecutorName.value = executor?.name ?: if (mode == FirewallMode.SIMULATION) "Simulation" else "None"

        // Probe device with resolved executor
        if (executor != null) {
            val probeResult = BlockingProbe.probe(executor)
            _currentProbe.value = probeResult
        } else if (mode == FirewallMode.SIMULATION) {
            _currentProbe.value = ProbeResult(
                strategy = BlockingStrategy.PackageNetworking(),
                capabilityLevel = CapabilityLevel.FULL,
                description = "Simulation Mode (Dry Run)"
            )
        } else {
            _currentProbe.value = ProbeResult(
                strategy = BlockingStrategy.Unsupported,
                capabilityLevel = CapabilityLevel.UNSUPPORTED,
                description = "No active privileged executor configured"
            )
        }

        executor
    }

    /**
     * Applies block/unblock for a single application and verifies real OS state.
     * Returns the verified state (true if verified blocked, false if verified allowed).
     */
    suspend fun applyAppBlockWithVerification(
        app: AppItem,
        shouldBlock: Boolean,
        mode: FirewallMode
    ): Boolean = withContext(Dispatchers.IO) {
        val executor = resolveExecutor(mode)

        if (mode == FirewallMode.SIMULATION) {
            commandLogDao.insertLog(
                CommandLog(
                    command = "[SIMULATION] Set ${app.packageName} blocked = $shouldBlock",
                    exitCode = 0,
                    output = "Simulated action succeeded.",
                    mode = "SIMULATION"
                )
            )
            return@withContext shouldBlock
        }

        if (executor == null) {
            commandLogDao.insertLog(
                CommandLog(
                    command = "Set ${app.packageName} blocked = $shouldBlock",
                    exitCode = -1,
                    output = "No privileged executor available (Root, Shizuku, or ADB Helper).",
                    mode = mode.name
                )
            )
            return@withContext false
        }

        val strategy = _currentProbe.value.strategy
        val cmd = if (shouldBlock) {
            firewallEngine.buildBlockCommand(app, strategy)
        } else {
            firewallEngine.buildUnblockCommand(app, strategy)
        }

        // Execute action
        val execResult = executor.exec(cmd)
        commandLogDao.insertLog(
            CommandLog(
                command = cmd,
                exitCode = execResult.exitCode,
                output = execResult.output.ifEmpty { "Success" },
                mode = executor.name
            )
        )

        // VERIFICATION: Read the rule back using the matching query subcommand
        val queryCmd = firewallEngine.buildQueryCommand(app, strategy)
        val queryResult = executor.exec(queryCmd)
        val realState = firewallEngine.parseQueryState(app, strategy, queryResult.exitCode, queryResult.output)

        commandLogDao.insertLog(
            CommandLog(
                command = "Verify: $queryCmd",
                exitCode = queryResult.exitCode,
                output = "Verified real state: ${if (realState) "BLOCKED" else "ALLOWED"} (Raw: ${queryResult.output.take(60)})",
                mode = executor.name
            )
        )

        realState
    }

    /**
     * Applies rules for multiple apps.
     */
    suspend fun syncAllRules(
        apps: List<AppItem>,
        mode: FirewallMode
    ): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val verifiedResults = mutableMapOf<String, Boolean>()
        for (app in apps) {
            val verified = applyAppBlockWithVerification(app, app.isBlocked, mode)
            verifiedResults[app.packageName] = verified
        }
        verifiedResults
    }

    suspend fun flushAll(mode: FirewallMode): ExecResult = withContext(Dispatchers.IO) {
        val executor = resolveExecutor(mode) ?: return@withContext ExecResult(-1, "No executor available")
        val strategy = _currentProbe.value.strategy
        val cmd = firewallEngine.buildFlushScript(strategy)
        val res = executor.exec(cmd)
        commandLogDao.insertLog(
            CommandLog(
                command = "Flush all firewall rules: $cmd",
                exitCode = res.exitCode,
                output = res.output,
                mode = executor.name
            )
        )
        res
    }

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

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        commandLogDao.clearLogs()
    }
}
