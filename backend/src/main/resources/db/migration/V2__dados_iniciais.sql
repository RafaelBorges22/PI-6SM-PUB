-- Dados de demonstracao: 3 blocos, 12 vagas e 4 dispositivos (secao 8 da especificacao).
-- Os usuarios iniciais sao criados pelo DataSeeder, porque o hash BCrypt precisa
-- ser gerado pelo mesmo PasswordEncoder usado no login.

INSERT INTO blocos (nome, descricao, localizacao, status) VALUES
    ('Bloco A', 'Estacionamento Principal', 'Terreo', 'ATIVO'),
    ('Bloco B', 'Estacionamento Secundario', 'Terreo', 'ATIVO'),
    ('Bloco C', 'Estacionamento Coberto', 'Subsolo 1', 'ATIVO');

INSERT INTO dispositivos (nome, mac_address, tipo, mqtt_topic, status) VALUES
    ('ESP32-VAGA-01', 'AA:BB:CC:DD:EE:01', 'ESP32', 'smart-parking/vaga/01', 'OFFLINE'),
    ('ESP32-VAGA-02', 'AA:BB:CC:DD:EE:02', 'ESP32', 'smart-parking/vaga/02', 'OFFLINE'),
    ('ESP32-VAGA-03', 'AA:BB:CC:DD:EE:03', 'ESP32', 'smart-parking/vaga/03', 'OFFLINE'),
    ('ESP32-VAGA-04', 'AA:BB:CC:DD:EE:04', 'ESP32', 'smart-parking/vaga/04', 'OFFLINE');

-- Bloco A: vagas 1 a 4, cada uma com o seu ESP32.
INSERT INTO vagas (numero, tipo, status, bloco_id, dispositivo_id)
SELECT 1, 'NORMAL', 'LIVRE', b.id, d.id
FROM blocos b, dispositivos d WHERE b.nome = 'Bloco A' AND d.nome = 'ESP32-VAGA-01';

INSERT INTO vagas (numero, tipo, status, bloco_id, dispositivo_id)
SELECT 2, 'NORMAL', 'LIVRE', b.id, d.id
FROM blocos b, dispositivos d WHERE b.nome = 'Bloco A' AND d.nome = 'ESP32-VAGA-02';

INSERT INTO vagas (numero, tipo, status, bloco_id, dispositivo_id)
SELECT 3, 'PCD', 'LIVRE', b.id, d.id
FROM blocos b, dispositivos d WHERE b.nome = 'Bloco A' AND d.nome = 'ESP32-VAGA-03';

INSERT INTO vagas (numero, tipo, status, bloco_id, dispositivo_id)
SELECT 4, 'IDOSO', 'LIVRE', b.id, d.id
FROM blocos b, dispositivos d WHERE b.nome = 'Bloco A' AND d.nome = 'ESP32-VAGA-04';

-- Bloco B: vagas 5 a 8, ainda sem dispositivo instalado.
INSERT INTO vagas (numero, tipo, status, bloco_id)
SELECT 5, 'NORMAL', 'LIVRE', id FROM blocos WHERE nome = 'Bloco B';
INSERT INTO vagas (numero, tipo, status, bloco_id)
SELECT 6, 'NORMAL', 'LIVRE', id FROM blocos WHERE nome = 'Bloco B';
INSERT INTO vagas (numero, tipo, status, bloco_id)
SELECT 7, 'MOTO', 'LIVRE', id FROM blocos WHERE nome = 'Bloco B';
INSERT INTO vagas (numero, tipo, status, bloco_id)
SELECT 8, 'MOTO', 'LIVRE', id FROM blocos WHERE nome = 'Bloco B';

-- Bloco C: vagas 9 a 12.
INSERT INTO vagas (numero, tipo, status, bloco_id)
SELECT 9, 'NORMAL', 'LIVRE', id FROM blocos WHERE nome = 'Bloco C';
INSERT INTO vagas (numero, tipo, status, bloco_id)
SELECT 10, 'NORMAL', 'LIVRE', id FROM blocos WHERE nome = 'Bloco C';
INSERT INTO vagas (numero, tipo, status, bloco_id)
SELECT 11, 'PCD', 'LIVRE', id FROM blocos WHERE nome = 'Bloco C';
INSERT INTO vagas (numero, tipo, status, bloco_id)
SELECT 12, 'NORMAL', 'MANUTENCAO', id FROM blocos WHERE nome = 'Bloco C';
