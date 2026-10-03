package br.com.estoqueapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record ProdutoRequest(
    @NotBlank(message = "Código é obrigatório") @Size(max = 60, message = "Código deve ter até 60 caracteres") String codigo,
    @NotBlank(message = "Nome é obrigatório") @Size(max = 150, message = "Nome deve ter até 150 caracteres") String nome
) {
    public ProdutoRequest {
        codigo = codigo == null ? null : codigo.strip().toUpperCase(Locale.ROOT);
        nome = nome == null ? null : nome.strip();
    }
}
