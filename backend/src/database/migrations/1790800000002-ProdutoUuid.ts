import { MigrationInterface, QueryRunner } from 'typeorm';

export class ProdutoUuid1790800000002 implements MigrationInterface {
  async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      ALTER TABLE produtos ADD COLUMN uuid_novo uuid NOT NULL DEFAULT gen_random_uuid();
      ALTER TABLE estoques ADD COLUMN produto_uuid uuid;
      UPDATE estoques e SET produto_uuid = p.uuid_novo FROM produtos p WHERE p.id = e.produto_id;
      ALTER TABLE estoques ALTER COLUMN produto_uuid SET NOT NULL;

      ALTER TABLE estoques DROP CONSTRAINT estoques_produto_id_fkey;
      ALTER TABLE estoques DROP CONSTRAINT estoques_loja_id_produto_id_key;
      DROP INDEX idx_estoques_produto_loja;
      ALTER TABLE estoques DROP COLUMN produto_id;
      ALTER TABLE produtos DROP CONSTRAINT produtos_pkey;

      -- Guarda o identificador anterior para uma reversão sem perda de vínculos.
      -- A identidade continua gerando esse valor para produtos criados depois.
      ALTER TABLE produtos RENAME COLUMN id TO id_legado;
      ALTER TABLE produtos ADD CONSTRAINT produtos_id_legado_key UNIQUE (id_legado);
      ALTER TABLE produtos RENAME COLUMN uuid_novo TO id;
      ALTER TABLE produtos ADD CONSTRAINT produtos_pkey PRIMARY KEY (id);
      ALTER TABLE estoques RENAME COLUMN produto_uuid TO produto_id;
      ALTER TABLE estoques ADD CONSTRAINT estoques_produto_id_fkey
        FOREIGN KEY (produto_id) REFERENCES produtos(id) ON DELETE RESTRICT;
      ALTER TABLE estoques ADD CONSTRAINT estoques_loja_id_produto_id_key UNIQUE (loja_id, produto_id);
      CREATE INDEX idx_estoques_produto_loja ON estoques (produto_id, loja_id);
    `);
  }

  async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      ALTER TABLE estoques ADD COLUMN produto_inteiro integer;
      UPDATE estoques e SET produto_inteiro = p.id_legado FROM produtos p WHERE p.id = e.produto_id;
      ALTER TABLE estoques ALTER COLUMN produto_inteiro SET NOT NULL;
      ALTER TABLE estoques DROP CONSTRAINT estoques_produto_id_fkey;
      ALTER TABLE estoques DROP CONSTRAINT estoques_loja_id_produto_id_key;
      DROP INDEX idx_estoques_produto_loja;
      ALTER TABLE estoques DROP COLUMN produto_id;
      ALTER TABLE produtos DROP CONSTRAINT produtos_pkey;
      ALTER TABLE produtos DROP COLUMN id;
      ALTER TABLE produtos DROP CONSTRAINT produtos_id_legado_key;
      ALTER TABLE produtos RENAME COLUMN id_legado TO id;
      ALTER TABLE produtos ADD CONSTRAINT produtos_pkey PRIMARY KEY (id);
      ALTER TABLE estoques RENAME COLUMN produto_inteiro TO produto_id;
      ALTER TABLE estoques ADD CONSTRAINT estoques_produto_id_fkey
        FOREIGN KEY (produto_id) REFERENCES produtos(id) ON DELETE RESTRICT;
      ALTER TABLE estoques ADD CONSTRAINT estoques_loja_id_produto_id_key UNIQUE (loja_id, produto_id);
      CREATE INDEX idx_estoques_produto_loja ON estoques (produto_id, loja_id);
    `);
  }
}
