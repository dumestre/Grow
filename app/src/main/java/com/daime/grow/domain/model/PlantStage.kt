package com.daime.grow.domain.model

object PlantStage {
    const val ALL = "Todas"
    const val SEEDLING = "Muda"
    const val VEGETATIVE = "Vegetativo"
    const val FLOWER = "Flora"

    val entries = listOf(SEEDLING, VEGETATIVE, FLOWER)
    val filterEntries = listOf(ALL) + entries
}

