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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.ui.components.PlantCard
import com.daime.grow.ui.viewmodel.HomeViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel,
    onOpenDetails: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onAddPlant: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    var plantPendingDelete by remember { mutableStateOf<com.daime.grow.domain.model.Plant?>(null) }
    var orderedPlants by remember { mutableStateOf(state.plants) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dropIndex by remember { mutableStateOf<Int?>(null) }
    var draggedPlantId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var draggedCardBounds by remember { mutableStateOf<Rect?>(null) }
    var trashFabBounds by remember { mutableStateOf<Rect?>(null) }
    val reorderStepXPx = with(LocalDensity.current) { 170.dp.toPx() }
    val reorderStepYPx = with(LocalDensity.current) { 190.dp.toPx() }
    val isDragging = draggedIndex != null
    val isOverTrash = isDragging &&
        draggedCardBounds != null &&
        trashFabBounds != null &&
        draggedCardBounds!!.overlaps(
            Rect(
                left = trashFabBounds!!.left - 24f,
                top = trashFabBounds!!.top - 24f,
                right = trashFabBounds!!.right + 24f,
                bottom = trashFabBounds!!.bottom + 24f
            )
        )
    val draggedScale by animateFloatAsState(targetValue = if (isOverTrash) 0.68f else 1f, label = "dragged-scale")
    val homeFabBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp

    LaunchedEffect(state.plants) {
        val currentIds = state.plants.map { it.id }.toSet()
        orderedPlants = orderedPlants.filter { it.id in currentIds }
        val knownIds = orderedPlants.map { it.id }.toSet()
        val newItems = state.plants.filter { it.id !in knownIds }
        if (newItems.isNotEmpty()) {
            orderedPlants = orderedPlants + newItems
        }
        if (draggedPlantId != null && draggedPlantId !in currentIds) {
            draggedPlantId = null
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
            }
            val plantsToRender = orderedPlants
            val dragFromIndex = draggedIndex
            val dragToIndex = dropIndex
            val previewPlants = if (
                isDragging &&
                draggedPlantId != null &&
                dragFromIndex != null &&
                dragToIndex != null &&
                dragFromIndex in plantsToRender.indices &&
                dragToIndex in plantsToRender.indices
            ) {
                plantsToRender.toMutableList().apply {
                    val dragged = removeAt(dragFromIndex)
                    add(dragToIndex, dragged)
                }
            } else {
                plantsToRender
            }
            if (plantsToRender.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = stringResource(R.string.home_empty_state),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                itemsIndexed(previewPlants, key = { _, plant -> plant.id }) { index, plant ->
                    val isDraggedItem = plant.id == draggedPlantId
                    val fromIndex = draggedIndex
                    val toIndex = dropIndex
                    val dragCompensation = if (
                        isDragging &&
                        isDraggedItem &&
                        fromIndex != null &&
                        toIndex != null
                    ) {
                        val fromCol = fromIndex % 2
                        val toCol = toIndex % 2
                        val fromRow = fromIndex / 2
                        val toRow = toIndex / 2
                        IntOffset(
                            x = ((toCol - fromCol) * reorderStepXPx).roundToInt(),
                            y = ((toRow - fromRow) * reorderStepYPx).roundToInt()
                        )
                    } else {
                        IntOffset.Zero
                    }
                    PlantCard(
                        plant = plant,
                        onClick = { onOpenDetails(plant.id) },
                        onLongPress = null,
                        onDeleteClick = { plantPendingDelete = plant },
                        isEditing = isDragging,
                        isShaking = isDragging && isDraggedItem,
                        isSelected = isDraggedItem,
                        isDropTarget = isDragging && !isDraggedItem && dropIndex == index,
                        modifier = Modifier
                            .then(
                                if (isDragging && isDraggedItem) {
                                    Modifier.offset {
                                        IntOffset(
                                            x = dragOffsetX.roundToInt() - dragCompensation.x,
                                            y = dragOffsetY.roundToInt() - dragCompensation.y
                                        )
                                    }
                                        .graphicsLayer {
                                            scaleX = draggedScale
                                            scaleY = draggedScale
                                        }
                                        .zIndex(2f)
                                        .onGloballyPositioned { coordinates ->
                                            draggedCardBounds = coordinates.boundsInRoot()
                                        }
                                } else {
                                    Modifier
                                }
                            )
                            .pointerInput(plant.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        val currentIndex = orderedPlants.indexOfFirst { it.id == plant.id }
                                        if (currentIndex == -1) return@detectDragGesturesAfterLongPress
                                        draggedIndex = currentIndex
                                        dropIndex = currentIndex
                                        draggedPlantId = plant.id
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        val draggedId = draggedPlantId
                                        if (isOverTrash && draggedId != null) {
                                            viewModel.deletePlantImmediately(draggedId)
                                            orderedPlants = orderedPlants.filterNot { it.id == draggedId }
                                        } else {
                                            val from = draggedIndex
                                            val to = dropIndex
                                            if (
                                                from != null &&
                                                to != null &&
                                                from != to &&
                                                from in orderedPlants.indices &&
                                                to in orderedPlants.indices
                                            ) {
                                                orderedPlants = orderedPlants.toMutableList().apply {
                                                    val moved = removeAt(from)
                                                    add(to, moved)
                                                }
                                            }
                                        }
                                        if (orderedPlants.isNotEmpty()) {
                                            viewModel.updatePlantsOrder(orderedPlants.map { it.id })
                                        }
                                        draggedIndex = null
                                        dropIndex = null
                                        draggedPlantId = null
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        draggedCardBounds = null
                                    },
                                    onDragCancel = {
                                        draggedIndex = null
                                        dropIndex = null
                                        draggedPlantId = null
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        draggedCardBounds = null
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (orderedPlants.isEmpty()) return@detectDragGesturesAfterLongPress
                                        change.consume()
                                        val from = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                        dragOffsetX += dragAmount.x
                                        dragOffsetY += dragAmount.y

                                        val colShift = (dragOffsetX / reorderStepXPx).roundToInt()
                                        val rowShift = (dragOffsetY / reorderStepYPx).roundToInt()
                                        val target = (from + colShift + (rowShift * 2)).coerceIn(0, orderedPlants.lastIndex)
                                        dropIndex = target
                                    }
                                )
                            }
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

        FloatingActionButton(
            onClick = {
                if (isDragging) return@FloatingActionButton
                onAddPlant()
            },
            containerColor = if (isDragging) Color(0xFFC62828) else androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = homeFabBottomPadding)
                .onGloballyPositioned { coordinates ->
                    trashFabBounds = coordinates.boundsInRoot()
                }
                .graphicsLayer {
                    if (isDragging) {
                        scaleX = if (isOverTrash) 1.12f else 1f
                        scaleY = if (isOverTrash) 1.12f else 1f
                    }
                }
        ) {
            Icon(
                imageVector = if (isDragging) Icons.Rounded.Delete else Icons.Default.Add,
                contentDescription = if (isDragging) {
                    stringResource(R.string.home_delete_confirm)
                } else {
                    stringResource(R.string.fab_add_plant)
                }
            )
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
    }
}
