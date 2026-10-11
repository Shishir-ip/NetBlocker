package com.example.engine

import android.content.Context
import com.example.data.model.AppItem
import com.example.data.model.BlockedRule

class NativeFirewallEngine(private val context: Context) {

    companion object {
        const val CHAIN_NAME = "NETBLOCKER_OUT"
    }

    /**
     * Builds the block command according to the active strategy.
     */
    fun buildBlockCommand(app: AppItem, strategy: BlockingStrategy): String {
        return when (strategy) {
            is BlockingStrategy.RootIptables -> {
                buildRootBlockCommand(app)
            }
            is BlockingStrategy.PackageNetworking -> {
                "cmd connectivity set-package-networking-enabled ${app.packageName} false"
            }
            is BlockingStrategy.UidFirewallRule -> {
                "cmd connectivity set-uid-firewall-rule ${strategy.chain} ${app.uid} deny"
            }
            is BlockingStrategy.BackgroundBlacklist -> {
                "cmd netpolicy add restrict-background-blacklist ${app.uid}"
            }
            is BlockingStrategy.Unsupported -> {
                "echo 'Unsupported firewall strategy on this device'"
            }
        }
    }

    /**
     * Builds the unblock command according to the active strategy.
     */
    fun buildUnblockCommand(app: AppItem, strategy: BlockingStrategy): String {
        return when (strategy) {
            is BlockingStrategy.RootIptables -> {
                buildRootUnblockCommand(app)
            }
            is BlockingStrategy.PackageNetworking -> {
                "cmd connectivity set-package-networking-enabled ${app.packageName} true"
            }
            is BlockingStrategy.UidFirewallRule -> {
                "cmd connectivity set-uid-firewall-rule ${strategy.chain} ${app.uid} allow"
            }
            is BlockingStrategy.BackgroundBlacklist -> {
                "cmd netpolicy remove restrict-background-blacklist ${app.uid}"
            }
            is BlockingStrategy.Unsupported -> {
                "echo 'Unsupported firewall strategy on this device'"
            }
        }
    }

    /**
     * Builds the query command to read the real system state back.
     */
    fun buildQueryCommand(app: AppItem, strategy: BlockingStrategy): String {
        return when (strategy) {
            is BlockingStrategy.RootIptables -> {
                // Returns exitCode 0 if the rule is present in iptables
                "iptables -C $CHAIN_NAME -m owner --uid-owner ${app.uid} -j DROP 2>/dev/null"
            }
            is BlockingStrategy.PackageNetworking -> {
                "cmd connectivity get-package-networking-enabled ${app.packageName}"
            }
            is BlockingStrategy.UidFirewallRule -> {
                "cmd connectivity get-uid-firewall-rule ${strategy.chain} ${app.uid}"
            }
            is BlockingStrategy.BackgroundBlacklist -> {
                "cmd netpolicy list restrict-background-blacklist"
            }
            is BlockingStrategy.Unsupported -> {
                "echo 'Unsupported'"
            }
        }
    }

    /**
     * Parses the command output to determine if the app is REALLY blocked at the OS level.
     */
    fun parseQueryState(
        app: AppItem,
        strategy: BlockingStrategy,
        exitCode: Int,
        output: String
    ): Boolean {
        return when (strategy) {
            is BlockingStrategy.RootIptables -> {
                // If exit code is 0, the rule is active
                exitCode == 0
            }
            is BlockingStrategy.PackageNetworking -> {
                // Output typically contains "false" or "disabled"
                output.contains("false", ignoreCase = true) ||
                output.contains("disabled", ignoreCase = true)
            }
            is BlockingStrategy.UidFirewallRule -> {
                output.contains("deny", ignoreCase = true) ||
                output.contains("denied", ignoreCase = true)
            }
            is BlockingStrategy.BackgroundBlacklist -> {
                output.contains(app.uid.toString())
            }
            is BlockingStrategy.Unsupported -> {
                false
            }
        }
    }

