import { Body, Controller, Delete, Get, Param, ParseUUIDPipe, Patch, Post, Query } from '@nestjs/common';
import { ApiBadRequestResponse, ApiConflictResponse, ApiCreatedResponse, ApiNotFoundResponse, ApiOkResponse, ApiOperation, ApiParam, ApiQuery, ApiTags } from '@nestjs/swagger';
import { EstoqueProdutoDto, ListaProdutosDto, ProdutoDto } from './produtos.dto';
import { CriarProdutoDto, EditarProdutoDto } from './produtos.input.dto';
import { ProdutosService } from './produtos.service';

@ApiTags('Produtos e estoque')
@Controller('produtos')
export class ProdutosController {
  constructor(private readonly produtos: ProdutosService) {}

  @Post()
  @ApiOperation({ summary: 'Cadastrar produto', description: 'Produto nasce ATIVO com UUID gerado no banco. Não cria saldo de estoque.' })
  @ApiCreatedResponse({ type: ProdutoDto })
  @ApiBadRequestResponse({ description: 'Campos inválidos ou não permitidos.' })
  @ApiConflictResponse({ description: 'Código já cadastrado.' })
  criar(@Body() input: CriarProdutoDto) { return this.produtos.criar(input); }

  @Get(':id')
  @ApiOperation({ summary: 'Consultar produto pelo UUID' })
  @ApiParam({ name: 'id', type: String, format: 'uuid' })
  @ApiOkResponse({ type: ProdutoDto })
  @ApiBadRequestResponse({ description: 'UUID inválido.' })
  @ApiNotFoundResponse({ description: 'Produto não encontrado.' })
  consultar(@Param('id', new ParseUUIDPipe({ version: '4' })) id: string) { return this.produtos.consultar(id); }

  @Patch(':id')
  @ApiOperation({ summary: 'Editar campos do produto', description: 'Envie somente os campos a alterar. ID e status não são editáveis. Descrição e categoria aceitam null para limpar.' })
  @ApiParam({ name: 'id', type: String, format: 'uuid' })
  @ApiOkResponse({ type: ProdutoDto })
  @ApiBadRequestResponse({ description: 'Campos inválidos, não permitidos ou objeto vazio.' })
  @ApiNotFoundResponse({ description: 'Produto não encontrado.' })
  @ApiConflictResponse({ description: 'Código já cadastrado.' })
  editar(@Param('id', new ParseUUIDPipe({ version: '4' })) id: string, @Body() input: EditarProdutoDto) { return this.produtos.editar(id, input); }

  @Delete(':id')
  @ApiOperation({ summary: 'Inativar produto', description: 'Preserva o registro, estoques e histórico. Recusa reserva ou transferência aberta. Repetir a operação mantém o produto inativo.' })
  @ApiParam({ name: 'id', type: String, format: 'uuid' })
  @ApiOkResponse({ type: ProdutoDto })
  @ApiBadRequestResponse({ description: 'UUID inválido.' })
  @ApiNotFoundResponse({ description: 'Produto não encontrado.' })
  @ApiConflictResponse({ description: 'Produto reservado ou com transferência aberta.' })
  inativar(@Param('id', new ParseUUIDPipe({ version: '4' })) id: string) { return this.produtos.inativar(id); }

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
