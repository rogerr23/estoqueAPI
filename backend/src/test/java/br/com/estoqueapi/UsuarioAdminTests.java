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
class UsuarioAdminTests extends DatabaseTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private MockHttpSession login(String email, String senha) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf()).param("email", email).param("senha", senha))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private long criar(MockHttpSession admin, String email, String perfil) throws Exception {
        var body = "{\"nome\":\"Maria\",\"email\":\"" + email + "\",\"senha\":\"segredo1\",\"perfil\":\"" + perfil + "\"}";
        var r = mvc.perform(post("/api/usuarios").session(admin).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("perfil").value(perfil))
            .andExpect(jsonPath("senha").doesNotExist()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void adminCriaFuncionarioQueOperaComProprioNomeEApareceNasAtividades() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        mvc.perform(get("/api/auth/me").session(admin)).andExpect(jsonPath("perfil").value("ADMIN"));
        String email = "maria-" + UUID.randomUUID() + "@example.test";
        long id = criar(admin, email, "FUNCIONARIO");

        var maria = login(email, "segredo1");
        String codigo = "CT-U-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        mvc.perform(post("/api/produtos").session(maria).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"" + codigo + "\",\"nome\":\"Item\"}")).andExpect(status().isCreated());

        mvc.perform(get("/api/usuarios").session(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id==" + id + ")].produtosCadastrados").value(1));
        mvc.perform(get("/api/usuarios/" + id + "/atividades").session(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tipo").value("CADASTRO_PRODUTO"));
    }

    @Test
    void funcionarioNaoAcessaAdministracao() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        String email = "joao-" + UUID.randomUUID() + "@example.test";
        criar(admin, email, "FUNCIONARIO");
        var joao = login(email, "segredo1");
        mvc.perform(get("/api/usuarios").session(joao)).andExpect(status().isForbidden())
            .andExpect(jsonPath("codigo").value("ACESSO_NEGADO"));
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void desativadoNaoEntraEEmailDuplicadoEhRejeitado() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        String email = "ana-" + UUID.randomUUID() + "@example.test";
        long id = criar(admin, email, "FUNCIONARIO");
        mvc.perform(post("/api/usuarios").session(admin).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Outra\",\"email\":\"" + email.toUpperCase() + "\",\"senha\":\"segredo1\",\"perfil\":\"FUNCIONARIO\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("codigo").value("EMAIL_DUPLICADO"));
        mvc.perform(delete("/api/usuarios/" + id).session(admin).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").with(csrf()).param("email", email).param("senha", "segredo1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminNaoRemoveOProprioAcesso() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        var me = json.readTree(mvc.perform(get("/api/auth/me").session(admin)).andReturn().getResponse().getContentAsString());
        long id = me.get("id").asLong();
        mvc.perform(delete("/api/usuarios/" + id).session(admin).with(csrf())).andExpect(status().isConflict());
        mvc.perform(put("/api/usuarios/" + id).session(admin).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"X\",\"email\":\"teste@example.test\",\"perfil\":\"FUNCIONARIO\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("codigo").value("OPERACAO_NAO_PERMITIDA"));
    }
}
