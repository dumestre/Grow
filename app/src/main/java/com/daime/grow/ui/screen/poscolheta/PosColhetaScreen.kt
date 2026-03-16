package com.daime.grow.ui.screen.poscolheta

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.data.local.entity.HarvestBatchEntity
import com.daime.grow.ui.viewmodel.PosColhetaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
                0 -> HarvestTabContent(
                    batches = dryingBatches,
                    emptyMessage = "Nenhuma planta em secagem no momento.",
                    onBurp = viewModel::burp
                )
                1 -> HarvestTabContent(
                    batches = curingBatches,
                    emptyMessage = "Nenhuma planta em cura no momento.",
                    onBurp = viewModel::burp
                )
            }
        }
    }
}

@Composable
private fun HarvestTabContent(
    batches: List<HarvestBatchEntity>,
    emptyMessage: String,
    onBurp: (Long) -> Unit
) {
    if (batches.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(batches, key = { it.id }) { batch ->
                HarvestBatchCard(batch = batch, onBurp = { onBurp(batch.id) })
            }
        }
    }
}

@Composable
fun HarvestBatchCard(
    batch: HarvestBatchEntity,
    onBurp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = batch.plantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = batch.strain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HumidityIndicator(humidity = batch.currentHumidity)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Colhida em: ${formatDate(batch.harvestDate)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    batch.nextBurpDate?.let {
                        Text(
                            text = "Próximo respiro: ${formatTime(it)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (it < System.currentTimeMillis()) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                }
                
                if (batch.status == "CURING") {
                    IconButton(onClick = onBurp) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = "Fazer respiro (Burp)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HumidityIndicator(humidity: Float?) {
    if (humidity == null) return
    
    val color by animateColorAsState(
        targetValue = when {
            humidity > 70f -> Color(0xFFD32F2F) // Vermelho - Risco de mofo
            humidity > 65f -> Color(0xFFFBC02D) // Amarelo - Alerta
            humidity < 55f -> Color(0xFF1976D2) // Azul - Muito seco
            else -> Color(0xFF388E3C) // Verde - Ideal
        },
        label = "HumidityColor"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Opacity,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${humidity.toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    val diff = timestamp - System.currentTimeMillis()
    if (diff < 0) return "Atrasado!"
    
    val hours = diff / 3600000
    val minutes = (diff % 3600000) / 60000
    
    return if (hours > 0) "em ${hours}h ${minutes}m" else "em ${minutes}m"
}
