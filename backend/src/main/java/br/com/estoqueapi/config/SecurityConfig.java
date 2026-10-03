package br.com.estoqueapi.config;

import br.com.estoqueapi.dto.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper json) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .anyRequest().authenticated())
            .formLogin(login -> login
                .loginPage("/api/auth/login")
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email").passwordParameter("senha")
                .successHandler((request, response, authentication) ->
                    responder(json, response, 200, ((FuncionarioPrincipal) authentication.getPrincipal()).getUsuario()))
                .failureHandler((request, response, error) ->
                    responder(json, response, 401, new ErroResponse("CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos"))))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    if (authentication == null) {
                        responder(json, response, 401, new ErroResponse("NAO_AUTENTICADO", "Autenticação necessária"));
                    } else {
                        response.setStatus(204);
                    }
                }))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) ->
                    responder(json, response, 401, new ErroResponse("NAO_AUTENTICADO", "Autenticação necessária")))
                .accessDeniedHandler((request, response, error) ->
                    responder(json, response, 403, new ErroResponse(
                        error instanceof CsrfException ? "CSRF_INVALIDO" : "ACESSO_NEGADO",
                        error instanceof CsrfException ? "Token CSRF ausente ou inválido" : "Acesso negado"))));
        return http.build();
    }

    private static void responder(ObjectMapper json, HttpServletResponse response, int status, Object body)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        json.writeValue(response.getWriter(), body);
    }
}
