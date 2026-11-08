# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versões seguem [SemVer](https://semver.org/lang/pt-BR/).

## [Não lançado]

### Corrigido

- Tema claro: ao abrir o app, o tema escuro não aparece mais por um instante antes do claro.

## [0.6.0] - 2026-11-08

### Adicionado

- Visual novo "livro de guerra": paleta própria em tema escuro e claro, com cores fixas (sem as cores do papel de parede). Títulos e números em Cinzel, texto em Alegreya Sans com números alinhados e tags em JetBrains Mono, as três com licença OFL na pasta `files/licenses`.
- Componentes do design: escudo com o nível do CV, quadros de números, barra de nível (dourada em progresso, couro no máximo), grade de tropas com o nível no canto e borda dourada no máximo, placar da guerra, faixa offline, erro de tela cheia com código, skeleton que respeita "menos movimento" e 19 ícones de traço.
- Telas no design novo: onboarding (logo, campo da tag em monoespaçada, ajuda e aviso de fã), aba Eu, Jogador, aba e tela Clã, Guerra, CWL, Buscar e Configurações.
- Guerra: quem falta atacar em fichas, mapa com as estrelas de cada ataque e o CV do adversário na mesma posição, e o resumo do WhatsApp preso embaixo da tela, com "Personalizar" numa folha que sobe de baixo.
- Configurações: tema Escuro (padrão), Claro ou Igual ao sistema, salvo no aparelho; as barras do sistema seguem o tema. Aviso de conteúdo de fã com o texto completo e o link da política da Supercell.
- Nomes de tropas, feitiços, máquinas, heróis e pets em português quando o app está em português. O que não está na tabela, como equipamentos, fica com o nome do jogo.
- Ícone do app novo (livro de couro, espadas e três estrelas), adaptativo e com camada monocromática para os ícones temáticos do Android 13+. PNG de 512 para a Play Store em `androidApp/src/main/ic_launcher-playstore.png`.
- Números com separador de milhar (5.312).
- Ícones do Fan Kit da Supercell nas tropas, feitiços, máquinas, pets e heróis do perfil: 66 imagens em WebP de 192 px (617 KB no total), cortadas em quadrado. Unidades que o kit não tem e os equipamentos ficam com as iniciais.
- Cartão de perfil: o ícone Compartilhar no topo das telas Eu e Jogador abre uma prévia e gera uma imagem PNG de 1080 × 1350 (4:5, o WhatsApp mostra inteira) no tema do app, com cantoneiras douradas, nome, tag, clã, CV, troféus, estrelas de guerra, nível, 4 heróis e o aviso de fã. No Android vai pelo share sheet; no iOS o ícone fica escondido por enquanto.
- Comparar: "Comparar comigo" na tela Jogador (ao lado de Seguir, com uma conta ativa) abre a sua conta e o jogador lado a lado: CV, nível, troféus e estrelas de guerra, cada herói e as doações e ataques vencidos da temporada. Quem está à frente fica em verde com ▲; empate fica neutro. No topo, o resumo de quem ganha em mais itens. Atualiza os dois perfis ao abrir e ao puxar, e sem internet compara com os perfis salvos.

### Removido

- Tela "Em breve" (`PlaceholderScreen`), que não era mais usada.

## [0.5.0] - 2026-11-07

### Adicionado

- Servidor: `/v1/clans/{tag}` mais completo, com tipo de entrada, nível e liga de cada membro (a liga nova do ranqueado tem preferência sobre a antiga).
- Servidor: `GET /v1/clans/search?name=` busca clãs por nome, com mínimo de 3 letras e até 20 resultados num DTO enxuto (tag, nome, brasão, nível, membros e local). Fora do tamanho responde `INVALID_QUERY` (400). Cache de 5 minutos por nome.
- Aba Clã: Info e Membros do clã da conta ativa. Membros ordena por posição, CV, troféus e doações, mostra a liga, as doações e as recebidas da temporada, e marca "você". Tocar num membro abre a tela Jogador. Estados para sem conta, sem clã (com "Buscar clãs"), primeira carga e sem conexão com o último resultado salvo. Puxar para atualizar.
- Tela Jogador: o perfil de qualquer jogador com os componentes da aba Eu e o botão Seguir. A linha do clã abre a tela Clã.
- Tela Clã: Info, Membros e Guerra de qualquer clã. A Guerra usa os componentes da aba Guerra (guerra atual, CWL e resumo do WhatsApp), com "você" ainda marcado pela conta ativa.
- Aba Buscar: seletor Jogador | Clã. O jogador é buscado por tag; texto que não é tag avisa que a busca por nick chega na v0.7. O clã é buscado por tag ou por nome, e os resultados têm uma estrela para seguir sem abrir o clã.
- Seguindo: jogadores e clãs juntos, até 50 no total, abrem sem internet com o último dado. "Editar" mostra o remover de cada um.
- Recentes: os 10 últimos jogadores e clãs abertos pela busca, com "Limpar".
- Brasões de clã e ícones de liga com Coil 3, com marcador enquanto carrega e quando falha.
- Room na versão 3, com migração automática: clãs salvos, a lista Seguindo e o clã de cada conta. Vão para o banco só as contas, os clãs das contas e o que é seguido; o resto fica na memória enquanto o app está aberto.
- Textos novos em português e inglês.

