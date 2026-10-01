import { MigrationInterface, QueryRunner } from 'typeorm';

export class BaseEstoque1790800000000 implements MigrationInterface {
  async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE lojas (
        id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        codigo varchar(30) NOT NULL UNIQUE,
        nome varchar(120) NOT NULL,
        telefone varchar(20), logradouro varchar(150), numero varchar(20),
        complemento varchar(100), bairro varchar(100), cidade varchar(100),
        uf varchar(2), cep varchar(8),
        status varchar(10) NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO', 'INATIVO')),
        criado_em timestamptz NOT NULL DEFAULT now(),
        atualizado_em timestamptz NOT NULL DEFAULT now()
      );
      CREATE TABLE produtos (
        id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        codigo varchar(50) NOT NULL UNIQUE,
        nome varchar(150) NOT NULL,
        descricao text, categoria varchar(80),
        preco numeric(12,2) NOT NULL CHECK (preco >= 0),
        unidade_medida varchar(2) NOT NULL DEFAULT 'UN' CHECK (unidade_medida = 'UN'),
        status varchar(10) NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO', 'INATIVO')),
        criado_em timestamptz NOT NULL DEFAULT now(),
        atualizado_em timestamptz NOT NULL DEFAULT now()
      );
      CREATE TABLE estoques (
        id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        loja_id integer NOT NULL REFERENCES lojas(id) ON DELETE RESTRICT,
        produto_id integer NOT NULL REFERENCES produtos(id) ON DELETE RESTRICT,
        quantidade_fisica integer NOT NULL DEFAULT 0 CHECK (quantidade_fisica >= 0),
        quantidade_reservada integer NOT NULL DEFAULT 0 CHECK (quantidade_reservada >= 0),
        criado_em timestamptz NOT NULL DEFAULT now(),
        atualizado_em timestamptz NOT NULL DEFAULT now(),
        UNIQUE (loja_id, produto_id),
        CHECK (quantidade_reservada <= quantidade_fisica)
      );
      CREATE INDEX idx_estoques_produto_loja ON estoques (produto_id, loja_id);
    `);
  }

  async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query('DROP TABLE estoques; DROP TABLE produtos; DROP TABLE lojas;');
  }
}
