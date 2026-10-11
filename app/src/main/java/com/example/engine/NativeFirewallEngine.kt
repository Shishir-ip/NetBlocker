package com.example.engine

import android.content.Context
import android.os.Build
import com.example.data.model.BlockedRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val isSuccess: Boolean = (exitCode == 0)
)

class NativeFirewallEngine(private val context: Context) {

    companion object {
        const val CHAIN_NAME = "NETBLOCKER_OUT"
    }

    private var cachedRootAvailable: Boolean? = null

    /**
     * Checks if the device has root access available.
     */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        cachedRootAvailable?.let { return@withContext it }

        val suPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        val suBinaryExists = suPaths.any { File(it).exists() }
        if (!suBinaryExists) {
            cachedRootAvailable = false
            return@withContext false
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitCode = process.waitFor()
            val available = (exitCode == 0)
            cachedRootAvailable = available
            available
        } catch (_: Exception) {
            cachedRootAvailable = false
            false
        }
    }

    /**
     * Executes a shell command via `su` (if asRoot is true) or standard `sh`.
     */
    suspend fun executeCommand(command: String, asRoot: Boolean = true): CommandResult =
        withContext(Dispatchers.IO) {
            try {
                val shell = if (asRoot) "su" else "sh"
                val process = Runtime.getRuntime().exec(arrayOf(shell, "-c", command))

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val outputBuilder = StringBuilder()

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    outputBuilder.appendLine(line)
                }
                while (errorReader.readLine().also { line = it } != null) {
                    outputBuilder.appendLine("ERR: $line")
                }

                val exitCode = process.waitFor()
                CommandResult(
                    exitCode = exitCode,
                    output = outputBuilder.toString().trim()
                )
            } catch (e: Exception) {
                CommandResult(
                    exitCode = -1,
                    output = "Execution failed: ${e.message}"
                )
            }
        }

    /**
     * Generates the complete iptables script for applying active blocked rules.
     */
    fun buildIptablesScript(rules: List<BlockedRule>): String {
        val activeRules = rules.filter { it.isBlocked && (it.blockWifi || it.blockMobile || it.blockLan) }
        val sb = StringBuilder()

        sb.appendLine("# ===========================================")
        sb.appendLine("# NetBlocker Native Firewall Rules")
        sb.appendLine("# Generated at: ${System.currentTimeMillis()}")
        sb.appendLine("# Active Blocked Applications: ${activeRules.size}")
        sb.appendLine("# ===========================================")
        sb.appendLine()
        sb.appendLine("# Initialize custom chain in iptables (IPv4)")
        sb.appendLine("iptables -N $CHAIN_NAME 2>/dev/null || true")
        sb.appendLine("iptables -C OUTPUT -j $CHAIN_NAME 2>/dev/null || iptables -I OUTPUT 1 -j $CHAIN_NAME")
        sb.appendLine("iptables -F $CHAIN_NAME")
        sb.appendLine()
        sb.appendLine("# Initialize custom chain in ip6tables (IPv6)")
        sb.appendLine("ip6tables -N $CHAIN_NAME 2>/dev/null || true")
        sb.appendLine("ip6tables -C OUTPUT -j $CHAIN_NAME 2>/dev/null || ip6tables -I OUTPUT 1 -j $CHAIN_NAME")
        sb.appendLine("ip6tables -F $CHAIN_NAME")
        sb.appendLine()

        for (rule in activeRules) {
            sb.appendLine("# Rule for: ${rule.appName} (${rule.packageName}) - UID ${rule.uid}")
            val allBlocked = rule.blockWifi && rule.blockMobile && rule.blockLan

            if (allBlocked) {
                sb.appendLine("iptables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -j DROP")
                sb.appendLine("ip6tables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -j DROP")
            } else {
                if (rule.blockWifi) {
                    sb.appendLine("iptables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -o wlan+ -j DROP")
                    sb.appendLine("iptables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -o ap+ -j DROP")
                    sb.appendLine("ip6tables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -o wlan+ -j DROP")
                }
                if (rule.blockMobile) {
                    sb.appendLine("iptables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -o rmnet+ -j DROP")
                    sb.appendLine("iptables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -o ccmni+ -j DROP")
                    sb.appendLine("ip6tables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -o rmnet+ -j DROP")
                }
                if (rule.blockLan) {
                    sb.appendLine("iptables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -o eth+ -j DROP")
                    sb.appendLine("iptables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -o usb+ -j DROP")
                    sb.appendLine("ip6tables -A $CHAIN_NAME -m owner --uid-owner ${rule.uid} -o eth+ -j DROP")
                }
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * Builds ADB Shell commands for non-root users using Android netpolicy and appops.
     */
    fun buildAdbShellScript(rules: List<BlockedRule>): String {
        val activeRules = rules.filter { it.isBlocked && (it.blockWifi || it.blockMobile || it.blockLan) }
        val sb = StringBuilder()

        sb.appendLine("#!/bin/bash")
        sb.appendLine("# ===========================================")
        sb.appendLine("# NetBlocker Non-Root ADB Shell Commands")
        sb.appendLine("# Run via: adb shell")
        sb.appendLine("# Or execute directly from your PC terminal")
        sb.appendLine("# ===========================================")
        sb.appendLine()

        for (rule in activeRules) {
            sb.appendLine("# Block network access for ${rule.appName} (${rule.packageName}) UID: ${rule.uid}")
            // Restrict background rules via cmd netpolicy
            sb.appendLine("cmd netpolicy set restrict-background-rules ${rule.uid} 1")
            sb.appendLine("cmd netpolicy set metered-network-allowlist ${rule.uid} false")
            sb.appendLine("cmd appops set ${rule.packageName} RUN_IN_BACKGROUND ignore")
            sb.appendLine("cmd appops set ${rule.packageName} RUN_ANY_IN_BACKGROUND ignore")
            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * Generates a Windows Batch (.bat) file with ADB commands.
     */
    fun buildWindowsBatScript(rules: List<BlockedRule>): String {
        val activeRules = rules.filter { it.isBlocked && (it.blockWifi || it.blockMobile || it.blockLan) }
        val sb = StringBuilder()

        sb.appendLine("@echo off")
        sb.appendLine("echo =========================================")
        sb.appendLine("echo NetBlocker ADB Windows Rule Applier")
        sb.appendLine("echo =========================================")
        sb.appendLine("echo Applying network blocks via ADB...")
        sb.appendLine()

        for (rule in activeRules) {
            sb.appendLine("echo Blocking ${rule.appName} (UID: ${rule.uid})...")
            sb.appendLine("adb shell cmd netpolicy set restrict-background-rules ${rule.uid} 1")
            sb.appendLine("adb shell cmd netpolicy set metered-network-allowlist ${rule.uid} false")
            sb.appendLine("adb shell cmd appops set ${rule.packageName} RUN_IN_BACKGROUND ignore")
            sb.appendLine("adb shell cmd appops set ${rule.packageName} RUN_ANY_IN_BACKGROUND ignore")
            sb.appendLine()
        }

        sb.appendLine("echo Network policies applied successfully!")
        sb.appendLine("pause")
        return sb.toString()
    }

    /**
     * Generates specific command for a single application.
     */
    fun getSingleAppAdbCommand(rule: BlockedRule): String {
        return "adb shell \"cmd netpolicy set restrict-background-rules ${rule.uid} 1 && cmd appops set ${rule.packageName} RUN_IN_BACKGROUND ignore\""
    }

    /**
     * Generates single app root iptables command.
     */
    fun getSingleAppRootCommand(rule: BlockedRule): String {
        val targets = mutableListOf<String>()
        if (rule.blockWifi) targets.add("wlan+")
        if (rule.blockMobile) targets.add("rmnet+")
        if (rule.blockLan) targets.add("eth+")

        if (targets.isEmpty()) return "# No interfaces selected"
        return targets.joinToString(" && ") { iface ->
            "iptables -I OUTPUT 1 -m owner --uid-owner ${rule.uid} -o $iface -j DROP"
        }
    }

    /**
     * Flushes and clears all NetBlocker iptables chains.
     */
    fun buildFlushScript(): String {
        return """
            iptables -F $CHAIN_NAME 2>/dev/null || true
            ip6tables -F $CHAIN_NAME 2>/dev/null || true
        """.trimIndent()
    }
}
