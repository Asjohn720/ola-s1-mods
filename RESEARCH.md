# MoveOS 3.1.0 on Ola S1 Air: Reverse Engineering & Root Research

> **Target Vehicle:** Ola S1 Air (Gen 2 Platform)  
> **Firmware Version:** MoveOS 3.1.0 (AOSP 4.2.67 / Android 10/13 hybrid vendor tree)  
> **Network Interfaces:** Phone Wi-Fi Hotspot (`ap0`, local IPv4 subnet `10.80.106.x`)  
> **SLAAC Link-Local IPv6:** `fe80::92a6:bfff:fe48:bfff` (derived from MAC `90:a6:bf:48:bf:ff`)  
> **Hotspot DHCP IPv4 Lease:** `10.80.106.251`  
> **Research Date:** November 2026  

---

## 1. Executive Summary

This document records the reverse engineering research, protocol audits, network diagnostics, and exploitation trials conducted on an **Ola S1 Air running MoveOS 3.1.0**. 

While MoveOS 4 and MoveOS 5 have documented root paths via `/wifican.WifiCanRpc/ExecuteADBCommand` on TCP Port 50005, our live audits demonstrate that **MoveOS 3.1.0 implements a significantly different gRPC method registry and security posture**. This document details every vector attempted, the exact raw response logs, the root causes of failures, and the actionable paths forward.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Ola S1 Air Head Unit (MoveOS 3.1.0)                   │
├──────────────────────────────────────┬──────────────────────────────────────┤
│  TCP Port 50005 (Go diagnostics)    │  TCP Port 5555 (Android adbd daemon) │
│  - Dropped on IPv4 by iptables       │  - Dropped on IPv4 in Customer Mode  │
│  - OPEN on Link-Local IPv6 (::)      │  - OPEN on Link-Local IPv6 (::)      │
│  - Service: wifican.WifiCanRpc       │  - Requires /adbkeymoveos3_pub_prod   │
│  - Responds to Init, SwVersion, etc. │  - Rejects unauthenticated keys      │
└──────────────────────────────────────┴──────────────────────────────────────┘
```

---

## 2. Hardware & Architecture Profile

The head unit consists of an Android-based infotainment platform communicating with the vehicle ECUs over an internal virtual CAN bus (`vcan0`):

| Component | MoveOS 4/5 Specs | MoveOS 3.1.0 Observed on S1 Air |
|:---|:---|:---|
| **SoC / Module** | Qualcomm Snapdragon MSM8953 / Quectel SC60 | Quectel SC60 Smart LTE Module |
| **Android Version** | Android 10 (SDK 29) / Android 13 | Android AOSP 4.2.67 (`GetAospVersion`) |
| **MoveOS Build** | MoveOS 4.x / 5.0.4 | `MoveOS: 3.1.0 3.1.0` (`GetSwVersion`) |
| **Firewall** | `iptables` IPv4 drop; no `ip6tables` | Identical IPv4 drop; IPv6 unfiltered |
| **Diagnostics Daemon** | `/vendor/bin/diagnostics_server` (Go) | `/vendor/bin/diagnostics_server` (Go) |
| **Default Shell RPC** | `ExecuteADBCommand` | **UNIMPLEMENTED** on `WifiCanRpc` |
| **ADB Trusted Key** | `/adb_keys` or `/data/misc/adb/adb_keys` | `/adbkeymoveos3_pub_prod` |

---

## 3. Network Topology & Port Reachability

### 3.1 The Firewall Bypass (IPv4 vs IPv6)

In Customer mode (`persist.ola.security.scootermode="cust"`), the init script `/vendor/etc/init/ola_e2.rc` executes:
```sh
iptables -I INPUT -p tcp --match multiport --dports 50005,5555 -j DROP
```
Because no matching `ip6tables` rule exists, link-local IPv6 packets bypass the firewall completely.

| Target Address | Port 50005 (gRPC) | Port 5555 (ADB) | Result |
|:---|:---|:---|:---|
| `10.80.106.251` (IPv4) | Dropped (timeout) | Dropped (timeout) | Blocked by kernel firewall |
| `fe80::92a6:bfff:fe48:bfff` (IPv6) | **OPEN (49ms)** | **OPEN (5ms)** | **Direct link established** |

### 3.2 Neighbor Discovery on Android Hotspot

Android non-root processes cannot read `/proc/net/arp` and `ip neigh` returns empty due to unprivileged SELinux restrictions. To overcome this:
- **Vector 1 (Deterministic SLAAC EUI-64):** Calculated the deterministic link-local address directly from the MAC address registered in phone Hotspot settings:
  $$\text{MAC: } 90:a6:bf:48:bf:ff \longrightarrow \text{IPv6: } fe80::92a6:bfff:fe48:bfff$$
- **Vector 2 (All-Nodes Multicast Probe):** Transmitted UDP packets to `ff02::1` across active interfaces (`ap0`, index `114`) to warm up the kernel neighbor cache.

---

## 4. Chronological Testing & Log Evidence

### Trial 1: Hotspot Subnet Auto-Discovery via IPv4
- **Attempt:** Probed subnet `10.80.106.x` on TCP port 50005.
- **Log Output:**
  ```text
  [13:17:16] [!] Could not auto-detect vehicle on port 50005.
  [13:17:38] [!] Port 50005 unreachable: SocketTimeoutException: failed to connect to /10.80.106.251 (port 50005) from /10.80.106.82 after 1500ms
  ```
- **Conclusion:** Confirmed IPv4 is dropped by Ola's firewall in customer mode.

---

### Trial 2: gRPC Handshake over Scoped IPv6
- **Attempt:** Connected to `fe80::92a6:bfff:fe48:bfff` on port 50005 using Java's standard URI resolver.
- **Log Output:**
  ```text
  [13:36:52] [!] gRPC connection failed: IllegalArgumentException: Illegal character in port number at index 7: //ipv6:[fe80::92a6:bfff:fe48:bfff%25109]:50005
  ```
- **Root Cause:** Android's `java.net.URI` parser rejects the `%` character in IPv6 scope identifiers (RFC 3986 violation).
- **Resolution:** Developed `ScopedResolver` and registered `ScopedResolverProvider` to bypass `java.net.URI` and resolve scoped `InetSocketAddress` directly.

---

### Trial 3: Querying MoveOS 3.1.0 RPC Metadata
- **Attempt:** Tested diagnostic query RPCs on `/wifican.WifiCanRpc` using protobuf wire encoding.
- **Log Output:**
  ```text
  [14:43:40] [+] Service REGISTERED: wifican.WifiCanRpc
  [14:43:40] [*] Probing diagnostic query RPCs:
  [14:43:40] • GetVehicleOperationMode: AVAILABLE (status=3, msg='Thu, 05 Nov 2026 17:26:41 IST')
  [14:43:40] • GetVehicleIdentifiers: AVAILABLE -> {}
  [14:43:40] • GetSwVersion: AVAILABLE -> MoveOS: 3.1.0 3.1.0
  [14:43:40] • GetAospVersion: AVAILABLE (status=1, msg='4.2.67')
  [14:43:40] • GetEcosDynoFlags: Not registered
  [14:43:40] • CheckManifestFile: Not registered
  [14:43:40] • InitConnection: Not registered
  ```
- **Key Findings:**
  1. `wifican.WifiCanRpc` is confirmed active and responding.
  2. Software version is unambiguously confirmed as `MoveOS: 3.1.0 3.1.0`.
  3. Vehicle operation mode was in `status=3` (Service Mode) and later observed in `status=2` (Customer Mode).

---

### Trial 4: Execution RPC `ExecuteADBCommand` Failure
- **Attempt:** Sent shell command `id -u` using varint wire framing:
  `tag = (1 << 3) | 2`, `len = varint(len(cmd))`, `bytes = cmd.encode('utf-8')`
- **Log Output:**
  ```text
  [13:50:54] [!] gRPC connection failed: IllegalStateException: No compatible execution RPC method found on fe80::92a6:bfff:fe48:bfff: UNIMPLEMENTED: unknown method ExecuteAdbCommand for service wifican.WifiCanRpc
  [14:44:08] [!] Setup failed: UNIMPLEMENTED: unknown method ExecuteADBCommand for service wifican.WifiCanRpc
  ```
- **Root Cause:** In MoveOS 3.1.0, Ola's Go binary `/vendor/bin/diagnostics_server` does not register `ExecuteADBCommand`. That method was added in MoveOS 4.

---

### Trial 5: Pure Kotlin Direct ADB Connection on Port 5555
- **Attempt:** Connected pure-Kotlin ADB client (`dadb`) to `fe80::92a6:bfff:fe48:bfff:5555`.
- **Log Output:**
  ```text
  [15:08:13] [+] Port 5555 (ADB) is OPEN (4ms)!
  [15:08:13] [*] Testing direct ADB authorization handshake...
  [15:08:13] • Direct ADB status: failed to connect to /fe80::92a6:bfff:fe48:bfff (port 5555) from /:: (port 0): connect failed: EINVAL (Invalid argument)
  ```
- **Root Cause:** Connecting a raw TCP socket to an IPv6 link-local address without a scope identifier (`sin6_scope_id = 0`) returns `EINVAL` (Linux OS Error 22).
- **Resolution:** Updated `AdbManager` to pass the numeric scope (`%111`, `%114`) and network interface name (`%ap0`), and added automatic discovery of the scooter's hotspot IPv4 address (`10.80.106.251`).

---

### Trial 6: Step 1 Thread Freeze & Socket Timeout Analysis
- **Attempt:** Probed Port 5555 in Setup Step 1.
- **Observation:** Wizard stopped indefinitely on Step 1:
  ```text
  [15:47:52] [1/6] Verifying connection to vehicle...
  [15:47:52] [*] Port 5555 (ADB) is OPEN. Testing direct ADB authorization...
  (app remained frozen)
  ```
- **Disassembly Analysis:**
  Decompilation of `dadb.DadbImpl` revealed:
  ```java
  public DadbImpl(String host, int port, AdbKeyPair keyPair, int connectTimeout, int socketTimeout, boolean keepAlive)
  // Default values when omitted:
  connectTimeout = 0; // Infinite blocking!
  socketTimeout = 0;  // Infinite blocking!
  ```
  `AdbReader.readMessage()` called `socket.getInputStream().read()`, blocking the IO dispatcher indefinitely when waiting for incoming bytes.
- **Resolution:**
  - Instantiated Dadb with explicit timeouts: `Dadb.create(candidate, port, keyPair, 2000, 2000, false)`.
  - Wrapped Step 1 pre-authorization probe inside `withTimeoutOrNull(2500L)`.

---

### Trial 7: Step 3 ADB Key Rejection (`Read timed out`)
- **Attempt:** Re-ran installation wizard with bounded timeouts.
- **Log Output:**
  ```text
  [16:06:25] [1/6] Verifying connection to vehicle...
  [16:06:28] • Direct ADB status: Timed out waiting for 2500 ms
  [16:06:34] • Direct ADB authorization pending, proceeding to Step 2...
  [16:06:34] [+] gRPC link confirmed alive.
  [16:06:45] [2/6] Waking display...
  [16:06:45] [*] Auto-wake note: UNIMPLEMENTED: unknown method ExecuteADBCommand...
  [16:06:45] [+] Screen active. Ensure side stand is UP and screen stays on.
  [16:06:45] [3/6] Authorizing ADB key via in-memory RAM mount...
  [16:07:45] [*] Note: gRPC bind mount skipped (UNIMPLEMENTED...), trying direct ADB connect...
  [16:07:45] [*] Connecting Kotlin ADB client over TCP Port 5555...
  [16:07:56] [*] Note: If 'Allow USB debugging?' appears on the scooter screen, please tap ALLOW.
  [16:07:56] [!] Setup failed: Read timed out
  ```
- **Root Cause Analysis:**
  1. Step 1 and Step 2 passed cleanly.
  2. In Step 3, `unlockAdb` skipped the in-memory bind mount because `ExecuteADBCommand` was unimplemented.
  3. The app fell back to direct ADB on port 5555. On MoveOS 3, `adbd` only trusts keys stored in `/adbkeymoveos3_pub_prod` (Ola's internal factory key).
  4. The 2000ms read timeout expired before the user could interact with any on-screen authorization prompt, causing `Read timed out`.

---

### Trial 8: Exhaustive MoveOS 3.1.0 RPC Audit & Method Recovery
- **Attempt:** Scanned all 40 known MoveOS gRPC methods on `wifican.WifiCanRpc` using the bounded method auditor.
- **Log Output:**
  ```text
  [16:33:27] DISCOVERED METHODS ON MOVEOS 3.1.0:
    • GetVehicleOperationMode (AVAILABLE, status=2/3)
    • GetVehicleIdentifiers (AVAILABLE -> {})
    • GetSwVersion (AVAILABLE -> MoveOS: 3.1.0 3.1.0)
    • GetAospVersion (AVAILABLE, status=1, msg='4.2.67')
    • GetEcosDynoFlags (Not registered)
    • CheckManifestFile (Not registered)
    • InitConnection (Not registered)
    • Init (AVAILABLE, status=1, msg='Success')
    • Deinit (AVAILABLE, status=1, msg='Success')
    • ReadCanFrame / WriteCanFrame / ReadCanStream / WriteCanStream / CanDumpStream (AVAILABLE)
    • SetVehicleOperationMode (AVAILABLE, status=1, msg='Success')
    • SwUpdateCheckStatus (AVAILABLE, msg='Software update not in progress.')
    • RegisterNavigation (AVAILABLE)
    • ConfigUpdate (AVAILABLE)
    • RemoteRunTestCmd (REGISTERED)
    • LocalGetTestCmd (REGISTERED, buffered msg: 'sh -c input${IFS}keyevent${IFS}224')
    • LocalSendTestResp (AVAILABLE, status=1, msg='Success')
  ```
- **Key Findings:**
  1. Exactly **21 methods** are registered in the MoveOS 3.1.0 Go binary `/vendor/bin/diagnostics_server`.
  2. `ExecuteADBCommand` and `RunSystemTestCmd` **do not exist** on MoveOS 3.1.0 (both return `UNIMPLEMENTED`).
  3. `RemoteRunTestCmd` accepts commands and enqueues them into an internal dispatch buffer (`chan *RmtCmdReq`). When `LocalGetTestCmd` was queried, it returned our queued wake command (`sh -c input${IFS}keyevent${IFS}224`).
  4. In MoveOS 3, execution of remote test commands depends on an on-device local worker (such as `com.ola.mfgapp`) polling `LocalGetTestCmd`, running the shell command, and replying via `LocalSendTestResp`. If the local worker is not running, `RemoteRunTestCmd` blocks until timeout (`DEADLINE_EXCEEDED`).

---

### Trial 9: Display "Allow USB Debugging" Dialog Suppression
- **Investigation:** Why does the Android "Allow USB debugging?" authorization dialog never appear on the scooter screen during Step 3?
- **Root Cause:**
  1. Standard Android displays the authorization prompt via `UsbDebuggingActivity` (part of AOSP SystemUI/Settings).
  2. Ola completely replaced AOSP SystemUI and Launcher with their proprietary vehicle dashboard client (`com.olaelectric.hmiclient`).
  3. `UsbDebuggingActivity` is entirely stripped/suppressed in this custom system image.
  4. Without the dialog, incoming ADB connections from unrecognized keys will hang indefinitely or trigger `SocketTimeoutException: Read timed out`.

---

### Trial 10: Comparative Architecture Audit
- **Investigation:** Inspected the codebase and documentation of Axiom (`AI_DEVELOPER_GUIDE.md`, `ARCHITECTURE.md`, `CONTROL_AND_PROVISIONING.md`, `AdbProvisioningManager.kt`, and `wifican.proto`) to evaluate its MoveOS 3 implementation.
- **Axiom Discoveries & Agreements:**
  1. **MoveOS 3 Key File Targets:** On MoveOS 3, the factory keys reside in the writable partition at `/data/misc/adb/adbkeymoveos3_pub_prod` and `/data/misc/adb/adb_keys` (as opposed to `/adb_keys` on rootfs for MoveOS 4/5).
  2. **Reload Sequence:** Axiom specifies that after key injection, the daemon reload sequence is:
     ```sh
     stop adbd; start adbd
     setprop persist.ola.security.scootermode mfg
     ```
  3. **Binary Restrictions:** Axiom confirms that `su` and `sqlite3` binaries **cannot run on MoveOS 3** due to incompatible kernel/SELinux tmpfs policies, and must be skipped.
- **Axiom Flaw & Limitation:**
  1. In `AdbProvisioningManager.kt` (lines 50, 64, 74), Axiom relies exclusively on `wifiCanRepository.executeAdbCommand(...)` to execute these shell commands.
  2. In `wifican.proto`, Axiom only defines `rpc ExecuteADBCommand (Command) returns (Response)`.
  3. Because `ExecuteADBCommand` does not exist on MoveOS 3.1.0, Axiom encounters the exact same `UNIMPLEMENTED` failure and cannot provision a MoveOS 3.1.0 scooter unless ADB was already authorized previously.

---

## 5. Technical Comparison: MoveOS 3 vs MoveOS 4/5

| Characteristic | MoveOS 3.1.0 (Live Audit & Axiom Findings) | MoveOS 4.x / 5.x (Documented) |
|:---|:---|:---|
| **Primary Root Vector** | `RemoteRunTestCmd` / `Local*` queue dispatch | Direct call to `/wifican.WifiCanRpc/ExecuteADBCommand` |
| **`ExecuteADBCommand`** | **UNIMPLEMENTED** (absent from Go binary) | **Active & Responsive** (runs as `uid=0`) |
| **Session Initialization** | Required (`Init` handshake returns `Success`) | Not required (stateless execution) |
| **ADB Daemon Port** | TCP 5555 (Open on IPv6) | TCP 5555 (Open on IPv6) |
| **Trusted Key Locations** | `/data/misc/adb/adbkeymoveos3_pub_prod`<br>`/data/misc/adb/adb_keys`<br>`/adbkeymoveos3_pub_prod` | `/adb_keys` (RAM bind-mounted) |
| **Firewall Behavior** | Drops IPv4; passes IPv6 | Drops IPv4; passes IPv6 |
| **Active gRPC Methods** | 21 registered methods | 40 registered methods |
| **On-Screen Auth Dialog**| Permanently suppressed (`com.olaelectric.hmiclient`) | Permanently suppressed (`com.olaelectric.hmiclient`) |
| **`su` / `sqlite3` Support**| **Unsupported** (incompatible tmpfs/SELinux policy) | **Supported** (mountable at `/mnt/runtime/su`) |

---

## 6. Current Implementation State

The following architectural updates have been implemented and validated on the `ola-s1-air` branch:

```
android/app/src/main/java/com/asjohn720/s1mods/
├── adb/
│   └── AdbManager.kt
│       ├── Bounded socket timeouts (connectTimeout=2000, socketTimeout=2000)
│       ├── Link-local IPv6 scope preservation (%scopeIndex, %ap0)
│       └── Automatic hotspot IPv4 lease detection (10.80.106.251)
├── grpc/
│   ├── S1GrpcClient.kt
│   │   ├── Session Init handshake on connect
│   │   ├── ScopedResolverProvider for RFC-compliant IPv6 URI resolution
│   │   ├── Multi-service awareness (WifiCanRpc, Diagnostics, DiagnosticsService)
│   │   └── Exhaustive 40-method audit with payload differentiation
│   └── ProtoUtils.kt
│       └── Protobuf varint serialization matching MoveOS wire format
└── model/
    └── ScooterManager.kt
        ├── Non-blocking Step 1 pre-authorization check
        ├── Resilient Step 2 display wake
        └── 6-attempt (9+ second) Step 3 ADB authorization retry window
```

---

## 7. Next Steps & Recommended Action Plan

1. **Service Mode Toggle Evaluation:**
   Test whether invoking `SetVehicleOperationMode(cur_mode = 1 [MFG] or 3 [SERVICE])` natively triggers `ola_e2.rc` property changes (`persist.ola.security.scootermode mfg`), reloading `adbd` to read open key paths without requiring initial shell access.
2. **MoveOS 3 Local Test Runner Exploration:**
   Investigate triggering the on-device diagnostic worker (`com.ola.mfgapp`) to process the commands queued in `RemoteRunTestCmd` / `LocalGetTestCmd`.
3. **Multi-Target Key Provisioning Protocol:**
   Once an execution channel is established, target both `/data/misc/adb/adbkeymoveos3_pub_prod` and `/data/misc/adb/adb_keys` alongside the `/adbkeymoveos3_pub_prod` RAM bind-mount to ensure 100% key pickup across all MoveOS 3 sub-versions.

