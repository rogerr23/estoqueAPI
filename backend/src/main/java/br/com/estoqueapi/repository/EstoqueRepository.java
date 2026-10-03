package br.com.estoqueapi.repository;

import br.com.estoqueapi.entity.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    java.util.Optional<Estoque> findByLojaIdAndProdutoId(Long lojaId, Long produtoId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "produto")
    java.util.List<Estoque> findAllByLojaId(Long lojaId);
}
