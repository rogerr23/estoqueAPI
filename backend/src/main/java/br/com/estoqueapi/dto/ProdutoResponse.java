package br.com.estoqueapi.dto;

import br.com.estoqueapi.entity.Produto;

public record ProdutoResponse(Long id, String codigo, String nome) {
    public static ProdutoResponse from(Produto produto) {
        return new ProdutoResponse(produto.getId(), produto.getCodigo(), produto.getNome());
    }
}
