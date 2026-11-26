package br.com.estoqueapi.controller;

import br.com.estoqueapi.config.FuncionarioPrincipal;
import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<ProdutoResponse> cadastrar(@Valid @RequestBody ProdutoRequest entrada,
            @AuthenticationPrincipal FuncionarioPrincipal funcionario) {
        var produto = produtos.cadastrar(entrada, funcionario == null ? null : funcionario.getUsuario().id());
        return ResponseEntity.created(URI.create("/api/produtos/" + produto.id())).body(produto);
    }

    @GetMapping
    @Operation(summary = "Listar produtos por ID")
    public List<ProdutoResponse> listar() { return produtos.listar(); }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar código, nome e saldos por loja (somente administrador)")
    public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoAtualizacaoRequest entrada,
            @AuthenticationPrincipal FuncionarioPrincipal admin) {
        return produtos.atualizar(id, entrada, admin.getUsuario().id());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir produto (somente administrador); o histórico é preservado")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal FuncionarioPrincipal admin) {
        produtos.excluir(id, admin.getUsuario().id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/edicoes")
    @Operation(summary = "Edições de produtos feitas pelos administradores, da mais recente para a mais antiga")
    public List<ProdutoEdicaoResponse> edicoes() { return produtos.edicoes(); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar produto pelo ID")
    public ProdutoResponse buscar(@PathVariable Long id) { return produtos.buscar(id); }
}
