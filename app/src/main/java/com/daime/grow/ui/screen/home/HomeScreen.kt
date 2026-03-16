package com.daime.grow.ui.screen.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.ui.components.PlantCard
import com.daime.grow.ui.viewmodel.HomeViewModel
import com.daime.grow.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenDetails: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onAddPlant: () -> Unit,
    externalIsDragging: Boolean = false,
    onDraggingChanged: (Boolean) -> Unit = {},
    externalTrashBounds: Rect? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val columnsCount = if (isTablet) 4 else 2

    var plantPendingDelete by remember { mutableStateOf<com.daime.grow.domain.model.Plant?>(null) }
    var orderedPlants by remember { mutableStateOf(state.plants) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dropIndex by remember { mutableStateOf<Int?>(null) }
    var draggedPlantId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var draggedCardBounds by remember { mutableStateOf<Rect?>(null) }

    val reorderStepXPx = with(LocalDensity.current) { (configuration.screenWidthDp / columnsCount).dp.toPx() }
    val reorderStepYPx = with(LocalDensity.current) { 190.dp.toPx() }
    
    val isDragging = draggedIndex != null
    
    LaunchedEffect(isDragging) {
        onDraggingChanged(isDragging)
    }

    val isOverTrash = isDragging &&
        draggedCardBounds != null &&
        externalTrashBounds != null &&
        draggedCardBounds!!.overlaps(
            Rect(
                left = externalTrashBounds.left - 24f,
                top = externalTrashBounds.top - 24f,
                right = externalTrashBounds.right + 24f,
                bottom = externalTrashBounds.bottom + 24f
            )
        )
    val draggedScale by animateFloatAsState(targetValue = if (isOverTrash) 0.68f else 1f, label = "dragged-scale")

    LaunchedEffect(state.plants) {
        val currentIds = state.plants.map { it.id }.toSet()
        orderedPlants = orderedPlants.filter { it.id in currentIds }
        val knownIds = orderedPlants.map { it.id }.toSet()
        val newItems = state.plants.filter { it.id !in knownIds }
        if (newItems.isNotEmpty()) {
            orderedPlants = orderedPlants + newItems
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp)
                            .then(if (isTablet) Modifier.widthIn(max = 600.dp) else Modifier),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.home_search_label), style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnsCount),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 32.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp // Uso o innerPadding do Root + margem extra
            )
        ) {
            item(span = { GridItemSpan(columnsCount) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.home_focus_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            stringResource(R.string.home_focus_subtitle),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item(span = { GridItemSpan(columnsCount) }) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
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

            item(span = { GridItemSpan(columnsCount) }) {
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
            
            val previewPlants = if (isDragging && draggedIndex != null && dropIndex != null) {
                orderedPlants.toMutableList().apply {
                    val dragged = removeAt(draggedIndex!!)
                    add(dropIndex!!, dragged)
                }
            } else {
                orderedPlants
            }

            if (previewPlants.isEmpty()) {
                item(span = { GridItemSpan(columnsCount) }) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.home_empty_state),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                itemsIndexed(previewPlants, key = { _, plant -> plant.id }) { index, plant ->
                    val isDraggedItem = plant.id == draggedPlantId
                    val fromIndex = draggedIndex
                    val toIndex = dropIndex
                    val dragCompensation = if (isDragging && isDraggedItem && fromIndex != null && toIndex != null) {
                        val fromCol = fromIndex % columnsCount
                        val toCol = toIndex % columnsCount
                        val fromRow = fromIndex / columnsCount
                        val toRow = toIndex / columnsCount
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
                                        .zIndex(100f)
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
                                        } else if (draggedIndex != null && dropIndex != null && draggedIndex != dropIndex) {
                                            orderedPlants = orderedPlants.toMutableList().apply {
                                                val moved = removeAt(draggedIndex!!)
                                                add(dropIndex!!, moved)
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
                                        val target = (from + colShift + (rowShift * columnsCount)).coerceIn(0, orderedPlants.lastIndex)
                                        dropIndex = target
                                    }
                                )
                            }
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
                        Text(stringResource(R.string.home_delete_confirm), color = Color.Red)
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
