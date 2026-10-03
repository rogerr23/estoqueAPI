package br.com.estoqueapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferenciaRequest(
    @NotNull @Positive Long produtoId,
    @NotNull @Positive Long lojaOrigemId,
    @NotNull @Positive Long lojaDestinoId,
    @NotNull @Positive Integer quantidade
) {}
