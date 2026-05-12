package com.daime.grow.ui.screen.tips

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource


data class TipCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val tips: List<TipItem>
)

data class TipItem(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowTipsScreen(
    onBack: () -> Unit,
    hazeState: HazeState? = null
) {
    val categories = remember {
        listOf(
            TipCategory(
                title = "O Ciclo de Vida",
                icon = Icons.Rounded.Loop,
                color = Color(0xFF4CAF50),
                tips = listOf(
                    TipItem(
                        title = "Germinação (Dias 1-7)",
                        description = "Escuridão total e umidade. A semente usa sua própria energia reserva. Temperatura ideal: 20-25°C.",
                        icon = Icons.Rounded.Spa
                    ),
                    TipItem(
                        title = "Seedling (Semanas 1-3)",
                        description = "Luz suave (PPFD 100-300). Muita luz agora pode estressar a planta jovem. Foco em criar raízes.",
                        icon = Icons.Rounded.Grass
                    ),
                    TipItem(
                        title = "Vegetativo (Semanas 3-8)",
                        description = "Crescimento explosivo. 18h de luz. A planta quer 'comer' o máximo de fótons possível para crescer (PPFD 400-600).",
                        icon = Icons.Rounded.Park
                    ),
                    TipItem(
                        title = "Floração (Semanas 8-14)",
                        description = "Ciclo 12/12. A escuridão total é o gatilho. Máxima intensidade de luz necessária (PPFD 600-900+).",
                        icon = Icons.Rounded.FilterVintage
                    )
                )
            ),
            TipCategory(
                title = "Iluminação & Energia",
                icon = Icons.Rounded.WbSunny,
                color = Color(0xFFFFB300),
                tips = listOf(
                    TipItem(
                        title = "PPFD: A Intensidade Real",
                        description = "Não olhe para Watts, olhe para o PPFD. É a quantidade de fótons que realmente chegam na folha. Use nosso medidor!",
                        icon = Icons.Rounded.Speed
                    ),
                    TipItem(
                        title = "DLI: O Orçamento Diário",
                        description = "Daily Light Integral é o total de luz que a planta recebe em 24h. Pense nisso como o 'prato de comida' do dia.",
                        icon = Icons.Rounded.Calculate
                    ),
                    TipItem(
                        title = "Espectro PAR",
                        description = "Plantas amam Azul (450nm) para estrutura e Vermelho (660nm) para flores. O 'Full Spectrum' imita o sol perfeitamente.",
                        icon = Icons.Rounded.Gradient
                    ),
                    TipItem(
                        title = "Distância e Penetração",
                        description = "Luzes muito perto queimam; muito longe fazem a planta esticar. Encontre o 'sweet spot' usando o medidor do app.",
                        icon = Icons.Rounded.Straighten
                    )
                )
            ),
            TipCategory(
                title = "A Ciência da Luz",
                icon = Icons.Rounded.Lightbulb,
                color = Color(0xFFFBC02D),
                tips = listOf(
                    TipItem(
                        title = "Fotossíntese 101",
                        description = "Plantas transformam luz, água e CO2 em açúcar. Sem luz suficiente, a planta passa fome, não importa o fertilizante.",
                        icon = Icons.Rounded.AutoAwesome
                    ),
                    TipItem(
                        title = "A Saturação de Luz",
                        description = "Existe um limite. Muita luz sem CO2 extra causa estresse oxidativo. Observe as pontas das folhas 'rezando' para cima.",
                        icon = Icons.Rounded.Thermostat
                    )
                )
            ),
            TipCategory(
                title = "Nutrição (NPK)",
                icon = Icons.Rounded.Science,
                color = Color(0xFF9C27B0),
                tips = listOf(
                    TipItem(
                        title = "Vegetativo - Alto Nitrogênio",
                        description = "NPK típico: 3-1-2 ou 2-1-2. Nitrogênio para folhas verdes escuras e crescimento rápido. Não exagere ou queima as raízes.",
                        icon = Icons.Rounded.Eco
                    ),
                    TipItem(
                        title = "Floração - Fósforo e Potássio",
                        description = "NPK typical: 1-3-3 ou 1-2-3. P para raízes e resina. K para densidade das buds e THC. Reduzir N para quase zero.",
                        icon = Icons.Rounded.FilterVintage
                    ),
                    TipItem(
                        title = "Overfeeding (Queimadura de Nutri)",
                        description = "Pontas das folhas amarelas ou marrons = muito nutri. Lave com água limpa (dreno 20%). Sempre comece com 50% da dose.",
                        icon = Icons.Rounded.Warning
                    ),
                    TipItem(
                        title = "pH Correcto",
                        description = "Solo: 6.0-7.0. Hydro/Coco: 5.5-6.5. pH errado = bloqueio de nutrientes mesmo que estejam presentes. Use medidor.",
                        icon = Icons.Rounded.Science
                    )
                )
            ),
            TipCategory(
                title = "Rega e Umidade",
                icon = Icons.Rounded.WaterDrop,
                color = Color(0xFF2196F3),
                tips = listOf(
                    TipItem(
                        title = "Frequência de Rega",
                        description = "Veg: a cada 2-3 dias. Flor: a cada 3-4 dias (planta bebe menos). Método do dedo: 2-3cm seco = regue. Menos em floração tardia.",
                        icon = Icons.Rounded.Waves
                    ),
                    TipItem(
                        title = "Qualidade da Água",
                        description = "EC (condutividade) ideal: 0.8-1.2 veg, 1.2-1.6 flor. Use filtro ou deixe repousar 24h. pH após ajuste: verifique sempre.",
                        icon = Icons.Rounded.FilterAlt
                    ),
                    TipItem(
                        title = "Umidade Ideal",
                        description = "Seedling: 70-80%. Veg: 50-70%. Floração: 40-50%. Alta umidade em flor = risco de bolor. Use desumidificador no fim.",
                        icon = Icons.Rounded.Water
                    ),
                    TipItem(
                        title = "Drenagem",
                        description = "Vasos com furos EM TODO LADO. Encharcamento = raízes sem oxígeno = apodrecimento. Use bandeja com pedras pra drenar.",
                        icon = Icons.Rounded.Layers
                    )
                )
            ),
            TipCategory(
                title = "Temperatura e Ambiente",
                icon = Icons.Rounded.Thermostat,
                color = Color(0xFFFF5722),
                tips = listOf(
                    TipItem(
                        title = "Temperatura Perfeita",
                        description = "Dia: 22-28°C. Noite: 18-24°C. Diferença dia/noite (DIF) de 5-10°C ajuda no crescimento. Acima de 30°C = estresse severo.",
                        icon = Icons.Rounded.DeviceThermostat
                    ),
                    TipItem(
                        title = "Ventilação",
                        description = "Ar fresco entrando SEMPRE. CO2 fresco = crescimento 20% melhor. Extractors sugando o ar quente. Ventiladores oscilantes pra circulação.",
                        icon = Icons.Rounded.Air
                    ),
                    TipItem(
                        title = "CO2 Suplementar",
                        description = "Em grow rooms seladas: 800-1500ppm acelera crescimento. Só faz sentido com LED forte. Em grows normais: ventilação é suficiente.",
                        icon = Icons.Rounded.Cloud
                    ),
                    TipItem(
                        title = "Controle de Odor",
                        description = "Floração = odor Forte! Filtros de carvão são essenciais. Depuradores de carbono, ozônio em casos extremos.",
                        icon = Icons.Rounded.FilterAlt
                    )
                )
            ),
            TipCategory(
                title = "Técnicas de Treino (HIG-TECH)",
                icon = Icons.Rounded.Construction,
                color = Color(0xFFE91E63),
                tips = listOf(
                    TipItem(
                        title = "LST (Low Stress Training)",
                        description = "Dobre caule principal suavemente, amarre com fio soft. Quebra apical não! Promove múltiplos tops uniformes. Comece cedo na veg.",
                        icon = Icons.Rounded.Balance
                    ),
                    TipItem(
                        title = "SCROG (Screen of Green)",
                        description = "Tela a 20-30cm acima das plantas. Enfie plantas pela tela. Preenche toda a tela uniformemente. Máximo aproveitamento de luz.",
                        icon = Icons.Rounded.GridOn
                    ),
                    TipItem(
                        title = "SOG (Sea of Green)",
                        description = "Muitas plantas pequenas em vez de poucas grandes. Clone direto pra flor, 4-6 semanas veg máximo. Rápido e produtivo.",
                        icon = Icons.Rounded.Waves
                    ),
                    TipItem(
                        title = "Topping / Fimming",
                        description = "TOPPING: corta ponta do caule principal acima do nó 4-5. Dois tops nascem. FIMMING: corta 80% da ponta. 4+ tops. Promove arbusto.",
                        icon = Icons.Rounded.ContentCut
                    ),
                    TipItem(
                        title = "Defoliação (Defoliating)",
                        description = "Remove folhas grandes que bloqueiam luz dos buds. 2-3x durante floração. Menos é mais! Só folhas que não recebem luz direta.",
                        icon = Icons.Rounded.RemoveCircle
                    )
                )
            ),
            TipCategory(
                title = "Colheita e Curing",
                icon = Icons.Rounded.Agriculture,
                color = Color(0xFF795548),
                tips = listOf(
                    TipItem(
                        title = "Quando Colher",
                        description = "Pistilos (pêlos): 70-90% marrons/avermelhados. Tricomas: branco leitoso + 10-20% âmbar. Use lupa 60x+ ou microscópio.",
                        icon = Icons.Rounded.Visibility
                    ),
                    TipItem(
                        title = "Flush Final",
                        description = "2 semanas antes da colheita: água limpa SEM nutrientes. Remove acúmulo de sal. Gosto mais suave, queima melhor.",
                        icon = Icons.Rounded.WaterDrop
                    ),
                    TipItem(
                        title = "Secagem (Drying)",
                        description = "Invertido (de cabeça pra baixo), 18-21°C, umidade 50-60%. Escuro total. 7-14 dias até galhos quebrarem.",
                        icon = Icons.Rounded.Dry
                    ),
                    TipItem(
                        title = "Curing (Curagem)",
                        description = "Potes de vidro, buracos sehari. Primeira semana: abrir 2x ao dia por 15min. Depois 1x ao dia. Mínimo 2-4 semanas.",
                        icon = Icons.Rounded.Inventory
                    ),
                    TipItem(
                        title = "Armazenamento",
                        description = "Potes herméticos, lugar fresco e escuro. Não congelar. Boa cura dura meses mantendo potência. Boveda packs ajudam.",
                        icon = Icons.Rounded.Inventory2
                    )
                )
            ),
            TipCategory(
                title = "Pragas e Problemas",
                icon = Icons.Rounded.BugReport,
                color = Color(0xFFD32F2F),
                tips = listOf(
                    TipItem(
                        title = "Ácaros (Spider Mites)",
                        description = "Pequenos pontos amarelos nas folhas, teias finas. Spray de água + sabão neutro, Óleo de Neem. Isolar planta. Reaplique 3x.",
                        icon = Icons.Rounded.PestControl
                    ),
                    TipItem(
                        title = "Fungos (Botrytis, Powdery Mildew)",
                        description = "Botrytis: bud cinza/mofo = REMOVER IMEDIATAMENTE. Powdery mildew: pó branco = leite spray 1:9. Ventilação é prevenção.",
                        icon = Icons.Rounded.Healing
                    ),
                    TipItem(
                        title = "Deficiências Comuns",
                        description = "Deficiência N: folhas amarelas embaixo. Deficiência P: caule roxo. Deficiência K: bordas marrons. Deficiência Mg: veias verdes, amarelo entre.",
                        icon = Icons.Rounded.Warning
                    ),
                    TipItem(
                        title = "Prevenção",
                        description = "Inspecione novas plantas por 2 semanas. Quarentena total. Ambiente limpo. Armadilhas pegajosas. Neem preventivo semanal ajuda.",
                        icon = Icons.Rounded.Visibility
                    )
                )
            )
        )
    }

    var expandedCategory by remember { mutableStateOf<Int?>(0) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val horizontalPadding = if (isTablet) 32.dp else 20.dp

    Scaffold(
        modifier = Modifier.then(if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LightRoleHero()
            }

            item {
                Text(
                    "Categorias",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(categories.size) { index ->
                val category = categories[index]
                val isExpanded = expandedCategory == index

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    onClick = {
                        expandedCategory = if (isExpanded) null else index
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(category.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    category.icon,
                                    contentDescription = null,
                                    tint = category.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                category.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider()
                                category.tips.forEach { tip ->
                                    TipCard(tip = tip, categoryColor = category.color)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun LightRoleHero() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD600),
                            Color(0xFFFF6D00)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "A Luz é o Motor",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Diferente de nós, plantas não comem matéria. Elas comem luz. " +
                    "Através da fotossíntese, a planta transforma fótons em energia química, " +
                    "construindo cada folha e cada flor. Entender a luz é a chave para um cultivo de elite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun TipCard(
    tip: TipItem,
    categoryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                tip.icon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tip.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                tip.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
