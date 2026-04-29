package com.daime.grow.ui.screen.ppfd

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.daime.grow.domain.model.LightSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PPFDScreen(
    onBack: () -> Unit,
    viewModel: PPFDViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startMeasuring()
        onDispose { viewModel.stopMeasuring() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medidor PPFD", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!uiState.isSensorAvailable) {
                SensorUnavailableState()
            } else {
                MeasurementDisplay(uiState.ppfd, uiState.lux)
                
                SourceSelectionCard(
                    selectedSource = uiState.selectedSource,
                    onSourceSelected = viewModel::updateLightSource
                )

                CalibrationCard(
                    multiplier = uiState.calibrationMultiplier,
                    onMultiplierChange = viewModel::updateCalibration
                )

                AccuracyTipsCard()
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MeasurementDisplay(ppfd: Double, lux: Float) {
    val color by animateColorAsState(
        targetValue = getPPFDColor(ppfd),
        label = "PPFDColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PPFD ATUAL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "%.0f".format(ppfd),
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            
            Text(
                text = "μmol/m²/s",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = color.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Text(
                    text = getPPFDClassification(ppfd),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Sensor: ${"%.0f".format(lux)} Lux",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SourceSelectionCard(
    selectedSource: LightSource,
    onSourceSelected: (LightSource) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Fonte de Luz",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            LightSource.entries.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSourceSelected(source) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (source == selectedSource),
                        onClick = { onSourceSelected(source) }
                    )
                    Text(
                        text = source.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibrationCard(
    multiplier: Float,
    onMultiplierChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calibração Manual",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(multiplier * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Slider(
                value = multiplier,
                onValueChange = onMultiplierChange,
                valueRange = 0.5f..2.0f,
                steps = 15
            )
            
            Text(
                text = "Ajuste se tiver um medidor PAR profissional para comparar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AccuracyTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = Color(0xFFEF6C00)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Dica de Precisão Profissional",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Coloque uma tira de papel sulfite branco (80g) sobre o sensor frontal para criar um difusor. Isso melhora drasticamente a precisão da leitura em barracas de cultivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D4037)
                )
            }
        }
    }
}

@Composable
private fun SensorUnavailableState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sensor de Luz não encontrado",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Seu dispositivo não possui o hardware necessário para medição de luz ambiente.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun getPPFDColor(ppfd: Double): Color {
    return when {
        ppfd < 200 -> Color(0xFF81C784) // Verde claro
        ppfd < 600 -> Color(0xFF4CAF50) // Verde
        ppfd < 900 -> Color(0xFFFFB300) // Amarelo/Laranja
        ppfd < 1200 -> Color(0xFFF44336) // Vermelho
        else -> Color(0xFFD32F2F) // Vermelho escuro
    }
}

fun getPPFDClassification(ppfd: Double): String {
    return when {
        ppfd < 200 -> "Clones / Mudas"
        ppfd < 400 -> "Vegetativo Inicial"
        ppfd < 600 -> "Vegetativo"
        ppfd < 900 -> "Floração"
        ppfd < 1200 -> "Floração Intensa"
        else -> "Extremo (Estresse)"
    }
}
