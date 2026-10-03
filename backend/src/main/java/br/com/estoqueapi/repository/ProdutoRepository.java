package br.com.estoqueapi.repository;

import br.com.estoqueapi.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    java.util.Optional<Produto> findByCodigo(String codigo);
}