### Corrigido

- Resumo do WhatsApp: `*` e `_` nos nomes não viram mais negrito nem itálico (só no texto gerado; o app mostra o nome original).

## [0.4.1] - 2026-11-07

### Adicionado

- Resumo do WhatsApp personalizável: em "Personalizar", abaixo de Copiar e Compartilhar, uma ficha por parte (placar, tempo, quem falta atacar ou quem não atacou, força por CV) e a prévia exata do texto. Só aparecem as fichas que mudam o texto naquele estado.
- Partes novas, desligadas por padrão: "Melhores ataques" (os 3 estrelas; sem nenhum, os 3 melhores por estrelas e destruição) e "Ataques ruins" (0 ou 1 estrela), no formato `• Dantas → #4 (CV 15) ★★★ 100%`, até 5 linhas e um "+N". Entram em guerra, na encerrada e na CWL; não na preparação.
- A escolha fica salva no aparelho e vale para a guerra e a CWL. Sem mexer, o texto é o mesmo da v0.4.0.

## [0.4.0] - 2026-11-06

### Adicionado

- Servidor: guerra atual completa, com posição do alvo e ordem de cada ataque, quantas vezes cada membro foi atacado e o melhor ataque sofrido. As posições do mapa vão de 1 a N, sem buracos.
- Servidor: `GET /v1/clans/{tag}/cwl/current` junta o grupo da CWL (`currentwar/leaguegroup`) e a guerra de cada rodada (`clanwarleagues/wars/{warTag}`) num DTO próprio: rodadas com a guerra do clã sempre do lado `clan`, rodada atual e classificação do grupo (estrelas + 10 por vitória e destruição somada). Rodadas não sorteadas (`#0`) não geram chamada. Fora da CWL responde com o estado `NOT_IN_CWL`; registro privado vira `WAR_LOG_PRIVATE`.
- Servidor: cache da CWL de 1 minuto, guerras de rodadas encerradas guardadas por 24 horas e até 4 buscas de rodada ao mesmo tempo.
- App: aba Guerra com as sub-abas Guerra atual e CWL, sempre do clã da conta ativa. Estados: convite sem conta, sem clã, registro privado com passo a passo, sem guerra, procurando adversário, preparação, em guerra, encerrada e fora da CWL.
- Ataques pendentes em destaque (nome e quantos faltam, com "você" marcado) e, na guerra encerrada, quem não atacou.
- Comparação de força na preparação só por CV: CV médio, CV máximo e quantos de cada CV.
- CWL: fichas das rodadas, guerra da rodada escolhida e classificação do grupo.
- Resumo para o WhatsApp com modelo fixo (placar, tempo restante e quem falta atacar; força por CV na preparação; rodada na CWL), em português e inglês. Botões Copiar e Compartilhar (share sheet do Android; no iOS só Copiar).
- Atualiza só ao abrir a aba e ao puxar. Skeleton na primeira carga, faixa "Sem conexão" e o último resultado salvo no Room (banco na versão 2, com migração automática).
- Textos novos em português e inglês.

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

[Não lançado]: https://github.com/danilodox/warbook/compare/v0.6.0...HEAD
[0.6.0]: https://github.com/danilodox/warbook/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/danilodox/warbook/compare/v0.4.1...v0.5.0
[0.4.1]: https://github.com/danilodox/warbook/compare/v0.4.0...v0.4.1
[0.4.0]: https://github.com/danilodox/warbook/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/danilodox/warbook/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/danilodox/warbook/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/danilodox/warbook/releases/tag/v0.1.0
