import { ApiProperty } from '@nestjs/swagger';

export class ProdutoDto {
  @ApiProperty({ format: 'uuid', example: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11' }) id!: string;
  @ApiProperty({ example: 'DEMO-NOTE-001' }) codigo!: string;
  @ApiProperty({ example: 'Notebook de demonstração' }) nome!: string;
  @ApiProperty({ type: String, nullable: true }) descricao!: string | null;
  @ApiProperty({ type: String, nullable: true }) categoria!: string | null;
  @ApiProperty({ example: '3500.00', description: 'Valor decimal em reais, representado como texto.' }) preco!: string;
  @ApiProperty({ enum: ['ATIVO', 'INATIVO'] }) status!: string;
  @ApiProperty({ type: String, format: 'date-time' }) criadoEm!: Date;
  @ApiProperty({ type: String, format: 'date-time' }) atualizadoEm!: Date;
}

export class EstoqueLojaDto {
  @ApiProperty({ format: 'uuid', example: '7efb4b55-a0dc-4e3a-8fbf-5079b7dd9859' }) lojaId!: string;
  @ApiProperty({ example: 'DEMO-CENTRO' }) lojaCodigo!: string;
  @ApiProperty({ example: 'Centro (demonstração)' }) lojaNome!: string;
  @ApiProperty({ enum: ['ATIVO', 'INATIVO'] }) lojaStatus!: string;
  @ApiProperty({ example: 5 }) quantidadeFisica!: number;
  @ApiProperty({ example: 0 }) quantidadeReservada!: number;
  @ApiProperty({ example: 5 }) quantidadeDisponivel!: number;
  @ApiProperty({ enum: ['SEM_ESTOQUE', 'BAIXO_ESTOQUE', 'NORMAL'] }) classificacao!: string;
}

export class EstoqueProdutoDto {
  @ApiProperty({ type: ProdutoDto }) produto!: ProdutoDto;
  @ApiProperty({ type: [EstoqueLojaDto] }) lojas!: EstoqueLojaDto[];
}

export class ListaProdutosDto {
  @ApiProperty({ type: [ProdutoDto] }) produtos!: ProdutoDto[];
  @ApiProperty({ example: 1 }) pagina!: number;
  @ApiProperty({ example: 50 }) limite!: number;
}
