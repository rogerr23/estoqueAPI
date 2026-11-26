package br.com.estoqueapi.config;

import br.com.estoqueapi.dto.UsuarioResponse;
import br.com.estoqueapi.entity.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.List;

public class FuncionarioPrincipal extends User {
    private final UsuarioResponse usuario;

    public FuncionarioPrincipal(Usuario usuario) {
        // Usuários desativados não conseguem entrar; o perfil vira a autoridade ROLE_ADMIN/ROLE_FUNCIONARIO.
        super(usuario.getEmail(), usuario.getSenhaHash(), usuario.isAtivo(), true, true, true,
            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
        this.usuario = UsuarioResponse.from(usuario);
    }
    public UsuarioResponse getUsuario() { return usuario; }
}
