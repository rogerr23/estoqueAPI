package br.com.estoqueapi.service;

import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.exception.NegocioException;
import br.com.estoqueapi.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LojaService {
    private final LojaRepository lojas;
    private final ProdutoRepository produtos;
    private final EstoqueRepository estoques;

    public LojaService(LojaRepository lojas, ProdutoRepository produtos, EstoqueRepository estoques) {
        this.lojas = lojas; this.produtos = produtos; this.estoques = estoques;
    }

    public List<LojaResponse> listar() {
        return lojas.findAllByOrderByIdAsc().stream().map(l -> new LojaResponse(l.getId(), l.getNome())).toList();
    }

    /** Somente ADMIN (ver SecurityConfig). A loja nova começa sem estoque. */
    @Transactional
    public LojaResponse criar(LojaRequest entrada) {
        if (lojas.existsByNomeIgnoreCase(entrada.nome())) throw nomeDuplicado();
        var loja = lojas.saveAndFlush(new br.com.estoqueapi.entity.Loja(entrada.nome()));
        return new LojaResponse(loja.getId(), loja.getNome());
    }

    @Transactional
    public LojaResponse renomear(Long id, LojaRequest entrada) {
        var loja = lojas.findById(id).orElseThrow(() ->
            new NegocioException(HttpStatus.NOT_FOUND, "LOJA_NAO_ENCONTRADA", "Loja não encontrada"));
        if (lojas.existsByNomeIgnoreCaseAndIdNot(entrada.nome(), id)) throw nomeDuplicado();
        loja.renomear(entrada.nome());
        return new LojaResponse(loja.getId(), loja.getNome());
    }

    private NegocioException nomeDuplicado() {
        return new NegocioException(HttpStatus.CONFLICT, "LOJA_DUPLICADA", "Já existe uma loja com esse nome");
    }

    public List<EstoqueResponse> estoque(Long lojaId) {
        if (!lojas.existsById(lojaId)) {
            throw new NegocioException(HttpStatus.NOT_FOUND, "LOJA_NAO_ENCONTRADA", "Loja não encontrada");
        }
        var saldos = estoques.findAllByLojaId(lojaId).stream()
            .collect(Collectors.toMap(e -> e.getProduto().getId(), e -> e.getQuantidade()));
        return produtos.findAllByAtivoTrueOrderByIdAsc().stream().map(p ->
            new EstoqueResponse(p.getId(), p.getCodigo(), p.getNome(), saldos.getOrDefault(p.getId(), 0))).toList();
    }
}
