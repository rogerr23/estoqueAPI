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

import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.demo.email=teste@example.test"})
@ActiveProfiles({"dev", "test"})
@AutoConfigureMockMvc
@Transactional
class ExclusaoAdminTests extends DatabaseTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LojaRepository lojas;

    private MockHttpSession login(String email, String senha) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf()).param("email", email).param("senha", senha))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private long id(org.springframework.test.web.servlet.MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private long loja(String nome) { return lojas.findFirstByNomeOrderByIdAsc(nome).orElseThrow().getId(); }

    private long produtoComEstoque(MockHttpSession s, String codigo, int qtd) throws Exception {
        return id(mvc.perform(post("/api/produtos").session(s).with(csrf()).contentType("application/json")
            .content("{\"codigo\":\"" + codigo + "\",\"nome\":\"Item\",\"lojaId\":" + loja("Loja A") + ",\"quantidade\":" + qtd + "}"))
            .andExpect(status().isCreated()).andReturn());
    }

    private long transferir(MockHttpSession s, long produto, long origem, long destino, int qtd) throws Exception {
        return id(mvc.perform(post("/api/transferencias").session(s).with(csrf()).contentType("application/json")
            .content("{\"produtoId\":" + produto + ",\"lojaOrigemId\":" + origem + ",\"lojaDestinoId\":" + destino + ",\"quantidade\":" + qtd + "}"))
            .andExpect(status().isCreated()).andReturn());
    }

    private String codigo() { return "CT-EX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }

    @Test
    void excluirTransferenciaDevolveEstoqueERegistra() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        long a = loja("Loja A"), b = loja("Loja B");
        long produto = produtoComEstoque(admin, codigo(), 10);
        long t = transferir(admin, produto, a, b, 4);
        mvc.perform(delete("/api/transferencias/" + t).session(admin).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/lojas/" + a + "/estoque").session(admin)).andExpect(jsonPath("$[?(@.produtoId==" + produto + ")].quantidade").value(10));
        mvc.perform(get("/api/lojas/" + b + "/estoque").session(admin)).andExpect(jsonPath("$[?(@.produtoId==" + produto + ")].quantidade").value(0));
        mvc.perform(get("/api/transferencias").session(admin)).andExpect(jsonPath("$[?(@.id==" + t + ")]").value(empty()));
        mvc.perform(get("/api/produtos/edicoes").session(admin)).andExpect(jsonPath("$[0].acao").value("EXCLUSAO_TRANSFERENCIA"));
        mvc.perform(delete("/api/transferencias/" + t).session(admin).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void naoExcluiTransferenciaSeDestinoJaRepassouAsUnidades() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        long a = loja("Loja A"), b = loja("Loja B");
        long produto = produtoComEstoque(admin, codigo(), 10);
        long t = transferir(admin, produto, a, b, 5);
        transferir(admin, produto, b, a, 3);
        mvc.perform(delete("/api/transferencias/" + t).session(admin).with(csrf()))
            .andExpect(status().isConflict()).andExpect(jsonPath("codigo").value("SALDO_INSUFICIENTE"));
    }

    @Test
    void excluirProdutoTiraDasListasMantemHistoricoEBloqueiaTransferencia() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        long a = loja("Loja A"), b = loja("Loja B");
        String codigo = codigo();
        long produto = produtoComEstoque(admin, codigo, 10);
        long t = transferir(admin, produto, a, b, 2);
        mvc.perform(delete("/api/produtos/" + produto).session(admin).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/produtos").session(admin)).andExpect(jsonPath("$[?(@.id==" + produto + ")]").value(empty()));
        mvc.perform(get("/api/lojas/" + a + "/estoque").session(admin)).andExpect(jsonPath("$[?(@.produtoId==" + produto + ")]").value(empty()));
        mvc.perform(get("/api/produtos/" + produto).session(admin)).andExpect(status().isNotFound());
        mvc.perform(get("/api/transferencias").session(admin)).andExpect(jsonPath("$[?(@.id==" + t + ")].produto.codigo").value(codigo));
        mvc.perform(post("/api/transferencias").session(admin).with(csrf()).contentType("application/json")
                .content("{\"produtoId\":" + produto + ",\"lojaOrigemId\":" + a + ",\"lojaDestinoId\":" + b + ",\"quantidade\":1}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/produtos/edicoes").session(admin)).andExpect(jsonPath("$[0].acao").value("EXCLUSAO_PRODUTO"));
        mvc.perform(post("/api/produtos").session(admin).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\"" + codigo + "\",\"nome\":\"De novo\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void funcionarioNaoExclui() throws Exception {
        var admin = login("teste@example.test", PASSWORD);
        long produto = produtoComEstoque(admin, codigo(), 10);
        long t = transferir(admin, produto, loja("Loja A"), loja("Loja B"), 1);
        String email = "f-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/usuarios").session(admin).with(csrf()).contentType("application/json")
                .content("{\"nome\":\"F\",\"email\":\"" + email + "\",\"senha\":\"segredo1\",\"perfil\":\"FUNCIONARIO\"}"))
            .andExpect(status().isCreated());
        var func = login(email, "segredo1");
        mvc.perform(delete("/api/produtos/" + produto).session(func).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/transferencias/" + t).session(func).with(csrf())).andExpect(status().isForbidden());
    }
}
