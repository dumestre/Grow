package com.daime.grow.ui.screen.add

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.ui.components.PhotoPickerBox
import com.daime.grow.ui.components.RoundedBackButton
import com.daime.grow.ui.screen.mural.UsernameDialog
import com.daime.grow.ui.viewmodel.AddPlantViewModel
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewPlantScreen(
    innerPadding: PaddingValues,
    viewModel: AddPlantViewModel,
    onSaved: (Long) -> Unit,
    onClose: () -> Unit,
    onCheckUser: (String, (Long) -> Unit, () -> Unit) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val strainRequester = remember { FocusRequester() }
    val mediumRequester = remember { FocusRequester() }
    val daysRequester = remember { FocusRequester() }

    var showSheet by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }

    var pendingCameraPhoto by remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val persistentUri = persistPhotoToAppStorage(context, uri)
            viewModel.onPhotoSelected(persistentUri)
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val persistentUri = persistPhotoToAppStorage(context, uri)
            viewModel.onPhotoSelected(persistentUri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onPhotoSelected(pendingCameraPhoto)
        } else {
            pendingCameraFile?.delete()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = createPersistentPhotoFile(context)
            val providerUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCameraFile = file
            pendingCameraPhoto = providerUri.toString()
            cameraUri = providerUri
            cameraUri?.let { cameraLauncher.launch(it) }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(16.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.new_plant_title)) },
                navigationIcon = { RoundedBackButton(onClick = onClose) }
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhotoPickerBox(photoUri = state.photoUri, onClick = { showSheet = true })

                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.new_plant_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { strainRequester.requestFocus() }
                        )
                    )

                    OutlinedTextField(
                        value = state.strain,
                        onValueChange = viewModel::onStrainChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(strainRequester),
                        label = { Text(stringResource(R.string.new_plant_strain)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { mediumRequester.requestFocus() }
                        )
                    )

                    Text(stringResource(R.string.new_plant_stage), style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlantStage.entries.forEach { phase ->
                            FilterChip(
                                onClick = { viewModel.onStageChange(phase) },
                                label = { Text(phase) },
                                selected = state.stage == phase
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.medium,
                        onValueChange = viewModel::onMediumChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(mediumRequester),
                        label = { Text(stringResource(R.string.new_plant_medium)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { daysRequester.requestFocus() }
                        )
                    )

                    OutlinedTextField(
                        value = state.days,
                        onValueChange = viewModel::onDaysChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(daysRequester),
                        label = { Text(stringResource(R.string.new_plant_days)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (!state.shareOnMural) {
                                    showUsernameDialog = true
                                } else {
                                    viewModel.onShareOnMuralChange(false)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = "Compartilhar no Mural",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Outros usuários poderão ver e comentar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = state.shareOnMural,
                            onCheckedChange = null,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .scale(0.7f)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.onHydroponicChange(!state.isHydroponic) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = "Método Hidropônico",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Notificações de troca de solução",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = state.isHydroponic,
                            onCheckedChange = { viewModel.onHydroponicChange(it) },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .scale(0.7f)
                        )
                    }
                }
            }

            if (!state.error.isNullOrBlank()) {
                Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.new_plant_save))
            }
        }
    }

    if (showSheet) {
        AlertDialog(
            onDismissRequest = { showSheet = false },
            title = {
                Text(
                    text = stringResource(R.string.new_plant_photo_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showSheet = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    getContentLauncher.launch("image/*")
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.new_plant_gallery),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showSheet = false
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    val file = createPersistentPhotoFile(context)
                                    val providerUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    pendingCameraFile = file
                                    pendingCameraPhoto = providerUri.toString()
                                    cameraUri = providerUri
                                    cameraUri?.let { cameraLauncher.launch(it) }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.new_plant_camera),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.new_plant_close))
                }
            }
        )
    }

    if (showUsernameDialog) {
        var usernameError by remember { mutableStateOf<String?>(null) }
        UsernameDialog(
            reason = "Para compartilhar plantas no mural, escolha um nome de usuário:",
            initialError = usernameError,
            onDismiss = { showUsernameDialog = false },
            onConfirm = { username ->
                usernameError = null
                onCheckUser(username, { _ ->
                    viewModel.onShareOnMuralChange(true)
                    showUsernameDialog = false
                }, {
                    usernameError = "Nome de usuário já está em uso"
                })
            }
        )
    }
}

private fun persistPhotoToAppStorage(context: Context, source: Uri): String? {
    return runCatching {
        val destination = createPersistentPhotoFile(context)
        val inputStream = context.contentResolver.openInputStream(source)
        if (inputStream != null) {
            inputStream.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destination).toString()
        } else {
            null
        }
    }.getOrNull()
}

private fun createPersistentPhotoFile(context: Context): File {
    val directory = File(context.filesDir, "plant_photos").apply { mkdirs() }
    return File(directory, "plant_${UUID.randomUUID()}.jpg")
}


