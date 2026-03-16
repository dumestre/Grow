package com.daime.grow.domain.model

data class SecurityPreferences(
    val lockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val pinHash: String = "",
    val maskHomeIcon: Boolean = true,
    val maskStoreCatalog: Boolean = true,
    val darkTheme: DarkThemeMode = DarkThemeMode.SYSTEM
)

enum class DarkThemeMode {
    SYSTEM,  // Segue o sistema
    LIGHT,   // Sempre claro
    DARK     // Sempre escuro
}
