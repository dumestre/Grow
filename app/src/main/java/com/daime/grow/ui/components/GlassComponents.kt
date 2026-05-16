package com.daime.grow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.blur.blurEffect

import com.daime.grow.ui.util.DeviceUtils

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    hazeState: HazeState? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.clip(shape)
    ) {
        // Camada de Fundo (Haze ou Background fixo)
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (hazeState != null && DeviceUtils.supportsBlurEffects) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurEffect {
                                blurRadius = 24.dp
                            }
                        }
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    }
                )
                .background(
                    if (DeviceUtils.supportsBlurEffects) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    } else {
                        Color.Transparent
                    }
                ) // Camada de cor base
        )

        // Camada de Borda e Brilho (Separada para não afetar o conteúdo)
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = shape
                )
        )

        // Conteúdo (Sem efeito haze/blur)
        content()
    }
}
