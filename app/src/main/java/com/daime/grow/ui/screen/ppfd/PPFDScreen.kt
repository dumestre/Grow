package com.daime.grow.ui.screen.ppfd

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.daime.grow.domain.model.LightSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PPFDScreen(
    onBack: () -> Unit,
    viewModel: PPFDViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sources = LightSource.entries
    val pagerState = rememberPagerState(
        initialPage = sources.indexOf(uiState.selectedSource),
        pageCount = { sources.size }
    )

    // Sincroniza o pager com o ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.updateLightSource(sources[pagerState.currentPage])
    }

    DisposableEffect(Unit) {
        viewModel.startMeasuring()
        onDispose { viewModel.stopMeasuring() }
    }

    val ppfdColor by animateColorAsState(
        targetValue = getPPFDColor(uiState.ppfd),
        animationSpec = tween(500),
        label = "PPFDColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Fundo Dinâmico (Efeito de Brilho)
        DynamicBackground(ppfdColor, uiState.ppfd)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Medidor PPFD", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!uiState.isSensorAvailable) {
                    SensorUnavailableState()
                } else {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Banner de status
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Info, 
                                null, 
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Modo experimental: Em fase de ajustes.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(0.5f))

                    // Mostrador Principal
                    MainReadout(
                        ppfd = uiState.ppfd,
                        lux = uiState.lux,
                        color = ppfdColor,
                        isHold = uiState.isHoldActive
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Botão HOLD centralizado acima da seleção de luz
                    HoldButton(
                        isHold = uiState.isHoldActive,
                        onHoldToggle = viewModel::toggleHold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seletor de Fonte por Swipe
                    LightSourcePager(
                        pagerState = pagerState,
                        sources = sources,
                        activeColor = ppfdColor
                    )

                    Spacer(modifier = Modifier.weight(0.5f))

                    // Controles de Rodapé (Apenas Calibração agora)
                    BottomControls(
                        multiplier = uiState.calibrationMultiplier,
                        onMultiplierChange = viewModel::updateCalibration
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun HoldButton(isHold: Boolean, onHoldToggle: () -> Unit) {
    IconButton(
        onClick = onHoldToggle,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (isHold) MaterialTheme.colorScheme.errorContainer 
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            )
            .border(
                width = 2.dp,
                color = if (isHold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = if (isHold) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            contentDescription = if (isHold) "Resumir" else "Hold",
            modifier = Modifier.size(36.dp),
            tint = if (isHold) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}


@Composable
private fun DynamicBackground(color: Color, ppfd: Double) {
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = (ppfd / 2000.0).coerceIn(0.1, 0.3).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    radius = 1200f
                )
            )
            .blur(100.dp)
    )
}

@Composable
private fun MainReadout(ppfd: Double, lux: Float, color: Color, isHold: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Círculo de brilho sutil
            Surface(
                modifier = Modifier.size(280.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.03f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
            ) {}

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isHold) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "HOLD",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(
                    text = "%.0f".format(ppfd),
                    fontSize = 100.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isHold) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else color,
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    text = "μmol/m²/s",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
        ) {
            Text(
                text = getPPFDClassification(ppfd).uppercase(),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "${"%.0f".format(lux)} LUX",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun LightSourcePager(
    pagerState: androidx.compose.foundation.pager.PagerState, 
    sources: List<LightSource>,
    activeColor: Color
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val itemWidth = 120.dp
    val coroutineScope = rememberCoroutineScope()
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "FONTE DE LUZ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(itemWidth),
            contentPadding = PaddingValues(horizontal = (screenWidth - itemWidth) / 2),
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 0.dp
        ) { page ->
            val source = sources[page]
            val isSelected = pagerState.currentPage == page
            val sourceColor = getSourceColor(source)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(itemWidth)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        }
                    )
                    .graphicsLayer {
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                        alpha = 1f - (pageOffset * 0.45f).coerceIn(0f, 1f)
                        scaleX = 1f - (pageOffset * 0.2f).coerceIn(0f, 1f)
                        scaleY = 1f - (pageOffset * 0.2f).coerceIn(0f, 1f)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) sourceColor.copy(alpha = 0.1f) 
                            else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) sourceColor.copy(alpha = 0.3f) else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = if (isSelected) sourceColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = source.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Indicadores do Pager
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(sources.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 10.dp else 6.dp, 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) activeColor 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                )
            }
        }
    }
}

fun getSourceColor(source: LightSource): Color {
    return when (source) {
        LightSource.SUNLIGHT -> Color(0xFFFFD600) // Dourado Sol
        LightSource.LED_WHITE -> Color(0xFF81D4FA) // Azul/Branco Frio
        LightSource.HPS -> Color(0xFFFF6D00) // Laranja Sódio
    }
}

@Composable
private fun BottomControls(
    multiplier: Float,
    onMultiplierChange: (Float) -> Unit
) {
    var showCalibration by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Botão Calibração Centralizado e Circular
        IconButton(
            onClick = { showCalibration = !showCalibration },
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (showCalibration) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune, 
                contentDescription = "Calibração",
                tint = if (showCalibration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        if (showCalibration) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ajuste de Calibração", style = MaterialTheme.typography.labelLarge)
                        Text("${(multiplier * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = multiplier,
                        onValueChange = onMultiplierChange,
                        valueRange = 0.5f..2.0f,
                        steps = 15
                    )
                    Text(
                        "Ajuste a sensibilidade do sensor se tiver um medidor PAR de referência.",
                        style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
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
        ppfd < 200 -> Color(0xFF4CAF50) // Verde Suave
        ppfd < 600 -> Color(0xFF8BC34A) // Verde Lima
        ppfd < 900 -> Color(0xFFFFC107) // Amber
        ppfd < 1200 -> Color(0xFFFF9800) // Laranja
        else -> Color(0xFFF44336) // Vermelho
    }
}

fun getPPFDClassification(ppfd: Double): String {
    return when {
        ppfd < 200 -> "Mudas / Clones"
        ppfd < 400 -> "Vegetativo Inicial"
        ppfd < 600 -> "Vegetativo"
        ppfd < 900 -> "Floração"
        ppfd < 1200 -> "Floração Intensa"
        else -> "Risco de Estresse"
    }
}

// Polyfill para absoluteValue
private val Float.absoluteValue get() = kotlin.math.abs(this)
