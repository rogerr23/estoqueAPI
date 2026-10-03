package br.com.estoqueapi.dto;

import br.com.estoqueapi.entity.Transferencia;
import java.time.Instant;

public record TransferenciaResponse(Long id, ProdutoResponse produto, LojaResponse lojaOrigem,
        LojaResponse lojaDestino, int quantidade, Instant dataHora, UsuarioResponse usuario) {
    public static TransferenciaResponse from(Transferencia transferencia) {
        var usuario = transferencia.getUsuario();
        var origem = transferencia.getLojaOrigem();
        var destino = transferencia.getLojaDestino();
        return new TransferenciaResponse(transferencia.getId(), ProdutoResponse.from(transferencia.getProduto()),
            new LojaResponse(origem.getId(), origem.getNome()), new LojaResponse(destino.getId(), destino.getNome()),
            transferencia.getQuantidade(), transferencia.getDataHora(),
            new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail()));
    }
}
