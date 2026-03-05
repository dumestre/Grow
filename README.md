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

## Download no GitHub (Release APK)
O projeto possui workflow em `.github/workflows/release.yml` para gerar e publicar APK assinado no GitHub Releases.

### 1. Configure os Secrets no GitHub
Em `Settings > Secrets and variables > Actions`, crie:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Para gerar `ANDROID_KEYSTORE_BASE64` no Windows (PowerShell):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("D:\\Keys\\grow-release.jks"))
```

### 2. Gere uma release por tag
Quando enviar uma tag `v*`, o workflow roda e publica o APK:
```powershell
git tag v1.0.0
git push origin v1.0.0
```

### 3. Onde baixar
O APK fica em `GitHub > Releases` com nome `Grow-vX.Y.Z.apk`.
