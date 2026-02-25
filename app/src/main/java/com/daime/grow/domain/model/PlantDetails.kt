package com.daime.grow.domain.model

data class PlantDetails(
    val plant: Plant,
    val events: List<PlantEvent>,
    val wateringLogs: List<WateringLog>,
    val nutrientLogs: List<NutrientLog>,
    val checklistItems: List<ChecklistItem>
)

