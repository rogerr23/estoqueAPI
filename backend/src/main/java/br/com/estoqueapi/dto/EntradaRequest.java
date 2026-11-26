package br.com.estoqueapi.dto;

import jakarta.validation.constraints.*;

public record EntradaRequest(
    @NotNull(message = "Produto é obrigatório") @Positive(message = "Produto inválido") Long produtoId,
    @NotNull(message = "Loja é obrigatória") @Positive(message = "Loja inválida") Long lojaId,
    @NotNull(message = "Quantidade é obrigatória") @Positive(message = "Quantidade deve ser maior que zero") Integer quantidade,
    @Size(max = 200, message = "Observação deve ter até 200 caracteres") String observacao
) {
    public EntradaRequest {
        observacao = observacao == null || observacao.isBlank() ? null : observacao.strip();
    }
}
