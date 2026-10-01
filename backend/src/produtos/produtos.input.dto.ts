import { ApiProperty, ApiPropertyOptional, PartialType } from '@nestjs/swagger';
import { Transform } from 'class-transformer';
import { IsNotEmpty, IsOptional, IsString, Matches, MaxLength } from 'class-validator';

export class CriarProdutoDto {
  @ApiProperty({ example: 'CAMISA-001', maxLength: 50 })
  @Transform(({ value }: { value: unknown }) => typeof value === 'string' ? value.trim().toUpperCase() : value)
  @IsString() @IsNotEmpty() @MaxLength(50)
  codigo!: string;

  @ApiProperty({ example: 'Camisa azul', maxLength: 150 })
  @Transform(({ value }: { value: unknown }) => typeof value === 'string' ? value.trim() : value)
  @IsString() @IsNotEmpty() @MaxLength(150)
  nome!: string;

  @ApiProperty({ example: '49.90', type: String, description: 'Preço em reais como texto decimal, até 10 dígitos inteiros e 2 decimais, sem sinal ou vírgula.' })
  @IsString()
  @Matches(/^\d{1,10}(\.\d{1,2})?$/, { message: 'preco deve ser texto decimal não negativo, como "49.90", com até duas casas decimais' })
  preco!: string;

  @ApiPropertyOptional({ type: String, nullable: true, example: 'Camisa de algodão', maxLength: 10000 })
  @Transform(({ value }: { value: unknown }) => typeof value === 'string' ? value.trim() : value)
  @IsOptional() @IsString() @MaxLength(10000)
  descricao?: string | null;

  @ApiPropertyOptional({ type: String, nullable: true, example: 'Vestuário', maxLength: 80 })
  @Transform(({ value }: { value: unknown }) => typeof value === 'string' ? value.trim() : value)
  @IsOptional() @IsString() @MaxLength(80)
  categoria?: string | null;

}

export class EditarProdutoDto extends PartialType(CriarProdutoDto, { skipNullProperties: false }) {}
