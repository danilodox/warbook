-- Índice de nicks (v0.7.0): cresce só com o que passa pelo Warbook (jogadores consultados e membros
-- de clãs abertos). Só dados públicos da API do jogo. Tags sem o '#', já normalizadas.

-- Busca por semelhança de texto. É uma extensão "trusted" desde o Postgres 13: o dono do banco cria.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE player_index (
    tag             TEXT        PRIMARY KEY,
    name            TEXT        NOT NULL,
    -- Minúsculas, sem acentos e com letras estilizadas trocadas pelas comuns; é onde a busca procura.
    name_normalized TEXT        NOT NULL,
    town_hall       SMALLINT    NOT NULL,
    clan_tag        TEXT,
    clan_name       TEXT,
    seen_at         TIMESTAMPTZ NOT NULL
);

CREATE INDEX player_index_name_trgm ON player_index USING gin (name_normalized gin_trgm_ops);
