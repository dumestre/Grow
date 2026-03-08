package com.daime.grow.ui.screen.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.ui.components.RoundedBackButton
import com.daime.grow.ui.viewmodel.PlantDetailUiEvent
import com.daime.grow.ui.viewmodel.PlantDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantDetailScreen(
    innerPadding: PaddingValues,
    viewModel: PlantDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val details = state.details
    val snackbarHostState = remember { SnackbarHostState() }
    val wateringInvalidMessage = stringResource(R.string.detail_watering_invalid)
    val wateringSavedMessage = stringResource(R.string.detail_watering_saved)
    val nutrientsInvalidMessage = stringResource(R.string.detail_nutrients_invalid)
    val nutrientsSavedMessage = stringResource(R.string.detail_nutrients_saved)
    val stageUpdatedMessage = stringResource(R.string.detail_stage_updated)
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PlantDetailUiEvent.WateringInvalid -> snackbarHostState.showSnackbar(wateringInvalidMessage)
                PlantDetailUiEvent.WateringSaved -> snackbarHostState.showSnackbar(wateringSavedMessage)
                PlantDetailUiEvent.NutrientsInvalid -> snackbarHostState.showSnackbar(nutrientsInvalidMessage)
                PlantDetailUiEvent.NutrientsSaved -> snackbarHostState.showSnackbar(nutrientsSavedMessage)
                PlantDetailUiEvent.StageUpdated -> snackbarHostState.showSnackbar(stageUpdatedMessage)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(details?.plant?.name ?: stringResource(R.string.detail_title_fallback)) },
                navigationIcon = { RoundedBackButton(onClick = onBack) },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (details == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text(stringResource(R.string.detail_loading), modifier = Modifier.padding(16.dp))
            }
            return@Scaffold
        }

        val phaseOrder = listOf(PlantStage.SEEDLING, PlantStage.VEGETATIVE, PlantStage.FLOWER)
        val checklistByPhase = details.checklistItems
            .groupBy { it.phase }
            .toList()
            .sortedBy { entry ->
                val idx = phaseOrder.indexOf(entry.first)
                if (idx == -1) Int.MAX_VALUE else idx
            }
        var expandedPhases by remember(details.plant.id) {
            mutableStateOf(setOf(details.plant.stage))
        }
        var expandedWatering by remember(details.plant.id) { mutableStateOf(false) }
        var expandedNutrients by remember(details.plant.id) { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            item {
                DetailAccentCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.detail_current_phase, details.plant.stage),
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlantStage.entries.forEach { stage ->
                                FilterChip(
                                    selected = details.plant.stage == stage,
                                    onClick = { viewModel.updatePlantStage(stage) },
                                    label = { Text(stage) }
                                )
                            }
                        }
                        Text(stringResource(R.string.detail_strain, details.plant.strain))
                        Text(stringResource(R.string.detail_medium, details.plant.medium))
                        Text(stringResource(R.string.detail_days, details.plant.days.toString()))
                    }
                }
            }

            item {
                DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.detail_quick_actions_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.detail_quick_actions_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                stringResource(R.string.detail_quick_action_watering) to "Rega",
                                stringResource(R.string.detail_quick_action_pruning) to "Poda",
                                stringResource(R.string.detail_quick_action_transplant) to "Transplante",
                                stringResource(R.string.detail_quick_action_flush) to "Flush",
                                stringResource(R.string.detail_quick_action_harvest) to "Colheita"
                            ).forEach { (label, eventType) ->
                                AssistChip(
                                    onClick = { viewModel.addQuickAction(eventType, "Ação rápida: $label") },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedWatering = !expandedWatering },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.detail_watering))
                            Icon(
                                imageVector = if (expandedWatering) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                        if (expandedWatering) {
                            OutlinedTextField(
                                value = state.wateringVolume,
                                onValueChange = viewModel::onWateringVolumeChange,
                                label = { Text(stringResource(R.string.detail_watering_volume)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.wateringInterval,
                                onValueChange = viewModel::onWateringIntervalChange,
                                label = { Text(stringResource(R.string.detail_watering_interval)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.wateringSubstrate,
                                onValueChange = viewModel::onWateringSubstrateChange,
                                label = { Text(stringResource(R.string.detail_substrate)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Button(onClick = viewModel::saveWatering, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.detail_save_watering))
                            }
                        }
                    }
                }
            }

            item {
                DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedNutrients = !expandedNutrients },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.detail_nutrients))
                            Icon(
                                imageVector = if (expandedNutrients) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                        if (expandedNutrients) {
                            OutlinedTextField(
                                value = state.nutrientWeek,
                                onValueChange = viewModel::onNutrientWeekChange,
                                label = { Text(stringResource(R.string.detail_week)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.nutrientEc,
                                onValueChange = viewModel::onNutrientEcChange,
                                label = { Text(stringResource(R.string.detail_ec)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.nutrientPh,
                                onValueChange = viewModel::onNutrientPhChange,
                                label = { Text(stringResource(R.string.detail_ph)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Button(onClick = viewModel::saveNutrients, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.detail_save_nutrients))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.detail_checklist_by_stage),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.detail_checklist_desc),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            items(checklistByPhase, key = { it.first }) { phaseGroup ->
                val phase = phaseGroup.first
                val phaseItems = phaseGroup.second
                val doneCount = phaseItems.count { it.done }
                val isExpanded = phase in expandedPhases
                DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedPhases = if (isExpanded) expandedPhases - phase else expandedPhases + phase
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.detail_phase, phase),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.detail_checklist_completed, doneCount, phaseItems.size),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                        if (isExpanded) {
                            phaseItems.forEachIndexed { index, checklistItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        checklistItem.task,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(
                                        checked = checklistItem.done,
                                        onCheckedChange = { viewModel.toggleChecklist(checklistItem, it) }
                                    )
                                }
                                if (index < phaseItems.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.detail_timeline), style = MaterialTheme.typography.titleMedium)
            }

            items(details.events, key = { it.id }) { event ->
                DetailAccentCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(event.type, style = MaterialTheme.typography.titleMedium)
                        if (event.note.isNotBlank()) Text(event.note)
                        Text(
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(event.createdAt)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailAccentCard(
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.CardColors = CardDefaults.cardColors(),
    elevation: androidx.compose.material3.CardElevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    content: @Composable () -> Unit
) {
    val accent = MaterialTheme.colorScheme.tertiary
    Card(
        modifier = modifier,
        colors = colors,
        elevation = elevation,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent.copy(alpha = 0.9f),
                                accent.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    }
}
