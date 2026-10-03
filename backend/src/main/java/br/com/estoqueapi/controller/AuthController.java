package br.com.estoqueapi.controller;

import br.com.estoqueapi.config.FuncionarioPrincipal;
import br.com.estoqueapi.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record CsrfResponse(String token, String headerName, String parameterName) {}

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @GetMapping("/csrf")
    @Operation(summary = "Obter token CSRF; obter novamente após login e logout")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName(), token.getParameterName());
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar funcionário autenticado")
    public UsuarioResponse me(@AuthenticationPrincipal FuncionarioPrincipal principal) {
        return principal.getUsuario();
    }
}
