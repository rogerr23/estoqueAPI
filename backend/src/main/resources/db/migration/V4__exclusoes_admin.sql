-- Produto excluído some das listas, mas continua no histórico de transferências.
ALTER TABLE produtos ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
-- O registro de edições passa a guardar também as exclusões feitas pelo administrador.
ALTER TABLE produto_edicoes ADD COLUMN acao VARCHAR(30) NOT NULL DEFAULT 'EDICAO'
    CHECK (acao IN ('EDICAO', 'EXCLUSAO_PRODUTO', 'EXCLUSAO_TRANSFERENCIA'));
