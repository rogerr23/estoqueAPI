package br.com.estoqueapi.repository;

import br.com.estoqueapi.entity.Entrada;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    @EntityGraph(attributePaths = {"produto", "loja", "usuario"})
    java.util.List<Entrada> findAllByOrderByDataHoraDescIdDesc();

    @EntityGraph(attributePaths = {"produto", "loja"})
    java.util.List<Entrada> findAllByUsuarioIdOrderByDataHoraDescIdDesc(Long usuarioId);

    /** Linhas: usuarioId, total de entradas, última data. */
    @Query("select e.usuario.id, count(e), max(e.dataHora) from Entrada e group by e.usuario.id")
    java.util.List<Object[]> resumoPorUsuario();
}
