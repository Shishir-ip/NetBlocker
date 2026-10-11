package com.example.data.model

enum class FirewallMode(val displayName: String, val description: String) {
    AUTO("Auto-Detect", "Automatically use Root if available, or generate ADB commands"),
    ROOT("Root (su)", "Direct execution of iptables rules via root access"),
    ADB_SHELL("ADB Shell / Shizuku", "Native Android cmd netpolicy & privileged adb shell commands"),
    SIMULATION("Test / Inspection Mode", "Preview and inspect generated network rules without executing")
}

enum class FilterType(val label: String) {
    ALL("All"),
    USER("User Apps"),
    SYSTEM("System"),
    BLOCKED("Blocked Only")
}
