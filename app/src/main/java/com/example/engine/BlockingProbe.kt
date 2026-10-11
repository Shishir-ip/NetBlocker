package com.example.engine

import android.os.Build
import com.example.executor.PrivilegedExecutor
import com.example.executor.RootExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class CapabilityLevel(val displayName: String, val description: String) {
    FULL("Full", "Complete network access cut-off for the application"),
    PARTIAL("Partial", "Background data restricted only (metered / mobile)"),
    UNSUPPORTED("Unsupported", "Firewall commands not supported on this Android version")
}

sealed class BlockingStrategy {
    data object RootIptables : BlockingStrategy()
    data class PackageNetworking(val subcmd: String = "set-package-networking-enabled") : BlockingStrategy()
    data class UidFirewallRule(val chain: String) : BlockingStrategy()
    data object BackgroundBlacklist : BlockingStrategy()
    data object Unsupported : BlockingStrategy()
}

data class ProbeResult(
    val strategy: BlockingStrategy,
    val capabilityLevel: CapabilityLevel,
    val description: String,
    val rawHelpOutput: String = ""
)

object BlockingProbe {

    suspend fun probe(executor: PrivilegedExecutor): ProbeResult = withContext(Dispatchers.IO) {
        if (executor is RootExecutor) {
            return@withContext ProbeResult(
                strategy = BlockingStrategy.RootIptables,
                capabilityLevel = CapabilityLevel.FULL,
                description = "Root iptables kernel firewall (Full per-interface & master control)"
            )
        }

        // 1. Probe cmd connectivity help
        val connectivityHelpRes = executor.exec("cmd connectivity help")
        val connHelp = connectivityHelpRes.output

        if (connHelp.contains("set-package-networking-enabled")) {
            return@withContext ProbeResult(
                strategy = BlockingStrategy.PackageNetworking(),
                capabilityLevel = CapabilityLevel.FULL,
                description = "cmd connectivity set-package-networking-enabled (Native full block)",
                rawHelpOutput = connHelp
            )
        }

        if (connHelp.contains("set-uid-firewall-rule")) {
            // Determine chain supported by the platform
            val chain = when {
                connHelp.contains("restricted", ignoreCase = true) -> "restricted"
                connHelp.contains("standby", ignoreCase = true) -> "standby"
                connHelp.contains("powersave", ignoreCase = true) -> "powersave"
                connHelp.contains("doze", ignoreCase = true) -> "doze"
                connHelp.contains("none", ignoreCase = true) -> "none"
                else -> "restricted"
            }
            return@withContext ProbeResult(
                strategy = BlockingStrategy.UidFirewallRule(chain),
                capabilityLevel = CapabilityLevel.FULL,
                description = "cmd connectivity set-uid-firewall-rule $chain (Native firewall chain)",
                rawHelpOutput = connHelp
            )
        }

        // 2. Fallback to cmd netpolicy help
        val netpolicyHelpRes = executor.exec("cmd netpolicy help")
        val netpolicyHelp = netpolicyHelpRes.output

        if (netpolicyHelp.contains("restrict-background-blacklist") ||
            netpolicyHelp.contains("add restrict-background-blacklist")
        ) {
            return@withContext ProbeResult(
                strategy = BlockingStrategy.BackgroundBlacklist,
                capabilityLevel = CapabilityLevel.PARTIAL,
                description = "cmd netpolicy restrict-background-blacklist (Partial: background data only)",
                rawHelpOutput = netpolicyHelp
            )
        }

        // 3. Fallback check: On Android 13+ (API >= 33), set-package-networking-enabled is standard
        if (Build.VERSION.SDK_INT >= 33) {
            val testProbe = executor.exec("cmd connectivity get-package-networking-enabled android")
            if (testProbe.exitCode == 0 || !testProbe.output.contains("Unknown command")) {
                return@withContext ProbeResult(
                    strategy = BlockingStrategy.PackageNetworking(),
                    capabilityLevel = CapabilityLevel.FULL,
                    description = "cmd connectivity set-package-networking-enabled (Android 13+ standard)"
                )
            }
        }

        return@withContext ProbeResult(
            strategy = BlockingStrategy.Unsupported,
            capabilityLevel = CapabilityLevel.UNSUPPORTED,
            description = "Not supported on this Android version (${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
        )
    }
}
