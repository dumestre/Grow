package com.daime.grow.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.daime.grow.R
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.ui.util.DeviceUtils
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.blur.blurEffect
import com.daime.grow.ui.components.AppContentHazeKey

@OptIn(ExperimentalHazeApi::class)
@Composable
fun HomeTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedStage: String,
    onStageChange: (String) -> Unit,
    sortAscending: Boolean,
    onToggleSort: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    // IMPORTANTE: Para evitar crashes e loops (feedback loop), agora usamos um HazeState local 
    // em vez do rootHazeState para componentes que estão dentro do hazeSource.
    // No Android 15, forçamos opacidade total para estabilidade
    var backgroundColor = if (DeviceUtils.supportsBlurEffects) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 0.5.dp,
                color = Color(0xFFFF69B4).copy(alpha = 0.5f), // Rosa fino e suave
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // Camada de Fundo (Blur se suportado, caso contrário Sólido)
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (hazeState != null && DeviceUtils.supportsBlurEffects) {
                        Modifier.hazeEffect(state = hazeState) {
                            canDrawArea = { area -> area.key == AppContentHazeKey }
                            blurEffect {
                                blurRadius = 20.dp
                                noiseFactor = 0f
                                backgroundColor = backgroundColor
                            }
                        }
                    } else {
                        Modifier.background(backgroundColor)
                    }
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeSearchField(
                query = query,
                onQueryChange = onQueryChange,
            )
            HomeStageFilterChips(
                selectedStage = selectedStage,
                onStageChange = onStageChange,
            )
            FilterChip(
                onClick = onToggleSort,
                selected = true,
                leadingIcon = {
                    Icon(
                        imageVector = if (sortAscending) Icons.Rounded.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        stringResource(
                            if (sortAscending) R.string.home_sort_asc else R.string.home_sort_desc
                        ),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    labelColor = MaterialTheme.colorScheme.primary,
                    selectedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = true,
                    borderColor = MaterialTheme.colorScheme.primary,
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HomeStageFilterChips(
    selectedStage: String,
    onStageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PlantStage.filterEntries.forEach { phase ->
                val isSelected = selectedStage == phase
                FilterChip(
                    onClick = { onStageChange(phase) },
                    label = {
                        Text(
                            text = phase,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    selected = isSelected,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        stringResource(R.string.home_search_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
