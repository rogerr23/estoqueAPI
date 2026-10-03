package br.com.estoqueapi.controller;

import br.com.estoqueapi.config.FuncionarioPrincipal;
import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.service.TransferenciaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transferencias")
public class TransferenciaController {
    private final TransferenciaService transferencias;
    public TransferenciaController(TransferenciaService transferencias) { this.transferencias = transferencias; }

    @PostMapping
    @Operation(summary = "Transferir produto entre lojas; responsável e horário vêm do servidor")
    public ResponseEntity<TransferenciaResponse> transferir(@Valid @RequestBody TransferenciaRequest entrada,
            @AuthenticationPrincipal FuncionarioPrincipal funcionario) {
        return ResponseEntity.status(201).body(transferencias.transferir(entrada, funcionario.getUsuario().id()));
    }

    @GetMapping
    @Operation(summary = "Consultar histórico do mais recente para o mais antigo")
    public List<TransferenciaResponse> historico() { return transferencias.historico(); }
}
