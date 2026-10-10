-- Smart Parking - schema inicial (secao 21 da especificacao)

CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(150) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'USER'))
);

CREATE TABLE blocos (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    descricao   VARCHAR(255),
    localizacao VARCHAR(150),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    CONSTRAINT ck_blocos_status CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE TABLE dispositivos (
    id                 BIGSERIAL PRIMARY KEY,
    nome               VARCHAR(100) NOT NULL,
    mac_address        VARCHAR(17)  NOT NULL,
    tipo               VARCHAR(20)  NOT NULL DEFAULT 'ESP32',
    mqtt_topic         VARCHAR(150) NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    ultima_comunicacao TIMESTAMP,
    CONSTRAINT uk_dispositivos_nome UNIQUE (nome),
    CONSTRAINT uk_dispositivos_mac UNIQUE (mac_address),
    CONSTRAINT uk_dispositivos_topic UNIQUE (mqtt_topic),
    CONSTRAINT ck_dispositivos_tipo CHECK (tipo IN ('ESP32', 'ESP8266', 'OUTRO')),
    CONSTRAINT ck_dispositivos_status CHECK (status IN ('ONLINE', 'OFFLINE', 'MANUTENCAO'))
);

-- A vaga e o lado dono do vinculo 1:1 com o dispositivo (coluna dispositivo_id UNIQUE).
CREATE TABLE vagas (
    id             BIGSERIAL PRIMARY KEY,
    numero         INTEGER     NOT NULL,
    tipo           VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status         VARCHAR(20) NOT NULL DEFAULT 'LIVRE',
    bloco_id       BIGINT      NOT NULL,
    dispositivo_id BIGINT,
    CONSTRAINT uk_vagas_numero UNIQUE (numero),
    CONSTRAINT uk_vagas_dispositivo UNIQUE (dispositivo_id),
    CONSTRAINT fk_vagas_bloco FOREIGN KEY (bloco_id) REFERENCES blocos (id),
    CONSTRAINT fk_vagas_dispositivo FOREIGN KEY (dispositivo_id) REFERENCES dispositivos (id),
    CONSTRAINT ck_vagas_tipo CHECK (tipo IN ('NORMAL', 'PCD', 'IDOSO', 'MOTO')),
    CONSTRAINT ck_vagas_status CHECK (status IN ('LIVRE', 'OCUPADA', 'MANUTENCAO'))
);

CREATE TABLE medicoes (
    id             BIGSERIAL PRIMARY KEY,
    dispositivo_id BIGINT        NOT NULL,
    distancia      NUMERIC(8, 2) NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    data_hora      TIMESTAMP     NOT NULL,
    CONSTRAINT fk_medicoes_dispositivo FOREIGN KEY (dispositivo_id) REFERENCES dispositivos (id) ON DELETE CASCADE,
    CONSTRAINT ck_medicoes_status CHECK (status IN ('LIVRE', 'OCUPADA', 'MANUTENCAO'))
);

CREATE INDEX idx_vagas_bloco ON vagas (bloco_id);
CREATE INDEX idx_vagas_status ON vagas (status);
CREATE INDEX idx_medicoes_dispositivo_data ON medicoes (dispositivo_id, data_hora DESC);
