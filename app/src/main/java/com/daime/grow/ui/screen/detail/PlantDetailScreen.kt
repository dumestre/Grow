package com.daime.grow.ui.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
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

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PlantDetailUiEvent.WateringInvalid -> snackbarHostState.showSnackbar(wateringInvalidMessage)
                PlantDetailUiEvent.WateringSaved -> snackbarHostState.showSnackbar(wateringSavedMessage)
                PlantDetailUiEvent.NutrientsInvalid -> snackbarHostState.showSnackbar(nutrientsInvalidMessage)
                PlantDetailUiEvent.NutrientsSaved -> snackbarHostState.showSnackbar(nutrientsSavedMessage)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            CenterAlignedTopAppBar(
                title = { Text(details?.plant?.name ?: stringResource(R.string.detail_title_fallback)) },
                navigationIcon = { RoundedBackButton(onClick = onBack) }
            )

            if (details == null) {
                Text(stringResource(R.string.detail_loading))
                return
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.detail_phase, details.plant.stage))
                        Text(stringResource(R.string.detail_strain, details.plant.strain))
                        Text(stringResource(R.string.detail_medium, details.plant.medium))
                        Text(stringResource(R.string.detail_days, details.plant.days.toString()))
                    }
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Regar", "Podar", "Transplante", "Flush", "Colheita").forEach { action ->
                        AssistChip(
                            onClick = { viewModel.addQuickAction(action) },
                            label = { Text(action) }
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.detail_watering))
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

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.detail_nutrients))
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

            item {
                Text(
                    stringResource(R.string.detail_checklist_by_stage),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
            }

            items(details.checklistItems, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.task)
                            Text(item.phase)
                        }
                        Checkbox(
                            checked = item.done,
                            onCheckedChange = { viewModel.toggleChecklist(item.id, it) }
                        )
                    }
                }
            }

            item {
                Text(stringResource(R.string.detail_timeline), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            }

            items(details.events, key = { it.id }) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(event.type, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        if (event.note.isNotBlank()) Text(event.note)
                        Text(
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(event.createdAt)),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

