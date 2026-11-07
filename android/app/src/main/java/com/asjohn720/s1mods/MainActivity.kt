package com.asjohn720.s1mods

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.ui.screens.ConnectVehicleScreen
import com.asjohn720.s1mods.ui.screens.CustomApkScreen
import com.asjohn720.s1mods.ui.screens.SetupScreen
import com.asjohn720.s1mods.ui.screens.ShellScreen
import com.asjohn720.s1mods.ui.screens.ToolboxScreen
import com.asjohn720.s1mods.ui.theme.*

enum class AppTab(val title: String, val icon: ImageVector) {
    SETUP("Setup", Icons.Default.Bolt),
    TOOLBOX("Tools", Icons.Default.Dashboard),
    CUSTOM_APK("App Manager", Icons.Default.Download),
    TERMINAL("Shell", Icons.Default.Terminal)
}

class MainActivity : ComponentActivity() {

    private lateinit var scooterManager: ScooterManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scooterManager = ScooterManager(applicationContext)

        setContent {
            S1ModsTheme {
                MainAppScaffold(scooterManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scooterManager.disconnect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(manager: ScooterManager) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("s1mods_prefs", android.content.Context.MODE_PRIVATE) }
    var selectedTab by remember { mutableStateOf(AppTab.SETUP) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember {
        mutableStateOf(!prefs.getBoolean("has_seen_onboarding", false))
    }
    val isConnected by manager.isConnected.collectAsState()
    var bypassConnectionGate by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)
    }

    if (!isConnected && !bypassConnectionGate) {
        ConnectVehicleScreen(
            manager = manager,
            onBypass = { bypassConnectionGate = true },
            onOpenHelp = { showOnboardingDialog = true }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showAboutDialog = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceVariantDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ElectricScooter,
                                    contentDescription = null,
                                    tint = AccentMint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "S1Mods",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "by Anand (@asjohn720)",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    },
                    actions = {
                        // Status Pill - Tap to disconnect or switch vehicle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isConnected) AccentMintSubtle else SurfaceVariantDark)
                                .clickable {
                                    manager.disconnect()
                                    bypassConnectionGate = false
                                }
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
                                text = if (isConnected) "Connected" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isConnected) AccentMint else TextMuted
                            )
                        }

                        IconButton(onClick = { showOnboardingDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "How to Use",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "About & Author",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BgDark,
                        titleContentColor = TextPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(width = 1.dp, color = DividerColor, shape = RoundedCornerShape(0.dp))
                ) {
                    AppTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentMint,
                                selectedTextColor = AccentMint,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = AccentMintSubtle
                            )
                        )
                    }
                }
            },
            containerColor = BgDark
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    AppTab.SETUP -> SetupScreen(manager)
                    AppTab.TOOLBOX -> ToolboxScreen(manager)
                    AppTab.CUSTOM_APK -> CustomApkScreen(manager)
                    AppTab.TERMINAL -> ShellScreen(manager)
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = AccentMint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("S1Mods v1.0.0", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Author & Project Credits", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Author Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Developer",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Anand (@Asjohn720)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Automated discovery, in-memory ADB unlock workflow & Android app architecture.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SocialButton("GitHub", "https://github.com/Asjohn720", context, modifier = Modifier.weight(1f))
                                SocialButton("X / Twitter", "https://x.com/asjohn720", context, modifier = Modifier.weight(1f))
                                SocialButton("Instagram", "https://www.instagram.com/asjohn720", context, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Text(
                        text = "Independent open-source utility under MIT License. Not affiliated with Ola Electric.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showOnboardingDialog) {
        AlertDialog(
            onDismissRequest = {
                prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                showOnboardingDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentMintSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ElectricScooter,
                            contentDescription = null,
                            tint = AccentMint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "How to Use S1Mods",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Quick 4-step connection guide",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OnboardingStepItem(
                        step = "1",
                        title = "Turn on Phone Hotspot",
                        description = "Enable your mobile Wi-Fi Hotspot on this phone."
                    )
                    OnboardingStepItem(
                        step = "2",
                        title = "Connect Scooter",
                        description = "On your Ola scooter: Settings → Wi-Fi, then connect to your phone's Hotspot."
                    )
                    OnboardingStepItem(
                        step = "3",
                        title = "Tap Auto Scan",
                        description = "Tap 'Auto Scan' in the app to discover your scooter automatically."
                    )
                    OnboardingStepItem(
                        step = "4",
                        title = "Fallback: Use Manual Mode",
                        description = "If scan doesn't detect it, use 'Manual Mode' and enter the Hotspot MAC ID or IP."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                        showOnboardingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It, Let's Start!", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun OnboardingStepItem(step: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantDark)
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AccentMintSubtle),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentMint)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = description, fontSize = 11.sp, color = TextSecondary, lineHeight = 14.sp)
        }
    }
}

@Composable
fun SocialButton(label: String, url: String, context: android.content.Context, modifier: Modifier = Modifier) {
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BgDark, contentColor = TextPrimary)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
