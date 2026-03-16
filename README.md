# Grow (Android nativo)

App Android nativo em Kotlin para gerenciamento de cultivo pessoal.

[![Version](https://img.shields.io/badge/version-1.3-blue.svg)](https://github.com/seu-usuario/Grow/releases)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com/)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9%2B-purple.svg)](https://kotlinlang.org/)

## 📱 Sobre

O **Grow** é um aplicativo para gerenciamento de cultivo pessoal, desenvolvido com Jetpack Compose e Material Design 3. Acompanhe fases de crescimento, regas, nutrientes e muito mais.

### ✨ Principais Recursos

- 🪴 **Gerenciamento de Plantas** - Cadastro ilimitado com fotos e informações detalhadas
- 💧 **Controle de Regas** - Lembretes automáticos e histórico completo
- 📊 **Nutrientes** - Acompanhamento de EC, pH e semana de cultivo
- ✅ **Checklist por Fase** - Tarefas específicas para cada etapa
- 📅 **Timeline** - Histórico cronológico de todos os eventos
- 🌐 **Mural da Comunidade** - Compartilhe seu progresso (opcional)
- 🔒 **Segurança** - Bloqueio por PIN ou biometria
- 🌙 **Tema Escuro** - Suporte nativo a dark mode

## 📸 Screenshots

<div align="center">
  <img src="fastlane/metadata/android/pt-BR/images/phoneScreenshots/1.png" width="200" alt="Tela inicial"/>
  <img src="fastlane/metadata/android/pt-BR/images/phoneScreenshots/2.png" width="200" alt="Detalhes da planta"/>
  <img src="fastlane/metadata/android/pt-BR/images/phoneScreenshots/3.png" width="200" alt="Pós-colheita"/>
</div>

## 📥 Download

### Google Play Store
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" alt="Disponível na Google Play" height="80">](https://play.google.com/store/apps/details?id=com.daime.grow)

### GitHub Releases
Baixe a versão mais recente diretamente do [GitHub Releases](https://github.com/seu-usuario/Grow/releases).

## 🛠️ Stack Tecnológica

- **Linguagem:** Kotlin 1.9+
- **UI:** Jetpack Compose + Material Design 3
- **Arquitetura:** MVVM + Clean Architecture
- **Banco de Dados:** Room (SQLite)
- **Navegação:** Navigation Compose
- **Reatividade:** StateFlow + Flow
- **DI:** Manual (ViewModel Factories)
- **Imagens:** Coil
- **Background:** WorkManager
- **Preferências:** DataStore
- **Segurança:** BiometricPrompt + Criptografia Android
- **Backend (Opcional):** Supabase (Mural da Comunidade)

## 📁 Estrutura do Projeto

```
app/src/main/java/com/daime/grow/
├── core/                      # Módulo core
│   └── AppContainer.kt
├── data/                      # Camada de dados
│   ├── backup/
│   ├── local/                 # Room Database
│   │   ├── dao/
│   │   ├── entity/
│   │   └── migration/
│   ├── preferences/
│   ├── reminder/
│   ├── repository/
│   └── remote/                # Supabase
├── domain/                    # Regras de negócio
│   ├── model/
│   ├── repository/
│   └── usecase/
└── ui/                        # Camada de apresentação
    ├── components/
    ├── navigation/
    ├── screen/
    ├── theme/
    ├── util/
    └── viewmodel/
```

## 🚀 Build e Execução

### Pré-requisitos
- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 11+
- Android SDK 26+

### Passos

1. **Clone o repositório**
   ```bash
   git clone https://github.com/seu-usuario/Grow.git
   cd Grow
   ```

2. **Configure as variáveis de ambiente (opcional)**
   Crie `local.properties` na raiz:
   ```properties
   SUPABASE_URL=sua_url_aqui
   SUPABASE_ANON_KEY=sua_chave_aqui
   ```

3. **Sincronize o Gradle**
   - Abra no Android Studio
   - Aguarde a sincronização

4. **Execute o app**
   - Selecione um dispositivo/emulador
   - Pressione Run (Shift+F10)

### Comandos Gradle

```bash
# Build debug
./gradlew assembleDebug

# Build release (AAB)
./gradlew bundleRelease

# Testes unitários
./gradlew testDebugUnitTest

# Testes instrumentados
./gradlew assembleDebugAndroidTest
```

## 🔐 Configuração de Release

### 1. Gerar Keystore
```bash
keytool -genkey -v -keystore grow-release.jks -keyalg RSA -keysize 2048 -alias grow -validity 10000
```

### 2. Configurar keystore.properties
```properties
storePassword=SUA_SENHA
keyPassword=SUA_SENHA_DE_CHAVE
keyAlias=grow
storeFile=C\\:/caminho/para/grow-release.jks
```

### 3. Gerar AAB assinado
```bash
./gradlew clean
./gradlew bundleRelease
```

## 📚 Documentação

- [**MELHORIAS_REALIZADAS.md**](MELHORIAS_REALIZADAS.md) - Histórico completo de melhorias
- [**PLAY_STORE_GUIDE.md**](PLAY_STORE_GUIDE.md) - Guia de publicação na Play Store
- [**PRIVACY_POLICY.md**](PRIVACY_POLICY.md) - Política de Privacidade

## 🤝 Contribuindo

Contribuições são bem-vindas! Siga os passos:

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

### Padrões de Código

- **Lint:** `./gradlew lint`
- **Format:** ktlint (configurado no projeto)
- **Convenções:** [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)

## 📄 Licença

Este projeto está licenciado sob a MIT License - veja o arquivo [LICENSE](LICENSE) para detalhes.

```
MIT License

Copyright (c) 2024-2026 Grow

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

## 📞 Contato

- **GitHub:** [@seu-usuario](https://github.com/seu-usuario)
- **E-mail:** seu-email@exemplo.com
- **Play Store:** [Grow - Gerenciador de Cultivo](https://play.google.com/store/apps/details?id=com.daime.grow)

## 🙏 Agradecimentos

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Supabase](https://supabase.com/)
- [Android Developers](https://developer.android.com/)

---

<div align="center">
  <strong>Feito com 💚 e Kotlin</strong>
</div>
