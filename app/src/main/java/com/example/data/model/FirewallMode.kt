package com.example.data.model

enum class FirewallMode(val displayName: String, val description: String) {
    AUTO("Auto-Detect", "Prioritizes Root > Shizuku > ADB Helper"),
    ROOT("Root (su)", "Direct kernel-level iptables control via Superuser"),
    SHIZUKU("Shizuku", "On-device privileged shell execution via Shizuku service"),
    ADB_HELPER("ADB Helper", "Persistent local socket helper running via app_process"),
    SIMULATION("Test / Simulation", "Inspect commands without modifying system state")
}

enum class FilterType(val label: String) {
    ALL("All"),
    USER("User Apps"),
    SYSTEM("System"),
    BLOCKED("Blocked Only")
}
