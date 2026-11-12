package com.asjohn720.s1mods.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.SocialButton
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.ui.components.VehicleConnectionCard
import com.asjohn720.s1mods.ui.theme.*

/**
 * Startup Connection Gate Screen.
 * Uses the exact same connection card and manual mode dialog as SetupScreen.
 */
@Composable
fun ConnectVehicleScreen(
    manager: ScooterManager,
    onBypass: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    val isConnected by manager.isConnected.collectAsState()
    val scooterIp by manager.scooterIp.collectAsState()
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // App Logo & Branding
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceVariantDark)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ElectricScooter,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "S1Mods",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "by Asjohn720",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showAbout = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Connect your scooter to continue",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // The EXACT same Connection Card from SetupScreen
        VehicleConnectionCard(
            manager = manager,
            isRunning = false
        )

        // Proceed to Dashboard Button (When connected)
        if (isConnected) {
            Button(
                onClick = onBypass,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark)
            ) {
                Text(
                    text = "Continue",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // How to connect — looks tappable (bordered, with icon + chevron).
        OutlinedButton(
            onClick = onOpenHelp,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CardBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("How to connect", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextMuted)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Skip to offline mode — plain text, with a quiet one-line disclaimer.
        if (!isConnected) {
            Text(
                text = "Continue in offline mode",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBypass() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        Text(
            text = "Use at your own risk. The developer is not liable.",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("S1Mods", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("By Anand (@asjohn720)", fontSize = 13.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SocialButton("GitHub", "https://github.com/Asjohn720", context, modifier = Modifier.weight(1f))
                        SocialButton("X", "https://x.com/asjohn720", context, modifier = Modifier.weight(1f))
                        SocialButton("Instagram", "https://www.instagram.com/asjohn720.exe", context, modifier = Modifier.weight(1f))
                    }
                    Text("MIT License. Unofficial, not affiliated with Ola Electric. 'Ola' and 'MoveOS' are trademarks of Ola Electric; 'S1' is used descriptively.", fontSize = 11.sp, color = TextMuted, lineHeight = 15.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Close", color = TextPrimary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
