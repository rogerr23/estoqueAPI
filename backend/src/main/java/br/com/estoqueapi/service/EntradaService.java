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

    public EntradaService(ProdutoRepository produtos, LojaRepository lojas, EstoqueRepository estoques,
            UsuarioRepository usuarios, EntradaRepository entradas) {
        this.produtos = produtos; this.lojas = lojas; this.estoques = estoques;
        this.usuarios = usuarios; this.entradas = entradas;
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

    @Transactional(readOnly = true)
    public List<EntradaResponse> listar() {
        return entradas.findAllByOrderByDataHoraDescIdDesc().stream().map(EntradaResponse::from).toList();
    }
}
