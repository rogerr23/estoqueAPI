package br.com.estoqueapi.dto;

public record EstoqueResponse(Long produtoId, String codigo, String nome, int quantidade) {}
