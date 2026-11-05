# 🛵 Ola S1 Quick Mod & Navigation Suite

[![Target OS](https://img.shields.io/badge/MoveOS-2.x%20|%203.x%20|%204.x%20|%205.x-green.svg)](#supported-vehicles--compatibility-matrix)
[![Head Unit OS](https://img.shields.io/badge/Head%20Unit-Android%207.1.1-blue.svg)](#technical-specifications)
[![Stack](https://img.shields.io/badge/Stack-Python%203%20+%20ADB-brightgreen.svg)](#key-features)
[![Protocol](https://img.shields.io/badge/Protocol-Root%20gRPC%20(Port%2050005)-orange.svg)](#how-it-works)
[![Safety](https://img.shields.io/badge/Safety-100%25%20In--Memory%20RAM%20Mount-blueviolet.svg)](#safety-reversibility--zero-flash-modification)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An automated, one-click setup utility to unlock wireless ADB, bypass aggressive vehicle sleep timeouts, and configure **Floating Touch Navigation (Back, Home, Settings)** and **Ad-Free Music (Square)** on Ola S1 electric scooters.

Zero soldering, zero flash partition writes, and 100% reversible with a single scooter restart.

---

## ✨ Key Features

* 🔍 **Zero-Config Auto-Discovery:** Automatically scans IPv6 link-local multicast and discovers the scooter's head unit IP over your home Wi-Fi or hotspot.
* ⚡ **Anti-Sleep Mechanism (Diagnostic App):** Launches Ola's native **Diagnostic App (`com.ola.mfgapp`)** during setup, suppressing the Vehicle State Manager from locking the screen or turning off Wi-Fi.
* 🔓 **Safe & Reversible In-Memory ADB Unlock:** Authorizes ADB over TCP port 5555 using a temporary RAM bind-mount (`mount --bind`). No system, vendor, or boot partitions are ever modified.
* 📱 **Floating Touch Navigation Controls:** Downloads and deploys **Quick Ball** to provide essential touch buttons (Back, Home, Settings) directly on the infotainment display.
* 🎵 **Ad-Free Music Player:** Automatically fetches and installs **Square Music**, giving you offline and local music playback directly from the dash.
* 🛠️ **Automated Permission Grants:** Grants `SYSTEM_ALERT_WINDOW` (overlay), `WRITE_SETTINGS`, and Android Accessibility Services via ADB without manual prompt tapping.
* 🎯 **1-Click Execution:** Double-click `setup.bat` and let the utility handle discovery, waking, ADB unlocking, downloading, and installation automatically.
* 🐍 **Direct Root Controller (`ola.py`):** Includes a standalone, weakly-typed gRPC root control script to inspect vehicle properties, open settings, take screenshots, or execute root shell commands.

---

## 📱 Supported Vehicles & Compatibility Matrix

| Vehicle Model | Platform / Gen | Head Unit SoC | MoveOS Version | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Ola S1 Pro (Gen 1)** | 7" 720p Touch Display | Qualcomm Snapdragon 212 (APQ8009) | MoveOS 2, 3, 4, 5 | ✅ **100% Tested & Verified** |
| **Ola S1 (Gen 1)** | 7" 720p Touch Display | Qualcomm Snapdragon 212 (APQ8009) | MoveOS 2, 3, 4, 5 | ✅ **100% Tested & Verified** |
| **Ola S1 Pro (Gen 2)** | 7" Touch Display | Qualcomm Snapdragon Platform | MoveOS 3, 4, 5 | ✅ **Supported** |
| **Ola S1 Air** | 7" Touch Display | Qualcomm Snapdragon Platform | MoveOS 3, 4, 5 | ✅ **Supported** |
| **Ola S1 X+** | 5" / 7" Display | Qualcomm Snapdragon Platform | MoveOS 4, 5 | ✅ **Supported** (gRPC enabled) |

---

## 📋 Technical Specifications

| Component / Layer | Specification | Details |
| :--- | :--- | :--- |
| **Head Unit OS** | Android 7.1.1 (Nougat) | Customized AOSP build tailored for MoveOS |
| **SoC / Architecture** | Qualcomm APQ8009 / Snapdragon | Quad-Core ARM Cortex-A7 (32-bit `armeabi-v7a`) / ARM64 |
| **Communication Daemon** | `wifican` gRPC Service | Weakly-typed root execution service on TCP port `50005` |
| **Interface Addressing** | IPv6 Link-Local (`fe80::/64`) | Discovered via `ff02::1` multicast neighbor probing |
| **ADB Auth Target** | `/data/misc/adb/adbkeymoveos3_pub_prod` | Temporary bind-mounted from `/data/local/tmp/adb_keys_mine` |
| **Vehicle Subsystems** | Infotainment (HMI) Only | MCU, BMS, and motor controllers remain strictly isolated |

---

## ⚡ Quick Start Guide

### Prerequisites

Before starting, ensure:
1. **Same Wi-Fi Network:** Your Windows PC and Ola Scooter must be connected to the **same Wi-Fi network** (or PC connected to scooter hotspot / phone hotspot sharing connection).
2. **Scooter Awake:** The scooter screen must be active (press power button or tap display).
3. **Python 3:** Download and install from [python.org](https://www.python.org/downloads/) (tick **"Add Python to PATH"**).
4. **Android Platform Tools (ADB):** Download from [Google Platform Tools](https://developer.android.com/tools/releases/platform-tools) and ensure `adb.exe` is in your system PATH.

---

### Method 1: 1-Click Automated Setup (Recommended)

Simply double-click **`setup.bat`** in the repository root.

The script will automatically:
1. Locate your Android Platform Tools (`adb.exe`).
2. Verify or install Python dependencies (`grpcio`).
3. Scan your Wi-Fi interface for your scooter's IPv6 link-local address.
4. Launch the Ola Diagnostic App to suppress sleep timeouts.
5. In-memory bind your PC's ADB key and connect over TCP 5555.
6. Fetch the latest releases of **Quick Ball** and **Square** from GitHub.
7. Install APKs, grant overlay/accessibility permissions, and launch the Quick Ball setup UI.

---

### Method 2: Command Line (CMD / PowerShell)

Run `setup.bat` directly from terminal:
```cmd
setup.bat
```

Or execute via Python:
```bash
python setup.py
```

If you already know the scooter's IPv6 address and network interface scope ID, skip scanning by passing arguments:
```cmd
setup.bat --ipv6 fe80::ea8d:a6ff:fea2:faf0 --scope 22
```

---

## 🧠 Why Does the Diagnostic App Open? (Anti-Sleep)

On MoveOS, a background service named **Vehicle State Manager** enforces strict inactivity timeouts. Even when third-party apps or native settings menus are open, the vehicle manager will lock the display and power down Wi-Fi.

**How This Tool Solves It:**
The script automatically launches Ola's internal **Manufacturing / Diagnostic App (`com.ola.mfgapp`)**. While this application is in the foreground, the vehicle state manager suppresses inactivity sleep and screen timeout. This guarantees your Wi-Fi link remains active throughout file downloads and installation.

---

## 📱 Quick Ball Configuration (1-Minute Guide)

Once setup finishes, the **Quick Ball** configuration screen appears on your scooter dash:

1. **Turn ON:** Toggle the master switch at the top of the Quick Ball screen.
2. **Select Shortcuts:** Tap on **"Select shortcuts"** (or **"Shortcuts"**).
3. **Remove Defaults:** Delete unnecessary default toggles (e.g., Torch, Wi-Fi, Bluetooth).
4. **Configure Essential Shortcuts:**
   * ⬅️ **Back:** Emulates Android Back button in any app.
   * 🏠 **Home:** Instantly returns to the Ola dashboard and speedometer.
   * ⚙️ **Settings:** Opens native Android Settings directly.
   * 🎵 **Square:** Tap `App` -> Select `Square` for your ad-free music player.
5. **Position Floating Ball:** Drag the floating icon to your preferred edge of the screen.

Now you have instant touch navigation to switch between apps and return to the speedometer with a single tap!

---

## 🛠️ Direct Head Unit Control (`ola.py`)

The included `ola.py` script communicates directly with the scooter's root gRPC service on port 50005 without requiring pairing or ADB:

```bash
# Display scooter hardware & firmware details
python ola.py info --ipv6 fe80::ea8d:a6ff:fea2:faf0%22

# Launch native Android Settings menu (Wi-Fi, Bluetooth, Apps, Display, etc.)
python ola.py settings --ipv6 fe80::ea8d:a6ff:fea2:faf0%22

# Open specific settings panel
python ola.py wifi --ipv6 fe80::ea8d:a6ff:fea2:faf0%22
python ola.py bluetooth --ipv6 fe80::ea8d:a6ff:fea2:faf0%22

# Launch Diagnostic App
python ola.py diag --ipv6 fe80::ea8d:a6ff:fea2:faf0%22

# Capture head-unit screenshot to PC
python ola.py screenshot dash.png --ipv6 fe80::ea8d:a6ff:fea2:faf0%22

# Execute arbitrary root shell commands
python ola.py shell "ls -la /data/local/tmp" --ipv6 fe80::ea8d:a6ff:fea2:faf0%22

# Interactive root shell session
python ola.py interactive --ipv6 fe80::ea8d:a6ff:fea2:faf0%22
```

---

## 🔒 Safety, Reversibility & Zero Flash Modification

* **Zero Partition Writes:** No system, vendor, boot, or recovery partitions are touched or flashed.
* **In-Memory RAM Bind Mount:** The ADB key authorization uses `mount --bind` pointing to `/data/local/tmp`. It exists only in temporary RAM.
* **Instant Reversibility:** A standard scooter restart or reboot immediately unmounts the temporary bind and returns the vehicle to its factory state.
* **Subsystem Isolation:** The tool communicates exclusively with the Android infotainment unit (HMI). It has no access to vehicle powertrain, throttle controls, BMS, or motor controllers.

---

## 📁 Repository Structure

```
ola-s1-mods/
├── downloads/             # Local cache for fetched application packages (.gitkeep)
├── .gitignore             # Standard Git ignore rules (APKs, Python cache, OS files)
├── LICENSE                # MIT License with research & safety disclaimer
├── ola.py                 # Pure Python gRPC root communication engine
├── README.md              # Project documentation & user guide
├── requirements.txt       # Python dependencies (grpcio)
├── setup.bat              # 1-Click Windows launcher
└── setup.py               # Automated discovery, unlock, and installation engine
```

---

## 👨‍💻 Author & Contact

* **Anand ([@Asjohn720](https://github.com/Asjohn720))** — Automated discovery engine, in-memory ADB unlock workflow, and project packaging.
  * 🌐 **GitHub:** [@Asjohn720](https://github.com/Asjohn720)
  * 🐦 **X (Twitter):** [@asjohn720](https://x.com/asjohn720)
  * 📸 **Instagram:** [@asjohn720](https://www.instagram.com/asjohn720)

### 🤝 Acknowledgments & Credits

* **Chayan Mistry** — [Quick Ball](https://github.com/chayanforyou/QuickBall) navigation overlay for Android.
* **Lelonio** — [Square Music](https://github.com/Lelonio/Square) lightweight, ad-free music player.
* The open-source Android and reverse engineering research community.

---

## ⚖️ Legal Disclaimer & Fair Use Notice

This project is an independent academic, educational, and interoperability research initiative conducted on privately owned consumer hardware:

* **Lawful Interoperability:** Research and development are carried out under lawful interoperability doctrines, specifically **Section 52(1)(ab) of the Indian Copyright Act, 1957**, which explicitly permits the reverse engineering and adaptation of computer programs to achieve interoperability with other programs or devices, and international Fair Use principles.
* **Safety & Powertrain Non-Interference:** At no point does this software modify, reflash, or compromise safety-critical vehicle components such as the Motor Controller Unit (MCU), Battery Management System (BMS), accelerator/throttle mapping, or braking subsystems. All interactions are confined strictly to the userland Android infotainment head unit.
* **No Cloud or Backend Interference:** No Ola Electric remote servers, telematics infrastructure, cellular eSIM services, or subscription backends are accessed, compromised, or disrupted.
* **Trademarks:** All brand names, logos, and trademarks (including "Ola", "Ola Electric", "MoveOS", and "Android") are the property of their respective owners and are used here purely for nominative product identification.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
