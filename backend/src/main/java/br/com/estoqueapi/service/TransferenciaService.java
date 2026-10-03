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

    public TransferenciaService(ProdutoRepository produtos, LojaRepository lojas, EstoqueRepository estoques,
            UsuarioRepository usuarios, TransferenciaRepository transferencias) {
        this.produtos = produtos; this.lojas = lojas; this.estoques = estoques;
        this.usuarios = usuarios; this.transferencias = transferencias;
    }

    @Transactional
    public TransferenciaResponse transferir(TransferenciaRequest entrada, Long usuarioId) {
        validar(entrada);
        // Um bloqueio por produto serializa suas movimentações, inclusive quando
        // o estoque de destino ainda não existe. Produtos diferentes seguem em paralelo.
        var produto = produtos.buscarParaTransferencia(entrada.produtoId()).orElseThrow(() ->
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
