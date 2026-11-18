package com.example.ui

enum class MainTab(val label: String) {
    HOME("Inicio"),
    SHORTS("Shorts"),
    CREATE("Crear"),
    SUBSCRIPTIONS("Suscripciones"),
    PROFILE("Mi perfil")
}

sealed interface AppSubScreen {
    data class ChannelDetail(val channelId: String) : AppSubScreen
    data object CreateOrEditChannel : AppSubScreen
    data class UploadVideo(val isShort: Boolean = false, val isPost: Boolean = false) : AppSubScreen
    data object LiveBroadcast : AppSubScreen
    data object DeiPlayStudio : AppSubScreen
    data object YouTubeConnect : AppSubScreen
    data object Notifications : AppSubScreen
    data object SettingsSecurity : AppSubScreen
    data class LibrarySection(val initialTab: Int = 0) : AppSubScreen
    data class LiveViewer(val sessionId: String) : AppSubScreen
}
