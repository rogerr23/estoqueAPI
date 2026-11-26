package br.com.estoqueapi.dto;

import br.com.estoqueapi.entity.Entrada;
import java.time.Instant;

public record EntradaResponse(Long id, ProdutoResponse produto, LojaResponse loja, int quantidade,
        String observacao, Instant dataHora, UsuarioResponse usuario) {
    public static EntradaResponse from(Entrada e) {
        return new EntradaResponse(e.getId(), ProdutoResponse.from(e.getProduto()),
            new LojaResponse(e.getLoja().getId(), e.getLoja().getNome()), e.getQuantidade(), e.getObservacao(),
            e.getDataHora(), UsuarioResponse.from(e.getUsuario()));
    }
}
