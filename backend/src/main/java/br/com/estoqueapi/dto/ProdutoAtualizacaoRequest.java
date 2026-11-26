package br.com.estoqueapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Locale;

/** Edição feita pelo administrador. Lojas fora de "estoques" mantêm o saldo atual. */
public record ProdutoAtualizacaoRequest(
    @NotBlank(message = "Código é obrigatório") @Size(max = 60, message = "Código deve ter até 60 caracteres") String codigo,
    @NotBlank(message = "Nome é obrigatório") @Size(max = 150, message = "Nome deve ter até 150 caracteres") String nome,
    @Valid List<@NotNull Saldo> estoques,
    /** Opcional: ausente mantém o valor atual. */
    @PositiveOrZero(message = "Estoque mínimo não pode ser negativo") Integer estoqueMinimo
) {
    public ProdutoAtualizacaoRequest {
        codigo = codigo == null ? null : codigo.strip().toUpperCase(Locale.ROOT);
        nome = nome == null ? null : nome.strip();
    }

    public record Saldo(
        @NotNull(message = "Loja é obrigatória") @Positive(message = "Loja inválida") Long lojaId,
        @NotNull(message = "Quantidade é obrigatória") @PositiveOrZero(message = "Quantidade não pode ser negativa") Integer quantidade
    ) {}
}
