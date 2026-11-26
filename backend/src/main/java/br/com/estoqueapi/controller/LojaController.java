package br.com.estoqueapi.controller;

import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.service.LojaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/lojas")
public class LojaController {
    private final LojaService lojas;
    public LojaController(LojaService lojas) { this.lojas = lojas; }

    @GetMapping
    @Operation(summary = "Listar lojas disponíveis")
    public List<LojaResponse> listar() { return lojas.listar(); }

    @PostMapping
    @Operation(summary = "Cadastrar loja (somente administrador)")
    public ResponseEntity<LojaResponse> criar(@Valid @RequestBody LojaRequest entrada) {
        var loja = lojas.criar(entrada);
        return ResponseEntity.created(URI.create("/api/lojas/" + loja.id())).body(loja);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renomear loja (somente administrador)")
    public LojaResponse renomear(@PathVariable Long id, @Valid @RequestBody LojaRequest entrada) {
        return lojas.renomear(id, entrada);
    }

    @GetMapping("/{id}/estoque")
    @Operation(summary = "Consultar todos os produtos; estoque ausente retorna zero")
    public List<EstoqueResponse> estoque(@PathVariable Long id) { return lojas.estoque(id); }
}
