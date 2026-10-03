package br.com.estoqueapi.controller;

import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.service.LojaService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lojas")
public class LojaController {
    private final LojaService lojas;
    public LojaController(LojaService lojas) { this.lojas = lojas; }

    @GetMapping
    @Operation(summary = "Listar lojas disponíveis")
    public List<LojaResponse> listar() { return lojas.listar(); }

    @GetMapping("/{id}/estoque")
    @Operation(summary = "Consultar todos os produtos; estoque ausente retorna zero")
    public List<EstoqueResponse> estoque(@PathVariable Long id) { return lojas.estoque(id); }
}
