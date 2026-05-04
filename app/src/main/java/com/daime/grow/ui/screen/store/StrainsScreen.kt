package com.daime.grow.ui.screen.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.daime.grow.data.remote.SupabaseClient
import com.daime.grow.data.remote.model.StrainDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrainsScreen(
    innerPadding: PaddingValues,
    onStrainClick: (String) -> Unit = {}
) {
    var strains by remember { mutableStateOf<List<StrainDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val columnsCount = if (isTablet) 3 else 2
    val horizontalPadding = if (isTablet) 32.dp else 16.dp

    val filters = listOf("Todas", "Frutíferas", "Hortaliças", "Ervas", "Legumes", "Sativa", "Indica", "Híbrida")

    LaunchedEffect(Unit) {
        loadStrains(
            onSuccess = { strains = it },
            onError = { error = it },
            onLoading = { isLoading = it }
        )
    }

    val filteredStrains = strains.filter { strain ->
        val matchesFilter = when (selectedFilter) {
            "Sativa" -> strain.tipo?.lowercase()?.contains("sativa") == true
            "Indica" -> strain.tipo?.lowercase()?.contains("indica") == true
            "Híbrida" -> strain.tipo?.lowercase()?.contains("hybrid") == true || 
                         strain.tipo?.lowercase()?.contains("híbrida") == true ||
                         strain.tipo?.lowercase()?.contains("hibrida") == true
            "Frutíferas" -> strain.tipo?.lowercase()?.contains("frut") == true || 
                             strain.descricao?.lowercase()?.contains("frut") == true
            "Hortaliças" -> strain.tipo?.lowercase()?.contains("hort") == true || 
                             strain.descricao?.lowercase()?.contains("hort") == true
            "Ervas" -> strain.tipo?.lowercase()?.contains("erva") == true || 
                        strain.descricao?.lowercase()?.contains("erva") == true
            "Legumes" -> strain.tipo?.lowercase()?.contains("legum") == true || 
                          strain.descricao?.lowercase()?.contains("legum") == true
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() || 
            strain.name.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            return@Scaffold
        }

        if (error != null) {
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
                        Icons.Default.Grass,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = error ?: "Erro ao carregar strains",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = {
                        loadStrains(
                            onSuccess = { strains = it },
                            onError = { error = it },
                            onLoading = { isLoading = it }
                        )
                    }) {
                        Text("Tentar Novamente")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF9C27B0).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF9C27B0),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Página informativa. Valores médios podem variar conforme o cultivado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9C27B0)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cepas",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${strains.size} cepas disponíveis",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter || (filter == "Todas" && selectedFilter == null),
                            onClick = {
                                selectedFilter = if (filter == "Todas") null else filter
                            },
                            label = { Text(filter) },
                            leadingIcon = if (selectedFilter == filter || (filter == "Todas" && selectedFilter == null)) {
                                {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (filteredStrains.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Grass,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Nenhuma strain encontrada",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val cardHeight = if (isTablet) 220.dp else 180.dp
                items(filteredStrains.chunked(columnsCount)) { rowStrains ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowStrains.forEach { strain ->
                            StrainCard(
                                strain = strain,
                                onClick = { onStrainClick(strain.id) },
                                modifier = Modifier.weight(1f),
                                imageHeight = cardHeight
                            )
                        }
                        if (rowStrains.size < columnsCount) {
                            repeat(columnsCount - rowStrains.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 80.dp))
            }
        }
    }
}

@Composable
fun StrainCard(
    strain: StrainDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageHeight: androidx.compose.ui.unit.Dp = 180.dp
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
                            Icons.Default.Grass,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = strain.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (strain.tipo != null) {
                            Text(
                                text = strain.tipo,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (strain.thc != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "THC ${strain.thc}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (strain.cbd != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF9C27B0)
                        ) {
                            Text(
                                text = "CBD ${strain.cbd}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (strain.tempoFloracao != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🌸 ${strain.tempoFloracao}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (strain.rendimento != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📈 ${strain.rendimento}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (strain.descricao != null) {
                    Text(
                        text = strain.descricao,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun loadStrains(
    onSuccess: (List<StrainDto>) -> Unit,
    onError: (String) -> Unit,
    onLoading: (Boolean) -> Unit
) {
    kotlinx.coroutines.MainScope().launch {
        onLoading(true)
        try {
            val client = SupabaseClient.clientOrNull
            if (client == null) {
                onLoading(false)
                onError("Supabase não configurado")
                return@launch
            }

            val strains = client.from("strains")
                .select()
                .decodeList<StrainDto>()
                .filter { it.ativo }

            onLoading(false)
            onSuccess(strains)
        } catch (e: Exception) {
            onLoading(false)
            onError("Erro ao carregar strains: ${e.message}")
        }
    }
}
