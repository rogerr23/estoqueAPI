package br.com.estoqueapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponse(String codigo, String mensagem, Map<String, String> campos) {
    public ErroResponse(String codigo, String mensagem) { this(codigo, mensagem, Map.of()); }
}
