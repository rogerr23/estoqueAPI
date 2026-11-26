package br.com.estoqueapi.dto;

import br.com.estoqueapi.entity.ProdutoEdicao;
import java.time.Instant;

public record ProdutoEdicaoResponse(Long id, Instant dataHora, ProdutoResponse produto, UsuarioResponse usuario, String alteracoes) {
    public static ProdutoEdicaoResponse from(ProdutoEdicao e) {
        return new ProdutoEdicaoResponse(e.getId(), e.getDataHora(), ProdutoResponse.from(e.getProduto()),
            UsuarioResponse.from(e.getUsuario()), e.getAlteracoes());
    }
}
