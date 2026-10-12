-- Campos adicionais exigidos pela avaliacao (ao menos 5 campos e 3 tipos por entidade)

ALTER TABLE blocos ADD COLUMN capacidade INTEGER NOT NULL DEFAULT 0;
ALTER TABLE blocos ADD COLUMN coberto    BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE vagas ADD COLUMN valor_hora DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE vagas ADD COLUMN observacao VARCHAR(255);

ALTER TABLE dispositivos ADD COLUMN data_instalacao DATE;

ALTER TABLE users ADD COLUMN data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Valores iniciais coerentes para os registros criados em V2
UPDATE blocos SET capacidade = (SELECT COUNT(*) FROM vagas v WHERE v.bloco_id = blocos.id);
UPDATE vagas SET valor_hora = 5.00;
UPDATE dispositivos SET data_instalacao = CURRENT_DATE;
