import { MigrationInterface, QueryRunner } from 'typeorm';

export class RemoverUnidadeMedida1790800000004 implements MigrationInterface {
  async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query('ALTER TABLE produtos DROP COLUMN unidade_medida');
  }

  async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query("ALTER TABLE produtos ADD COLUMN unidade_medida varchar(2) NOT NULL DEFAULT 'UN' CHECK (unidade_medida = 'UN')");
  }
}
