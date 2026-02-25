package com.daime.grow.ui.screen.add

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daime.grow.R
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.ui.components.PhotoPickerBox
import com.daime.grow.ui.components.RoundedBackButton
import com.daime.grow.ui.viewmodel.AddPlantUiEvent
import com.daime.grow.ui.viewmodel.AddPlantViewModel
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewPlantScreen(
    innerPadding: PaddingValues,
    viewModel: AddPlantViewModel,
    onSaved: (Long) -> Unit,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraPhoto by remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val requiredFieldsError = stringResource(R.string.new_plant_required_fields_error)
    val savedMessage = stringResource(R.string.new_plant_saved)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AddPlantUiEvent.RequiredFieldsError -> snackbarHostState.showSnackbar(requiredFieldsError)
                AddPlantUiEvent.Saved -> snackbarHostState.showSnackbar(savedMessage)
            }
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val persisted = uri?.let { persistPhotoToAppStorage(context, it) }
        viewModel.onPhotoSelected(persisted)
    }

    val getContentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val persisted = uri?.let { persistPhotoToAppStorage(context, it) }
        viewModel.onPhotoSelected(persisted)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            viewModel.onPhotoSelected(pendingCameraPhoto)
        } else {
            pendingCameraFile?.let { runCatching { it.delete() } }
        }
        pendingCameraPhoto = null
        pendingCameraFile = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = createPersistentPhotoFile(context)
            val providerUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCameraFile = file
            pendingCameraPhoto = providerUri.toString()
            cameraUri = providerUri
            cameraUri?.let { cameraLauncher.launch(it) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.strain,
                        onValueChange = viewModel::onStrainChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.new_plant_strain)) },
                        singleLine = true
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
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.new_plant_medium)) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.days,
                        onValueChange = viewModel::onDaysChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.new_plant_days)) },
                        singleLine = true
                    )
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .align(androidx.compose.ui.Alignment.BottomCenter)
        )
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    showSheet = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        getContentLauncher.launch("image/*")
                    }
                }) {
                    Text(stringResource(R.string.new_plant_gallery))
                }

                TextButton(onClick = {
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
                }) {
                    Text(stringResource(R.string.new_plant_camera))
                }
            }
        }
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

