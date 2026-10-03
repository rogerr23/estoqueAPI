package br.com.estoqueapi.controller;

import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {
    private final ProdutoService produtos;
    public ProdutoController(ProdutoService produtos) { this.produtos = produtos; }

    @PostMapping
    @Operation(summary = "Cadastrar produto com código único normalizado")
    public ResponseEntity<ProdutoResponse> cadastrar(@Valid @RequestBody ProdutoRequest entrada) {
        var produto = produtos.cadastrar(entrada);
        return ResponseEntity.created(URI.create("/api/produtos/" + produto.id())).body(produto);
    }

    @GetMapping
    @Operation(summary = "Listar produtos por ID")
    public List<ProdutoResponse> listar() { return produtos.listar(); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar produto pelo ID")
    public ProdutoResponse buscar(@PathVariable Long id) { return produtos.buscar(id); }
}
