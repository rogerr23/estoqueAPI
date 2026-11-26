package br.com.estoqueapi.repository;

import br.com.estoqueapi.entity.Transferencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {
    @EntityGraph(attributePaths = {"produto", "lojaOrigem", "lojaDestino", "usuario"})
    java.util.List<Transferencia> findAllByOrderByDataHoraDescIdDesc();

    @EntityGraph(attributePaths = {"produto", "lojaOrigem", "lojaDestino"})
    java.util.List<Transferencia> findAllByUsuarioIdOrderByDataHoraDescIdDesc(Long usuarioId);

    /** Linhas: usuarioId, total de transferências, unidades movimentadas, última data. */
    @Query("select t.usuario.id, count(t), coalesce(sum(t.quantidade), 0), max(t.dataHora) from Transferencia t group by t.usuario.id")
    java.util.List<Object[]> resumoPorUsuario();
}