    private fun buildRootBlockCommand(app: AppItem): String {
        val sb = StringBuilder()
        sb.append("iptables -N $CHAIN_NAME 2>/dev/null || true; ")
        sb.append("iptables -C OUTPUT -j $CHAIN_NAME 2>/dev/null || iptables -I OUTPUT 1 -j $CHAIN_NAME; ")
        sb.append("ip6tables -N $CHAIN_NAME 2>/dev/null || true; ")
        sb.append("ip6tables -C OUTPUT -j $CHAIN_NAME 2>/dev/null || ip6tables -I OUTPUT 1 -j $CHAIN_NAME; ")

        // First remove old rules for this UID so we don't duplicate
        sb.append(buildRootUnblockCommand(app)).append("; ")

        val allBlocked = app.blockWifi && app.blockMobile && app.blockLan
        if (allBlocked) {
            sb.append("iptables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -j DROP; ")
            sb.append("ip6tables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -j DROP")
        } else {
            if (app.blockWifi) {
                sb.append("iptables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -o wlan+ -j DROP; ")
                sb.append("iptables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -o ap+ -j DROP; ")
                sb.append("ip6tables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -o wlan+ -j DROP; ")
            }
            if (app.blockMobile) {
                sb.append("iptables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -o rmnet+ -j DROP; ")
                sb.append("iptables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -o ccmni+ -j DROP; ")
                sb.append("ip6tables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -o rmnet+ -j DROP; ")
            }
            if (app.blockLan) {
                sb.append("iptables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -o eth+ -j DROP; ")
                sb.append("iptables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -o usb+ -j DROP; ")
                sb.append("ip6tables -A $CHAIN_NAME -m owner --uid-owner ${app.uid} -o eth+ -j DROP; ")
            }
        }
        return sb.toString().trimEnd(';', ' ')
    }

    private fun buildRootUnblockCommand(app: AppItem): String {
        return """
            iptables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -j DROP 2>/dev/null || true;
            iptables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -o wlan+ -j DROP 2>/dev/null || true;
            iptables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -o ap+ -j DROP 2>/dev/null || true;
            iptables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -o rmnet+ -j DROP 2>/dev/null || true;
            iptables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -o ccmni+ -j DROP 2>/dev/null || true;
            iptables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -o eth+ -j DROP 2>/dev/null || true;
            iptables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -o usb+ -j DROP 2>/dev/null || true;
            ip6tables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -j DROP 2>/dev/null || true;
            ip6tables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -o wlan+ -j DROP 2>/dev/null || true;
            ip6tables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -o rmnet+ -j DROP 2>/dev/null || true;
            ip6tables -D $CHAIN_NAME -m owner --uid-owner ${app.uid} -o eth+ -j DROP 2>/dev/null || true
        """.trimIndent().replace("\n", " ")
    }

    /**
     * Builds full script for all active rules using the given strategy.
     */
    fun buildFullScript(rules: List<BlockedRule>, strategy: BlockingStrategy): String {
        val active = rules.filter { it.isBlocked }
        val sb = StringBuilder()
        sb.appendLine("# ===========================================")
        sb.appendLine("# NetBlocker Active Rules Script")
        sb.appendLine("# Strategy: $strategy")
        sb.appendLine("# Target Apps: ${active.size}")
        sb.appendLine("# ===========================================")
        sb.appendLine()

        for (rule in active) {
            val app = AppItem(
                packageName = rule.packageName,
                appName = rule.appName,
                uid = rule.uid,
                isSystemApp = false,
                hasInternetPermission = true,
                isBlocked = rule.isBlocked,
                blockWifi = rule.blockWifi,
                blockMobile = rule.blockMobile,
                blockLan = rule.blockLan
            )
            sb.appendLine("# ${rule.appName} (${rule.packageName})")
            sb.appendLine(buildBlockCommand(app, strategy))
            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * Flush all rules.
     */
    fun buildFlushScript(strategy: BlockingStrategy): String {
        return when (strategy) {
            is BlockingStrategy.RootIptables -> {
                "iptables -F $CHAIN_NAME 2>/dev/null || true; ip6tables -F $CHAIN_NAME 2>/dev/null || true"
            }
            else -> {
                "# Unblock individually per app"
            }
        }
    }
}
