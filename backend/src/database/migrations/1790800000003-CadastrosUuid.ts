import { MigrationInterface, QueryRunner } from 'typeorm';

type Reference = { table: string; column: string; constraint: string };

// Identificadores são constantes da migration, nunca dados de requisições.
async function convert(
  runner: QueryRunner,
  table: string,
  references: Reference[],
  toUuid: boolean,
) {
  if (toUuid) await runner.query(`ALTER TABLE ${table} ADD COLUMN uuid_novo uuid NOT NULL DEFAULT gen_random_uuid()`);
  for (const ref of references) {
    await runner.query(`
      ALTER TABLE ${ref.table} ADD COLUMN ${ref.column}_novo ${toUuid ? 'uuid' : 'integer'};
      UPDATE ${ref.table} r SET ${ref.column}_novo = t.${toUuid ? 'uuid_novo' : 'id_legado'}
        FROM ${table} t WHERE t.id = r.${ref.column};
      ALTER TABLE ${ref.table} ALTER COLUMN ${ref.column}_novo SET NOT NULL;
      ALTER TABLE ${ref.table} DROP CONSTRAINT ${ref.constraint};
      ALTER TABLE ${ref.table} DROP COLUMN ${ref.column};
    `);
  }
  await runner.query(`ALTER TABLE ${table} DROP CONSTRAINT ${table}_pkey`);
  if (toUuid) {
    await runner.query(`
      ALTER TABLE ${table} RENAME COLUMN id TO id_legado;
      ALTER TABLE ${table} ADD CONSTRAINT ${table}_id_legado_key UNIQUE (id_legado);
      ALTER TABLE ${table} RENAME COLUMN uuid_novo TO id;
    `);
  } else {
    await runner.query(`
      ALTER TABLE ${table} DROP COLUMN id;
      ALTER TABLE ${table} DROP CONSTRAINT ${table}_id_legado_key;
      ALTER TABLE ${table} RENAME COLUMN id_legado TO id;
    `);
  }
  await runner.query(`ALTER TABLE ${table} ADD CONSTRAINT ${table}_pkey PRIMARY KEY (id)`);
  for (const ref of references) {
    await runner.query(`
      ALTER TABLE ${ref.table} RENAME COLUMN ${ref.column}_novo TO ${ref.column};
      ALTER TABLE ${ref.table} ADD CONSTRAINT ${ref.constraint}
        FOREIGN KEY (${ref.column}) REFERENCES ${table}(id) ON DELETE RESTRICT;
    `);
  }
}

const lojaReferences: Reference[] = [
  { table: 'estoques', column: 'loja_id', constraint: 'estoques_loja_id_fkey' },
  { table: 'colaboradores', column: 'loja_id', constraint: 'colaboradores_loja_id_fkey' },
];
const colaboradorReferences: Reference[] = [
  { table: 'movimentacoes_estoque', column: 'colaborador_id', constraint: 'movimentacoes_estoque_colaborador_id_fkey' },
];

async function removeStockIndexes(runner: QueryRunner) {
  await runner.query('ALTER TABLE estoques DROP CONSTRAINT estoques_loja_id_produto_id_key; DROP INDEX idx_estoques_produto_loja;');
}

async function restoreStockIndexes(runner: QueryRunner) {
  await runner.query('ALTER TABLE estoques ADD CONSTRAINT estoques_loja_id_produto_id_key UNIQUE (loja_id, produto_id); CREATE INDEX idx_estoques_produto_loja ON estoques (produto_id, loja_id);');
}

export class CadastrosUuid1790800000003 implements MigrationInterface {
  async up(runner: QueryRunner): Promise<void> {
    await removeStockIndexes(runner);
    await convert(runner, 'lojas', lojaReferences, true);
    await restoreStockIndexes(runner);
    await convert(runner, 'colaboradores', colaboradorReferences, true);
  }

  async down(runner: QueryRunner): Promise<void> {
    await convert(runner, 'colaboradores', colaboradorReferences, false);
    await removeStockIndexes(runner);
    await convert(runner, 'lojas', lojaReferences, false);
    await restoreStockIndexes(runner);
  }
}
