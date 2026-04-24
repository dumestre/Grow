package com.daime.grow.ui.screen.store

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.daime.grow.data.remote.SupabaseClient
import com.daime.grow.data.remote.model.StrainDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrainDetailScreen(
    strainId: String,
    onBack: () -> Unit
) {
    var strain by remember { mutableStateOf<StrainDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(strainId) {
        loadStrain(strainId) { result ->
            result.onSuccess { strain = it }
            result.onFailure { error = it.message }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error ?: "Erro ao carregar",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onBack) {
                            Text("Voltar")
                        }
                    }
                }
            }
            strain != null -> {
                StrainDetailContent(
                    strain = strain!!,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
private fun StrainDetailContent(
    strain: StrainDto,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val imageHeight = if (isTablet) 400.dp else 300.dp
    val horizontalPadding = if (isTablet) 48.dp else 20.dp

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .statusBarsPadding()
            ) {
                if (!strain.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = strain.imageUrl,
                        contentDescription = strain.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Spa,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = strain.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (strain.tipo != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = getTipoColor(strain.tipo)
                        ) {
                            Text(
                                text = strain.tipo.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (strain.thc != null) {
                    InfoCard(
                        icon = Icons.Rounded.Bolt,
                        title = "THC",
                        value = strain.thc,
                        color = Color(0xFFFF9800)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (strain.tempoFloracao != null) {
                        InfoCardSmall(
                            icon = Icons.Rounded.Schedule,
                            title = "Floração",
                            value = strain.tempoFloracao,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (strain.altura != null) {
                        InfoCardSmall(
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            title = "Altura",
                            value = strain.altura,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (strain.rendimento != null) {
                    InfoCard(
                        icon = Icons.Rounded.BarChart,
                        title = "Rendimento",
                        value = strain.rendimento,
                        color = Color(0xFF4CAF50)
                    )
                }

                if (strain.descricao != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Sobre",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strain.descricao,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!strain.buyLinks.isNullOrEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Acesse para saber mais sobre essas genéticas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinksList(links = strain.buyLinks ?: emptyList())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun LinksList(links: List<String>) {
    val context = LocalContext.current
    val visitedLinks = remember { mutableStateOf(setOf<String>()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        links.forEachIndexed { index, link ->
            val isVisited = visitedLinks.value.contains(link)
            
            TextButton(
                onClick = {
                    visitedLinks.value += link
                    val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isVisited) Color(0xFF4CAF50) else Color(0xFFE91E63)
                )
            ) {
                Text(
                    text = "→ Fonte ${index + 1}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InfoCardSmall(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getTipoColor(tipo: String): Color {
    return when (tipo.lowercase()) {
        "sativa" -> Color(0xFF4CAF50)
        "indica" -> Color(0xFF9C27B0)
        else -> Color(0xFF2196F3)
    }
}

private fun loadStrain(
    strainId: String,
    onResult: (Result<StrainDto>) -> Unit
) {
    kotlinx.coroutines.MainScope().launch {
        try {
            val client = SupabaseClient.clientOrNull
            if (client == null) {
                onResult(Result.failure(Exception("Supabase não configurado")))
                return@launch
            }

            val strain = client.from("strains")
                .select()
                .decodeList<StrainDto>()
                .find { it.id == strainId }

            if (strain != null) {
                onResult(Result.success(strain))
            } else {
                onResult(Result.failure(Exception("Strain não encontrada")))
            }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }
}
