package br.com.estoqueapi.repository;

import br.com.estoqueapi.entity.Transferencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"produto", "lojaOrigem", "lojaDestino", "usuario"})
    java.util.List<Transferencia> findAllByOrderByDataHoraDescIdDesc();
}
