package br.com.estoqueapi.config;

import br.com.estoqueapi.dto.ErroResponse;
import br.com.estoqueapi.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * A sessão guarda perfil e situação do momento do login. Este filtro confere o banco a cada
 * requisição: se o usuário foi desativado ou mudou de perfil, a sessão é encerrada na hora.
 */
public class SessaoAtualFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;
    private final ObjectMapper json;

    public SessaoAtualFilter(UsuarioRepository usuarios, ObjectMapper json) {
        this.usuarios = usuarios;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof FuncionarioPrincipal principal) {
            var sessao = principal.getUsuario();
            boolean valido = usuarios.findById(sessao.id())
                .filter(u -> u.isAtivo() && u.getPerfil().name().equals(sessao.perfil()))
                .isPresent();
            if (!valido) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                json.writeValue(response.getWriter(), new ErroResponse("NAO_AUTENTICADO",
                    "Seu acesso foi alterado por um administrador. Entre novamente."));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
