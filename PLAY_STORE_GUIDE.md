# Guia de Publicação na Play Store - Grow

## Checklist de Publicação

### 1. Preparação do Projeto

#### Atualizar Versão
```kotlin
// app/build.gradle.kts
defaultConfig {
    applicationId = "com.daime.grow"
    minSdk = 26
    targetSdk = 36
    versionCode = 4  // Incremente para cada release
    versionName = "1.3"  // Atualize conforme necessário
}
```

#### Gerar Keystore de Release
```bash
# Windows (PowerShell)
keytool -genkey -v -keystore grow-release.jks -keyalg RSA -keysize 2048 -alias grow -validity 10000
```

**Guarde em local seguro:**
- Arquivo `.jks` ou `.keystore`
- Senha do keystore
- Senha da chave (key password)
- Alias da chave

#### Configurar Assinatura no Gradle
Crie `keystore.properties` na raiz do projeto (não commit no Git):
```properties
storePassword=SUA_SENHA_AQUI
keyPassword=SUA_SENHA_DE_CHAVE_AQUI
keyAlias=grow
storeFile=../caminho/para/grow-release.jks
```

Atualize `app/build.gradle.kts`:
```kotlin
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    ...
    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = keystoreProperties["storeFile"] as File?
            storePassword = keystoreProperties["storePassword"] as String?
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 2. Gerar Android App Bundle (.aab)

```bash
# Limpar projeto
./gradlew clean

# Gerar AAB assinado
./gradlew bundleRelease

# O arquivo será gerado em:
# app/build/outputs/bundle/release/app-release.aab
```

### 3. Testar Build de Release

```bash
# Instalar em dispositivo físico para teste
./gradlew installRelease

# Ou usar o AAB no Google Play Console (teste interno)
```

### 4. Preparar Assets para Play Store

#### Ícones e Imagens Necessárias

| Asset | Dimensões | Formato | Observações |
|-------|-----------|---------|-------------|
| **Ícone do App** | 512x512 px | PNG 32-bit | Sem transparência, fundo sólido |
| **Feature Graphic** | 1024x500 px | PNG ou JPEG | Usada na destaque da Play Store |
| **Screenshots (Telefone)** | Mín. 320px, Máx. 3840px | PNG ou JPEG | Mínimo 2 screenshots |
| **Screenshots (Tablet)** | Mín. 320px, Máx. 3840px | PNG ou JPEG | Opcional, mas recomendado |
| **Vídeo Promocional** | - | YouTube | Opcional |

#### Comandos para Gerar Screenshots
```bash
# Usando Android Studio
# 1. Abra o app no emulador/dispositivo
# 2. Clique no ícone de câmera no Device Explorer
# 3. Salve as imagens

# Ou usando ADB
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### 5. Criar Conta de Desenvolvedor Google Play

1. Acesse: https://play.google.com/console
2. Clique em "Criar conta"
3. Pague a taxa única de US$ 25
4. Complete o cadastro com:
   - Nome ou razão social
   - Endereço
   - Telefone
   - E-mail

### 6. Criar Ficha da Loja

#### Informações Básicas
- **Nome do app:** Grow - Gerenciador de Cultivo
- **ID do pacote:** com.daime.grow
- **Categoria principal:** Estilo de vida
- **Categoria secundária:** Produtividade

#### Descrição Curta (80 caracteres)
```
Gerencie seu cultivo pessoal com acompanhamento de fases, regas e nutrientes.
```

