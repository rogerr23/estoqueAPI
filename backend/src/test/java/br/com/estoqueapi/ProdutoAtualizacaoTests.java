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
class ProdutoAtualizacaoTests extends DatabaseTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LojaRepository lojas;

    private MockHttpSession login(String email, String senha) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf()).param("email", email).param("senha", senha))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private String codigo() { return "CT-AT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }

    private long criarProduto(MockHttpSession s, String codigo) throws Exception {
        var r = mvc.perform(post("/api/produtos").session(s).with(csrf()).contentType("application/json")
            .content("{\"codigo\":\"" + codigo + "\",\"nome\":\"Original\"}")).andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void adminAtualizaCodigoNomeEQuantidades() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        long id = criarProduto(admin, codigo());
        long a = lojas.findFirstByNomeOrderByIdAsc("Loja A").orElseThrow().getId();
        long b = lojas.findFirstByNomeOrderByIdAsc("Loja B").orElseThrow().getId();
        String novo = codigo();
        mvc.perform(put("/api/produtos/" + id).session(admin).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\" " + novo.toLowerCase() + " \",\"nome\":\"Novo nome\",\"estoques\":[{\"lojaId\":" + a + ",\"quantidade\":30},{\"lojaId\":" + b + ",\"quantidade\":0}]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("codigo").value(novo)).andExpect(jsonPath("nome").value("Novo nome"));
        mvc.perform(get("/api/lojas/" + a + "/estoque").session(admin))
            .andExpect(jsonPath("$[?(@.produtoId==" + id + ")].quantidade").value(30));
        mvc.perform(put("/api/produtos/" + id).session(admin).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"" + novo + "\",\"nome\":\"X\",\"estoques\":[{\"lojaId\":" + a + ",\"quantidade\":-1}]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void edicaoFicaRegistradaVisivelATodosENasAtividadesDoAdmin() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        var me = json.readTree(mvc.perform(get("/api/auth/me").session(admin)).andReturn().getResponse().getContentAsString());
        String original = codigo();
        long id = criarProduto(admin, original);
        long a = lojas.findFirstByNomeOrderByIdAsc("Loja A").orElseThrow().getId();
        mvc.perform(put("/api/produtos/" + id).session(admin).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"" + original + "\",\"nome\":\"Renomeado\",\"estoques\":[{\"lojaId\":" + a + ",\"quantidade\":7}]}"))
            .andExpect(status().isOk());

        String email = "leitor-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/usuarios").session(admin).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Leitor\",\"email\":\"" + email + "\",\"senha\":\"segredo1\",\"perfil\":\"FUNCIONARIO\"}"))
            .andExpect(status().isCreated());
        var func = login(email, "segredo1");
        mvc.perform(get("/api/produtos/edicoes").session(func)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].produto.id").value(id))
            .andExpect(jsonPath("$[0].usuario.nome").value("Funcionário de demonstração"))
            .andExpect(jsonPath("$[0].alteracoes").value("Nome: Original → Renomeado; Quantidade Loja A: 0 → 7"));
        mvc.perform(get("/api/usuarios/" + me.get("id").asLong() + "/atividades").session(admin))
            .andExpect(jsonPath("$[0].tipo").value("EDICAO_PRODUTO"));
        // Funcionário vê só as edições, não as atividades dos outros.
        mvc.perform(get("/api/usuarios/" + me.get("id").asLong() + "/atividades").session(func)).andExpect(status().isForbidden());
    }

    @Test
    void codigoDuplicadoEhRejeitado() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        String existente = codigo();
        criarProduto(admin, existente);
        long id = criarProduto(admin, codigo());
        mvc.perform(put("/api/produtos/" + id).session(admin).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"" + existente + "\",\"nome\":\"X\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("codigo").value("CODIGO_DUPLICADO"));
    }

    @Test
    void funcionarioNaoAtualizaProduto() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        long id = criarProduto(admin, codigo());
        String email = "func-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/usuarios").session(admin).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"Func\",\"email\":\"" + email + "\",\"senha\":\"segredo1\",\"perfil\":\"FUNCIONARIO\"}"))
            .andExpect(status().isCreated());
        var func = login(email, "segredo1");
        mvc.perform(put("/api/produtos/" + id).session(func).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"" + codigo() + "\",\"nome\":\"Hack\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("codigo").value("ACESSO_NEGADO"));
    }
}
