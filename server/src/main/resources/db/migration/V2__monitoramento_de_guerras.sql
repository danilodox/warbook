-- Monitoramento de guerras (v0.8.0). A API do jogo só mostra a guerra atual; o histórico é este banco.
-- Nada daqui é apagado: o clã pausa, mas as guerras ficam. Tags sem o '#', já normalizadas.

-- Clãs com o monitoramento ligado. Ninguém desliga; pausa sozinho quando last_seen_at fica com mais de
-- 30 dias (calculado na leitura, sem coluna de estado).
CREATE TABLE monitored_clans (
    clan_tag           TEXT        PRIMARY KEY,
    clan_name          TEXT        NOT NULL,
    -- Membro verificado que ligou (só para referência).
    activated_by       TEXT        NOT NULL,
    activated_at       TIMESTAMPTZ NOT NULL,
    -- Última vez que um membro verificado abriu o app (ou ativou/reativou).
    last_seen_at       TIMESTAMPTZ NOT NULL,
    -- Quando o warlog foi importado (uma vez, ao ativar).
    warlog_imported_at TIMESTAMPTZ
);

-- Uma linha por guerra, do ponto de vista do clã monitorado (clan_* = nós, opponent_* = eles).
-- Identidade: clã + fim da guerra (não muda depois da preparação e também vem no warlog).
CREATE TABLE wars (
    id                     BIGSERIAL        PRIMARY KEY,
    clan_tag               TEXT             NOT NULL REFERENCES monitored_clans (clan_tag),
    -- 'WAR' (guerra comum) ou 'CWL' (rodada da liga).
    war_type               TEXT             NOT NULL,
    -- 'WARLOG' (só o resultado) ou 'SNAPSHOT' (retrato completo, com membros e ataques).
    source                 TEXT             NOT NULL,
    -- Estado no último retrato: 'PREPARATION', 'IN_WAR' ou 'WAR_ENDED'.
    state                  TEXT             NOT NULL,
    -- Só na CWL: tag da rodada (com '#') e temporada (ex.: 2026-11).
    war_tag                TEXT             UNIQUE,
    cwl_season             TEXT,
    team_size              SMALLINT         NOT NULL,
    attacks_per_member     SMALLINT         NOT NULL,
    preparation_start_time TIMESTAMPTZ,
    start_time             TIMESTAMPTZ,
    end_time               TIMESTAMPTZ      NOT NULL,
    clan_name              TEXT             NOT NULL,
    clan_stars             SMALLINT         NOT NULL,
    clan_destruction       DOUBLE PRECISION NOT NULL,
    -- Ataques usados (o warlog nem sempre manda: nulo).
    clan_attacks           SMALLINT,
    opponent_tag           TEXT,
    opponent_name          TEXT,
    opponent_badge_url     TEXT,
    opponent_stars         SMALLINT         NOT NULL,
    opponent_destruction   DOUBLE PRECISION NOT NULL,
    opponent_attacks       SMALLINT,
    -- 'WIN', 'LOSE' ou 'TIE'; nulo enquanto a guerra não acabou.
    result                 TEXT,
    captured_at            TIMESTAMPTZ      NOT NULL,
    UNIQUE (clan_tag, end_time)
);

CREATE INDEX wars_clan_end ON wars (clan_tag, end_time DESC);

-- Escalação dos dois lados (precisa do CV e da posição do inimigo para reconstruir o mapa).
CREATE TABLE war_members (
    war_id       BIGINT   NOT NULL REFERENCES wars (id),
    tag          TEXT     NOT NULL,
    is_opponent  BOOLEAN  NOT NULL,
    name         TEXT     NOT NULL,
    town_hall    SMALLINT NOT NULL,
    -- Posição de 1 a N no mapa (sem os buracos da mapPosition da API).
    map_position SMALLINT NOT NULL,
    PRIMARY KEY (war_id, tag)
);

CREATE INDEX war_members_tag ON war_members (tag);

-- Ataques dos dois lados. attack_order é a ordem da API ('order'), única dentro da guerra.
CREATE TABLE war_attacks (
    war_id       BIGINT   NOT NULL REFERENCES wars (id),
    attack_order SMALLINT NOT NULL,
    attacker_tag TEXT     NOT NULL,
    defender_tag TEXT     NOT NULL,
    stars        SMALLINT NOT NULL,
    destruction  SMALLINT NOT NULL,
    -- Segundos; a API manda, mas guardamos só por garantia (nulo se faltar).
    duration     SMALLINT,
    PRIMARY KEY (war_id, attack_order)
);

CREATE INDEX war_attacks_attacker ON war_attacks (attacker_tag);
