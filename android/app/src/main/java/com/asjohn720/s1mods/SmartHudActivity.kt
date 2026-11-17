package com.asjohn720.s1mods

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.asjohn720.s1mods.model.ScooterManager
import com.asjohn720.s1mods.ui.screens.SmartHudScreen
import com.asjohn720.s1mods.ui.theme.S1ModsTheme

/**
 * Dedicated Full-Screen Immersive Smart HUD Activity for the Ola S1 7-inch Touchscreen.
 * Can be launched standalone via ADB:
 *   am start -n com.asjohn720.s1mods/.SmartHudActivity
 * Or triggered from the Balls Floating Navigation overlay.
 */
class SmartHudActivity : ComponentActivity() {

    private lateinit var scooterManager: ScooterManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Immersive Sticky Full-screen for 7" dash
        hideSystemUI()

        scooterManager = ScooterManager(applicationContext)

        setContent {
            S1ModsTheme {
                SmartHudScreen(scooterManager)
            }
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scooterManager.disconnect()
    }
}
