package br.com.estoqueapi.repository;

import br.com.estoqueapi.entity.Loja;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LojaRepository extends JpaRepository<Loja, Long> {
    java.util.Optional<Loja> findFirstByNomeOrderByIdAsc(String nome);
    java.util.List<Loja> findAllByOrderByIdAsc();
}
