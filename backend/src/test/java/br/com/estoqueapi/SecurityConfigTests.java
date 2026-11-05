package br.com.estoqueapi;

import br.com.estoqueapi.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = SecurityConfigTests.PublicacaoController.class)
@Import(SecurityConfig.class)
class SecurityConfigTests {
    @Autowired MockMvc mvc;

    @Test
    void permiteCarregarLoginEAssetsSemSessao() throws Exception {
        for (String rota : new String[]{"/", "/index.html", "/assets/app.js"}) {
            mvc.perform(get(rota)).andExpect(status().isOk());
        }
    }

    @Test
    void publicarInterfaceMantemApiESwaggerProtegidos() throws Exception {
        for (String rota : new String[]{"/api/produtos", "/swagger-ui/index.html", "/v3/api-docs"}) {
            mvc.perform(get(rota)).andExpect(status().isUnauthorized());
        }
    }

    @RestController
    static class PublicacaoController {
        @GetMapping({"/", "/index.html", "/assets/app.js", "/api/produtos",
                "/swagger-ui/index.html", "/v3/api-docs"})
        String conteudo() { return "conteudo"; }
    }
}
