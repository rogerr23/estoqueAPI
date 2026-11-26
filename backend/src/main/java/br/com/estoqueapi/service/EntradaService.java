package br.com.estoqueapi.service;

import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.entity.*;
import br.com.estoqueapi.exception.NegocioException;
import br.com.estoqueapi.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

/** Entrada de mercadoria: soma unidades ao estoque de uma loja e registra quem fez. */
@Service
public class EntradaService {
    private final ProdutoRepository produtos;
    private final LojaRepository lojas;
    private final EstoqueRepository estoques;
    private final UsuarioRepository usuarios;
    private final EntradaRepository entradas;
    private final ProdutoEdicaoRepository registros;

    public EntradaService(ProdutoRepository produtos, LojaRepository lojas, EstoqueRepository estoques,
            UsuarioRepository usuarios, EntradaRepository entradas, ProdutoEdicaoRepository registros) {
        this.produtos = produtos; this.lojas = lojas; this.estoques = estoques;
        this.usuarios = usuarios; this.entradas = entradas; this.registros = registros;
    }

    @Transactional
    public EntradaResponse registrar(EntradaRequest entrada, Long usuarioId) {
        // Mesmo bloqueio por produto das transferências, para os saldos não se cruzarem.
        var produto = produtos.buscarParaTransferencia(entrada.produtoId()).filter(Produto::isAtivo).orElseThrow(() ->
            new NegocioException(HttpStatus.NOT_FOUND, "PRODUTO_NAO_ENCONTRADO", "Produto não encontrado"));
        var loja = lojas.findById(entrada.lojaId()).orElseThrow(() ->
            new NegocioException(HttpStatus.NOT_FOUND, "LOJA_NAO_ENCONTRADA", "Loja não encontrada"));
        var estoque = estoques.findByLojaIdAndProdutoId(loja.getId(), produto.getId())
            .orElseGet(() -> new Estoque(loja, produto, 0));
        if ((long) estoque.getQuantidade() + entrada.quantidade() > Integer.MAX_VALUE) {
            throw new NegocioException(HttpStatus.CONFLICT, "LIMITE_ESTOQUE", "Quantidade excede o limite do estoque");
        }
        estoque.creditar(entrada.quantidade());
        estoques.saveAndFlush(estoque);
        var salva = entradas.saveAndFlush(new Entrada(produto, loja, entrada.quantidade(), entrada.observacao(),
            Instant.now(), usuarios.getReferenceById(usuarioId)));
        return EntradaResponse.from(salva);
    }

    /**
     * Somente ADMIN: desfaz a entrada (retira as unidades da loja) e apaga o registro.
     * Recusa se a loja já não tiver essas unidades, para o estoque nunca ficar negativo.
     */
    @Transactional
    public void excluir(Long id, Long adminId) {
        var entrada = entradas.findById(id).orElseThrow(() ->
            new NegocioException(HttpStatus.NOT_FOUND, "ENTRADA_NAO_ENCONTRADA", "Entrada não encontrada"));
        // Mesmo bloqueio por produto usado nas outras movimentações.
        var produto = produtos.buscarParaTransferencia(entrada.getProduto().getId()).orElseThrow();
        var loja = entrada.getLoja();
        var saldo = estoques.findByLojaIdAndProdutoId(loja.getId(), produto.getId());
        if (saldo.isEmpty() || saldo.get().getQuantidade() < entrada.getQuantidade()) {
            throw new NegocioException(HttpStatus.CONFLICT, "SALDO_INSUFICIENTE",
                "A loja já não tem essas unidades para retirar; ajuste o estoque antes de excluir");
        }
        saldo.get().debitar(entrada.getQuantidade());
        var autor = entrada.getUsuario().getNome();
        entradas.delete(entrada);
        registros.save(new ProdutoEdicao(produto, usuarios.getReferenceById(adminId), Instant.now(),
            "Excluiu a entrada #%d de %s: %d un. na %s (estoque retirado da loja)".formatted(
                id, autor, entrada.getQuantidade(), loja.getNome()), "EXCLUSAO_ENTRADA"));
        estoques.flush();
        entradas.flush();
    }

    @Transactional(readOnly = true)
    public List<EntradaResponse> listar() {
        return entradas.findAllByOrderByDataHoraDescIdDesc().stream().map(EntradaResponse::from).toList();
    }
}
