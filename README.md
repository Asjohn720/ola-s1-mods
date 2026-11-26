# 🛵 S1Mods - Open-Source Infotainment Utility for MoveOS

[![Target OS](https://img.shields.io/badge/MoveOS-≥%204.0.5%20(4.0.5%20%26%20Above)-green.svg)](#supported-vehicles--compatibility-matrix)
[![Head Unit OS](https://img.shields.io/badge/Head%20Unit-Android%2010%20%7C%2013-blue.svg)](#technical-specifications)
[![Stack](https://img.shields.io/badge/Stack-Kotlin%20Compose%20+%20ADB-brightgreen.svg)](#key-features)
[![Protocol](https://img.shields.io/badge/Protocol-Diagnostics%20gRPC%20%28Port%2050005%29-orange.svg)](#technical-specifications)
[![Safety](https://img.shields.io/badge/Safety-100%25%20In--Memory%20RAM%20Mount-blueviolet.svg)](#safety-reversibility--zero-flash-modification)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

An automated, one-click open-source standalone Android application to enable wireless ADB, configure **Floating Touch Navigation (Back, Home, Settings)** and **Music Player** on MoveOS-powered infotainment units running **MoveOS 4.0.5 and above**.

Zero soldering, zero flash partition writes, and 100% reversible with a single vehicle restart.

---

## ⚠️ Disclaimer & Scope of Modification

S1Mods is an independent, community-developed project and is not affiliated with, endorsed by, or sponsored by Ola Electric.

- **No Modification to Core Vehicle Systems:** S1Mods is designed to extend the scooter's infotainment experience. It does not modify the stock vehicle firmware, motor controller (MCU), battery management system (BMS), throttle/powertrain mappings, braking, or core vehicle control logic.
- **Independent Functionality:** All additional features provided by S1Mods operate through separately installed applications, services, and supported interfaces.
- **Stock Software Issues:** S1Mods is not responsible for bugs, malfunctions, or unexpected behaviour originating from Ola's original software, firmware, or hardware.
- **No Replacement for Official Features:** S1Mods does not replace, repair, or guarantee the functionality of features provided by Ola Electric.
- **User Responsibility:** Users are responsible for understanding the modifications they install and following the provided installation and usage instructions. Modifying your scooter may void its warranty.
- **Compatibility:** Features may behave differently across scooter generations and MoveOS versions. Compatibility and stability are not guaranteed on every configuration.
- **No Cloud or Telematics Access:** The software communicates exclusively over your local Wi-Fi or phone hotspot network. It does not access, intercept, or tamper with Ola cloud servers, telematics infrastructure, eSIM connectivity, user accounts, or private signing keys.

S1Mods is provided as-is, without warranties of any kind. Use it at your own discretion and risk.

All trademarks, product names, and brand references belong to their respective owners.

---

## ⚡ Quick Start: Android App (No PC Needed)

1. Turn on **Personal Hotspot** on your Android phone.
2. On your scooter display, go to **Settings > Wi-Fi** and connect to your phone's hotspot.
3. Install and open the **S1Mods** APK from **Releases** (or build from [`android/`](android/)) on your phone.
4. Tap **Scan** *(allow Location permission when prompted — Android requires this to scan local devices)*. If auto-scan is blocked by your phone's security, copy the scooter's **MAC or IP** from **Hotspot > Connected Devices** and paste it via the **Edit (✏️)** button.
5. Ensure the scooter's **side stand is UP** and screen is awake.
6. Tap **1-CLICK AUTOMATED SETUP** — within seconds, navigation controls and music mod are installed!

---

## 🎥 Video Demonstration (Ola S1 Pro Gen 2)

Watch the modded infotainment interface and floating touch navigation running live on an **Ola S1 Pro (Gen 2)** with MoveOS:

[▶️ **Click here to watch / download the Demonstration Video (`demo.mp4`)**](demo.mp4)

<video src="demo.mp4" controls="controls" width="100%" style="max-height: 520px;">
  <p>Your browser does not support the video tag. You can <a href="demo.mp4">download or view the video directly here</a>.</p>
</video>

> [!NOTE]
> **Notice on Voice Actions / Custom Shortcuts in the Video:**
> The voice action shortcut demonstrated in the video is **private** and not part of this public release, as it relies on personal account credentials and private session keys. You can access and wake your vehicle normally using the official companion app, the touchscreen passkey on the dashboard, or through standard local diagnostic interfaces.

---

## ✨ Key Features

* 📲 **All-in-One Android App (`S1Mods`):** Complete standalone Android application to auto-detect your scooter over a phone hotspot, authorize wireless ADB, install bundled mod APKs, capture screenshots, and open settings directly from your phone — **zero PC required!** Source located in [`android/`](android/).
* 🔍 **Zero-Config Auto-Discovery & Hotspot Helper:** Automatically scans IPv6 link-local multicast and local hotspot subnets. Includes an instant MAC-to-IPv6 converter using SLAAC EUI-64 standards.
* 🛡️ **Zero Power-Manager Tampering (Safe Auto-Lock):** Does **NOT** alter the system power manager or force persistent stay-on. Your scooter's built-in auto-lock and anti-theft security remain 100% untouched.
* 🛡️ **Safe & Reversible In-Memory ADB Authorization:** Authorizes ADB over TCP port 5555 using a temporary RAM bind-mount (`mount --bind`). No system, vendor, or boot partitions are ever modified.
* 📱 **Floating Touch Navigation Controls:** Installs **Balls** to provide essential touch buttons (Back, Home, Settings) directly on the infotainment display.
* 🎵 **Offline & Streaming Music Player:** Installs **Music Mod**, giving you ad-free music playback directly from the dash.
* 📦 **Pre-Bundled Local APKs:** Modded application packages are pre-bundled in `apks/` (`balls-1.0.0-lite-release.apk` and `musicmod-release.apk`) for instant, offline installation with zero external dependencies.
* 🛠️ **Automated Permission Grants:** Grants `SYSTEM_ALERT_WINDOW` (overlay), `WRITE_SETTINGS`, and Android Accessibility Services via ADB without manual prompt tapping.
* 🎯 **1-Click Execution:** Run directly from the S1Mods Android App.

---

## 📱 Supported Vehicles & Compatibility Matrix

> [!IMPORTANT]
> **MoveOS Firmware Requirement:** S1Mods is officially supported **only on MoveOS 4.0.5 and above** (including MoveOS 4.0.5+ and MoveOS 5.x). Older firmware versions (MoveOS 1.x, 2.x, 3.x, and MoveOS 4 builds below 4.0.5) are **not supported**.

> [!NOTE]
> **Display Requirement:** S1Mods requires an **Android-based 7" touchscreen head unit** with Wi-Fi connectivity. Only Ola models equipped with the 7-inch Android infotainment display are compatible.

### ✅ Supported Vehicles (7" Android Touchscreen Models)

| Vehicle Model | Display Screen | Head Unit SoC | MoveOS Version | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Ola S1 Pro (Gen 2)** | 7.0" TFT Touchscreen (800x480 / 720p) | Qualcomm Snapdragon 625 (`msm8953`, 3GB RAM) | MoveOS ≥ 4.0.5 (4.0.5+ / 5.x) | ✅ **100% Tested & Verified** |
| **Ola S1 Pro (Gen 1)** | 7.0" TFT 720p Touchscreen (1280x768) | Qualcomm Snapdragon 212 (`APQ8009`, 3GB RAM) | MoveOS ≥ 4.0.5 (4.0.5+ / 5.x) | 🟢 **Supported (Compatible)** |
| **Ola S1 (Gen 1)** | 7.0" TFT 720p Touchscreen (1280x768) | Qualcomm Snapdragon 212 (`APQ8009`, 3GB RAM) | MoveOS ≥ 4.0.5 (4.0.5+ / 5.x) | 🟢 **Supported (Compatible)** |
| **Ola S1 Air** | 7.0" TFT Touchscreen (800x480) | Octa-Core Processor (3GB RAM) | MoveOS ≥ 4.0.5 (4.0.5+ / 5.x) | 🟢 **Supported (Compatible)** |

### 🎛️ MoveOS Firmware Compatibility Breakdown

| MoveOS Release | Compatibility | Notes |
| :--- | :--- | :--- |
| **MoveOS 5.x** | ✅ **Fully Supported** | Verified with in-memory ADB authorization, Floating Touch Navigation (Balls), Music Mod, and projection. |
| **MoveOS 4.0.5+** (4.0.5, 4.1.x, etc.) | ✅ **Fully Supported** | Baseline supported firmware version for all S1Mods features. |
| **MoveOS 4.0.0 – 4.0.4** | ❌ **Not Supported** | Early MoveOS 4 builds lack required diagnostics/overlay stability. Update your scooter to MoveOS 4.0.5 or above. |
| **MoveOS 2.x & 3.x** | ❌ **Not Supported** | Legacy firmware interfaces and older partition layouts are incompatible. |

### ❌ Incompatible Vehicles (Segmented LCD / Non-Android Models)

The budget **Ola S1 X** series uses a conventional non-touch digital instrument cluster running on a basic microcontroller (MCU/RTOS) without an Android head unit, Wi-Fi hardware, or ADB capability:

| Incompatible Model | Display Hardware | Reason for Incompatibility |
| :--- | :--- | :--- |
| **Ola S1 X (2 kWh / 3 kWh / 4 kWh)** | 4.3" / 3.5" Segmented LCD (Non-Touch) | Microcontroller-based instrument cluster. No Android OS, no Wi-Fi, no APK support. |
| **Ola S1 X+** | 5.0" Segmented LCD (Non-Touch) | Non-touch monochrome LCD panel operated via handlebar buttons. No Android head unit. |

---

## 📋 Technical Specifications

> Verified live on **Ola S1 Pro (Gen 2)** running MoveOS (4.0.5+) on Android 10 (Snapdragon 625).

| Component / Layer | Specification | Technical Details |
| :--- | :--- | :--- |
| **Target Vehicle** | Ola S1 Pro (Gen 2) | 7" Touchscreen Infotainment Unit (A/B Partitioning) |
| **Head Unit OS** | Android 10 (API Level 29) | AOSP Build `QKQ1.191008.001` (MoveOS) |
| **Processor (SoC)** | Qualcomm Snapdragon 625 (`msm8953`) | Octa-Core ARM Cortex-A53 @ 2.0 GHz (64-bit) |
| **Primary ABI** | `arm64-v8a` (64-bit) | Native 64-bit userland (compatible with `armeabi-v7a`) |
| **Graphics (GPU)** | Qualcomm Adreno 506 | Hardware acceleration for native Kanzi HMI dashboard |
| **Diagnostics Service** | Diagnostics gRPC Daemon | Local diagnostics service on TCP port `50005` |
| **Network Interface** | IPv6 Link-Local (`fe80::/64`) | Discovered via Wi-Fi `ff02::1` multicast ICMPv6 probing / Hotspot |
| **ADB Auth Target** | `/data/misc/adb/adbkeymoveos3_pub_prod` | In-memory bind-mounted from `/data/local/tmp/adb_keys_mine` |
| **Subsystem Safety** | HMI Infotainment Layer Only | Powertrain, BMS, and MCU remain physically & logically isolated |

---

## 🔒 Safety, Reversibility & Zero Flash Modification

* **Zero Partition Writes:** No system, vendor, boot, or recovery partitions are touched or flashed.
* **In-Memory RAM Bind Mount:** The ADB key authorization uses `mount --bind` pointing to `/data/local/tmp`. It exists only in temporary RAM.
* **Instant Reversibility:** A standard vehicle restart immediately unmounts the temporary bind and returns the vehicle to its factory state.
* **Subsystem Isolation:** The tool communicates exclusively with the Android infotainment unit (HMI). It has no access to vehicle powertrain, throttle controls, BMS, or motor controllers.
* **Zero Persistent System Tampering:** No modifications persist across reboot unless explicitly installed as a standalone userland application by the user.

---

## 📁 Repository Structure

```
ola-s1-mods/
├── android/               # S1Mods Standalone Android Application
├── .gitignore             # Standard Git ignore rules
├── CHANGELOG.md           # Version release notes
├── LICENSE                # GNU GPLv3 License with research & safety disclaimer
└── README.md              # Project documentation & user guide
```

---

## 👨‍💻 Author & Contact

* **Anand ([@Asjohn720](https://github.com/Asjohn720))** — Network discovery, device connection workflow, Android app architecture, and project packaging.
  - 🌐 **GitHub:** [@Asjohn720](https://github.com/Asjohn720)
  - 🐦 **X (Twitter):** [@asjohn720](https://x.com/asjohn720)
  - 📸 **Instagram:** [@asjohn720.exe](https://www.instagram.com/asjohn720.exe)

### 🤝 Acknowledgments & Credits

* **Dev Vikram Singh ([@itsmobsys](https://github.com/itsmobsys))** — Core reverse-engineering research on the Ola S1 `wifican` diagnostics gRPC protocol and the `scootermode`/ADB-key authorization technique in the [`ola-s1-rea`](https://github.com/itsmobsys/ola-s1-rea) project.
* **Special thanks — Axiom ([pitzel/axiom](https://codeberg.org/pitzel/axiom), Apache-2.0)** 🤝 — for its open diagnostic, CAN telemetry and fault-detection research on Ola scooters.
* **Lelonio** — [Square](https://github.com/Lelonio/Square), the open-source music player the bundled Music Mod is a fork of.
* **shihabal3amri** — [DiPlay](https://github.com/shihabal3amri/DiPlay), the open-source projection app that OpenPlay is a fork of.
* The open-source Android and automotive reverse engineering community.

> Open-source bundled/downloadable apps (Music Mod, Android Auto, OpenPlay) are distributed under the license of their upstream projects, with their `LICENSE`/`NOTICE` preserved.
>
> **Important — SmartBall / Balls APK (`android/app/src/main/assets/balls-1.1.0-lite-release.apk`):** This APK is **closed-source** and is **not** covered by the GPLv3 license of this repository. Anyone using, forking, or redistributing this repository must use the bundled `balls-1.1.0-lite-release.apk` **strictly as-is, without any modification, patching, decompilation, or tampering**.
>
> *"CarPlay"* is a trademark of Apple Inc.; *"Android Auto"* of Google LLC — used only to describe compatibility, with no affiliation.

---

## ⚖️ Legal Disclaimer & Fair Use Notice

> [!IMPORTANT]
> **LEGAL DISCLAIMER & TRADEMARK NOTICE:**
> This repository and application are an independent, non-commercial, open-source educational utility developed by independent software enthusiasts. This project is **NOT** affiliated, associated, authorized, endorsed by, or in any way officially connected with **Ola Electric Mobility Ltd.**, **ANI Technologies Pvt. Ltd.**, or any of their subsidiaries or affiliates.
> 
> *"Ola"*, *"Ola Electric"* and *"MoveOS"* are trademarks of Ola Electric Mobility Ltd. *"S1"*, *"S1 Pro"*, *"S1 Air"*, *"S1 X"*, and other model names are product designations used only descriptively to indicate compatibility — no claim of trademark ownership is made over them by Ola or by this project. Any reference to these names within this repository, documentation, or application is strictly descriptive and nominative fair use (in accordance with Section 30 of the Indian Trade Marks Act, 1999).
> 
> *"CarPlay"* is a trademark of Apple Inc.; *"Android Auto"* is a trademark of Google LLC — referenced solely to describe projection compatibility, with no affiliation or endorsement.
> 
> The developers assume no liability for the use or misuse of this software. Modifying your scooter may void its warranty. Use at your own discretion and risk.

* **Lawful Interoperability:** Research and development are carried out under lawful interoperability doctrines, specifically **Section 52(1)(ab) of the Indian Copyright Act, 1957**, which explicitly permits the reverse engineering and adaptation of computer programs by a lawful owner to achieve interoperability with other programs or devices, and international Fair Use principles.
* **Safety & Powertrain Non-Interference:** At no point does this software modify, reflash, or compromise safety-critical vehicle components such as the Motor Controller Unit (MCU), Battery Management System (BMS), accelerator/throttle mapping, or braking subsystems. All interactions are confined strictly to the userland Android infotainment head unit.
* **No Cloud or Backend Interference:** No remote servers, telematics infrastructure, cellular eSIM services, or subscription backends are accessed, compromised, or disrupted. The app talks only to the scooter on the local network and does not use any Ola account, cloud credential, or private signing key. The app generates its own local ADB key on the connecting device.
* **Owner's Own Device:** All operations run against a device the user owns and physically controls, over its own local network, at the owner's initiative. Any vehicle data shown (VIN, registration, telemetry) is read from the owner's own scooter for their own personal diagnostic use.
* **Distribution & Sideloading:** This app is **not** published on the Google Play Store. It is released only as an open-source APK on GitHub for owners who choose to sideload it.
* **As-Is Provision:** S1Mods is provided "AS-IS", without warranties of any kind, express or implied.

---

## 📄 License

The S1Mods source code in this repository is released under the [GNU General Public License v3.0 (GPLv3)](LICENSE).

**Exception for Bundled SmartBall APK:** `android/app/src/main/assets/balls-1.1.0-lite-release.apk` is closed-source software and is excluded from the GPLv3 license. Any user or fork must use and keep this APK **strictly as-is without any modification**.

All product names, logos, and brands are property of their respective owners. All company, product, and service names used in this software are for identification and compatibility purposes only.
