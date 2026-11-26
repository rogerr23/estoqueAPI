package br.com.estoqueapi;

import br.com.estoqueapi.repository.LojaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.demo.email=teste@example.test"})
@ActiveProfiles({"dev", "test"})
@AutoConfigureMockMvc
@Transactional
class ProdutoEstoqueInicialTests extends DatabaseTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LojaRepository lojas;

    private MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                .param("email", "teste@example.test").param("senha", PASSWORD))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private String codigo() { return "CT-EI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }

    @Test
    void cadastroComQuantidadeJaEntraNoEstoqueDaLoja() throws Exception {
        var sessao = login();
        long loja = lojas.findFirstByNomeOrderByIdAsc("Loja A").orElseThrow().getId();
        String codigo = codigo();
        var r = mvc.perform(post("/api/produtos").session(sessao).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"" + codigo + "\",\"nome\":\"Mochila\",\"lojaId\":" + loja + ",\"quantidade\":15}"))
            .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/lojas/" + loja + "/estoque").session(sessao))
            .andExpect(jsonPath("$[?(@.produtoId==" + id + ")].quantidade").value(15));
    }

    @Test
    void quantidadeSemLojaOuLojaInexistenteSaoRejeitadas() throws Exception {
        var sessao = login();
        mvc.perform(post("/api/produtos").session(sessao).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"" + codigo() + "\",\"nome\":\"X\",\"quantidade\":5}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/produtos").session(sessao).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"" + codigo() + "\",\"nome\":\"X\",\"lojaId\":999999,\"quantidade\":5}"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/produtos").session(sessao).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"" + codigo() + "\",\"nome\":\"X\",\"lojaId\":1,\"quantidade\":-1}"))
            .andExpect(status().isBadRequest());
    }
}
