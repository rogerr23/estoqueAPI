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
class EntradaTests extends DatabaseTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LojaRepository lojas;

    private MockHttpSession login(String email, String senha) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf()).param("email", email).param("senha", senha))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test
    void funcionarioRegistraEntradaQueSomaNoEstoqueEApareceNasAtividades() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        long loja = lojas.findFirstByNomeOrderByIdAsc("Loja B").orElseThrow().getId();
        var r = mvc.perform(post("/api/produtos").session(admin).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"CT-EN-" + UUID.randomUUID().toString().substring(0, 8) + "\",\"nome\":\"Item\"}"))
            .andExpect(status().isCreated()).andReturn();
        long produto = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
        String email = "rec-" + UUID.randomUUID() + "@example.test";
        var criado = mvc.perform(post("/api/usuarios").session(admin).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Recebedor\",\"email\":\"" + email + "\",\"senha\":\"segredo1\",\"perfil\":\"FUNCIONARIO\"}"))
            .andExpect(status().isCreated()).andReturn();
        long funcId = json.readTree(criado.getResponse().getContentAsString()).get("id").asLong();
        var func = login(email, "segredo1");

        String body = "{\"produtoId\":" + produto + ",\"lojaId\":" + loja + ",\"quantidade\":%d,\"observacao\":\" NF 123 \"}";
        mvc.perform(post("/api/entradas").session(func).with(csrf()).contentType("application/json").content(body.formatted(8)))
            .andExpect(status().isCreated()).andExpect(jsonPath("usuario.nome").value("Recebedor"))
            .andExpect(jsonPath("observacao").value("NF 123"));
        mvc.perform(post("/api/entradas").session(func).with(csrf()).contentType("application/json").content(body.formatted(2)))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/lojas/" + loja + "/estoque").session(func))
            .andExpect(jsonPath("$[?(@.produtoId==" + produto + ")].quantidade").value(10));
        mvc.perform(get("/api/entradas").session(func)).andExpect(status().isOk()).andExpect(jsonPath("$[0].quantidade").value(2));
        mvc.perform(get("/api/usuarios").session(admin)).andExpect(jsonPath("$[?(@.id==" + funcId + ")].entradas").value(2));
        mvc.perform(get("/api/usuarios/" + funcId + "/atividades").session(admin)).andExpect(jsonPath("$[0].tipo").value("ENTRADA"));
    }

    @Test
    void rejeitaQuantidadeInvalidaEProdutoExcluido() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        long loja = lojas.findFirstByNomeOrderByIdAsc("Loja A").orElseThrow().getId();
        var r = mvc.perform(post("/api/produtos").session(admin).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"CT-EN-" + UUID.randomUUID().toString().substring(0, 8) + "\",\"nome\":\"Item\"}"))
            .andExpect(status().isCreated()).andReturn();
        long produto = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/entradas").session(admin).with(csrf()).contentType("application/json")
                .content("{\"produtoId\":" + produto + ",\"lojaId\":" + loja + ",\"quantidade\":0}"))
            .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/produtos/" + produto).session(admin).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(post("/api/entradas").session(admin).with(csrf()).contentType("application/json")
                .content("{\"produtoId\":" + produto + ",\"lojaId\":" + loja + ",\"quantidade\":3}"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/entradas").with(csrf()).contentType("application/json")
                .content("{\"produtoId\":" + produto + ",\"lojaId\":" + loja + ",\"quantidade\":3}"))
            .andExpect(status().isUnauthorized());
    }
}
