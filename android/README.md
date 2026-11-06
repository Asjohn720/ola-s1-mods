# 🛵 S1Mods Android Application

**S1Mods** is an all-in-one open-source Android application designed to configure, mod, and control MoveOS-powered infotainment units directly from your smartphone over Wi-Fi or Phone Hotspot — completely eliminating the need for a laptop or PC.

> [!IMPORTANT]
> **LEGAL DISCLAIMER & TRADEMARK NOTICE:**
> This application is an independent, non-commercial, open-source educational utility developed by independent software enthusiasts. This project is **NOT** affiliated, associated, authorized, endorsed by, or in any way officially connected with **Ola Electric Mobility Ltd.**, **ANI Technologies Pvt. Ltd.**, or any of their subsidiaries or affiliates.
> The names "Ola", "Ola Electric", "MoveOS", and related trademarks are registered trademarks of their respective owners. Any reference to these marks is strictly for descriptive and nominative fair use (Section 30 of the Indian Trade Marks Act, 1999) to indicate compatibility.

---

## 🚀 Key Features

* **📱 Phone Hotspot / Wi-Fi Discovery:** Automatically scans link-local IPv6 neighbors (`fe80::...`) and probes the scooter's root diagnostics service on TCP port `50005`. Includes a built-in SLAAC EUI-64 MAC-to-IPv6 converter for instant hotspot connection.
* **🛡️ Zero Power-Manager Tampering (Vehicle Auto-Lock Preserved):** Unlike unsafe scripts, S1Mods does **NOT** tamper with system power manager or force `svc power stayon true`. Your scooter's auto-lock security remains 100% functional.
* **⚡ 1-Click Automated Setup Wizard:**
  1. Detects scooter & verifies port 50005 connection.
  2. Displays pre-flight safety check (Side stand UP + keep screen active).
  3. Generates/loads RSA 2048 ADB key and binds it via safe in-memory RAM mount (`mount --bind`).
  4. Connects built-in Kotlin ADB client over TCP port `5555`.
  5. Extracts and installs bundled APKs (**Balls Navigation** and **Music Mod**) directly from app assets.
  6. Automatically grants `SYSTEM_ALERT_WINDOW`, `WRITE_SETTINGS`, and Android Accessibility Service permissions.
  7. Returns scooter to the official dashboard.
* **🧰 Scooter Toolbox:**
  * **Dashboard Home:** Instant 1-tap return to the speedometer screen.
  * **Direct Settings Shortcuts:** One-tap access to Android Settings, Wi-Fi, Bluetooth, Applications, Display, Sound, and Date & Time.
  * **Live Screenshot Viewer:** Takes a screenshot from the scooter display, previews it on your phone, and lets you save it to storage.
  * **System Specs Inspector:** Inspect MoveOS build, Android version, vehicle model, UID, and scooter security mode (`serv` vs `prod`).
  * *(Unsafe reboot and stay-on controls have been intentionally removed).*
* **📦 Custom APK Installer:** File picker to install any third-party `.apk` from your phone storage onto the scooter via wireless ADB.
* **💻 Interactive Root Shell:** Full root (`uid 0`) terminal with quick preset commands and one-click output clipboard copy.

---

## 🛠️ How to Use with Phone Hotspot

1. **Turn on Personal Hotspot** on your Android phone.
2. On your scooter touchscreen, go to **Settings > Wi-Fi** and connect to your phone's hotspot.
3. Open **S1Mods** on your phone.
4. Tap **Scan** *(allow Location permission when prompted — Android requires this to scan local devices)*. If auto-scan is blocked by your phone's security, copy the scooter's **MAC or IP** from **Hotspot > Connected Devices** and paste it via the **Edit (✏️)** button.
5. Ensure scooter **side stand is UP** and screen is active.
6. Tap **1-Click Automated Setup** and confirm the pre-flight prompt.
7. Within seconds, the floating touch navigation ball will appear on your scooter screen!

---

## 🏗️ Build Instructions

To build the APK using Gradle:
```bash
cd android
./gradlew assembleDebug
```
The output APK is generated at:
`android/app/build/outputs/apk/debug/app-debug.apk`
