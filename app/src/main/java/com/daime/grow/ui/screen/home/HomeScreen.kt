package com.daime.grow.ui.screen.home

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
import com.daime.grow.domain.model.Plant
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.ui.components.PlantCard
import com.daime.grow.ui.components.shimmerEffect
import com.daime.grow.ui.theme.GrowTheme
import com.daime.grow.ui.viewmodel.HomeViewModel
import com.daime.grow.ui.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
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
    externalTrashBounds: Rect? = null,
    hazeState: HazeState? = null
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val searchFocusRequester = remember { FocusRequester() }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var ignoreNextOutsideTap by remember { mutableStateOf(false) }
    val isSearchScrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 24
        }
    }
    val showCompactSearch = isSearchScrolled && !isSearchExpanded && state.query.isBlank()
    
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val columnsCount = if (isTablet) 4 else 2
    val searchHorizontalPadding = if (isTablet) 32.dp else 16.dp
    val expandedSearchWidth = (configuration.screenWidthDp.dp - (searchHorizontalPadding * 2)).coerceAtMost(600.dp)

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

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            searchFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isSearchScrolled) {
        if (!isSearchScrolled) {
            isSearchExpanded = false
        }
    }

    Scaffold(
        modifier = Modifier
            .then(if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isSearchScrolled, state.query.isBlank()) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Final)
                                    if (event.changes.any { it.changedToUpIgnoreConsumed() }) {
                                        if (ignoreNextOutsideTap) {
                                            ignoreNextOutsideTap = false
                                        } else {
                                            focusManager.clearFocus()
                                        }
                                        if (isSearchScrolled && state.query.isBlank()) {
                                            isSearchExpanded = false
                                        }
                                    }
                                }
                            }
                        }
                        .padding(horizontal = searchHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        top = if (showCompactSearch) 64.dp else 96.dp,
                        bottom = innerPadding.calculateBottomPadding() + 64.dp
                    )
                ) {
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
                    items(if (isTablet) 8 else 6) {
                        HomePlantCardPlaceholder()
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

            HomeSearchAndStageFilters(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                isCompact = showCompactSearch,
                expandedWidth = expandedSearchWidth,
                selectedStage = state.stageFilter,
                onStageChange = viewModel::onStageChange,
                focusRequester = searchFocusRequester,
                onCompactClick = {
                    ignoreNextOutsideTap = true
                    isSearchExpanded = true
                },
                onFocusChanged = { isFocused ->
                    isSearchExpanded = isFocused || !isSearchScrolled || state.query.isNotBlank()
                },
                modifier = Modifier
                    .statusBarsPadding()
                    .offset(y = (-6).dp)
                    .padding(horizontal = searchHorizontalPadding)
                    .align(Alignment.TopStart)
                    .zIndex(10f)
            )
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

@Composable
private fun HomeSearchAndStageFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    isCompact: Boolean,
    expandedWidth: Dp,
    selectedStage: String,
    onStageChange: (String) -> Unit,
    focusRequester: FocusRequester? = null,
    onCompactClick: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isCompact) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            HomeSearchField(
                query = query,
                onQueryChange = onQueryChange,
                isCompact = true,
                expandedWidth = expandedWidth,
                focusRequester = focusRequester,
                onCompactClick = onCompactClick,
                onFocusChanged = onFocusChanged
            )
            HomeStageFilterChips(
                selectedStage = selectedStage,
                onStageChange = onStageChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, top = 2.dp)
            )
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeSearchField(
                query = query,
                onQueryChange = onQueryChange,
                isCompact = false,
                expandedWidth = expandedWidth,
                focusRequester = focusRequester,
                onCompactClick = onCompactClick,
                onFocusChanged = onFocusChanged
            )
            HomeStageFilterChips(
                selectedStage = selectedStage,
                onStageChange = onStageChange,
                modifier = Modifier.width(expandedWidth)
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
    isCompact: Boolean,
    expandedWidth: Dp,
    focusRequester: FocusRequester? = null,
    onCompactClick: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fieldWidth by animateDpAsState(
        targetValue = if (isCompact) 48.dp else expandedWidth,
        label = "home-search-width"
    )
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(if (isCompact) 16.dp else 14.dp)
    val showTextContent = fieldWidth > 56.dp

    Surface(
        modifier = modifier
            .width(fieldWidth)
            .height(48.dp)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                shape = shape
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        tonalElevation = if (isCompact) 4.dp else 2.dp,
        shadowElevation = if (isCompact) 3.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .padding(end = if (showTextContent) 16.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .then(if (isCompact) Modifier.clickable(onClick = onCompactClick) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = if (isCompact) stringResource(R.string.home_search_label) else null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (showTextContent) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                            .onFocusChanged { onFocusChanged(it.isFocused) },
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
}

@Composable
private fun HomePlantCardPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(12.dp))
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index == 3) 0.64f else 1f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .shimmerEffect()
                )
                if (index != 3) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    val now = System.currentTimeMillis()
    val plants = listOf(
        Plant(
            id = 1,
            name = "Green Apple",
            strain = "Hybrid",
            stage = PlantStage.VEGETATIVE,
            medium = "Solo organico",
            days = 24,
            photoUri = null,
            nextWateringDate = now,
            createdAt = now
        ),
        Plant(
            id = 2,
            name = "Purple Kush",
            strain = "Indica",
            stage = PlantStage.FLOWER,
            medium = "Coco",
            days = 51,
            photoUri = null,
            nextWateringDate = now + 86_400_000,
            createdAt = now
        ),
        Plant(
            id = 3,
            name = "Lemon Haze",
            strain = "Sativa",
            stage = PlantStage.SEEDLING,
            medium = "Perlita",
            days = 8,
            photoUri = null,
            nextWateringDate = null,
            createdAt = now
        )
    )

    GrowTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 96.dp, bottom = 96.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    FilterChip(
                        onClick = {},
                        selected = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                contentDescription = null
                            )
                        },
                        label = { Text("Mais antigas") }
                    )
                }
                itemsIndexed(plants, key = { _, plant -> plant.id }) { _, plant ->
                    PlantCard(
                        plant = plant,
                        onClick = {},
                        onDeleteClick = {}
                    )
                }
            }

            HomeSearchAndStageFilters(
                query = "",
                onQueryChange = {},
                isCompact = false,
                expandedWidth = 358.dp,
                selectedStage = PlantStage.ALL,
                onStageChange = {},
                modifier = Modifier
                    .statusBarsPadding()
                    .offset(y = (-6).dp)
                    .padding(horizontal = 16.dp)
                    .align(Alignment.TopStart)
            )
        }
    }
}
