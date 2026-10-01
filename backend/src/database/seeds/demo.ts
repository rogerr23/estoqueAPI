import { randomBytes, scryptSync } from 'node:crypto';
import dataSource from '../data-source';
import { readEnvironment } from '../../config/environment';

async function seed() {
  if (readEnvironment().nodeEnv !== 'development') {
    throw new Error('Dados de demonstração só podem ser criados em NODE_ENV=development');
  }
  await dataSource.initialize();
  try {
    await dataSource.transaction(async (manager) => {
      // Serializa execuções simultâneas do seed, sem bloquear outras operações.
      await manager.query('SELECT pg_advisory_xact_lock(1790800000)');
      const lojaIds: string[] = [];
      for (const [codigo, nome] of [['DEMO-CENTRO', 'Centro (demonstração)'], ['DEMO-BARRA', 'Barra (demonstração)']]) {
        await manager.query('INSERT INTO lojas (codigo, nome) VALUES ($1, $2) ON CONFLICT (codigo) DO NOTHING', [codigo, nome]);
        const rows: { id: string }[] = await manager.query('SELECT id FROM lojas WHERE codigo = $1', [codigo]);
        lojaIds.push(rows[0].id);
      }
      // Autor apenas para rastrear a carga inicial. Não é uma conta de login.
      const salt = randomBytes(16).toString('hex');
      const hash = scryptSync(randomBytes(32), salt, 64).toString('hex');
      await manager.query(`
        INSERT INTO colaboradores (loja_id, nome, email, senha_hash, perfil, status)
        VALUES ($1, 'Autor da carga de demonstração', 'seed@example.invalid', $2, 'ADMINISTRADOR', 'INATIVO')
        ON CONFLICT (email) DO NOTHING
      `, [lojaIds[0], `scrypt:${salt}:${hash}`]);
      const authors: { id: string }[] = await manager.query("SELECT id FROM colaboradores WHERE email = 'seed@example.invalid'");
      for (const [codigo, nome, preco, quantidade] of [
        ['DEMO-NOTE-001', 'Notebook de demonstração', '3500.00', 5],
        ['DEMO-CAMISA-001', 'Camisa de demonstração', '49.90', 1],
      ] as const) {
        const products: { id: string }[] = await manager.query(`
          INSERT INTO produtos (codigo, nome, preco, categoria)
          VALUES ($1, $2, $3, 'Demonstração') ON CONFLICT (codigo) DO NOTHING RETURNING id
        `, [codigo, nome, preco]);
        // Não altera saldo nem repete entradas para produto já criado.
        if (products.length === 0) continue;
        const stocks: { id: number }[] = await manager.query(`
          INSERT INTO estoques (loja_id, produto_id, quantidade_fisica)
          VALUES ($1, $2, $3) RETURNING id
        `, [lojaIds[0], products[0].id, quantidade]);
        await manager.query(`
          INSERT INTO movimentacoes_estoque (estoque_id, tipo, delta_fisico, colaborador_id, motivo)
          VALUES ($1, 'ENTRADA', $2, $3, 'Saldo inicial de demonstração')
        `, [stocks[0].id, quantidade, authors[0].id]);
      }
    });
    console.log('Demonstração preparada; saldos existentes foram preservados.');
  } finally {
    await dataSource.destroy();
  }
}

seed().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : 'Falha ao preparar demonstração');
  process.exitCode = 1;
});
