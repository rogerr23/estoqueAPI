package br.com.estoqueapi.config;

import br.com.estoqueapi.dto.UsuarioResponse;
import br.com.estoqueapi.entity.Usuario;
import org.springframework.security.core.userdetails.User;
import java.util.List;

public class FuncionarioPrincipal extends User {
    private final UsuarioResponse usuario;

    public FuncionarioPrincipal(Usuario usuario) {
        super(usuario.getEmail(), usuario.getSenhaHash(), List.of());
        this.usuario = new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
    public UsuarioResponse getUsuario() { return usuario; }
}
