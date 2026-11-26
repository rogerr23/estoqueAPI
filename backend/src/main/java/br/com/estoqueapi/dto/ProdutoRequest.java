package br.com.estoqueapi.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;

/** Loja e quantidade são opcionais: quando informadas, o produto já entra no estoque dessa loja. */
public record ProdutoRequest(
    @NotBlank(message = "Código é obrigatório") @Size(max = 60, message = "Código deve ter até 60 caracteres") String codigo,
    @NotBlank(message = "Nome é obrigatório") @Size(max = 150, message = "Nome deve ter até 150 caracteres") String nome,
    @Positive(message = "Loja inválida") Long lojaId,
    @PositiveOrZero(message = "Quantidade não pode ser negativa") Integer quantidade
) {
    public ProdutoRequest {
        codigo = codigo == null ? null : codigo.strip().toUpperCase(Locale.ROOT);
        nome = nome == null ? null : nome.strip();
    }

    public ProdutoRequest(String codigo, String nome) { this(codigo, nome, null, null); }
}
