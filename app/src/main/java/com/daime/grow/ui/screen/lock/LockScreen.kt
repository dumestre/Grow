package com.daime.grow.ui.screen.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.daime.grow.R
import com.daime.grow.ui.viewmodel.LockUiState

@Composable
fun LockScreen(
    state: LockUiState,
    onPinChange: (String) -> Unit,
    onUnlockWithPin: () -> Unit,
    onTryBiometric: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.lock_subtitle), style = MaterialTheme.typography.bodyMedium)

                if (state.pinEnabled) {
                    OutlinedTextField(
                        value = state.pinInput,
                        onValueChange = { onPinChange(it.filter(Char::isDigit).take(6)) },
                        label = { Text(stringResource(R.string.lock_pin)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(onClick = onUnlockWithPin, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.lock_unlock_pin))
                    }
                }

                if (state.biometricEnabled) {
                    Button(onClick = onTryBiometric, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (state.pinEnabled) stringResource(R.string.lock_use_biometric)
                            else stringResource(R.string.lock_use_device_credential)
                        )
                    }
                }

                if (!state.pinEnabled && state.biometricEnabled) {
                    Text(
                        stringResource(R.string.lock_device_credential_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!state.error.isNullOrBlank()) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

