# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versões seguem [SemVer](https://semver.org/lang/pt-BR/).

## [Não lançado]

### Adicionado

- Servidor proxy para a API oficial do Clash of Clans (a chave é presa a IP e não pode ir no app):
  - `GET /v1/players/{tag}`, `/v1/clans/{tag}` e `/v1/clans/{tag}/war/current`, com DTOs próprios no `shared`.
  - Erros no formato `{ code, message }`: `INVALID_TAG`, `NOT_FOUND`, `WAR_LOG_PRIVATE`, `API_MAINTENANCE`, `RATE_LIMITED`, `UNAUTHORIZED` e `UPSTREAM_ERROR`.
  - Cache em memória (Caffeine): guerra 1 min, jogador 2 min, clã 5 min.
  - `POST /v1/installs` devolve um token JWT por instalação, exigido em todo o `/v1/*`.
  - Limite de requisições por IP e por instalação.
  - Configuração por variáveis de ambiente (`COC_API_KEY`, `JWT_SECRET`), lidas do `server/.env` fora do git; sem elas o servidor não sobe.
- Normalização de tag no `shared` (sem `#`, maiúsculas, `O`→`0`, só caracteres válidos).
- App: cliente Ktor com URL por ambiente (emulador `10.0.2.2`, celular pelo `warbook.serverUrl` no `local.properties`), token da instalação no DataStore e repositório com sucesso ou erro tipado.
- Aba Buscar com tela mínima de teste: tag → cartão do jogador ou mensagem de erro, em português e inglês.

### Segurança

- HTTP sem criptografia liberado só no build debug.
- Logs do servidor em INFO e sem a chave, o segredo ou o token (com teste).

## [0.1.0] - 2026-11-02

Recomeço do projeto em Kotlin Multiplatform. O app Android antigo fica na tag `arquivo/v0-android-antigo`.

### Adicionado

- Projeto KMP com os módulos `shared` (contrato da API), `composeApp` (UI compartilhada), `androidApp`, `iosApp` e `server`.
- Navegação type-safe com as abas vazias Eu, Clã, Guerra e Buscar, e Configurações na engrenagem do topo.
- Koin 4 para injeção de dependência.
- Tema base Material 3 (só tokens; o visual final vem depois).
- Textos em português e inglês com Compose Resources.
- Servidor Ktor com `GET /health`.
- Detekt e ktlint.
- CI no GitHub Actions: lint, testes, build do app e do servidor, Gitleaks e Trivy.
- `versionName` e `versionCode` derivados da tag git.

[Não lançado]: https://github.com/danilodox/warbook/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/danilodox/warbook/releases/tag/v0.1.0
