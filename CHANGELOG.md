# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versões seguem [SemVer](https://semver.org/lang/pt-BR/).

## [Não lançado]

## [0.3.0] - 2026-11-04

### Adicionado

- Servidor: `POST /v1/players/{tag}/verify` confere o token da API do jogo (`verifytoken`) e devolve uma prova de verificação (JWT com a tag e a instalação). O token do jogo não é guardado nem logado; o limite é próprio: 5 por hora por instalação e 10 por hora por IP.
- Jogador no contrato com liga, máquinas de cerco, pets e equipamentos de herói; super tropas e tropas da vila do construtor ficam de fora.
- App: Room 3 com as contas do aparelho e o último perfil de cada uma, para abrir sem internet.
- Onboarding: tag com ajuda "Onde acho minha tag?" → "É você?" → verificação opcional com passo a passo. "Só explorar, sem conta" abre a Buscar.
- Primeira tela: sem conta e sem "Só explorar" abre o onboarding; senão, a aba Eu.
- Aba Eu: perfil da conta ativa com heróis, equipamentos, tropas, feitiços, máquinas e pets em texto com barra de progresso, e os números da temporada. Puxar para atualizar, skeleton na primeira carga e faixa "Sem conexão" com o horário do perfil salvo.
- Troca de conta numa lista de baixo: tocar troca, segurar remove, e "Adicionar conta" reabre o onboarding. Selo "Verificado" na conta verificada.
- Configurações: gerenciar contas, verificar a conta ativa, versão e o aviso de conteúdo de fã da Supercell.
- Textos novos em português e inglês.

### Segurança

- O campo do token usa teclado de senha e o token não vai para o estado salvo da tela.
- Teste garante que o token do jogo e a prova não aparecem nos logs do servidor.

## [0.2.0] - 2026-11-02

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

[Não lançado]: https://github.com/danilodox/warbook/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/danilodox/warbook/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/danilodox/warbook/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/danilodox/warbook/releases/tag/v0.1.0
