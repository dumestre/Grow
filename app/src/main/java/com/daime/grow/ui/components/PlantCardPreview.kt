package com.daime.grow.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.daime.grow.domain.model.Plant
import com.daime.grow.ui.theme.GrowTheme

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun PlantCardPreview() {
    GrowTheme {
        PlantCard(
            plant = Plant(
                id = 1,
                name = "Green Apple",
                strain = "Hybrid",
                stage = "Vegetativo",
                medium = "Solo orgânico",
                days = 24,
                photoUri = null,
                nextWateringDate = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            ),
            onClick = {}
        )
    }
}

