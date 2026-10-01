import { BadRequestException, ConflictException, Injectable, NotFoundException } from '@nestjs/common';
import { DataSource, QueryFailedError } from 'typeorm';
import { EstoqueLojaDto, EstoqueProdutoDto, ListaProdutosDto, ProdutoDto } from './produtos.dto';
import { CriarProdutoDto, EditarProdutoDto } from './produtos.input.dto';

const productColumns = 'id, codigo, nome, descricao, categoria, preco, status, criado_em AS "criadoEm", atualizado_em AS "atualizadoEm"';

function handleConflict(error: unknown): never {
  if (error instanceof QueryFailedError && (error.driverError as { code?: string }).code === '23505') {
    throw new ConflictException('Já existe um produto com esse código, inclusive entre os inativos');
  }
  throw error;
}

@Injectable()
export class ProdutosService {
  constructor(private readonly database: DataSource) {}

  async consultar(id: string): Promise<ProdutoDto> {
    const rows: ProdutoDto[] = await this.database.query(`SELECT ${productColumns} FROM produtos WHERE id = $1`, [id]);
    if (!rows[0]) throw new NotFoundException('Produto não encontrado');
    return rows[0];
  }

  async criar(input: CriarProdutoDto): Promise<ProdutoDto> {
    try {
      const rows: ProdutoDto[] = await this.database.query(`
        INSERT INTO produtos (codigo, nome, preco, descricao, categoria)
        VALUES ($1, $2, $3, $4, $5) RETURNING ${productColumns}
      `, [input.codigo, input.nome, input.preco, input.descricao || null, input.categoria || null]);
      return rows[0];
    } catch (error) { handleConflict(error); }
  }

  async editar(id: string, input: EditarProdutoDto): Promise<ProdutoDto> {
    // Lista fixa: impede editar ID, status, saldos ou campos internos.
    const fields = { codigo: 'codigo', nome: 'nome', preco: 'preco', descricao: 'descricao', categoria: 'categoria' } as const;
    const values: unknown[] = [];
    const assignments: string[] = [];
    for (const key of Object.keys(fields) as (keyof typeof fields)[]) {
      if (input[key] === undefined) continue;
      values.push(input[key] === '' ? null : input[key]);
      assignments.push(`${fields[key]} = $${values.length}`);
    }
    if (assignments.length === 0) throw new BadRequestException('Informe pelo menos um campo para editar');
    values.push(id);
    try {
      const rows: ProdutoDto[] = await this.database.query(`
        WITH atualizado AS (
          UPDATE produtos SET ${assignments.join(', ')}, atualizado_em = now()
          WHERE id = $${values.length} RETURNING ${productColumns}
        ) SELECT * FROM atualizado
      `, values);
      if (!rows[0]) throw new NotFoundException('Produto não encontrado');
      return rows[0];
    } catch (error) { handleConflict(error); }
  }

  async inativar(id: string): Promise<ProdutoDto> {
    return this.database.transaction(async (manager) => {
      const rows: ProdutoDto[] = await manager.query(`SELECT ${productColumns} FROM produtos WHERE id = $1 FOR UPDATE`, [id]);
      if (!rows[0]) throw new NotFoundException('Produto não encontrado');
      if (rows[0].status === 'INATIVO') return rows[0];
      const reservations: { existe: boolean }[] = await manager.query('SELECT EXISTS (SELECT 1 FROM estoques WHERE produto_id = $1 AND quantidade_reservada > 0) AS existe', [id]);
      if (reservations[0].existe) throw new ConflictException('Produto possui estoque reservado e não pode ser inativado');
      // Transferências ainda não existem nesta etapa. Quando suas tabelas forem
      // criadas, aplica RN20 também a pedidos abertos sem reserva.
      const tables: { existe: boolean }[] = await manager.query("SELECT to_regclass('transferencias') IS NOT NULL AND to_regclass('itens_transferencia') IS NOT NULL AS existe");
      if (tables[0].existe) {
        const open: { existe: boolean }[] = await manager.query(`
          SELECT EXISTS (
            SELECT 1 FROM itens_transferencia i JOIN transferencias t ON t.id = i.transferencia_id
            WHERE i.produto_id = $1 AND t.status IN ('SOLICITADA','APROVADA','EM_TRANSITO','RECEBIDA')
          ) AS existe
        `, [id]);
        if (open[0].existe) throw new ConflictException('Produto participa de transferência aberta e não pode ser inativado');
      }
      const updated: ProdutoDto[] = await manager.query(`WITH atualizado AS (UPDATE produtos SET status = 'INATIVO', atualizado_em = now() WHERE id = $1 RETURNING ${productColumns}) SELECT * FROM atualizado`, [id]);
      return updated[0];
    });
  }

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
