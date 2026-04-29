package com.daime.grow.domain.model

enum class LightSource(
    val displayName: String,
    val factor: Double
) {
    SUNLIGHT("Luz Solar", 0.0185),
    LED_WHITE("LED (Full Spectrum)", 0.0155),
    HPS("HPS / Sódio", 0.0125)
}
