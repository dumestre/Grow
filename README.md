# Grow (Android nativo)

App Android nativo em Kotlin para gerenciamento de cultivo pessoal.

## Stack
- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite)
- Navigation Compose
- ViewModel + StateFlow
- Coil
- WorkManager
- DataStore
- BiometricPrompt + PIN local

## Estrutura
```text
app/src/main/java/com/daime/grow/
  core/
    AppContainer.kt
  data/
    backup/BackupManager.kt
    local/
      dao/*
      entity/*
      migration/Migrations.kt
      GrowDatabase.kt
    preferences/SecurityPreferencesRepository.kt
    reminder/*
    repository/GrowRepositoryImpl.kt
  domain/
    model/*
    repository/GrowRepository.kt
    usecase/ChecklistFactory.kt
  ui/
    navigation/NavRoute.kt
    components/*
    screen/
      add/NewPlantScreen.kt
      detail/PlantDetailScreen.kt
      home/HomeScreen.kt
      lock/LockScreen.kt
      settings/SettingsScreen.kt
    theme/*
    viewmodel/*
    GrowRoot.kt
  GrowApplication.kt
  MainActivity.kt
```

## Build e execução
1. Abra o projeto no Android Studio (Hedgehog+ recomendado).
2. Sincronize o Gradle.
3. Rode `./gradlew :app:assembleDebug` (Windows: `gradlew.bat :app:assembleDebug`).
4. Instale no emulador/dispositivo com API 26+.
5. Para testes unitários: `./gradlew :app:testDebugUnitTest`.
6. Para compilar testes instrumentados: `./gradlew :app:assembleDebugAndroidTest`.

## Seeds
Os seeds são criados automaticamente em primeiro uso via `GrowRepositoryImpl.seedDataIfNeeded()`.
