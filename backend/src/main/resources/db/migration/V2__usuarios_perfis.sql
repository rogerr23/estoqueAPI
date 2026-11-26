-- Perfis de acesso e desativação lógica (preserva o histórico de quem já operou).
ALTER TABLE usuarios ADD COLUMN perfil VARCHAR(20) NOT NULL DEFAULT 'FUNCIONARIO'
    CHECK (perfil IN ('ADMIN', 'FUNCIONARIO'));
ALTER TABLE usuarios ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
-- Instalações existentes: o primeiro usuário cadastrado passa a ser administrador.
UPDATE usuarios SET perfil = 'ADMIN' WHERE id = (SELECT min(id) FROM usuarios);

-- Registra quem cadastrou cada produto (nulo para produtos anteriores).
ALTER TABLE produtos ADD COLUMN cadastrado_por_id BIGINT REFERENCES usuarios(id);
ALTER TABLE produtos ADD COLUMN cadastrado_em TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_produtos_cadastrado_por ON produtos(cadastrado_por_id);
CREATE INDEX idx_transferencias_usuario ON transferencias(usuario_id);
