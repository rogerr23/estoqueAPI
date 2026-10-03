package br.com.estoqueapi.repository;

import br.com.estoqueapi.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    java.util.Optional<Produto> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    java.util.List<Produto> findAllByOrderByIdAsc();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Produto p where p.id = :id")
    java.util.Optional<Produto> buscarParaTransferencia(@org.springframework.data.repository.query.Param("id") Long id);
}
