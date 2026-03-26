package com.daime.grow.ui.screen.poscolheta

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.daime.grow.data.local.entity.HarvestBatchEntity
import com.daime.grow.ui.viewmodel.HarvestBatchWithPhoto
import com.daime.grow.ui.viewmodel.PosColhetaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun PosColhetaScreen(
    innerPadding: PaddingValues,
    viewModel: PosColhetaViewModel
) {
    val tabs = listOf("Secagem", "Cura")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val dryingBatches by viewModel.dryingBatches.collectAsStateWithLifecycle()
    val curingBatches by viewModel.curingBatches.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> DryingTabContent(
                    batches = dryingBatches,
                    emptyMessage = "Nenhuma planta em secagem.\nColha suas plantas quando estiverem prontas!",
                    onStartCure = viewModel::startCuring,
                    onBurp = viewModel::burp
                )
                1 -> CuringTabContent(
                    batches = curingBatches,
                    emptyMessage = "Nenhuma planta em cura.\nFinalize a secagem para começar a cura!",
                    onBurp = viewModel::burp
                )
            }
        }
    }
}

@Composable
private fun DryingTabContent(
    batches: List<HarvestBatchWithPhoto>,
    emptyMessage: String,
    onStartCure: (Long) -> Unit,
    onBurp: (Long) -> Unit
) {
    if (batches.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.LocalFlorist,
            message = emptyMessage
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(batches, key = { it.batch.id }) { batchWithPhoto ->
                DryingBatchCard(
                    batchWithPhoto = batchWithPhoto,
                    onStartCure = { onStartCure(batchWithPhoto.batch.id) },
                    onBurp = { onBurp(batchWithPhoto.batch.id) }
                )
            }
        }
    }
}

@Composable
private fun CuringTabContent(
    batches: List<HarvestBatchWithPhoto>,
    emptyMessage: String,
    onBurp: (Long) -> Unit
) {
    if (batches.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Science,
            message = emptyMessage
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(batches, key = { it.batch.id }) { batchWithPhoto ->
                CuringBatchCard(
                    batchWithPhoto = batchWithPhoto,
                    onBurp = { onBurp(batchWithPhoto.batch.id) }
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DryingBatchCard(
    batchWithPhoto: HarvestBatchWithPhoto,
    onStartCure: () -> Unit,
    onBurp: () -> Unit
) {
    val batch = batchWithPhoto.batch
    val photoUri = batchWithPhoto.photoUri
    
    val daysInDrying = ((System.currentTimeMillis() - batch.harvestDate) / (1000 * 60 * 60 * 24)).toInt()
    val dryingProgress = (daysInDrying / 10f * 100).coerceIn(0f, 100f)
    
    val burpRecommendation = when {
        daysInDrying < 3 -> "2x ao dia (manhã e tarde)"
        daysInDrying < 7 -> "1x ao dia"
        else -> "A cada 2-3 dias"
    }

    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Iniciar Cura?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Deseja mover \"${batch.plantName}\" para a fase de cura?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStartCure()
                        showConfirmDialog = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = batch.plantName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Grass,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = batch.plantName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text(batch.strain, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Progresso da Secagem",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "$daysInDrying dias",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    DryingProgressBar(progress = dryingProgress)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Início: ${formatDate(batch.harvestDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ideal: 7-14 dias",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(
                        icon = Icons.Default.Air,
                        label = "Último Respiro",
                        value = if (batch.lastBurpDate != null) formatRelativeTime(batch.lastBurpDate) else "Não feito"
                    )
                    InfoChip(
                        icon = Icons.Default.Timer,
                        label = "Recomendado",
                        value = burpRecommendation
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBurp,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Air, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Respiro")
                    }
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Finalizar Secagem")
                    }
                }
            }
        }
    }
}

