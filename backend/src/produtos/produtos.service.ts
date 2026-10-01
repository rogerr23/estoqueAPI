import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { DataSource } from 'typeorm';
import { EstoqueLojaDto, EstoqueProdutoDto, ListaProdutosDto, ProdutoDto } from './produtos.dto';

const productColumns = 'id, codigo, nome, preco, unidade_medida AS "unidadeMedida", status';

@Injectable()
export class ProdutosService {
  constructor(private readonly database: DataSource) {}

  async buscar(busca: unknown, pagina: unknown): Promise<ListaProdutosDto> {
    if (busca !== undefined && (typeof busca !== 'string' || busca.trim().length === 0 || busca.length > 150)) {
      throw new BadRequestException('busca deve ser um texto não vazio de até 150 caracteres');
    }
    const page = pagina === undefined ? '1' : pagina;
    if (typeof page !== 'string' || !/^[1-9]\d*$/.test(page) || Number(page) > 1000000) {
      throw new BadRequestException('pagina deve ser um inteiro entre 1 e 1000000');
    }
    const term = typeof busca === 'string' ? busca.trim() : '';
    // % e _ enviados pelo usuário são caracteres literais, não curingas.
    const pattern = `%${term.replace(/[\\%_]/g, '\\$&')}%`;
    const produtos: ProdutoDto[] = await this.database.query(`
      SELECT ${productColumns} FROM produtos
      WHERE nome ILIKE $1 OR codigo ILIKE $1
      ORDER BY nome, id LIMIT 50 OFFSET $2
    `, [pattern, (Number(page) - 1) * 50]);
    return { produtos, pagina: Number(page), limite: 50 };
  }

  async estoque(id: string): Promise<EstoqueProdutoDto> {
    const products: ProdutoDto[] = await this.database.query(`SELECT ${productColumns} FROM produtos WHERE id = $1`, [id]);
    if (!products[0]) throw new NotFoundException('Produto não encontrado');
    const lojas: EstoqueLojaDto[] = await this.database.query(`
      SELECT l.id AS "lojaId", l.codigo AS "lojaCodigo", l.nome AS "lojaNome", l.status AS "lojaStatus",
        COALESCE(e.quantidade_fisica, 0) AS "quantidadeFisica",
        COALESCE(e.quantidade_reservada, 0) AS "quantidadeReservada",
        COALESCE(e.quantidade_fisica - e.quantidade_reservada, 0) AS "quantidadeDisponivel",
        CASE COALESCE(e.quantidade_fisica - e.quantidade_reservada, 0)
          WHEN 0 THEN 'SEM_ESTOQUE' WHEN 1 THEN 'BAIXO_ESTOQUE' ELSE 'NORMAL'
        END AS classificacao
      FROM lojas l LEFT JOIN estoques e ON e.loja_id = l.id AND e.produto_id = $1
      ORDER BY l.nome, l.id
    `, [id]);
    return { produto: products[0], lojas };
  }
}
