# 🛵 S1Mods - Open-Source Infotainment Utility for MoveOS

[![Target OS](https://img.shields.io/badge/MoveOS-2.x%20|%203.x%20|%204.x%20|%205.x-green.svg)](#supported-vehicles--compatibility-matrix)
[![Head Unit OS](https://img.shields.io/badge/Head%20Unit-Android%2010%20%7C%2013-blue.svg)](#technical-specifications)
[![Stack](https://img.shields.io/badge/Stack-Kotlin%20Compose%20+%20Python%203%20+%20ADB-brightgreen.svg)](#key-features)
[![Protocol](https://img.shields.io/badge/Protocol-Root%20gRPC%20%28Port%2050005%29-orange.svg)](#technical-specifications)
[![Safety](https://img.shields.io/badge/Safety-100%25%20In--Memory%20RAM%20Mount-blueviolet.svg)](#safety-reversibility--zero-flash-modification)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An automated, one-click open-source setup utility and standalone Android application to unlock wireless ADB, configure **Floating Touch Navigation (Back, Home, Settings)** and **Music Player** on MoveOS-powered infotainment units.

Zero soldering, zero flash partition writes, and 100% reversible with a single vehicle restart.

---

## ⚡ Quick Start: Android App (Recommended - No PC Needed)

1. Turn on **Personal Hotspot** on your Android phone.
2. On your scooter display, go to **Settings > Wi-Fi** and connect to your phone's hotspot.
3. Install and open [`S1Mods-v1.0.0.apk`](S1Mods-v1.0.0.apk) on your phone.
4. Tap **Scan** *(allow Location permission when prompted — Android requires this to scan local devices)*. If auto-scan is blocked by your phone's security, copy the scooter's **MAC or IP** from **Hotspot > Connected Devices** and paste it via the **Edit (✏️)** button.
5. Ensure the scooter's **side stand is UP** and screen is awake.
6. Tap **1-CLICK AUTOMATED SETUP** — within seconds, navigation controls and music mod are installed!

---

## 💻 Alternative Quick Start: Windows PC

1. **Same Wi-Fi Network:** Connect PC and Scooter to the same Wi-Fi or phone hotspot.
2. **Scooter Awake:** Side stand UP, screen active.
3. Run **`setup.bat`** in the repository root.

---

## 🎥 Video Demonstration (Ola S1 Pro Gen 2)

Watch the modded infotainment interface and floating touch navigation running live on an **Ola S1 Pro (Gen 2)** with MoveOS:

[▶️ **Click here to watch / download the Demonstration Video (`demo.mp4`)**](demo.mp4)

<video src="demo.mp4" controls="controls" width="100%" style="max-height: 520px;">
  <p>Your browser does not support the video tag. You can <a href="demo.mp4">download or view the video directly here</a>.</p>
</video>

> [!NOTE]
> **Notice on Siri / iOS Voice Unlock in the Video:**
> The Siri Shortcuts / iOS voice unlocking feature demonstrated in the video is **private** and not part of this public release, as it relies on personal account credentials and private session keys. You can unlock your vehicle normally using the official companion app, the touchscreen passkey on the dashboard, or through standard wireless ADB / gRPC commands.

---

## ✨ Key Features

* 📲 **All-in-One Android App (`S1Mods`):** Complete standalone Android application to auto-detect scooter over phone hotspot, unlock ADB, install bundled APKs, capture screenshots, open settings, and run root shell directly from your phone — **zero PC required!** Located in [`android/`](android/) and pre-compiled as [`S1Mods-v1.0.0.apk`](S1Mods-v1.0.0.apk).
* 🔍 **Zero-Config Auto-Discovery & Hotspot Helper:** Automatically scans IPv6 link-local multicast and local hotspot subnets. Includes an instant MAC-to-IPv6 converter using SLAAC EUI-64 standards.
* 🛡️ **Zero Power-Manager Tampering (Safe Auto-Lock):** Does **NOT** alter the system power manager or force persistent stay-on. Your scooter's built-in auto-lock and anti-theft security remain 100% untouched.
* 🔓 **Safe & Reversible In-Memory ADB Unlock:** Authorizes ADB over TCP port 5555 using a temporary RAM bind-mount (`mount --bind`). No system, vendor, or boot partitions are ever modified.
* 📱 **Floating Touch Navigation Controls:** Installs **Balls** to provide essential touch buttons (Back, Home, Settings) directly on the infotainment display.
* 🎵 **Offline & Streaming Music Player:** Installs **Music Mod**, giving you ad-free music playback directly from the dash.
* 📦 **Pre-Bundled Local APKs:** Modded application packages are pre-bundled in `apks/` (`balls-1.0.0-lite-release.apk` and `musicmod-release.apk`) for instant, offline installation with zero external dependencies.
* 🛠️ **Automated Permission Grants:** Grants `SYSTEM_ALERT_WINDOW` (overlay), `WRITE_SETTINGS`, and Android Accessibility Services via ADB without manual prompt tapping.
* 🎯 **1-Click Execution:** Run via Android App or double-click `setup.bat` on PC.
* 🐍 **Direct Root Controller (`ola.py`):** Includes a standalone, weakly-typed gRPC root control script to inspect vehicle properties, open settings, take screenshots, or execute root shell commands.

---

## 📱 Supported Vehicles & Compatibility Matrix

> [!NOTE]
> **Display Requirement:** S1Mods requires an **Android-based 7" touchscreen head unit** with Wi-Fi connectivity. Only Ola models equipped with the 7-inch Android infotainment display are compatible.

### ✅ Supported Vehicles (7" Android Touchscreen Models)

| Vehicle Model | Display Screen | Head Unit SoC | MoveOS Version | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Ola S1 Pro (Gen 2)** | 7.0" TFT Touchscreen (800x480 / 720p) | Qualcomm Snapdragon 625 (`msm8953`, 3GB RAM) | MoveOS 3, 4, 5 | ✅ **100% Tested & Verified** |
| **Ola S1 Pro (Gen 1)** | 7.0" TFT 720p Touchscreen (1280x768) | Qualcomm Snapdragon 212 (`APQ8009`, 3GB RAM) | MoveOS 2, 3, 4, 5 | 🟢 **Supported (Compatible)** |
| **Ola S1 (Gen 1)** | 7.0" TFT 720p Touchscreen (1280x768) | Qualcomm Snapdragon 212 (`APQ8009`, 3GB RAM) | MoveOS 2, 3, 4, 5 | 🟢 **Supported (Compatible)** |
| **Ola S1 Air** | 7.0" TFT Touchscreen (800x480) | Octa-Core Processor (3GB RAM) | MoveOS 3, 4, 5 | 🟢 **Supported (Compatible)** |

### ❌ Incompatible Vehicles (Segmented LCD / Non-Android Models)

The budget **Ola S1 X** series uses a conventional non-touch digital instrument cluster running on a basic microcontroller (MCU/RTOS) without an Android head unit, Wi-Fi hardware, or ADB capability:

| Incompatible Model | Display Hardware | Reason for Incompatibility |
| :--- | :--- | :--- |
| **Ola S1 X (2 kWh / 3 kWh / 4 kWh)** | 4.3" / 3.5" Segmented LCD (Non-Touch) | Microcontroller-based instrument cluster. No Android OS, no Wi-Fi, no APK support. |
| **Ola S1 X+** | 5.0" Segmented LCD (Non-Touch) | Non-touch monochrome LCD panel operated via handlebar buttons. No Android head unit. |

---

## 📋 Technical Specifications

> Verified live on **Ola S1 Pro (Gen 2)** running MoveOS on Android 10 (Snapdragon 625).

| Component / Layer | Specification | Technical Details |
| :--- | :--- | :--- |
| **Target Vehicle** | Ola S1 Pro (Gen 2) | 7" Touchscreen Infotainment Unit (A/B Partitioning) |
| **Head Unit OS** | Android 10 (API Level 29) | AOSP Build `QKQ1.191008.001` (MoveOS) |
| **Processor (SoC)** | Qualcomm Snapdragon 625 (`msm8953`) | Octa-Core ARM Cortex-A53 @ 2.0 GHz (64-bit) |
| **Primary ABI** | `arm64-v8a` (64-bit) | Native 64-bit userland (compatible with `armeabi-v7a`) |
| **Graphics (GPU)** | Qualcomm Adreno 506 | Hardware acceleration for native Kanzi HMI dashboard |
| **Diagnostics Service** | Diagnostics gRPC Daemon | Weakly-typed root (`uid 0`) execution on TCP port `50005` |
| **Network Interface** | IPv6 Link-Local (`fe80::/64`) | Discovered via Wi-Fi `ff02::1` multicast ICMPv6 probing / Hotspot |
| **ADB Auth Target** | `/data/misc/adb/adbkeymoveos3_pub_prod` | In-memory bind-mounted from `/data/local/tmp/adb_keys_mine` |
| **Subsystem Safety** | HMI Infotainment Layer Only | Powertrain, BMS, and MCU remain physically & logically isolated |

---

## 🛠️ Direct Head Unit Control (`ola.py`)

The included `ola.py` script communicates directly with the scooter's root gRPC service on port 50005 without requiring pairing or ADB:

```bash
# Display scooter hardware & firmware details
python ola.py info --ipv6 fe80::1234:5678:9abc:def0%22

# Launch native Android Settings menu (Wi-Fi, Bluetooth, Apps, Display, etc.)
python ola.py settings --ipv6 fe80::1234:5678:9abc:def0%22

# Open specific settings panel
python ola.py wifi --ipv6 fe80::1234:5678:9abc:def0%22
python ola.py bluetooth --ipv6 fe80::1234:5678:9abc:def0%22

# Capture head-unit screenshot to PC
python ola.py screenshot dash.png --ipv6 fe80::1234:5678:9abc:def0%22

# Execute arbitrary root shell commands
python ola.py shell "ls -la /data/local/tmp" --ipv6 fe80::1234:5678:9abc:def0%22

# Interactive root shell session
python ola.py interactive --ipv6 fe80::1234:5678:9abc:def0%22
```

---

## 🔒 Safety, Reversibility & Zero Flash Modification

* **Zero Partition Writes:** No system, vendor, boot, or recovery partitions are touched or flashed.
* **In-Memory RAM Bind Mount:** The ADB key authorization uses `mount --bind` pointing to `/data/local/tmp`. It exists only in temporary RAM.
* **Instant Reversibility:** A standard vehicle restart immediately unmounts the temporary bind and returns the vehicle to its factory state.
* **Subsystem Isolation:** The tool communicates exclusively with the Android infotainment unit (HMI). It has no access to vehicle powertrain, throttle controls, BMS, or motor controllers.

---

## 📁 Repository Structure

```
ola-s1-mods/
├── apks/                  # Pre-bundled mod APK packages
│   ├── balls-1.0.0-lite-release.apk
│   └── musicmod-release.apk
├── android/               # S1Mods Standalone Android Application
├── .gitignore             # Standard Git ignore rules
├── LICENSE                # MIT License with research & safety disclaimer
├── ola.py                 # Pure Python gRPC root communication engine
├── README.md              # Project documentation & user guide
├── requirements.txt       # Python dependencies (grpcio)
├── setup.bat              # 1-Click Windows launcher
└── setup.py               # Automated discovery, unlock, and installation engine
```

---

## 👨‍💻 Author & Contact

* **Anand ([@Asjohn720](https://github.com/Asjohn720))** — Automated discovery engine, in-memory ADB unlock workflow, Android app architecture, and project packaging.
  - 🌐 **GitHub:** [@Asjohn720](https://github.com/Asjohn720)
  - 🐦 **X (Twitter):** [@asjohn720](https://x.com/asjohn720)
  - 📸 **Instagram:** [@asjohn720](https://www.instagram.com/asjohn720)

### 🤝 Acknowledgments & Credits

* **Dev Vikram Singh ([@itsmobsys](https://github.com/itsmobsys))** — Core reverse-engineering research on the Ola S1 `wifican` diagnostics gRPC protocol and the original implementation of `ola.py` in the [`ola-s1-rea`](https://github.com/itsmobsys/ola-s1-rea) project.
* **Chayan Mistry** — [Quick Ball](https://github.com/chayanforyou/QuickBall) lightweight floating touch navigation overlay for Android.
* **Lelonio** — [Square Music](https://github.com/Lelonio/Square) lightweight, ad-free music player.
* The open-source Android and automotive reverse engineering community.

---

## ⚖️ Legal Disclaimer & Fair Use Notice

> [!IMPORTANT]
> **LEGAL DISCLAIMER & TRADEMARK NOTICE:**
> This repository and application are an independent, non-commercial, open-source educational utility developed by independent software enthusiasts. This project is **NOT** affiliated, associated, authorized, endorsed by, or in any way officially connected with **Ola Electric Mobility Ltd.**, **ANI Technologies Pvt. Ltd.**, or any of their subsidiaries or affiliates.
> 
> The names *"Ola"*, *"Ola Electric"*, *"MoveOS"*, *"S1"*, *"S1 Pro"*, and all related product names, trademarks, service marks, and emblems are registered trademarks of their respective owners. Any reference to these trademarks within this repository, documentation, or application is strictly for descriptive and nominative fair use (in accordance with Section 30 of the Indian Trade Marks Act, 1999) to indicate software/hardware compatibility.
> 
> The developers assume no liability for the use or misuse of this software. Use at your own discretion.

* **Lawful Interoperability:** Research and development are carried out under lawful interoperability doctrines, specifically **Section 52(1)(ab) of the Indian Copyright Act, 1957**, which explicitly permits the reverse engineering and adaptation of computer programs to achieve interoperability with other programs or devices, and international Fair Use principles.
* **Safety & Powertrain Non-Interference:** At no point does this software modify, reflash, or compromise safety-critical vehicle components such as the Motor Controller Unit (MCU), Battery Management System (BMS), accelerator/throttle mapping, or braking subsystems. All interactions are confined strictly to the userland Android infotainment head unit.
* **No Cloud or Backend Interference:** No remote servers, telematics infrastructure, cellular eSIM services, or subscription backends are accessed, compromised, or disrupted.

---

## 📄 License

This software is released under the [MIT License](LICENSE). All product names, logos, and brands are property of their respective owners. All company, product, and service names used in this software are for identification and compatibility purposes only.
