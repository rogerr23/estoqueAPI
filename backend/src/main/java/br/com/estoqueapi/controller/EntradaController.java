package br.com.estoqueapi.controller;

import br.com.estoqueapi.config.FuncionarioPrincipal;
import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.service.EntradaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/entradas")
public class EntradaController {
    private final EntradaService entradas;
    public EntradaController(EntradaService entradas) { this.entradas = entradas; }

    @PostMapping
    @Operation(summary = "Registrar entrada de mercadoria em uma loja; responsável e horário vêm do servidor")
    public ResponseEntity<EntradaResponse> registrar(@Valid @RequestBody EntradaRequest entrada,
            @AuthenticationPrincipal FuncionarioPrincipal funcionario) {
        return ResponseEntity.status(201).body(entradas.registrar(entrada, funcionario.getUsuario().id()));
    }

    @GetMapping
    @Operation(summary = "Listar entradas da mais recente para a mais antiga")
    public List<EntradaResponse> listar() { return entradas.listar(); }
}
