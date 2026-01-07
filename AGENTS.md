# AGENTS.md

## Build Commands
- Build: `gradlew.bat build` (Windows) or `./gradlew build`
- Run IDE with plugin: `gradlew.bat runIde`
- Test: `gradlew.bat test`
- Verify plugin: `gradlew.bat verifyPlugin`
- Check (lint + test): `gradlew.bat check`

## Architecture
JetBrains IDEA plugin for AI-powered Git commit message generation (Kotlin + Gradle).

- `src/main/kotlin/com/example/aicommit/` - Main source code
  - `action/` - IntelliJ action (GenerateCommitMessageAction)
  - `config/` - Settings/configuration (AICommitConfig, AICommitConfigurable)
  - `core/` - Core models (OpenAIResponse, LanguageMapper)
  - `util/` - Utilities (CommitMessageGenerator, DiffExtractor)
- `src/main/resources/META-INF/plugin.xml` - Plugin descriptor
- HTTP client: OkHttp + Jackson for JSON; API key stored via IntelliJ PasswordSafe

## Code Style
- **Language**: Kotlin, JVM target 17
- **Imports**: Explicit imports, no wildcards; IntelliJ Platform APIs + OkHttp + Jackson
- **Naming**: PascalCase for classes, camelCase for functions/properties
- **Error handling**: Custom `AICommitException`; use try-catch with meaningful messages
- **Comments**: In Chinese (项目中文注释); avoid unnecessary comments
