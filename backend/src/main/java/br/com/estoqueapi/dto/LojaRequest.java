package br.com.estoqueapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LojaRequest(
    @NotBlank(message = "Nome é obrigatório") @Size(max = 150, message = "Nome deve ter até 150 caracteres") String nome
) {
    public LojaRequest { nome = nome == null ? null : nome.strip().replaceAll("\\s+", " "); }
}
