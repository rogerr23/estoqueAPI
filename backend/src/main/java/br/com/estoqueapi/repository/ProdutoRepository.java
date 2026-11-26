package br.com.estoqueapi.repository;

import br.com.estoqueapi.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    java.util.Optional<Produto> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);
    java.util.List<Produto> findAllByOrderByIdAsc();
    java.util.List<Produto> findAllByAtivoTrueOrderByIdAsc();
    java.util.List<Produto> findAllByCadastradoPorIdOrderByCadastradoEmDescIdDesc(Long usuarioId);
    /** Linhas: usuarioId, produtos cadastrados, último cadastro. */
    @org.springframework.data.jpa.repository.Query("select p.cadastradoPor.id, count(p), max(p.cadastradoEm) from Produto p where p.cadastradoPor is not null group by p.cadastradoPor.id")
    java.util.List<Object[]> resumoPorUsuario();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Produto p where p.id = :id")
    java.util.Optional<Produto> buscarParaTransferencia(@org.springframework.data.repository.query.Param("id") Long id);
}
