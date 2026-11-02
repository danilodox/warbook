# Warbook

Estatísticas e histórico de guerras para Clash of Clans. Android agora, iOS depois.

> Projeto de fã, sem vínculo com a Supercell.

## Módulos

| Módulo | O que tem |
|---|---|
| `shared/` | Contrato da API, domínio e regras usados pelo app e pelo servidor |
| `composeApp/` | UI compartilhada (Compose Multiplatform) |
| `androidApp/` | Entrada do app Android |
| `iosApp/` | Entrada do app iOS (precisa de Mac) |
| `server/` | Servidor Ktor |

## Como rodar

- **App Android:** abrir a pasta no Android Studio e rodar `androidApp`, ou `./gradlew :androidApp:installDebug`.
- **Servidor:** `./gradlew :server:run` → `http://localhost:8080/health` (no emulador: `http://10.0.2.2:8080`).
- **Testes e lint:** `./gradlew check`. Para corrigir a formatação: `./gradlew ktlintFormat`.

## Versão

`versionName` e `versionCode` saem da última tag `vX.Y.Z` (sem tag: `0.0.0-dev`). Histórico em [CHANGELOG.md](CHANGELOG.md).
