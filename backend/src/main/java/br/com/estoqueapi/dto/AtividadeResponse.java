package br.com.estoqueapi.dto;

import java.time.Instant;

/** Uma ação realizada por um funcionário (transferência ou cadastro de produto). */
public record AtividadeResponse(String tipo, Instant dataHora, String descricao) {}
