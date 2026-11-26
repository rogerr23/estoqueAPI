package br.com.estoqueapi;

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
class LojaAdminTests extends DatabaseTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private MockHttpSession login(String email, String senha) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf()).param("email", email).param("senha", senha))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test
    void adminCriaERenomeiaLojaQueJaFuncionaNoEstoque() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        String nome = "Loja " + UUID.randomUUID().toString().substring(0, 6);
        var r = mvc.perform(post("/api/lojas").session(admin).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"  " + nome + "  \"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("nome").value(nome)).andReturn();
        long id = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/lojas/" + id + "/estoque").session(admin)).andExpect(status().isOk());
        mvc.perform(post("/api/lojas").session(admin).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"" + nome.toUpperCase() + "\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("codigo").value("LOJA_DUPLICADA"));
        mvc.perform(put("/api/lojas/" + id).session(admin).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"" + nome + " Centro\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("nome").value(nome + " Centro"));
        mvc.perform(post("/api/lojas").session(admin).with(csrf()).contentType("application/json").content("{\"nome\":\" \"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void funcionarioNaoCriaNemRenomeiaLoja() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        String email = "l-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/usuarios").session(admin).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"F\",\"email\":\"" + email + "\",\"senha\":\"segredo1\",\"perfil\":\"FUNCIONARIO\"}"))
            .andExpect(status().isCreated());
        var func = login(email, "segredo1");
        mvc.perform(post("/api/lojas").session(func).with(csrf()).contentType("application/json").content("{\"nome\":\"X\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/lojas/1").session(func).with(csrf()).contentType("application/json").content("{\"nome\":\"X\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/lojas").session(func)).andExpect(status().isOk());
    }
}
