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

@Service
public class TransferenciaService {
    private final ProdutoRepository produtos;
    private final LojaRepository lojas;
    private final EstoqueRepository estoques;
    private final UsuarioRepository usuarios;
    private final TransferenciaRepository transferencias;
    private final ProdutoEdicaoRepository registros;

    public TransferenciaService(ProdutoRepository produtos, LojaRepository lojas, EstoqueRepository estoques,
            UsuarioRepository usuarios, TransferenciaRepository transferencias, ProdutoEdicaoRepository registros) {
        this.produtos = produtos; this.lojas = lojas; this.estoques = estoques;
        this.usuarios = usuarios; this.transferencias = transferencias; this.registros = registros;
    }

    @Transactional
    public TransferenciaResponse transferir(TransferenciaRequest entrada, Long usuarioId) {
        validar(entrada);
        // Um bloqueio por produto serializa suas movimentações, inclusive quando
        // o estoque de destino ainda não existe. Produtos diferentes seguem em paralelo.
        var produto = produtos.buscarParaTransferencia(entrada.produtoId()).filter(Produto::isAtivo).orElseThrow(() ->
            erro(HttpStatus.NOT_FOUND, "PRODUTO_NAO_ENCONTRADO", "Produto não encontrado"));
        var origem = buscarLoja(entrada.lojaOrigemId());
        var destino = buscarLoja(entrada.lojaDestinoId());
        var usuario = usuarios.findById(usuarioId).orElseThrow(() ->
            erro(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Funcionário não encontrado"));
        var saldoOrigem = estoques.findByLojaIdAndProdutoId(origem.getId(), produto.getId());
        if (saldoOrigem.isEmpty() || saldoOrigem.get().getQuantidade() < entrada.quantidade()) {
            throw erro(HttpStatus.CONFLICT, "SALDO_INSUFICIENTE", "Saldo insuficiente na loja de origem");
        }
        var saldoDestino = estoques.findByLojaIdAndProdutoId(destino.getId(), produto.getId())
            .orElseGet(() -> new Estoque(destino, produto, 0));
        if ((long) saldoDestino.getQuantidade() + entrada.quantidade() > Integer.MAX_VALUE) {
            throw erro(HttpStatus.CONFLICT, "LIMITE_ESTOQUE", "Quantidade excede o limite do estoque de destino");
        }
        saldoOrigem.get().debitar(entrada.quantidade());
        saldoDestino.creditar(entrada.quantidade());
        estoques.save(saldoDestino);
        // Persiste os saldos antes do histórico; qualquer falha seguinte desfaz tudo.
        estoques.flush();
        var transferencia = transferencias.saveAndFlush(new Transferencia(produto, origem, destino,
            entrada.quantidade(), Instant.now(), usuario));
        return TransferenciaResponse.from(transferencia);
    }

    /**
     * Somente ADMIN: desfaz a transferência (devolve as unidades à origem) e apaga o registro.
     * Recusa se a loja de destino já não tiver as unidades para devolver.
     */
    @Transactional
    public void excluir(Long id, Long adminId) {
        var transferencia = transferencias.findById(id).orElseThrow(() ->
            erro(HttpStatus.NOT_FOUND, "TRANSFERENCIA_NAO_ENCONTRADA", "Transferência não encontrada"));
        // Mesmo bloqueio por produto usado nas transferências, para os saldos não se cruzarem.
        var produto = produtos.buscarParaTransferencia(transferencia.getProduto().getId()).orElseThrow();
        var origem = transferencia.getLojaOrigem();
        var destino = transferencia.getLojaDestino();
        int quantidade = transferencia.getQuantidade();
        var saldoDestino = estoques.findByLojaIdAndProdutoId(destino.getId(), produto.getId());
        if (saldoDestino.isEmpty() || saldoDestino.get().getQuantidade() < quantidade) {
            throw erro(HttpStatus.CONFLICT, "SALDO_INSUFICIENTE",
                "A loja de destino não tem mais essas unidades para devolver; ajuste o estoque antes de excluir");
        }
        var saldoOrigem = estoques.findByLojaIdAndProdutoId(origem.getId(), produto.getId())
            .orElseGet(() -> new Estoque(origem, produto, 0));
        saldoDestino.get().debitar(quantidade);
        saldoOrigem.creditar(quantidade);
        estoques.save(saldoOrigem);
        var autor = transferencia.getUsuario().getNome();
        transferencias.delete(transferencia);
        registros.save(new ProdutoEdicao(produto, usuarios.getReferenceById(adminId), Instant.now(),
            "Excluiu a transferência #%d de %s: %d un. de %s para %s (estoque devolvido à origem)".formatted(
                id, autor, quantidade, origem.getNome(), destino.getNome()), "EXCLUSAO_TRANSFERENCIA"));
        estoques.flush();
        transferencias.flush();
    }

    @Transactional(readOnly = true)
    public List<TransferenciaResponse> historico() {
        return transferencias.findAllByOrderByDataHoraDescIdDesc().stream().map(TransferenciaResponse::from).toList();
    }

    private void validar(TransferenciaRequest entrada) {
        if (entrada == null || entrada.produtoId() == null || entrada.produtoId() <= 0
                || entrada.lojaOrigemId() == null || entrada.lojaOrigemId() <= 0
                || entrada.lojaDestinoId() == null || entrada.lojaDestinoId() <= 0
                || entrada.quantidade() == null || entrada.quantidade() <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "IDs e quantidade devem ser inteiros positivos");
        }
        if (entrada.lojaOrigemId().equals(entrada.lojaDestinoId())) {
            throw erro(HttpStatus.BAD_REQUEST, "LOJAS_IGUAIS", "Origem e destino devem ser diferentes");
        }
    }

    private Loja buscarLoja(Long id) {
        return lojas.findById(id).orElseThrow(() ->
            erro(HttpStatus.NOT_FOUND, "LOJA_NAO_ENCONTRADA", "Loja não encontrada"));
    }

    private NegocioException erro(HttpStatus status, String codigo, String mensagem) {
        return new NegocioException(status, codigo, mensagem);
    }
}
