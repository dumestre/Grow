package com.daime.grow.ui.screen.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
    onBack: () -> Unit,
    onNavigateToPosColheta: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val details = state.details
    val snackbarHostState = remember { SnackbarHostState() }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updatePhoto(it.toString()) }
    }

    var showHarvestDialog by remember { mutableStateOf(false) }

    if (showHarvestDialog && details != null) {
        AlertDialog(
            onDismissRequest = { showHarvestDialog = false },
            title = {
                Text(
                    text = "Confirmar Colheita",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Deseja marcar \"${details.plant.name}\" como colhida?")
                    Text(
                        text = "A planta será enviada para a tela de secagem/ Cura.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.harvestPlant()
                        showHarvestDialog = false
                        onNavigateToPosColheta()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHarvestDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                PlantDetailUiEvent.WateringInvalid -> "Volume ou intervalo inválido"
                PlantDetailUiEvent.WateringSaved -> "Rega salva com sucesso!"
                PlantDetailUiEvent.NutrientsInvalid -> "Dados de nutrientes inválidos"
                PlantDetailUiEvent.NutrientsSaved -> "Nutrientes salvos com sucesso!"
                PlantDetailUiEvent.StageUpdated -> "Fase da planta atualizada"
                PlantDetailUiEvent.PhotoUpdated -> "Foto atualizada com sucesso!"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(details?.plant?.name ?: stringResource(R.string.detail_title_fallback)) },
                navigationIcon = { RoundedBackButton(onClick = onBack) },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .height(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF4081), // Rosa
                                    Color(0xFF9C27B0)  // Roxo
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            showHarvestDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.detail_harvest_button),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
        
        var expandedPhases by remember(details.plant.id) { mutableStateOf(setOf(details.plant.stage)) }
        var expandedWatering by remember(details.plant.id) { mutableStateOf(false) }
        var expandedNutrients by remember(details.plant.id) { mutableStateOf(false) }

        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PhotoSection(details, onUpdatePhoto = { imagePickerLauncher.launch("image/*") })
                    InfoSection(details, viewModel)
                    QuickActionsSection(viewModel)
                    WateringSection(state, viewModel, expandedWatering) { expandedWatering = it }
                    NutrientSection(state, viewModel, expandedNutrients) { expandedNutrients = it }
                }

                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ChecklistSection(checklistByPhase, expandedPhases, viewModel) { expandedPhases = it }
                    TimelineSection(details.events)
                }
            }
        } else {
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
                item { PhotoSection(details, onUpdatePhoto = { imagePickerLauncher.launch("image/*") }) }
                item { InfoSection(details, viewModel) }
                item { QuickActionsSection(viewModel) }
                item { WateringSection(state, viewModel, expandedWatering) { expandedWatering = it } }
                item { NutrientSection(state, viewModel, expandedNutrients) { expandedNutrients = it } }
                item { 
                    Column {
                        Text(stringResource(R.string.detail_checklist_by_stage), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        ChecklistSection(checklistByPhase, expandedPhases, viewModel) { expandedPhases = it }
                    }
                }
                item { Text(stringResource(R.string.detail_timeline), style = MaterialTheme.typography.titleMedium) }
                items(details.events) { event -> TimelineItem(event) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoSection(details: com.daime.grow.domain.model.PlantDetails, viewModel: PlantDetailViewModel) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsSection(viewModel: PlantDetailViewModel) {
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
                    stringResource(R.string.detail_quick_action_flush) to "Flush"
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

@Composable
private fun WateringSection(state: com.daime.grow.ui.viewmodel.PlantDetailUiState, viewModel: PlantDetailViewModel, expanded: Boolean, onExpand: (Boolean) -> Unit) {
    DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onExpand(!expanded) }, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.detail_watering))
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null)
            }
            if (expanded) {
                OutlinedTextField(value = state.wateringVolume, onValueChange = viewModel::onWateringVolumeChange, label = { Text(stringResource(R.string.detail_watering_volume)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = state.wateringInterval, onValueChange = viewModel::onWateringIntervalChange, label = { Text(stringResource(R.string.detail_watering_interval)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = state.wateringSubstrate, onValueChange = viewModel::onWateringSubstrateChange, label = { Text(stringResource(R.string.detail_substrate)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = viewModel::saveWatering, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.detail_save_watering)) }
            }
        }
    }
}

@Composable
private fun NutrientSection(state: com.daime.grow.ui.viewmodel.PlantDetailUiState, viewModel: PlantDetailViewModel, expanded: Boolean, onExpand: (Boolean) -> Unit) {
    DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onExpand(!expanded) }, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.detail_nutrients))
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null)
            }
            if (expanded) {
                OutlinedTextField(value = state.nutrientWeek, onValueChange = viewModel::onNutrientWeekChange, label = { Text(stringResource(R.string.detail_week)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = state.nutrientEc, onValueChange = viewModel::onNutrientEcChange, label = { Text(stringResource(R.string.detail_ec)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = state.nutrientPh, onValueChange = viewModel::onNutrientPhChange, label = { Text(stringResource(R.string.detail_ph)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = viewModel::saveNutrients, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.detail_save_nutrients)) }
            }
        }
    }
}

@Composable
private fun ChecklistSection(
    checklistByPhase: List<Pair<String, List<com.daime.grow.domain.model.ChecklistItem>>>,
    expandedPhases: Set<String>,
    viewModel: PlantDetailViewModel,
    onExpandToggle: (Set<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        checklistByPhase.forEach { (phase, items) ->
            val isExpanded = phase in expandedPhases
            val doneCount = items.count { it.done }
            DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp).animateContentSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExpandToggle(if (isExpanded) expandedPhases - phase else expandedPhases + phase) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = stringResource(R.string.detail_phase, phase), style = MaterialTheme.typography.titleSmall)
                            Text(text = stringResource(R.string.detail_checklist_completed, doneCount, items.size), style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null)
                    }
                    if (isExpanded) {
                        items.forEachIndexed { index, item ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.task, modifier = Modifier.weight(1f))
                                Checkbox(checked = item.done, onCheckedChange = { viewModel.toggleChecklist(item, it) })
                            }
                            if (index < items.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineSection(events: List<com.daime.grow.domain.model.PlantEvent>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.detail_timeline), style = MaterialTheme.typography.titleMedium)
        if (events.isEmpty()) {
            Text(stringResource(R.string.detail_no_events), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
        } else {
            events.forEach { TimelineItem(it) }
        }
    }
}

@Composable
private fun TimelineItem(event: com.daime.grow.domain.model.PlantEvent) {
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

@Composable
private fun PhotoSection(
    details: com.daime.grow.domain.model.PlantDetails,
    onUpdatePhoto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { onUpdatePhoto() }
        ) {
            if (details.plant.photoUri != null) {
                AsyncImage(
                    model = details.plant.photoUri,
                    contentDescription = "Foto da planta",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Toque para adicionar foto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            FloatingActionButton(
                onClick = onUpdatePhoto,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(40.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Atualizar foto",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailAccentCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}
