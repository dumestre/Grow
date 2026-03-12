package com.daime.grow.ui.screen.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                PlantDetailUiEvent.WateringInvalid -> "Volume ou intervalo inválido"
                PlantDetailUiEvent.WateringSaved -> "Rega salva com sucesso!"
                PlantDetailUiEvent.NutrientsInvalid -> "Dados de nutrientes inválidos"
                PlantDetailUiEvent.NutrientsSaved -> "Nutrientes salvos com sucesso!"
                PlantDetailUiEvent.StageUpdated -> "Fase da planta atualizada"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(details?.plant?.name ?: "Detalhes") },
                navigationIcon = { RoundedBackButton(onClick = onBack) },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (details == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text("Carregando detalhes...", modifier = Modifier.padding(16.dp))
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
                item { InfoSection(details, viewModel) }
                item { QuickActionsSection(viewModel) }
                item { WateringSection(state, viewModel, expandedWatering) { expandedWatering = it } }
                item { NutrientSection(state, viewModel, expandedNutrients) { expandedNutrients = it } }
                item { 
                    Column {
                        Text("Checklist por Estágio", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        ChecklistSection(checklistByPhase, expandedPhases, viewModel) { expandedPhases = it }
                    }
                }
                item { Text("Linha do Tempo", style = MaterialTheme.typography.titleMedium) }
                items(details.events) { event -> TimelineItem(event) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoSection(details: com.daime.grow.domain.model.PlantDetails, viewModel: PlantDetailViewModel) {
    DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estágio Atual: ${details.plant.stage}", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlantStage.entries.forEach { stage ->
                    FilterChip(
                        selected = details.plant.stage == stage,
                        onClick = { viewModel.updatePlantStage(stage) },
                        label = { Text(stage) }
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Strain: ${details.plant.strain.ifEmpty { "Não informada" }}")
            Text("Substrato: ${details.plant.medium.ifEmpty { "Não informado" }}")
            Text("Idade: ${details.plant.days} dias")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsSection(viewModel: PlantDetailViewModel) {
    DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ações Rápidas", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Rega", "Poda", "Transplante", "Flush", "Colheita").forEach { label ->
                    AssistChip(onClick = { viewModel.addQuickAction(label, "Ação rápida: $label") }, label = { Text(label) })
                }
            }
        }
    }
}

@Composable
private fun WateringSection(state: com.daime.grow.ui.viewmodel.PlantDetailUiState, viewModel: PlantDetailViewModel, expanded: Boolean, onExpand: (Boolean) -> Unit) {
    DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onExpand(!expanded) }, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Configurar Rega", style = MaterialTheme.typography.titleSmall)
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null)
            }
            if (expanded) {
                OutlinedTextField(value = state.wateringVolume, onValueChange = viewModel::onWateringVolumeChange, label = { Text("Volume (L/ml)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.wateringInterval, onValueChange = viewModel::onWateringIntervalChange, label = { Text("Intervalo (Horas)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = viewModel::saveWatering, modifier = Modifier.fillMaxWidth()) { Text("Salvar Rega") }
            }
        }
    }
}

@Composable
private fun NutrientSection(state: com.daime.grow.ui.viewmodel.PlantDetailUiState, viewModel: PlantDetailViewModel, expanded: Boolean, onExpand: (Boolean) -> Unit) {
    DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onExpand(!expanded) }, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Nutrientes / Solo", style = MaterialTheme.typography.titleSmall)
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null)
            }
            if (expanded) {
                OutlinedTextField(value = state.nutrientPh, onValueChange = viewModel::onNutrientPhChange, label = { Text("pH") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.nutrientEc, onValueChange = viewModel::onNutrientEcChange, label = { Text("EC/PPM") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = viewModel::saveNutrients, modifier = Modifier.fillMaxWidth()) { Text("Salvar Dados") }
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
            DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.animateContentSize()) {
                    ListItem(
                        headlineContent = { Text("Fase: $phase") },
                        supportingContent = { Text("${items.count { it.done }}/${items.size} concluídos") },
                        trailingContent = { Icon(if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null) },
                        modifier = Modifier.clickable { 
                            onExpandToggle(if (isExpanded) expandedPhases - phase else expandedPhases + phase)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (isExpanded) {
                        items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.task, modifier = Modifier.weight(1f))
                                Checkbox(checked = item.done, onCheckedChange = { viewModel.toggleChecklist(item, it) })
                            }
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
        Text("Linha do Tempo", style = MaterialTheme.typography.titleMedium)
        if (events.isEmpty()) {
            Text("Nenhum evento registrado ainda.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
        } else {
            events.forEach { TimelineItem(it) }
        }
    }
}

@Composable
private fun TimelineItem(event: com.daime.grow.domain.model.PlantEvent) {
    DetailAccentCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(event.type, style = MaterialTheme.typography.titleSmall)
            if (event.note.isNotBlank()) Text(event.note, style = MaterialTheme.typography.bodyMedium)
            Text(
                SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(event.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailAccentCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column { content() }
    }
}
