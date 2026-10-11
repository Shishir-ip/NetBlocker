# NetBlocker 🛡️

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-brightgreen.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.2-blue.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-purple.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](LICENSE)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Room-orange.svg)](#architecture)
[![No-VPN](https://img.shields.io/badge/Mechanism-Native%20iptables%20%2F%20ADB-red.svg)](#native-blocking-mechanism)
[![Display](https://img.shields.io/badge/Refresh%20Rate-120Hz%20Fluid%20(120fps)-cyan.svg)](#performance--fluidity)

> **NetBlocker** is a battery-efficient, native per-application network firewall for Android. Unlike traditional VPN-based blockers that route entire device traffic through a virtual tunnel, NetBlocker uses **kernel-level `iptables` rules** and **Android OS native `netpolicy`/`appops` ADB shell controls**. It allows you to block **Wi-Fi**, **Mobile Data**, and **LAN / Ethernet** access individually for each app without running persistent background services.

---

## 🌟 Key Features

- **🚫 True Native Blocking (Zero VPN Overhead)**:
  - No continuous VPN notification icon.
  - No battery drain from packet inspection loops.
  - Can be used simultaneously with your favorite VPN (WireGuard, OpenVPN, Cloudflare 1.1.1.1, etc.).
- **🎛️ Granular Interface Switches**:
  - 📶 **Wi-Fi Blocking** (`wlan+`, `ap+` interfaces).
  - 📱 **Mobile Cellular Data Blocking** (`rmnet+`, `ccmni+`, `pdp+` interfaces).
  - 🔌 **LAN / Ethernet Blocking** (`eth+`, `usb+` interfaces).
  - 🛑 **Master Switch** to toggle all outbound network traffic for an app in real time.
- **⚡ Two Powerful Execution Modes**:
  - **Root Mode (`su`)**: Direct on-device execution of IPv4 & IPv6 firewall rules via atomic custom chains (`NETBLOCKER_OUT`).
  - **ADB Shell Mode (Non-Root)**: Uses native Android `cmd netpolicy` and `cmd appops` controls via PC, Wireless ADB, or Shizuku.
- **📋 1-Click ADB Script Generator**:
  - Generates ready-to-run `.sh` (Linux/macOS/Termux/Shizuku) and `.bat` (Windows) scripts.
  - Single-app command copy for terminal pasting.
- **⚡ 120fps Display Optimization**:
  - Dynamic display mode negotiation prioritizing **120Hz**, **90Hz**, and **60Hz** panels.
  - Zero-jank Jetpack Compose architecture with stable list keys and an in-memory LRU icon cache.
- **🔄 Auto-Restore on Boot**:
  - Optional `BootReceiver` restores active firewall rules immediately after device reboot.
- **🔍 Built-in Diagnostics & Audit**:
  - Live command execution logs with millisecond timestamps and exit codes.
  - Real-time DNS socket ping tester to verify network drops.

---

## 🏗️ Native Blocking Mechanism

NetBlocker avoids the overhead of `VpnService` by applying rules directly at the operating system level:

### 1. Root / Kernel iptables Rules (Root Mode)
NetBlocker creates and manages an isolated subchain `NETBLOCKER_OUT` linked to Android's `OUTPUT` chain:
```bash
# Initialize isolated chains (IPv4 & IPv6)
iptables -N NETBLOCKER_OUT 2>/dev/null || true
iptables -C OUTPUT -j NETBLOCKER_OUT 2>/dev/null || iptables -I OUTPUT 1 -j NETBLOCKER_OUT

# Block specific UID on Wi-Fi
iptables -A NETBLOCKER_OUT -m owner --uid-owner <UID> -o wlan+ -j DROP

# Block specific UID on Mobile Data
iptables -A NETBLOCKER_OUT -m owner --uid-owner <UID> -o rmnet+ -j DROP

# Block specific UID on Ethernet / LAN
iptables -A NETBLOCKER_OUT -m owner --uid-owner <UID> -o eth+ -j DROP

# Block all network traffic for UID
iptables -A NETBLOCKER_OUT -m owner --uid-owner <UID> -j DROP
```

### 2. Android Network Policy Controls (ADB Shell Non-Root Mode)
For devices without root, NetBlocker uses Android's native framework commands accessible via ADB:
```bash
# Restrict background network rules for UID
adb shell cmd netpolicy set restrict-background-rules <UID> 1

# Exclude UID from metered networks (mobile data / tethering)
adb shell cmd netpolicy set metered-network-allowlist <UID> false

# Restrict app background execution ops
adb shell cmd appops set <PACKAGE_NAME> RUN_IN_BACKGROUND ignore
adb shell cmd appops set <PACKAGE_NAME> RUN_ANY_IN_BACKGROUND ignore
```

---

## 🚀 Setup & Usage Guide

### Method A: Root Access (Magisk / KernelSU / APatch)
1. Install and open **NetBlocker**.
2. When prompted, grant Superuser access to NetBlocker.
3. Toggle the switch for any application you want to block.
4. Customize Wi-Fi, Mobile Data, or LAN independently.
5. Tap **Sync Rules** — rules take effect immediately.

### Method B: Non-Root via PC ADB
1. Enable **Developer Options** and **USB Debugging** on your Android device (`Settings > About Phone > Tap 'Build Number' 7 times`).
2. Connect your device to your computer via USB.
3. Verify connection:
   ```bash
   adb devices
   ```
4. In NetBlocker, select apps you wish to block, tap the **ADB Script** action pill, and tap **Copy Full Script**.
5. Paste the commands into your terminal or run the generated script:
   ```bash
   adb shell < pasted_commands
   ```

### Method C: Non-Root On-Device (Wireless ADB / Shizuku / LADB)
1. Turn on **Wireless Debugging** in Developer Options.
2. Pair with **LADB**, **Termux**, or **Shizuku**.
3. Paste the generated NetBlocker commands into the terminal directly on your phone.

---

## 🏛️ Project Architecture

```
com.example/
├── NetBlockerApp.kt                # Application singleton & dependency wiring
├── MainActivity.kt                 # 120Hz display refresh optimizer & edge-to-edge host
├── data/
│   ├── model/
│   │   ├── AppItem.kt              # App UI presentation model with icon & toggle states
│   │   ├── BlockedRule.kt          # Room entity for persisted firewall rules
│   │   ├── CommandLog.kt           # Room entity for executed command audit trails
│   │   └── FirewallMode.kt         # Enum (AUTO, ROOT, ADB_SHELL, SIMULATION)
│   ├── db/
│   │   ├── NetBlockerDatabase.kt   # Room database definition
│   │   ├── BlockedRuleDao.kt       # Rule CRUD & bulk queries
│   │   └── CommandLogDao.kt        # Command audit logging DAO
│   └── repository/
│       ├── AppRepository.kt        # PackageManager integration & LRU icon cache
│       └── FirewallRepository.kt   # Command execution orchestrator & ping diagnostics
├── engine/
│   └── NativeFirewallEngine.kt     # iptables, ip6tables, and ADB script generator & runner
├── receiver/
│   └── BootReceiver.kt             # Auto-reapplies firewall rules on device boot
└── ui/
    ├── components/
    │   ├── AppRowItem.kt           # Fluid, responsive app row with granular chips
    │   ├── FirewallHeader.kt       # Real-time firewall status, counters & quick pills
    │   ├── AppDetailsBottomSheet.kt# Detailed network inspector & per-app ADB command box
    │   ├── AdbScriptDialog.kt      # Script previewer (.sh & .bat) with copy support
    │   ├── CommandLogsDialog.kt    # System command execution log viewer
    │   └── ModeSelectorDialog.kt   # Firewall engine mode picker
    ├── screens/
    │   └── MainScreen.kt           # Central screen with search, filters, and list
    ├── theme/
    │   ├── Color.kt                # Cyberpunk-inspired minimalist palette
    │   ├── Theme.kt                # Dynamic / M3 color scheme definitions
    │   └── Type.kt                 # Typography definitions
    └── viewmodel/
        └── NetBlockerViewModel.kt  # MVVM StateFlow manager
```

---

## 🛠️ Technical Dependencies

- **Android SDK**: `minSdk 24` (Android 7.0), `compileSdk 36`, `targetSdk 36`
- **Kotlin**: `2.2.10`
- **Jetpack Compose**: Compose BOM `2024.09.00`
- **Material Design 3**: `androidx.compose.material3`
- **Local Persistence**: AndroidX Room `2.7.0` (with KSP code generation)
- **Concurrency**: Kotlin Coroutines & StateFlow `1.10.2`
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

We welcome contributions! To contribute:

1. **Fork** the repository.
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Commit your changes:
   ```bash
   git commit -m "feat: add support for nftables syntax"
   ```
4. Push to your branch:
   ```bash
   git push origin feature/your-feature-name
   ```
5. Open a **Pull Request**.

Please ensure your code builds without warnings via `gradle compileDebugKotlin`.

---

## 📄 License

Licensed under the **Apache License, Version 2.0**. See [LICENSE](LICENSE) for details.
