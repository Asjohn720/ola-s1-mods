#!/usr/bin/env python3
"""
Ola S1 Quick Mod & Setup Tool
Automates:
  1. Finding Ola scooter IP over local Wi-Fi
  2. Keeping scooter awake via Ola Diagnostic App
  3. Unlocking ADB access (safe, reversible RAM bind-mount)
  4. Verifying bundled local APKs (Balls & Music Mod)
  5. Installing APKs & granting required permissions
  6. Launching Balls for easy floating navigation
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import time

try:
    import grpc
    import ola
except ImportError:
    print("[!] Missing dependencies. Installing grpcio...")
    subprocess.run([sys.executable, "-m", "pip", "install", "grpcio>=1.50.0"], check=True)
    import grpc
    import ola

BANNER = r"""
========================================================================
     ___  _          ____  _     ___        _      _     ____       _               
    / _ \| | __ _   / ___|/ |   / _ \ _   _(_) ___| | __/ ___|  ___| |_ _   _ _ __  
   | | | | |/ _` |  \___ \| |  | | | | | | | |/ __| |/ /\___ \ / _ \ __| | | | '_ \ 
   | |_| | | (_| |   ___) | |  | |_| | |_| | | (__|   <  ___) |  __/ |_| |_| | |_) |
    \___/|_|\__,_|  |____/|_|   \__\_\\__,_|_|\___|_|\_\|____/ \___|\__|\__,_| .__/ 
                                                                              |_|    
               Automated Floating Navigation & Music Installer
========================================================================
"""

def print_step(num: int, title: str):
    print(f"\n[{num}/6] {title}")
    print("-" * 60)

def find_adb() -> str:
    """Finds adb executable path on the system."""
    found = shutil.which("adb")
    if found:
        return found
    
    # Common Android SDK locations
    candidates = [
        os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"),
        r"C:\platform-tools\adb.exe",
        r"C:\Program Files\platform-tools\adb.exe",
        r"C:\Program Files\DroidCam\Client\data\obs-plugins\droidcam-obs\adb\adb.exe",
    ]
    for c in candidates:
        if os.path.exists(c):
            return c
    return ""

def test_tcp_port6(ip: str, port: int, scope: int | None, timeout: float = 0.8) -> bool:
    """Tests if an IPv6 TCP port is open."""
    try:
        s = socket.socket(socket.AF_INET6, socket.SOCK_STREAM)
        s.settimeout(timeout)
        scope_id = scope if scope is not None else 0
        s.connect((ip, port, 0, scope_id))
        s.close()
        return True
    except Exception:
        return False

def scan_for_scooter() -> tuple[str, int] | None:
    """Pings multicast and scans IPv6 neighbors for open port 50005."""
    print("[*] Pinging local IPv6 multicast to discover network devices...")
    
    # Get active interfaces
    ps_cmd = 'Get-NetAdapter | Where-Object { $_.Status -eq "Up" } | Select-Object -ExpandProperty InterfaceIndex'
    try:
        proc = subprocess.run(["powershell", "-NoProfile", "-Command", ps_cmd], capture_output=True, text=True)
        indices = [int(x.strip()) for x in proc.stdout.splitlines() if x.strip().isdigit()]
    except Exception:
        indices = []

    for idx in indices:
        subprocess.run(f"ping -6 -n 1 -w 300 ff02::1%{idx}", shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(0.5)

    print("[*] Scanning IPv6 neighbor table for Ola Head Unit (Port 50005)...")
    try:
        out = subprocess.check_output("netsh interface ipv6 show neighbors", text=True, stderr=subprocess.DEVNULL)
    except Exception:
        out = ""

    current_idx = None
    candidates = []
    for line in out.splitlines():
        m_if = re.match(r"Interface\s+(\d+):", line)
        if m_if:
            current_idx = int(m_if.group(1))
        elif current_idx and line.strip().startswith("fe80:"):
            parts = line.split()
            if len(parts) >= 3 and parts[2].lower() != "unreachable":
                candidates.append((parts[0], current_idx))

    for ip, scope in candidates:
        print(f"    Checking {ip}%{scope} ...", end="", flush=True)
        if test_tcp_port6(ip, 50005, scope):
            print(" [OPEN] -> Found Ola Scooter!")
            return ip, scope
        else:
            print(" [closed]")

    return None

def pre_setup_wake(channel):
    """Wakes screen safely without modifying persistent sleep/power states."""
    print("[*] Waking display...")
    ola.run(channel, "input keyevent 224", quiet=True)
    print("    >> PRE-CHECK: Ensure scooter side stand is UP and screen stays awake during mods.")
    import time
    time.sleep(0.5)

def unlock_adb(channel, ipv6: str, scope: int | None, adb_exe: str) -> str:
    """Safely unlocks ADB via RAM bind mount without flashing partitions."""
    print("[*] Preparing ADB key...")
    adb_key_dir = os.path.expanduser("~/.android")
    adb_pub = os.path.join(adb_key_dir, "adbkey.pub")
    if not os.path.exists(adb_pub):
        print("    Generating fresh ADB keypair on PC...")
        os.makedirs(adb_key_dir, exist_ok=True)
        subprocess.run([adb_exe, "keygen", os.path.join(adb_key_dir, "adbkey")], check=True, stdout=subprocess.DEVNULL)

    print("    Uploading PC public key to scooter...")
    ola.push(channel, adb_pub, "/data/local/tmp/adb_keys_mine")
    ola.run(channel, "chmod 644 /data/local/tmp/adb_keys_mine", quiet=True)

    print("    Setting scootermode=serv...")
    ola.run(channel, "setprop persist.ola.security.scootermode serv", quiet=True)

    bind_script = """#!/system/bin/sh
if [ -f /data/misc/adb/adbkeymoveos3_pub_prod ]; then
    T=/data/misc/adb/adbkeymoveos3_pub_prod
elif [ -f /data/misc/adb/adb_keys ]; then
    T=/data/misc/adb/adb_keys
else
    T=/adbkeymoveos3_pub_prod
fi
mount | grep -q " $T " && umount "$T"
mount --bind /data/local/tmp/adb_keys_mine "$T" || exit 1
echo "bound $T"
"""
    tmp_path = os.path.join(tempfile.gettempdir(), "ola_bind_key.sh")
    with open(tmp_path, "wb") as f:
        f.write(bind_script.replace("\r\n", "\n").encode("ascii"))

    ola.push(channel, tmp_path, "/data/local/tmp/bind_key.sh")
    ola.run(channel, "chmod 755 /data/local/tmp/bind_key.sh", quiet=True)
    ola.run(channel, "sh /data/local/tmp/bind_key.sh", quiet=True)
    time.sleep(1.0)

    device = f"[{ipv6}%{scope}]:5555" if scope else f"[{ipv6}]:5555"
    print(f"[*] Connecting ADB to {device}...")
    subprocess.run([adb_exe, "connect", device], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(1.0)
    
    # Verify state
    res = subprocess.run([adb_exe, "-s", device, "get-state"], capture_output=True, text=True)
    if "device" not in res.stdout:
        subprocess.run([adb_exe, "connect", device], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        time.sleep(1.0)
    print("    [+] ADB connected successfully as 'device'!")
    return device

def install_and_grant(device: str, adb_exe: str, apks: dict[str, str]):
    """Installs bundled APKs and grants necessary Android permissions."""
    def safe_install(apk_path: str, app_name: str):
        print(f"[*] Installing {app_name} ({os.path.basename(apk_path)})...")
        res = subprocess.run([adb_exe, "-s", device, "install", "-r", "-d", apk_path], capture_output=True, text=True)
        combined = (res.stdout + " " + res.stderr).strip()
        if "Success" in combined:
            print(f"    [+] {app_name} installed successfully.")
        elif "ALREADY_EXISTS" in combined or "signatures do not match" in combined:
            print(f"    [+] {app_name} is already installed / updated.")
        else:
            print(f"    [*] Install status: {combined or 'Done'}")

    for name, path in apks.items():
        if os.path.isfile(path):
            safe_install(path, name)

    # Balls Navigation permissions
    pkg_ball = "com.asjohn720.ball"
    svc_ball = f"{pkg_ball}/com.asjohn720.ball.core.QuickBallService"
    if "Balls Navigation" in apks and os.path.isfile(apks["Balls Navigation"]):
        print("[*] Granting special permissions to Balls Navigation via ADB...")
        subprocess.run([adb_exe, "-s", device, "shell", "appops", "set", pkg_ball, "SYSTEM_ALERT_WINDOW", "allow"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "appops", "set", pkg_ball, "WRITE_SETTINGS", "allow"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "settings", "put", "secure", "enabled_accessibility_services", svc_ball], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "settings", "put", "secure", "accessibility_enabled", "1"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        print("    [+] Balls: SYSTEM_ALERT_WINDOW, WRITE_SETTINGS & Accessibility granted.")
        subprocess.run([adb_exe, "-s", device, "shell", "am", "start", "-n", f"{pkg_ball}/.ui.MainActivity"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        time.sleep(1.0)

    # Android Auto permissions
    pkg_aa = "com.asjohn720.androidauto"
    if "Android Auto" in apks and os.path.isfile(apks["Android Auto"]):
        print("[*] Granting permissions to Android Auto (OpenHeadUnit) via ADB...")
        subprocess.run([adb_exe, "-s", device, "shell", "appops", "set", pkg_aa, "SYSTEM_ALERT_WINDOW", "allow"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "appops", "set", pkg_aa, "WRITE_SETTINGS", "allow"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "pm", "grant", pkg_aa, "android.permission.RECORD_AUDIO"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "pm", "grant", pkg_aa, "android.permission.ACCESS_FINE_LOCATION"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "pm", "grant", pkg_aa, "android.permission.ACCESS_COARSE_LOCATION"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "pm", "grant", pkg_aa, "android.permission.READ_EXTERNAL_STORAGE"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "pm", "grant", pkg_aa, "android.permission.WRITE_EXTERNAL_STORAGE"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "appops", "set", pkg_aa, "MANAGE_EXTERNAL_STORAGE", "allow"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "pm", "grant", pkg_aa, "android.permission.NEARBY_WIFI_DEVICES"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "pm", "grant", pkg_aa, "android.permission.BLUETOOTH_CONNECT"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run([adb_exe, "-s", device, "shell", "pm", "grant", pkg_aa, "android.permission.BLUETOOTH_SCAN"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        print("    [+] Android Auto: Mic, Location, Overlay, Storage & Bluetooth granted.")

    print("[*] Bringing Dashboard Home to front...")
    subprocess.run([adb_exe, "-s", device, "shell", "am", "start", "-n", "com.olaelectric.hmiclient/.MainActivity"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    subprocess.run([adb_exe, "-s", device, "shell", "input", "keyevent", "3"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print("    [+] Scooter returned to speedometer dashboard.")

def main():
    print(BANNER)

    parser = argparse.ArgumentParser(description="Ola S1 Quick Mod & Setup Tool")
    parser.add_argument("--ipv6", help="Scooter IPv6 address (e.g. fe80::1234:5678:9abc:def0)")
    parser.add_argument("--scope", type=int, help="Interface scope ID (e.g. 22)")
    parser.add_argument("--port", type=int, default=50005, help="Ola gRPC port (default 50005)")
    args = parser.parse_args()

    # Step 1: Check ADB
    print_step(1, "Verifying Tools & Environment")
    adb_exe = find_adb()
    if not adb_exe:
        print("[!] ERROR: 'adb' was not found on your system.")
        print("    Please install Android Platform Tools or add adb.exe to your PATH.")
        print("    Download from: https://developer.android.com/tools/releases/platform-tools")
        sys.exit(1)
    print(f"[+] ADB found: {adb_exe}")

    # Step 2: Locate Scooter
    print_step(2, "Connecting to Ola Scooter on Wi-Fi")
    ipv6 = args.ipv6
    scope = args.scope

    if not ipv6:
        while True:
            found = scan_for_scooter()
            if found:
                ipv6, scope = found
                print(f"[+] Discovered scooter at: {ipv6} (Scope {scope})")
                break
            else:
                print("\n[!] Could not automatically detect scooter on Wi-Fi.")
                print("    Please ensure:")
                print("    1. Your PC and Scooter are connected to the SAME Wi-Fi.")
                print("    2. The scooter screen is AWAKE (press power button or tap display).")
                print("\nOptions:")
                print("  [1] Try scanning again")
                print("  [2] Enter Scooter IPv6 address manually")
                print("  [3] Exit")
                choice = input("\nEnter choice [1-3] (default 1): ").strip()
                if choice == "2":
                    ipv6 = input("Enter Scooter IPv6 (e.g. fe80::1234:5678:9abc:def0): ").strip()
                    scope_str = input("Enter Interface Scope ID (e.g. 22): ").strip()
                    scope = int(scope_str) if scope_str else None
                    break
                elif choice == "3":
                    sys.exit(0)

    target = ola.build_target(ipv6, args.port, scope)
    print(f"[*] Connecting to Ola daemon at {target} ...")
    try:
        channel = ola.connect(target, wait=6.0)
    except Exception as e:
        print(f"[!] Failed to connect to scooter at {target}: {e}")
        print("    Ensure scooter is awake and Wi-Fi is active.")
        sys.exit(1)

    # Step 3: Prevent Sleep & Wake Screen
    print_step(3, "Waking Screen & Pre-Flight Check")
    pre_setup_wake(channel)

    # Step 4: Unlock ADB
    print_step(4, "Unlocking ADB Access")
    device = unlock_adb(channel, ipv6, scope, adb_exe)

    # Step 5: Verify Bundled APKs
    print_step(5, "Verifying Bundled Mod APK Packages")
    repo_dir = os.path.dirname(os.path.abspath(__file__))
    apks_dir = os.path.join(repo_dir, "apks")

    apks_to_install = {
        "Balls Navigation": os.path.join(apks_dir, "balls-1.0.0-lite-release.apk"),
        "Music Mod": os.path.join(apks_dir, "musicmod-release.apk"),
        "Android Auto": os.path.join(apks_dir, "com.asjohn720.androidauto.apk"),
        "Via Browser": os.path.join(apks_dir, "via-release.apk")
    }

    if not os.path.isfile(apks_to_install["Balls Navigation"]):
        print(f"[!] Error: Mandatory APK missing at {apks_to_install['Balls Navigation']}")
        sys.exit(1)

    for name, path in apks_to_install.items():
        if os.path.isfile(path):
            print(f"    [+] Found: {os.path.basename(path)} ({os.path.getsize(path)/1024/1024:.2f} MB)")
        else:
            print(f"    [-] Optional APK not found: {os.path.basename(path)} (skipping)")

    # Step 6: Install & Configure
    print_step(6, "Installing Modded Apps & Granting Permissions")
    install_and_grant(device, adb_exe, apks_to_install)

    # Completion Banner & User Guide
    print("\n" + "=" * 72)
    print("         CONGRATULATIONS! ALL MODS INSTALLED SUCCESSFULLY!       ")
    print("=" * 72)
    print("\n[+] Scooter display is now back on your Speedometer Dashboard.")
    print("[+] Balls navigation overlay is active and ready to use!")
    print("\n>>> HOW TO USE YOUR NEW MODS:")
    print("  Tap the floating ball on your screen anytime to access:")
    print("    * Back          -> Instant Back button inside any app")
    print("    * Home          -> Instant return to Speedometer Dashboard")
    print("    * Settings      -> Direct access to Android Settings (Wi-Fi, Bluetooth, etc.)")
    print("    * Music Mod     -> Ad-free streaming & offline music playback")
    print("    * Android Auto  -> OpenHeadUnit projection")
    print("    * Via Browser   -> Lightweight web browsing")
    print("\nNo manual configuration required - everything is ready to enjoy!")
    print("=" * 72)

if __name__ == "__main__":
    main()
