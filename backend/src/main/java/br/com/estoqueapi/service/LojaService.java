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

    public List<EstoqueResponse> estoque(Long lojaId) {
        if (!lojas.existsById(lojaId)) {
            throw new NegocioException(HttpStatus.NOT_FOUND, "LOJA_NAO_ENCONTRADA", "Loja não encontrada");
        }
        var saldos = estoques.findAllByLojaId(lojaId).stream()
            .collect(Collectors.toMap(e -> e.getProduto().getId(), e -> e.getQuantidade()));
        return produtos.findAllByOrderByIdAsc().stream().map(p ->
            new EstoqueResponse(p.getId(), p.getCodigo(), p.getNome(), saldos.getOrDefault(p.getId(), 0))).toList();
    }
}
