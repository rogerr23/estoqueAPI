-- Quantidade mínima desejada do produto somando todas as lojas (0 = sem alerta).
ALTER TABLE produtos ADD COLUMN estoque_minimo INTEGER NOT NULL DEFAULT 0 CHECK (estoque_minimo >= 0);
