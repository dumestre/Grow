package com.daime.grow.ui.screen.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
import com.daime.grow.domain.model.DarkThemeMode
import com.daime.grow.domain.model.SecurityPreferences
import com.daime.grow.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel,
    accountUsername: String?,
    accountId: String?,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val security by viewModel.security.collectAsStateWithLifecycle()
    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var revealPin by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val invalidPinMessage = stringResource(R.string.settings_invalid_pin)
    val pinUpdatedMessage = stringResource(R.string.settings_pin_updated)
    val backupExportedMessage = stringResource(R.string.settings_backup_exported)
    val backupExportErrorMessage = stringResource(R.string.settings_backup_export_error)
    val backupImportedMessage = stringResource(R.string.settings_backup_imported)
    val backupImportErrorMessage = stringResource(R.string.settings_backup_import_error)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsViewModel.UiEvent.InvalidPin -> snackbarHostState.showSnackbar(invalidPinMessage)
                SettingsViewModel.UiEvent.PinUpdated -> snackbarHostState.showSnackbar(pinUpdatedMessage)
                SettingsViewModel.UiEvent.BackupExported -> snackbarHostState.showSnackbar(backupExportedMessage)
                SettingsViewModel.UiEvent.BackupExportError -> snackbarHostState.showSnackbar(backupExportErrorMessage)
                SettingsViewModel.UiEvent.BackupImported -> snackbarHostState.showSnackbar(backupImportedMessage)
                SettingsViewModel.UiEvent.BackupImportError -> snackbarHostState.showSnackbar(backupImportErrorMessage)
            }
        }
    }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.importBackup(it) }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Theme Toggle Button
                    IconButton(
                        onClick = {
                            val newTheme = when (security.darkTheme) {
                                DarkThemeMode.DARK -> DarkThemeMode.LIGHT
                                else -> DarkThemeMode.DARK
                            }
                            viewModel.setDarkThemeMode(newTheme)
                        }
                    ) {
                        Icon(
                            imageVector = if (security.darkTheme == DarkThemeMode.DARK) {
                                Icons.Filled.LightMode
                            } else {
                                Icons.Filled.DarkMode
                            },
                            contentDescription = if (security.darkTheme == DarkThemeMode.DARK) {
                                "Mudar para tema claro"
                            } else {
                                "Mudar para tema escuro"
                            },
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(if (isTablet) 32.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val backupFileName = stringResource(R.string.settings_backup_file_name)
            val normalizedPin = pinInput.filter(Char::isDigit).take(6)
            val normalizedPinConfirm = pinConfirmInput.filter(Char::isDigit).take(6)
            val pinLengthInvalid = normalizedPin.isNotEmpty() && normalizedPin.length < 4
            val pinMismatch = normalizedPinConfirm.isNotEmpty() && normalizedPin != normalizedPinConfirm
            val canSavePin = normalizedPin.length in 4..6 && normalizedPin == normalizedPinConfirm

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = "Conta conectada",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = accountUsername?.let { "@$it" } ?: "Google",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (!accountId.isNullOrBlank()) {
                        Text(
                            text = "Identificador: $accountId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null
                        )
                        Text(
                            text = "Sair",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        SecuritySettingsContent(
                            security = security,
                            viewModel = viewModel,
                            normalizedPin = normalizedPin,
                            onPinInputChange = { pinInput = it },
                            normalizedPinConfirm = normalizedPinConfirm,
                            onPinConfirmInputChange = { pinConfirmInput = it },
                            revealPin = revealPin,
                            onRevealPinChange = { revealPin = it },
                            pinLengthInvalid = pinLengthInvalid,
                            pinMismatch = pinMismatch,
                            canSavePin = canSavePin,
                            onSavePin = {
                                viewModel.updatePin(normalizedPin)
                                pinInput = ""
                                pinConfirmInput = ""
                            }
                        )
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        BackupSettingsContent(
                            backupFileName = backupFileName,
                            onCreateDocument = { createDocument.launch(it) },
                            onOpenDocument = { openDocument.launch(arrayOf("application/json")) }
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    SecuritySettingsContent(
                        security = security,
                        viewModel = viewModel,
                        normalizedPin = normalizedPin,
                        onPinInputChange = { pinInput = it },
                        normalizedPinConfirm = normalizedPinConfirm,
                        onPinConfirmInputChange = { pinConfirmInput = it },
                        revealPin = revealPin,
                        onRevealPinChange = { revealPin = it },
                        pinLengthInvalid = pinLengthInvalid,
                        pinMismatch = pinMismatch,
                        canSavePin = canSavePin,
                        onSavePin = {
                            viewModel.updatePin(normalizedPin)
                            pinInput = ""
                            pinConfirmInput = ""
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    BackupSettingsContent(
                        backupFileName = backupFileName,
                        onCreateDocument = { createDocument.launch(it) },
                        onOpenDocument = { openDocument.launch(arrayOf("application/json")) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecuritySettingsContent(
    security: SecurityPreferences,
    viewModel: SettingsViewModel,
    normalizedPin: String,
    onPinInputChange: (String) -> Unit,
    normalizedPinConfirm: String,
    onPinConfirmInputChange: (String) -> Unit,
    revealPin: Boolean,
    onRevealPinChange: (Boolean) -> Unit,
    pinLengthInvalid: Boolean,
    pinMismatch: Boolean,
    canSavePin: Boolean,
    onSavePin: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RowSetting(
            title = stringResource(R.string.settings_lock_pin_biometric),
            checked = security.lockEnabled,
            onCheckedChange = viewModel::setLockEnabled
        )

        RowSetting(
            title = stringResource(R.string.settings_biometric),
            checked = security.biometricEnabled,
            onCheckedChange = viewModel::setBiometricEnabled
        )

        OutlinedTextField(
            value = normalizedPin,
            onValueChange = onPinInputChange,
            label = { Text(stringResource(R.string.settings_pin_label)) },
            visualTransformation = if (revealPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = pinLengthInvalid || pinMismatch,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = normalizedPinConfirm,
            onValueChange = onPinConfirmInputChange,
            label = { Text(stringResource(R.string.settings_pin_confirm_label)) },
            visualTransformation = if (revealPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = pinMismatch,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_reveal_pin))
            Switch(
                checked = revealPin,
                onCheckedChange = onRevealPinChange,
                modifier = Modifier.scale(0.7f)
            )
        }

        if (pinLengthInvalid) {
            Text(
                text = stringResource(R.string.settings_pin_length_error),
                color = MaterialTheme.colorScheme.error
            )
        } else if (pinMismatch) {
            Text(
                text = stringResource(R.string.settings_pin_mismatch_error),
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onSavePin,
            enabled = canSavePin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_save_pin))
        }
    }
}

@Composable
private fun BackupSettingsContent(
    backupFileName: String,
    onCreateDocument: (String) -> Unit,
    onOpenDocument: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_backup_title), style = MaterialTheme.typography.titleMedium)
        Button(onClick = { onCreateDocument(backupFileName) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_export_backup_json))
        }
        Button(onClick = { onOpenDocument() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_import_backup_json))
        }
    }
}

@Composable
private fun RowSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.padding(end = 12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.7f)
        )
    }
}
