package com.daime.grow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.daime.grow.data.preferences.SecurityPreferencesRepository
import com.daime.grow.data.worker.MuralNotificationWorker
import com.daime.grow.domain.model.DarkThemeMode
import com.daime.grow.ui.GrowRoot
import com.daime.grow.ui.theme.GrowTheme
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setupMuralWorker()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        checkNotificationPermission()

        val container = (application as GrowApplication).appContainer
        val securityRepository = SecurityPreferencesRepository(this)

        setContent {
            // Observar preferência de tema de forma reativa
            val darkThemeMode by securityRepository.observe()
                .map { it.darkTheme }
                .collectAsStateWithLifecycle(initialValue = DarkThemeMode.SYSTEM)

            val useDarkTheme = when (darkThemeMode) {
                DarkThemeMode.SYSTEM -> isSystemInDarkTheme()
                DarkThemeMode.LIGHT -> false
                DarkThemeMode.DARK -> true
            }

            GrowTheme(darkTheme = useDarkTheme) {
                GrowRoot(container)
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    setupMuralWorker()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            setupMuralWorker()
        }
    }

    private fun setupMuralWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MuralNotificationWorker>(
            1, TimeUnit.HOURS
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "mural_notifications_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