#### Descrição Completa (4000 caracteres)
```
🌱 Grow - Seu companheiro de cultivo pessoal

Acompanhe o desenvolvimento das suas plantas de forma simples e organizada. 
O Grow ajuda você a gerenciar todas as etapas do cultivo, desde o plantio 
até a colheita.

✨ PRINCIPAIS RECURSOS:

🪴 Gerenciamento de Plantas
- Cadastro ilimitado de plantas
- Fotos para acompanhamento visual
- Informações detalhadas (espécie, fase, substrato)
- Contador de dias de cultivo

💧 Controle de Regas e Nutrientes
- Lembretes automáticos de rega
- Registro de volume e intervalo
- Acompanhamento de EC e pH
- Histórico completo

✅ Checklist por Fase
- Tarefas específicas para cada fase
- Marcação de conclusão
- Acompanhamento de progresso

📊 Timeline de Eventos
- Registro automático de todas as ações
- Histórico de fases, regas e nutrientes
- Visualização cronológica

🔒 Segurança e Privacidade
- Bloqueio por PIN ou biometria
- Dados armazenados localmente
- Backup e restauração
- Total privacidade dos seus dados

🌐 Mural da Comunidade (Opcional)
- Compartilhe seu progresso
- Veja cultivos de outros usuários
- Use apelido para privacidade

🛒 Loja de Produtos
- Produtos recomendados para cultivo
- Nutrientes e substratos
- Ferramentas e acessórios

📱 Recursos Técnicos:
- Funciona offline (exceto Mural)
- Interface moderna Material Design 3
- Tema escuro automático
- Acessibilidade
- Backup em JSON

🔐 PRIVACIDADE:
Seus dados são armazenados localmente e NÃO são compartilhados. 
O compartilhamento no Mural é opcional e você controla o que publicar.

📥 BAIXE AGORA e organize seu cultivo com o Grow!
```

#### Classificação de Conteúdo
- **Classificação etária:** Livre (para todos os públicos)
- **Justificativa:** Aplicativo de produtividade/jardinagem sem conteúdo inadequado

### 7. Declaração de Segurança de Dados

Acesse: **Conteúdo do app > Segurança de dados**

#### Responda:

**Seus dados coletados:**
- ✅ Fotos (armazenadas localmente)
- ✅ Informações do aplicativo (dados de cultivo)
- ❌ Dados pessoais (nome, e-mail, etc.)
- ❌ Localização
- ❌ Informações financeiras

**Compartilhamento de dados:**
- ❌ Nenhum dado é compartilhado com terceiros
- ✅ Dados do Mural são visíveis a outros usuários (opcional)

**Práticas de segurança:**
- ✅ Dados em trânsito criptografados (HTTPS/Supabase)
- ✅ Dados em repouso criptografados (Room/SQLite)
- ✅ Usuário pode solicitar exclusão de dados

### 8. Política de Privacidade

1. Hospede o arquivo `PRIVACY_POLICY.md` em um URL público:
   - GitHub Pages: https://seu-usuario.github.io/Grow/privacy.html
   - Site pessoal
   - Google Docs (público)

2. Na Play Store, em **Conteúdo do app > Política de privacidade**:
   - Cole o URL da política

### 9. Upload do App

1. Acesse **Produção > Criar nova release**
2. Clique em **Criar**
3. Faça upload do `app-release.aab`
4. Preencha as informações da release:
   - Nome da versão (ex: "1.3 - Melhorias de UI")
   - Notas da versão (lista de mudanças)
5. Clique em **Salvar**
6. Revise e clique em **Iniciar lançamento para produção**

### 10. Pós-Lançamento

#### Monitoramento
- **Estatísticas:** Usuários instalados, desinstalações, avaliações
- **Crashlytics:** Relatórios de crash (se configurado)
- **Comentários:** Responda avaliações dos usuários

#### Atualizações
- Incremente `versionCode` e `versionName`
- Gere novo AAB assinado
- Faça upload como nova release

### 11. Configurações Adicionais

#### País/Região de Disponibilidade
- Selecione os países onde o app estará disponível
- Brasil (padrão)
- Outros países de língua portuguesa (opcional)

#### Preço e Distribuição
- **Preço:** Gratuito
- **Anúncios:** Não
- **Compras no app:** Não
- **Restrições de conteúdo:** Nenhuma

#### Acesso ao App
- **Login necessário:** Não
- **Instruções de login:** N/A

### 12. Timeline Estimada

| Etapa | Tempo |
|-------|-------|
| Preparação do projeto | 1-2 horas |
| Geração de assets | 2-4 horas |
| Revisão do Google Play | 1-7 dias |
| **Total** | **3-10 dias** |

### 13. Links Úteis

- [Central do Google Play Console](https://support.google.com/googleplay/android-developer)
- [Políticas de Conteúdo](https://play.google.com/about/developer-content-policy/)
- [Melhores Práticas de UI](https://developer.android.com/design/ui/mobile)
- [Guia de Lançamento](https://developer.android.com/google/play/publish)

---

## Contato para Suporte

Para dúvidas sobre o processo de publicação, consulte:
- Documentação oficial do Android Developer
- Fórum do Google Play Console
- Comunidade de desenvolvedores Android
