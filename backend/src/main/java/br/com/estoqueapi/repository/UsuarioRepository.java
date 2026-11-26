package br.com.estoqueapi.repository;

import br.com.estoqueapi.entity.Perfil;
import br.com.estoqueapi.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    java.util.Optional<Usuario> findByEmail(String email);
    java.util.Optional<Usuario> findFirstByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    long countByPerfilAndAtivoTrue(Perfil perfil);
    java.util.List<Usuario> findAllByOrderByNomeAscIdAsc();
}
