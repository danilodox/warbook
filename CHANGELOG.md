# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versões seguem [SemVer](https://semver.org/lang/pt-BR/).

## [Não lançado]

## [0.8.2] - 2026-11-21

### Adicionado

- Perfil (abas Eu e Jogador): o máximo de cada herói, equipamento, tropa, feitiço, máquina e pet agora é o que o CV do jogador permite, e não mais o máximo do jogo. O nível aparece como "nível/máximo do CV", e quem já chegou nele ganha um ✓ dourado. O título de cada seção diz quantos estão no máximo do CV.
- Tabela de níveis por CV (`files/unit_levels.json`), gerada por `tools/gerar_niveis_cv.py` a partir do `static_data.json` do clashy.py (MIT). Ela também diz de qual herói é cada equipamento. Depois de uma atualização do jogo, basta rodar o script de novo. Unidade que não estiver na tabela usa o máximo do jogo.
- Equipamentos de herói agrupados por herói: cada herói numa linha com retrato, nome e quantos estão no máximo, e os equipamentos num carrossel horizontal.
- Ícones do Fan Kit para 42 dos 43 equipamentos (o Noble Iron não está no kit e segue com a sigla) e para Arremessador, Druida, Bruxa das Ruínas, Golem Meteoro, Esquadrão Barch, Esquadrão Mago Gigante, Esmagador de Pedras, Lançador de Tropas, Aerotáxi, Água-viva Brava, Corvo Cobiçoso e Feitiço Raiva Líquida.
- Nomes em português dos equipamentos e das unidades novas, com as traduções oficiais do jogo.

### Corrigido

- Heróis: Rei, Rainha e Guardião apareciam minúsculos; Príncipe Servo e Duque Dragão, só com a sigla. Agora os seis têm retrato no mesmo quadro e no mesmo tamanho.

## [0.8.1] - 2026-11-21

### Corrigido

- Widget: abre em 3×1 no launcher. Antes ocupava 3×2, porque a altura mínima pedia duas células. Em uma linha mostra o placar, o tempo e os ataques que faltam; aumentado, mostra o layout completo.
- Widget: tem prévia na lista de widgets, com uma guerra de exemplo. No Android 15 ou mais novo ela é gerada pelo próprio widget; nas versões anteriores é uma imagem, em claro e escuro, em português e inglês.
- Widget: as cores vêm do tema do app, sem cópia à mão, e seguem o tema escolhido nas Configurações (escuro, claro ou o do aparelho). Trocar o tema redesenha o widget na hora.
- Servidor: quando a API do jogo recusa a conexão (acontece às vezes num dos IPs dela), tenta de novo até 2 vezes antes de dar erro. Timeout e erros da API continuam sem nova tentativa.

## [0.8.0] - 2026-11-20

### Adicionado

- Monitoramento de guerras: qualquer membro verificado liga o monitoramento do clã no cartão da aba Guerra. O Warbook importa na hora o registro de guerra do clã e passa a guardar cada guerra nova. Um clã pausado volta sozinho quando um membro verificado abre o app.
- Servidor: agendador que roda de hora em hora e tira um retrato completo de cada guerra 4 minutos antes do fim e logo depois que ela acaba, incluindo as rodadas da CWL. Ao subir, ele recupera as guerras que terminaram com o servidor desligado.
- Nota de cada ataque, calculada no `shared`: estrelas e destruição pesadas pela diferença de CV. Atacar o espelho vale fator 1,0. Alvo fácil e limpeza só contam quando o CV do alvo é menor que o do atacante. Cada ataque não usado vale −2.
- Histórico de guerras do clã: placar geral (vitórias, derrotas e empates) e a lista das guerras guardadas. O detalhe de cada guerra mostra o mapa com a nota de cada membro, quem não atacou e os alvos fáceis.
- Desempenho do jogador: média das notas, tendência, ataques usados e quantos foram em alvo fácil ou limpeza.
- Atividade dos membros: quantos ataques cada membro usou nas últimas guerras, com quem ficou devendo no topo.
- Planejador da CWL: sugere a escalação de 15 ou 30 pela nota × presença nas últimas 30 guerras guardadas. Tocar num membro tira ou põe na escalação, com contador e "Restaurar sugestão".
- Resumos para o WhatsApp: fim de guerra (resultado, melhores notas, quem não atacou e alvos fáceis), temporada da CWL (rodadas, placar e os melhores do clã) e escalação do planejador (com os reservas).
- Widget Android da guerra atual: placar, tempo que falta e quantos ataques ainda faltam. Atualiza a cada 30 minutos, o mínimo do Android, e abre o app ao tocar.
- Servidor: rotas `/v1/clans/{tag}/monitoring`, `/v1/clans/{tag}/wars`, `/v1/clans/{tag}/wars/{id}`, `/v1/clans/{tag}/players/{playerTag}/history`, `/v1/clans/{tag}/activity`, `/v1/clans/{tag}/cwl/season` e `/v1/clans/{tag}/cwl/planner`.
- Textos novos em português e inglês.

