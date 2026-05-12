package com.daime.grow.ui.screen.home

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.ui.components.PlantCard
import com.daime.grow.ui.theme.Poppins
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
    onOpenTips: () -> Unit = {},
    externalIsDragging: Boolean = false,
    onDraggingChanged: (Boolean) -> Unit = {},
    externalTrashBounds: Rect? = null
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val columnsCount = if (isTablet) 4 else 2

    var hasPlayedSound by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.plants) {
        if (!hasPlayedSound && state.plants.isNotEmpty()) {
            playOpeningSound(context)
            hasPlayedSound = true
        }
    }

    var plantPendingDelete by remember { mutableStateOf<com.daime.grow.domain.model.Plant?>(null) }
    var orderedPlants by remember { mutableStateOf(state.plants) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dropIndex by remember { mutableStateOf<Int?>(null) }
    var draggedPlantId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var draggedCardBounds by remember { mutableStateOf<Rect?>(null) }

    val reorderStepXPx = with(LocalDensity.current) { (configuration.screenWidthDp / columnsCount).dp.toPx() }
    val reorderStepYPx = with(LocalDensity.current) { 190.dp.toPx() }
    
    val isDragging = draggedIndex != null
    
    LaunchedEffect(isDragging) {
        onDraggingChanged(isDragging)
    }

    val isOverTrash by remember(isDragging, externalTrashBounds) {
        androidx.compose.runtime.derivedStateOf {
            val bounds = draggedCardBounds
            isDragging &&
            bounds != null &&
            externalTrashBounds != null &&
            bounds.overlaps(
                Rect(
                    left = externalTrashBounds.left - 24f,
                    top = externalTrashBounds.top - 24f,
                    right = externalTrashBounds.right + 24f,
                    bottom = externalTrashBounds.bottom + 24f
                )
            )
        }
    }
    val draggedScale by animateFloatAsState(targetValue = if (isOverTrash) 0.5f else 1f, label = "dragged-scale")

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
                            placeholder = { Text(stringResource(R.string.home_search_label), style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            textStyle = MaterialTheme.typography.bodyMedium,
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
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isTablet) 32.dp else 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 64.dp
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
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = Poppins,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            )
                            Text(
                                stringResource(R.string.home_focus_subtitle),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Poppins
                                )
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
                    val from = draggedIndex ?: -1
                    val to = dropIndex ?: -1
                    if (from != -1 && to != -1) {
                        orderedPlants.toMutableList().apply {
                            val dragged = removeAt(from)
                            add(to, dragged)
                        }
                    } else orderedPlants
                } else {
                    orderedPlants
                }

                if (state.isLoading) {
                    item(span = { GridItemSpan(columnsCount) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (previewPlants.isEmpty()) {
                    item(span = { GridItemSpan(columnsCount) }) {
                        AnimatedEmptyState(
                            message = stringResource(R.string.home_empty_state),
                            onAddPlant = onAddPlant
                        )
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
                            .animateItem()
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
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggedIndex = currentIndex
                                        dropIndex = currentIndex
                                        draggedPlantId = plant.id
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        val draggedId = draggedPlantId
                                        val from = draggedIndex
                                        val to = dropIndex
                                        if (isOverTrash && draggedId != null) {
                                            viewModel.deletePlantImmediately(draggedId)
                                            orderedPlants = orderedPlants.filterNot { it.id == draggedId }
                                        } else if (from != null && to != null && from != to) {
                                            orderedPlants = orderedPlants.toMutableList().apply {
                                                val moved = removeAt(from)
                                                add(to, moved)
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

private fun playOpeningSound(context: android.content.Context) {
    try {
        MediaPlayer.create(context, R.raw.open)?.apply {
            setOnCompletionListener { release() }
            start()
        }
    } catch (e: Exception) {
        Log.e("HomeScreen", "Erro ao tocar som de abertura", e)
    }
}

@Composable
fun AnimatedEmptyState(message: String, onAddPlant: () -> Unit) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "empty_state")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "eco_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "eco_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Eco,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.material3.Button(onClick = onAddPlant) {
            Text("Adicionar Primeira Planta")
        }
    }
}
