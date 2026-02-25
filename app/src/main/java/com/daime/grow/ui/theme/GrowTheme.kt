package com.daime.grow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun GrowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GrowColorScheme,
        typography = GrowTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp)
        ),
        content = content
    )
}

