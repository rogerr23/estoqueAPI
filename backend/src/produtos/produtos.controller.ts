import { Controller, Get, Param, ParseUUIDPipe, Query } from '@nestjs/common';
import { ApiBadRequestResponse, ApiNotFoundResponse, ApiOkResponse, ApiOperation, ApiParam, ApiQuery, ApiTags } from '@nestjs/swagger';
import { EstoqueProdutoDto, ListaProdutosDto } from './produtos.dto';
import { ProdutosService } from './produtos.service';

@ApiTags('Produtos e estoque')
@Controller('produtos')
export class ProdutosController {
  constructor(private readonly produtos: ProdutosService) {}

  @Get()
  @ApiOperation({ summary: 'Buscar produtos por trecho do nome ou código', description: 'Inclui cadastros ativos e inativos. Sem busca, lista produtos. Até 50 por página.' })
  @ApiQuery({ name: 'busca', required: false, type: String, example: 'Notebook' })
  @ApiQuery({ name: 'pagina', required: false, type: Number, example: 1, minimum: 1, maximum: 1000000 })
  @ApiOkResponse({ type: ListaProdutosDto })
  @ApiBadRequestResponse({ description: 'Busca ou página inválida.' })
  buscar(@Query('busca') busca: unknown, @Query('pagina') pagina: unknown) {
    return this.produtos.buscar(busca, pagina);
  }

  @Get(':id/estoques')
  @ApiOperation({ summary: 'Consultar estoque de um produto em todas as lojas', description: 'Inclui lojas inativas e combinações sem registro de estoque, apresentadas como zero. Baixo estoque = disponível exatamente 1.' })
  @ApiParam({ name: 'id', schema: { type: 'string', format: 'uuid', example: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11' } })
  @ApiOkResponse({ type: EstoqueProdutoDto })
  @ApiBadRequestResponse({ description: 'ID inválido.' })
  @ApiNotFoundResponse({ description: 'Produto não encontrado.' })
  estoque(@Param('id', new ParseUUIDPipe({ version: '4' })) id: string) {
    return this.produtos.estoque(id);
  }
}
