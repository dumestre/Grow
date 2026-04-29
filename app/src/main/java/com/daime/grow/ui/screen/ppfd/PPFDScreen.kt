package com.daime.grow.ui.screen.ppfd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PPFDScreen(
    onBack: () -> Unit,
    viewModel: PPFDViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculadora PPFD") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Calcule a densidade de fluxo de fótons fotossintéticos",
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = uiState.ppf,
                onValueChange = { viewModel.updatePPF(it) },
                label = { Text("PPF (μmol/s)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.area,
                onValueChange = { viewModel.updateArea(it) },
                label = { Text("Área (m²)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.calculate() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calcular PPFD")
            }

            if (uiState.result > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PPFD: ${"%.2f".format(uiState.result)} μmol/m²/s",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getPPFDClassification(uiState.result),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

fun getPPFDClassification(ppfd: Double): String {
    return when {
        ppfd < 200 -> "Baixo - Adequado para clones e mudas"
        ppfd < 400 -> "Médio-Baixo - Vegetativo inicial"
        ppfd < 600 -> "Médio - Vegetativo e floração inicial"
        ppfd < 900 -> "Alto - Floração (padrão recomendado)"
        ppfd < 1200 -> "Muito Alto - Floração intensa"
        else -> "Extremo - Cuidado com estresse térmico"
    }
}
