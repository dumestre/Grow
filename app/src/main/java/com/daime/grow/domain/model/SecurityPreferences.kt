package com.daime.grow.domain.model

data class SecurityPreferences(
    val lockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val pinHash: String = ""
)

