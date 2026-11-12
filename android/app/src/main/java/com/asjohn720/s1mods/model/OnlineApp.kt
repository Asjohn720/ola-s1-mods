package com.asjohn720.s1mods.model

/**
 * Metadata for downloadable online applications in App Manager.
 */
data class OnlineApp(
    val id: String,
    val displayName: String,
    val packageName: String,
    val downloadUrl: String,
    val fileName: String,
    val description: String,
    val sizeText: String,
    val tag: String? = null,
    val isHeavyRam: Boolean = false
)

object OnlineAppCatalog {
    val carPlayApp = OnlineApp(
        id = "openplay_carplay",
        displayName = "OpenPlay",
        packageName = "com.asjohn720.openplay",
        downloadUrl = "https://storage.googleapis.com/publish-apk/openplay-olas1-release.apk",
        fileName = "openplay-olas1-release.apk",
        description = "Phone-projection mod for the S1 display (CarPlay-compatible). Fork of DiPlay. 'CarPlay' is a trademark of Apple Inc.; not affiliated.",
        sizeText = "18.2 MB",
        tag = "Projection"
    )

    val apps = listOf(
        carPlayApp,
        OnlineApp(
            id = "firefox_focus",
            displayName = "Firefox Focus",
            packageName = "org.mozilla.focus",
            downloadUrl = "https://archive.mozilla.org/pub/focus/releases/159.0/android/focus-159.0-android-arm64-v8a/focus-159.0.multi.android-arm64-v8a.apk",
            fileName = "focus-159.0.multi.android-arm64-v8a.apk",
            description = "Privacy-focused web browser. Enables web browsing on your Ola S1 head unit.",
            sizeText = "~85 MB",
            tag = "High RAM Usage",
            isHeavyRam = true
        )
    )
}

