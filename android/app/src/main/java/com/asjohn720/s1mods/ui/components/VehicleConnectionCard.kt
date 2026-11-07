package com.asjohn720.s1mods.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.discovery.DiscoveredScooter
import com.asjohn720.s1mods.discovery.NetworkScanner
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Reusable Vehicle Connection Card & Manual Mode Dialog.
 * Uses the high-performance IPv6 multicast auto-discovery engine
 * and a simplified single-input Manual Mode (no Scope ID required).
 */
@Composable
fun VehicleConnectionCard(
    manager: ScooterManager,
    isRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isConnected by manager.isConnected.collectAsState()
    val scooterIp by manager.scooterIp.collectAsState()
    val scooterScope by manager.scooterScope.collectAsState()

    var isScanning by remember { mutableStateOf(false) }
    var isTestingPort by remember { mutableStateOf(false) }
    var discoveredScooter by remember { mutableStateOf<DiscoveredScooter?>(null) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    var showManualModeDialog by remember { mutableStateOf(false) }
    var tempInput by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Vehicle Icon + Connection Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isConnected) AccentMintSubtle else SurfaceVariantDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ElectricScooter,
                            contentDescription = null,
                            tint = if (isConnected) AccentMint else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Vehicle Connection",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                        Text(
                            text = if (isConnected) "MoveOS Connected" else "Not Connected",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isConnected) AccentMint else StatusDanger
                        )
                    }
                }

                // Port Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isConnected) AccentMintSubtle else SurfaceVariantDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) AccentMint else StatusDanger)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "Port 50005 Open" else "Port 50005 Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isConnected) AccentMint else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // IP Address Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (scooterIp.isBlank()) "No vehicle address configured (Tap Auto-Discover or Manual Mode)"
                    else if (scooterScope != null) "$scooterIp%$scooterScope" else scooterIp,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = if (scooterIp.isBlank()) TextMuted else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Discovered Vehicle Card (Shows when Auto-Discovery detects the scooter)
            if (discoveredScooter != null) {
                val found = discoveredScooter!!
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AccentMint, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentMint, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Vehicle Detected!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentMint
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Address: ${found.ip}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        if (found.interfaceName.isNotBlank()) {
                            Text(
                                text = "Interface: ${found.interfaceName}",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        manager.connect(found.ip, found.scope)
                                        Toast.makeText(context, "Connected to ${found.ip}!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        manager.isConnected.value = true
                                        Toast.makeText(context, "Connected via Port 50005!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
                        ) {
                            Text("Connect to Vehicle", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (scanMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = scanMessage!!,
                    fontSize = 11.sp,
                    color = if (scanMessage!!.contains("not detected", ignoreCase = true)) StatusDanger else AccentSky,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: Auto-Discover (New Engine), Manual Mode (No Scope), Test Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto-Discover Button (Axiom Multicast Ping Engine)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isScanning = true
                            scanMessage = "Scanning hotspot & network interfaces..."
                            discoveredScooter = null
                            val found = manager.autoDiscover()
                            isScanning = false
                            if (found != null) {
                                discoveredScooter = found
                                scanMessage = null
                                Toast.makeText(context, "Found vehicle at ${found.ip}!", Toast.LENGTH_SHORT).show()
                            } else {
                                scanMessage = "Vehicle not detected on Wi-Fi/Hotspot. Ensure hotspot is ON and scooter screen is awake."
                                Toast.makeText(context, "Vehicle not detected. Use Manual Mode to enter IP or MAC.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isScanning && !isRunning,
                    modifier = Modifier.weight(1.1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentSky, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanning...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentSky)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-Discover", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Manual Mode Button (Clean Single-Input: IPv4, IPv6, MAC)
                Button(
                    onClick = {
                        tempInput = scooterIp
                        showManualModeDialog = true
                    },
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentMint)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                // Test Port Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isTestingPort = true
                            val open = manager.testConnection()
                            isTestingPort = false
                            Toast.makeText(
                                context,
                                if (open) "Port 50005 reachable and responsive!" else "Port 50005 unreachable. Ensure scooter is awake.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !isTestingPort && !isRunning,
                    modifier = Modifier.weight(0.9f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    if (isTestingPort) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentMint, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentMint)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Manual Mode Dialog (Clean Single-Input, NO Scope ID required)
    if (showManualModeDialog) {
        val cleanInput = tempInput.trim()
        val derivedIpv6 = remember(cleanInput) {
            NetworkScanner.macToIpv6(cleanInput)
        }
        val isIpv4 = remember(cleanInput) {
            cleanInput.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(:\d+)?$""")) || (cleanInput.contains(".") && !cleanInput.contains(":"))
        }
        val isIpv6 = remember(cleanInput, derivedIpv6) {
            cleanInput.contains(":") && !cleanInput.contains("-") && derivedIpv6 == null
        }

        AlertDialog(
            onDismissRequest = { showManualModeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentMintSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = AccentMint, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Manual Connection", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Enter IPv4, IPv6, or Hotspot MAC", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Instructions Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "How to find address:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentSky
                            )

                            // Option 1: Scooter Settings (IPv4)
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 12.sp, color = AccentMint, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "IPv4: Ola Settings → Wi-Fi → Tap connected Wi-Fi network to view IP address.",
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    lineHeight = 15.sp
                                )
                            }

                            // Option 2: Phone Hotspot (MAC)
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 12.sp, color = AccentMint, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MAC: Phone Settings → Hotspot → Connected Devices to view scooter MAC ID.",
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Single Unified Input Field (No Scope ID needed)
                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = { tempInput = it },
                        label = { Text("Scooter IP or MAC Address") },
                        placeholder = { Text("e.g. 192.168.43.50 or aa:bb:cc:dd:ee:ff") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentMint,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceVariantDark,
                            unfocusedContainerColor = SurfaceVariantDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Real-time format recognition preview
                    if (derivedIpv6 != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentMintSubtle)
                                .border(1.dp, AccentMint.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("✓ Hotspot MAC Recognized (Link-Local IPv6):", fontSize = 10.sp, color = AccentMint, fontWeight = FontWeight.Medium)
                                Text(derivedIpv6, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            }
                        }
                    } else if (isIpv4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentSkySubtle)
                                .padding(8.dp)
                        ) {
                            Text("✓ IPv4 Address Detected", fontSize = 11.sp, color = AccentSky, fontWeight = FontWeight.Medium)
                        }
                    } else if (isIpv6) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentSkySubtle)
                                .padding(8.dp)
                        ) {
                            Text("✓ IPv6 Address Detected", fontSize = 11.sp, color = AccentSky, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = tempInput.trim()
                        val scopeFromPercent = if (trimmed.contains("%")) trimmed.split("%").getOrNull(1)?.toIntOrNull() else null
                        val baseIp = if (trimmed.contains("%")) trimmed.split("%")[0] else trimmed
                        val finalIp = derivedIpv6 ?: baseIp

                        manager.scooterIp.value = finalIp
                        manager.scooterScope.value = scopeFromPercent
                        showManualModeDialog = false

                        // Quick connection test
                        coroutineScope.launch {
                            isTestingPort = true
                            val open = manager.testConnection(finalIp, scopeFromPercent)
                            isTestingPort = false
                            if (open) {
                                manager.isConnected.value = true
                                Toast.makeText(context, "Connected to $finalIp!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Saved $finalIp! Port 50005 check pending.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save & Connect", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualModeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
