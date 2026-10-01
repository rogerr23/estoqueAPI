import { MigrationInterface, QueryRunner } from 'typeorm';

// Base mínima para rastrear a entrada inicial. Transferências e eventos
// serão acrescentados em migrations próprias quando esse fluxo for implementado.
export class HistoricoInicial1790800000001 implements MigrationInterface {
  async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE colaboradores (
        id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        loja_id integer NOT NULL REFERENCES lojas(id) ON DELETE RESTRICT,
        nome varchar(150) NOT NULL,
        email varchar(254) NOT NULL UNIQUE,
        senha_hash text NOT NULL,
        perfil varchar(20) NOT NULL CHECK (perfil IN ('FUNCIONARIO', 'ADMINISTRADOR')),
        status varchar(10) NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO', 'INATIVO')),
        criado_em timestamptz NOT NULL DEFAULT now(),
        atualizado_em timestamptz NOT NULL DEFAULT now()
      );
      CREATE TABLE movimentacoes_estoque (
        id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        estoque_id integer NOT NULL REFERENCES estoques(id) ON DELETE RESTRICT,
        tipo varchar(25) NOT NULL,
        delta_fisico integer NOT NULL,
        delta_reservado integer NOT NULL DEFAULT 0,
        colaborador_id integer NOT NULL REFERENCES colaboradores(id) ON DELETE RESTRICT,
        motivo text NOT NULL CHECK (length(trim(motivo)) > 0),
        ocorrido_em timestamptz NOT NULL DEFAULT now(),
        CONSTRAINT ck_movimentacao_manual CHECK (
          delta_reservado = 0 AND (
            (tipo = 'ENTRADA' AND delta_fisico > 0) OR
            (tipo = 'SAIDA' AND delta_fisico < 0) OR
            (tipo = 'AJUSTE' AND delta_fisico <> 0)
          )
        )
      );
      CREATE INDEX idx_movimentacoes_estoque_data ON movimentacoes_estoque (estoque_id, ocorrido_em);
    `);
  }

  async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query('DROP TABLE movimentacoes_estoque; DROP TABLE colaboradores;');
  }
}
