package com.daime.grow.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.ui.components.PlantCard
import com.daime.grow.ui.viewmodel.HomeUiEvent
import com.daime.grow.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel,
    onOpenDetails: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    var plantPendingDelete by remember { mutableStateOf<com.daime.grow.domain.model.Plant?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteUndoLabel = stringResource(R.string.home_delete_undo)
    val deletedMessageFormat = stringResource(R.string.home_deleted_message)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.ShowDeleteUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMessageFormat.format(event.plantName),
                        actionLabel = deleteUndoLabel
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 84.dp, top = 96.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            stringResource(R.string.home_focus_title),
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.home_focus_subtitle),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlantStage.filterEntries.forEach { phase ->
                        FilterChip(
                            onClick = { viewModel.onStageChange(phase) },
                            label = { Text(phase) },
                            selected = state.stageFilter == phase
                        )
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                FilterChip(
                    onClick = viewModel::toggleSort,
                    selected = true,
                    leadingIcon = {
                        Icon(
                            imageVector = if (state.sortAscending) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            stringResource(
                                if (state.sortAscending) R.string.home_sort_asc else R.string.home_sort_desc
                            )
                        )
                    }
                )
            }

            if (state.plants.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = stringResource(R.string.home_empty_state),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(state.plants, key = { it.id }) { plant ->
                    PlantCard(
                        plant = plant,
                        onClick = { onOpenDetails(plant.id) },
                        onLongPress = { plantPendingDelete = plant },
                        onDeleteClick = { plantPendingDelete = plant }
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text(stringResource(R.string.home_search_label)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    singleLine = true
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.home_settings)
                    )
                }
            }
        }

        plantPendingDelete?.let { plant ->
            AlertDialog(
                onDismissRequest = { plantPendingDelete = null },
                title = { Text(stringResource(R.string.home_delete_title)) },
                text = { Text(stringResource(R.string.home_delete_text, plant.name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.requestDelete(plant)
                            plantPendingDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.home_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { plantPendingDelete = null }) {
                        Text(stringResource(R.string.home_delete_cancel))
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
