package br.com.estoqueapi.service;

import br.com.estoqueapi.config.FuncionarioPrincipal;
import br.com.estoqueapi.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncionarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public FuncionarioDetailsService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return usuarios.findByEmail(email.strip()).map(FuncionarioPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }
}
