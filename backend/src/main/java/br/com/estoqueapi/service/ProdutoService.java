package br.com.estoqueapi.service;

import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.entity.Produto;
import br.com.estoqueapi.exception.NegocioException;
import br.com.estoqueapi.repository.ProdutoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProdutoService {
    private final ProdutoRepository produtos;
    public ProdutoService(ProdutoRepository produtos) { this.produtos = produtos; }

    @Transactional
    public ProdutoResponse cadastrar(ProdutoRequest entrada) {
        if (produtos.existsByCodigo(entrada.codigo())) throw duplicado();
        try {
            return ProdutoResponse.from(produtos.saveAndFlush(new Produto(entrada.codigo(), entrada.nome())));
        } catch (DataIntegrityViolationException error) {
            // A restrição única também cobre cadastros simultâneos do mesmo código.
            Throwable causa = error;
            while (causa != null) {
                if (causa instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                    throw duplicado();
                }
                causa = causa.getCause();
            }
            throw error;
        }
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar() {
        return produtos.findAllByOrderByIdAsc().stream().map(ProdutoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscar(Long id) {
        return produtos.findById(id).map(ProdutoResponse::from).orElseThrow(() ->
            new NegocioException(HttpStatus.NOT_FOUND, "PRODUTO_NAO_ENCONTRADO", "Produto não encontrado"));
    }

    private NegocioException duplicado() {
        return new NegocioException(HttpStatus.CONFLICT, "CODIGO_DUPLICADO", "Já existe um produto com esse código");
    }
}
