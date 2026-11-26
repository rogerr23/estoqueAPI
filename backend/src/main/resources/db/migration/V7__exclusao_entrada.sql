-- O registro de alterações passa a aceitar também a exclusão de entradas pelo administrador.
ALTER TABLE produto_edicoes DROP CONSTRAINT produto_edicoes_acao_check;
ALTER TABLE produto_edicoes ADD CONSTRAINT produto_edicoes_acao_check
    CHECK (acao IN ('EDICAO', 'EXCLUSAO_PRODUTO', 'EXCLUSAO_TRANSFERENCIA', 'EXCLUSAO_ENTRADA'));
