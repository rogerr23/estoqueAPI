package br.com.estoqueapi.dto;

import br.com.estoqueapi.entity.Perfil;
import jakarta.validation.constraints.*;
import java.util.Locale;

/** Senha é obrigatória no cadastro; na edição, vazia mantém a senha atual. */
public record UsuarioRequest(
    @NotBlank(message = "Nome é obrigatório") @Size(max = 150, message = "Nome deve ter até 150 caracteres") String nome,
    @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") @Size(max = 254, message = "E-mail deve ter até 254 caracteres") String email,
    @Size(min = 6, max = 72, message = "Senha deve ter entre 6 e 72 caracteres") String senha,
    @NotNull(message = "Perfil é obrigatório") Perfil perfil,
    Boolean ativo
) {
    public UsuarioRequest {
        nome = nome == null ? null : nome.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        senha = senha == null || senha.isEmpty() ? null : senha;
    }
}