### Alterado

- Servidor: migração V2 do Flyway cria as tabelas `monitored_clans`, `wars`, `war_members` e `war_attacks`. Ela roda sozinha na primeira subida.
- Dependência nova no Android: Glance 1.2.0, usado no widget.

## [0.7.0] - 2026-11-17

### Adicionado

- Busca de jogador por nick: Buscar › Jogador aceita o nick (3 a 50 letras) além da tag e mostra até 20 resultados com tag, CV e clã. Só aparecem jogadores que o Warbook já viu (perfis abertos e membros dos clãs abertos), e a tela avisa isso. Acentos, maiúsculas e letras estilizadas (ᴅᴀɴɪʟᴏ, ｄａｎｉｌｏ) não atrapalham.
- Servidor: `GET /v1/players/search?name=` com o índice de nicks no Postgres (pg_trgm). Fora do tamanho responde `INVALID_QUERY` (400), com o mesmo limite de pedidos da busca de clã. O índice é alimentado em segundo plano por `/v1/players/{tag}` e pelos membros de `/v1/clans/{tag}`, sem varredura.
- Raids da capital: sub-aba Clã › Capital no seu clã e na tela de qualquer clã. Medalhas de raid em destaque (por ataque, defesa e o total com 6 ataques), ataques de cada membro (x de 5 ou 6) com o Ouro da Capital saqueado, quem não atacou no raid mais recente, ataques sobrando, distritos destruídos, clãs atacados e defesas. As temporadas anteriores ficam em fichas por data.
- Resumo do raid para o WhatsApp, preso embaixo da Capital como o da Guerra: total de Ouro da Capital, ataques, distritos, medalhas, quem não atacou e quem atacou menos.
- Servidor: `GET /v1/clans/{tag}/capitalraidseasons` com as últimas 10 temporadas num DTO próprio e cache de 2 minutos. Funciona também com o registro de guerra privado.
- Textos novos em português e inglês.

### Alterado

- Servidor: agora precisa de um Postgres (`DATABASE_URL`, `DATABASE_USER` e `DATABASE_PASSWORD` no `server/.env`) e não sobe sem ele. As tabelas são criadas pelo Flyway na primeira subida.
- Buscar › Jogador: texto sem `#` só é tratado como tag quando tem algum número, como já era em Clã. Nicks como "Ryu" vão para a busca por nick.

### Corrigido

- Tema claro: ao abrir o app, o tema escuro não aparece mais por um instante antes do claro.
- Textos em inglês: o apóstrofo aparecia com uma barra antes (`don\'t`).

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

[Não lançado]: https://github.com/danilodox/warbook/compare/v0.8.1...HEAD
[0.8.1]: https://github.com/danilodox/warbook/compare/v0.8.0...v0.8.1
[0.8.0]: https://github.com/danilodox/warbook/compare/v0.7.0...v0.8.0
[0.7.0]: https://github.com/danilodox/warbook/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/danilodox/warbook/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/danilodox/warbook/compare/v0.4.1...v0.5.0
[0.4.1]: https://github.com/danilodox/warbook/compare/v0.4.0...v0.4.1
[0.4.0]: https://github.com/danilodox/warbook/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/danilodox/warbook/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/danilodox/warbook/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/danilodox/warbook/releases/tag/v0.1.0
