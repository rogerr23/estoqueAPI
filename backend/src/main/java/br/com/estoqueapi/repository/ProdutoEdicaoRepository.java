package br.com.estoqueapi.repository;

import br.com.estoqueapi.entity.ProdutoEdicao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoEdicaoRepository extends JpaRepository<ProdutoEdicao, Long> {
    @EntityGraph(attributePaths = {"produto", "usuario"})
    java.util.List<ProdutoEdicao> findAllByOrderByDataHoraDescIdDesc();

    /** Linhas: usuarioId, última edição. */
    @org.springframework.data.jpa.repository.Query("select e.usuario.id, max(e.dataHora) from ProdutoEdicao e group by e.usuario.id")
    java.util.List<Object[]> ultimaPorUsuario();

    @EntityGraph(attributePaths = "produto")
    java.util.List<ProdutoEdicao> findAllByUsuarioIdOrderByDataHoraDescIdDesc(Long usuarioId);
}
