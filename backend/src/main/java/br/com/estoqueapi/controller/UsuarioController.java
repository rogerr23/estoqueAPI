package br.com.estoqueapi.controller;

import br.com.estoqueapi.config.FuncionarioPrincipal;
import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

/** Acesso restrito ao perfil ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarios;
    public UsuarioController(UsuarioService usuarios) { this.usuarios = usuarios; }

    @GetMapping
    @Operation(summary = "Listar funcionários com o resumo do que cada um fez (admin)")
    public List<FuncionarioResumo> listar() { return usuarios.listar(); }

    @PostMapping
    @Operation(summary = "Cadastrar funcionário ou administrador (admin)")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest entrada) {
        var criado = usuarios.criar(entrada);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar funcionário; senha vazia mantém a atual (admin)")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest entrada,
            @AuthenticationPrincipal FuncionarioPrincipal admin) {
        return usuarios.atualizar(id, entrada, admin.getUsuario().id());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar funcionário; o histórico é preservado (admin)")
    public ResponseEntity<Void> desativar(@PathVariable Long id, @AuthenticationPrincipal FuncionarioPrincipal admin) {
        usuarios.desativar(id, admin.getUsuario().id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/atividades")
    @Operation(summary = "Atividades do funcionário, da mais recente para a mais antiga (admin)")
    public List<AtividadeResponse> atividades(@PathVariable Long id) { return usuarios.atividades(id); }
}
