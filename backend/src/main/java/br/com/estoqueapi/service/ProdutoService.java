package br.com.estoqueapi.service;

import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.entity.Estoque;
import br.com.estoqueapi.entity.Produto;
import br.com.estoqueapi.entity.ProdutoEdicao;
import br.com.estoqueapi.exception.NegocioException;
import br.com.estoqueapi.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProdutoService {
    private final ProdutoRepository produtos;
    private final UsuarioRepository usuarios;
    private final LojaRepository lojas;
    private final EstoqueRepository estoques;
    private final ProdutoEdicaoRepository edicoes;
    public ProdutoService(ProdutoRepository produtos, UsuarioRepository usuarios,
            LojaRepository lojas, EstoqueRepository estoques, ProdutoEdicaoRepository edicoes) {
        this.produtos = produtos;
        this.usuarios = usuarios;
        this.lojas = lojas;
        this.estoques = estoques;
        this.edicoes = edicoes;
    }

    @Transactional
    public ProdutoResponse cadastrar(ProdutoRequest entrada) { return cadastrar(entrada, null); }

    /** Registra o funcionário responsável pelo cadastro, quando informado. */
    @Transactional
    public ProdutoResponse cadastrar(ProdutoRequest entrada, Long usuarioId) {
        int quantidade = entrada.quantidade() == null ? 0 : entrada.quantidade();
        if (quantidade > 0 && entrada.lojaId() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Selecione a loja que recebe a quantidade inicial");
        }
        var loja = entrada.lojaId() == null ? null : lojas.findById(entrada.lojaId()).orElseThrow(() ->
            new NegocioException(HttpStatus.NOT_FOUND, "LOJA_NAO_ENCONTRADA", "Loja não encontrada"));
        produtos.findByCodigo(entrada.codigo()).ifPresent(existente -> {
            if (!existente.isAtivo()) throw new NegocioException(HttpStatus.CONFLICT, "CODIGO_DUPLICADO",
                "Esse código pertence a um produto excluído; use outro código");
            throw duplicado();
        });
        var responsavel = usuarioId == null ? null : usuarios.getReferenceById(usuarioId);
        try {
            var produto = produtos.saveAndFlush(new Produto(entrada.codigo(), entrada.nome(),
                responsavel, java.time.Instant.now()));
            // Produto novo nunca tem estoque: cria o saldo inicial na loja escolhida (mesma transação).
            if (loja != null) estoques.saveAndFlush(new Estoque(loja, produto, quantidade));
            return ProdutoResponse.from(produto);
        } catch (DataIntegrityViolationException error) {
            // A restrição única também cobre cadastros simultâneos do mesmo código.
            Throwable causa = error;
            while (causa != null) {
                if (causa instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                    throw duplicado();
                }
                causa = causa.getCause();
            }
            throw error;
        }
    }

    /** Somente ADMIN (ver SecurityConfig): altera código, nome e o saldo de cada loja informada. */
    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoAtualizacaoRequest entrada, Long usuarioId) {
        // Mesmo bloqueio das transferências: o ajuste não se mistura com uma movimentação em andamento.
        var produto = buscarAtivoBloqueado(id);
        if (produtos.existsByCodigoAndIdNot(entrada.codigo(), id)) throw duplicado();
        var saldos = entrada.estoques() == null ? List.<ProdutoAtualizacaoRequest.Saldo>of() : entrada.estoques();
        if (saldos.stream().map(ProdutoAtualizacaoRequest.Saldo::lojaId).distinct().count() != saldos.size()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Cada loja pode aparecer uma vez");
        }
        // Guarda o "antes → depois" de cada campo alterado para o registro de edições.
        var mudancas = new java.util.ArrayList<String>();
        if (!produto.getCodigo().equals(entrada.codigo())) mudancas.add("Código: " + produto.getCodigo() + " → " + entrada.codigo());
        if (!produto.getNome().equals(entrada.nome())) mudancas.add("Nome: " + produto.getNome() + " → " + entrada.nome());
        produto.atualizar(entrada.codigo(), entrada.nome());
        for (var saldo : saldos) {
            var loja = lojas.findById(saldo.lojaId()).orElseThrow(() ->
                new NegocioException(HttpStatus.NOT_FOUND, "LOJA_NAO_ENCONTRADA", "Loja não encontrada"));
            var estoque = estoques.findByLojaIdAndProdutoId(loja.getId(), produto.getId())
                .orElseGet(() -> new Estoque(loja, produto, 0));
            if (estoque.getQuantidade() != saldo.quantidade()) {
                mudancas.add("Quantidade " + loja.getNome() + ": " + estoque.getQuantidade() + " → " + saldo.quantidade());
            }
            estoque.definir(saldo.quantidade());
            estoques.save(estoque);
        }
        if (!mudancas.isEmpty()) {
            var texto = String.join("; ", mudancas);
            edicoes.save(new ProdutoEdicao(produto, usuarios.getReferenceById(usuarioId), java.time.Instant.now(),
                texto.length() > 2000 ? texto.substring(0, 1997) + "..." : texto));
        }
        try {
            produtos.flush();
            estoques.flush();
            edicoes.flush();
        } catch (DataIntegrityViolationException error) {
            if (violouUnico(error)) throw duplicado();
            throw error;
        }
        return ProdutoResponse.from(produto);
    }

    /** Somente ADMIN: o produto some das listas e do estoque, mas o histórico continua mostrando-o. */
    @Transactional
    public void excluir(Long id, Long usuarioId) {
        var produto = buscarAtivoBloqueado(id);
        produto.excluir();
        edicoes.save(new ProdutoEdicao(produto, usuarios.getReferenceById(usuarioId), java.time.Instant.now(),
            "Excluiu o produto " + produto.getCodigo() + " (" + produto.getNome() + ")", "EXCLUSAO_PRODUTO"));
    }

    private Produto buscarAtivoBloqueado(Long id) {
        return produtos.buscarParaTransferencia(id).filter(Produto::isAtivo).orElseThrow(() ->
            new NegocioException(HttpStatus.NOT_FOUND, "PRODUTO_NAO_ENCONTRADO", "Produto não encontrado"));
    }

    private static boolean violouUnico(Throwable causa) {
        while (causa != null) {
            if (causa instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) return true;
            causa = causa.getCause();
        }
        return false;
    }

    /** Visível a todos os usuários autenticados. */
    @Transactional(readOnly = true)
    public List<ProdutoEdicaoResponse> edicoes() {
        return edicoes.findAllByOrderByDataHoraDescIdDesc().stream().map(ProdutoEdicaoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar() {
        return produtos.findAllByAtivoTrueOrderByIdAsc().stream().map(ProdutoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscar(Long id) {
        return produtos.findById(id).filter(Produto::isAtivo).map(ProdutoResponse::from).orElseThrow(() ->
            new NegocioException(HttpStatus.NOT_FOUND, "PRODUTO_NAO_ENCONTRADO", "Produto não encontrado"));
    }

    private NegocioException duplicado() {
        return new NegocioException(HttpStatus.CONFLICT, "CODIGO_DUPLICADO", "Já existe um produto com esse código");
    }
}