@Composable
fun CuringBatchCard(
    batchWithPhoto: HarvestBatchWithPhoto,
    onBurp: () -> Unit
) {
    val batch = batchWithPhoto.batch
    val photoUri = batchWithPhoto.photoUri
    
    val daysInCuring = ((System.currentTimeMillis() - batch.harvestDate) / (1000 * 60 * 60 * 24)).toInt()
    val curingProgress = (daysInCuring / 56f * 100).coerceIn(0f, 100f)
    
    val burpRecommendation = when {
        daysInCuring < 14 -> "1x ao dia"
        daysInCuring < 28 -> "A cada 2 dias"
        daysInCuring < 42 -> "A cada 3-4 dias"
        else -> "Semanalmente"
    }
    
    val curingPhase = when {
        daysInCuring < 14 -> "Cura Ativa"
        daysInCuring < 28 -> "Cura Intermediária"
        daysInCuring < 56 -> "Cura Final"
        else -> "Curada e Pronta"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF9C27B0).copy(alpha = 0.08f),
                                Color(0xFF9C27B0).copy(alpha = 0.02f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF9C27B0).copy(alpha = 0.15f))
                                .border(2.dp, Color(0xFF9C27B0).copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = batch.plantName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = Color(0xFF9C27B0),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = batch.plantName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(batch.strain, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.height(24.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color(0xFF9C27B0).copy(alpha = 0.15f)
                                    )
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text(curingPhase, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.height(24.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF9C27B0)
                            )
                            Text(
                                text = "Progresso da Cura",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "$daysInCuring dias",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                    }

                    CuringProgressBar(progress = curingProgress)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Início: ${formatDate(batch.harvestDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ideal: 4-8 semanas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(
                        icon = Icons.Default.Air,
                        label = "Último Respiro",
                        value = if (batch.lastBurpDate != null) formatRelativeTime(batch.lastBurpDate) else "Não feito"
                    )
                    InfoChip(
                        icon = Icons.Default.Timer,
                        label = "Recomendado",
                        value = burpRecommendation
                    )
                }

                Button(
                    onClick = onBurp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Icon(Icons.Default.Air, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Fazer Respiro")
                }
            }
        }
    }
}

@Composable
private fun DryingProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "dryingProgress"
    )
    
    val progressColor = when {
        progress >= 100f -> Color(0xFF388E3C)
        progress >= 75f -> Color(0xFF8BC34A)
        progress >= 50f -> Color(0xFFFFC107)
        progress >= 25f -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            progressColor,
                            if (progress >= 100f) Color(0xFF4CAF50) else progressColor.copy(alpha = 0.7f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun CuringProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "curingProgress"
    )
    
    val progressColor = when {
        progress >= 100f -> Color(0xFF388E3C)
        progress >= 75f -> Color(0xFF9C27B0)
        progress >= 50f -> Color(0xFF7B1FA2)
        progress >= 25f -> Color(0xFFAB47BC)
        else -> Color(0xFFCE93D8)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            progressColor,
                            if (progress >= 100f) Color(0xFF4CAF50) else progressColor.copy(alpha = 0.7f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun HumidityBadge(humidity: Float?, compact: Boolean = false) {
    val humidityColor = when {
        humidity == null -> MaterialTheme.colorScheme.onSurfaceVariant
        humidity > 70f -> Color(0xFFD32F2F)
        humidity > 65f -> Color(0xFFFBC02D)
        humidity < 55f -> Color(0xFF1976D2)
        else -> Color(0xFF388E3C)
    }
    
    val animatedColor by animateColorAsState(
        targetValue = humidityColor,
        animationSpec = tween(500),
        label = "humidityColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(animatedColor.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Opacity,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = animatedColor
            )
            Text(
                text = humidity?.let { "${it.roundToInt()}%" } ?: "--%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = animatedColor
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val hours = diff / (1000 * 60 * 60)
    val days = hours / 24

    return when {
        days > 0 -> "${days}d atrás"
        hours > 0 -> "${hours}h atrás"
        else -> "Agora"
    }
}
