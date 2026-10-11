# NetBlocker 🛡️

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-brightgreen.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.2-blue.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-purple.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](LICENSE)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Room-orange.svg)](#architecture)
[![No-VPN](https://img.shields.io/badge/Mechanism-Native%20iptables%20%2F%20Connectivity-red.svg)](#native-blocking-mechanism)
[![Display](https://img.shields.io/badge/Refresh%20Rate-120Hz%20Fluid%20(120fps)-cyan.svg)](#performance--fluidity)

> **NetBlocker** is a battery-efficient, native per-application network firewall for Android. Unlike traditional VPN-based blockers that route entire device traffic through a virtual tunnel, NetBlocker uses **direct privileged executors** (Shizuku, One-Time ADB Helper Server, or Root) to invoke **real native Android OS network controls** (`cmd connectivity set-package-networking-enabled`, `set-uid-firewall-rule`, or kernel `iptables`).

---

## 🌟 Key Features

- **🚫 True Native Blocking (Zero VPN Overhead)**:
  - No continuous VPN notification icon.
  - 0% idle battery drain (no background packet capture loop).
  - Fully compatible with WireGuard, OpenVPN, Cloudflare 1.1.1.1, or corporate VPNs.
- **⚡ Three On-Device Privileged Executors**:
  - **Shizuku Mode (No PC Needed)**: Executes commands via Shizuku UserService (`IShizukuService` IPC) directly as UID 2000 (`shell`).
  - **One-Time ADB Helper Server**: Runs a lightweight `app_process` daemon on localhost with random token authentication. Stays alive until reboot; no PC needed after the initial command.
  - **Root Mode (`su`)**: Direct kernel-level iptables control (`NETBLOCKER_OUT` custom chain) supporting per-interface Wi-Fi, Mobile Data, and LAN switches.
- **🔍 Real OS-Level Rule Verification**:
  - Every toggle immediately queries the matching get/query subcommand from Android OS to confirm that the rule took effect.
  - Toggle states reflect real verified system state, not optimistic guesswork.
- **📱 Clear On-Device Setup Screen**:
  - Live status cards for Shizuku, One-Time ADB, and Root.
  - Automatic device capability probing (Full, Partial, or Unsupported).
- **⚡ 120fps Display Optimization**:
  - Dynamic display mode negotiation prioritizing **120Hz**, **90Hz**, and **60Hz** panels.
  - Zero-jank Jetpack Compose architecture with stable list keys and an in-memory LRU icon cache.
- **🔄 Auto-Restore on Boot**:
  - `BootReceiver` restores active firewall rules on reboot if an executor is available, or posts an actionable notification.

---

## 🏗️ Native Blocking Mechanism

NetBlocker probes the device at startup using `cmd connectivity help` and selects the best supported native mechanism:

### 1. Preferred Native Control (Android 13+ / AOSP)
```bash
# Block application network access
cmd connectivity set-package-networking-enabled <PACKAGE_NAME> false

# Restore application network access
cmd connectivity set-package-networking-enabled <PACKAGE_NAME> true

# Read back verified state
cmd connectivity get-package-networking-enabled <PACKAGE_NAME>
```

### 2. UID Firewall Rule Chain (Android 11–12)
```bash
# Deny network access for UID
cmd connectivity set-uid-firewall-rule restricted <UID> deny

# Restore network access for UID
cmd connectivity set-uid-firewall-rule restricted <UID> allow

# Read back verified state
cmd connectivity get-uid-firewall-rule restricted <UID>
```

### 3. Background Data Blacklist (Legacy Fallback)
```bash
# Restrict background metered data
cmd netpolicy add restrict-background-blacklist <UID>

# Remove restriction
cmd netpolicy remove restrict-background-blacklist <UID>
```

### 4. Root Mode with Kernel iptables
For rooted devices, NetBlocker creates an isolated custom chain `NETBLOCKER_OUT` enabling independent interface toggles:
```bash
# Initialize isolated chains
iptables -N NETBLOCKER_OUT 2>/dev/null || true
iptables -C OUTPUT -j NETBLOCKER_OUT 2>/dev/null || iptables -I OUTPUT 1 -j NETBLOCKER_OUT

# Block specific UID on Wi-Fi (wlan+)
iptables -A NETBLOCKER_OUT -m owner --uid-owner <UID> -o wlan+ -j DROP

# Block specific UID on Mobile Data (rmnet+)
iptables -A NETBLOCKER_OUT -m owner --uid-owner <UID> -o rmnet+ -j DROP

# Block specific UID on Ethernet / LAN (eth+)
iptables -A NETBLOCKER_OUT -m owner --uid-owner <UID> -o eth+ -j DROP

# Verify rule existence
iptables -C NETBLOCKER_OUT -m owner --uid-owner <UID> -j DROP
```

---

## 🚀 Setup Guide

### Method A: Shizuku (Recommended, No PC Required)
1. Install [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) from Google Play.
2. Start Shizuku via **Wireless Debugging** in Developer Options.
3. Open NetBlocker and tap **Setup Wizard**.
4. Tap **Grant Shizuku Permission**.
5. Once granted, all per-app toggles work instantly on your device!

### Method B: One-Time PC ADB Helper
1. Enable **USB Debugging** on your phone.
2. Connect your phone to your computer.
3. Open NetBlocker > **Setup Wizard** > **I have a PC**.
4. Copy and execute the ONE command shown:
   ```bash
   adb shell sh /sdcard/Android/data/com.aistudio.netblocker.kfxqmw/files/start.sh
   ```
5. The helper process (`netblocker_server`) will run in the background as the shell user until device restart. Disconnect from PC and use the app freely!

### Method C: Root Access (Magisk / KernelSU / APatch)
1. Open NetBlocker.
2. Grant Superuser privileges when prompted.
3. Enjoy full firewall controls with granular Wi-Fi, Mobile Data, and LAN switches!

---

## 🏛️ Project Architecture

```
com.example/
├── NetBlockerApp.kt                # Application singleton & executor wiring
├── MainActivity.kt                 # 120Hz display refresh optimizer & edge-to-edge host
├── executor/
│   ├── PrivilegedExecutor.kt       # Common abstraction interface & ExecResult
│   ├── RootExecutor.kt             # su execution implementation
│   ├── ShizukuExecutor.kt          # Shizuku UserService IPC implementation
│   └── AdbHelperExecutor.kt        # Local socket client to app_process server
├── shizuku/
│   ├── IShizukuService.aidl        # AIDL contract for Shizuku privileged service
│   └── ShizukuUserService.kt       # Bound privileged service implementation
├── server/
│   └── AdbHelperServer.kt          # Standalone app_process local socket server
├── engine/
│   ├── BlockingProbe.kt            # Probes device cmd connectivity subcommands
│   └── NativeFirewallEngine.kt     # Command generators & verification state parsers
├── data/
│   ├── model/
│   │   ├── AppItem.kt              # App UI model with toggles & cached icons
│   │   ├── BlockedRule.kt          # Room entity for persisted firewall rules
│   │   ├── CommandLog.kt           # Room entity for execution audit trails
│   │   └── FirewallMode.kt         # Enum (AUTO, ROOT, SHIZUKU, ADB_HELPER, SIMULATION)
│   ├── db/
│   │   ├── NetBlockerDatabase.kt   # Room database
│   │   ├── BlockedRuleDao.kt       # Rule CRUD & persistence
│   │   └── CommandLogDao.kt        # Command audit logging DAO
│   └── repository/
│       ├── AppRepository.kt        # PackageManager integration & LRU icon cache
│       └── FirewallRepository.kt   # Executor resolution & verified toggle engine
├── receiver/
│   └── BootReceiver.kt             # Restores firewall rules or alerts on reboot
└── ui/
    ├── screens/
    │   ├── MainScreen.kt           # Main screen with status chips, search, and list
    │   └── SetupScreen.kt          # Setup wizard with 3 cards (Shizuku, PC, Root)
    └── components/
        ├── AppRowItem.kt           # Fluid, responsive app row
        ├── FirewallHeader.kt       # Status banner, counters & setup trigger
        └── AppDetailsBottomSheet.kt# In-depth network inspector & command copy
```

---

## 🛠️ Technical Dependencies

- **Android SDK**: `minSdk 24`, `compileSdk 36`, `targetSdk 36`
- **Kotlin**: `2.2.10`
- **Jetpack Compose**: Compose BOM `2024.09.00`
- **Material Design 3**: `androidx.compose.material3`
- **Shizuku API**: `dev.rikka.shizuku:api:13.1.5` & `dev.rikka.shizuku:provider:13.1.5`
- **Local Persistence**: AndroidX Room `2.7.0` (with KSP code generation)
- **Build System**: Gradle 9+ (Kotlin DSL `.gradle.kts`)

---

## 📦 Building from Source

```bash
# Clone repository
git clone https://github.com/yourusername/netblocker.git
cd netblocker

# Build debug APK
gradle assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

---

## 🤝 Contribution Guidelines

We welcome pull requests! Please follow standard Kotlin style guidelines and verify builds with `gradle compileDebugKotlin` before submitting.

---

## 📄 License

Licensed under the **Apache License, Version 2.0**. See [LICENSE](LICENSE) for details.
