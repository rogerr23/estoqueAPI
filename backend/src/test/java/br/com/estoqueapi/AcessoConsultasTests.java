package br.com.estoqueapi;

import br.com.estoqueapi.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.demo.email=teste@example.test", "app.demo.password=senha-exclusiva-teste"})
@ActiveProfiles({"dev", "test"})
@AutoConfigureMockMvc
@Transactional
class AcessoConsultasTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProdutoRepository produtos;
    @Autowired LojaRepository lojas;
    @Autowired EstoqueRepository estoques;

    private MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                .param("email", "teste@example.test").param("senha", "senha-exclusiva-teste"))
            .andExpect(status().isOk()).andExpect(jsonPath("email").value("teste@example.test"))
            .andExpect(jsonPath("senhaHash").doesNotExist()).andReturn().getRequest().getSession(false);
    }

    @Test
    void sessaoCsrfFixationERenovacaoLogoutFuncionam() throws Exception {
        var inicial = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var sessao = (MockHttpSession) inicial.getRequest().getSession(false);
        var token = json.readTree(inicial.getResponse().getContentAsString()).get("token").asText();
        String idAnterior = sessao.getId();
        mvc.perform(post("/api/auth/login").session(sessao).header("X-CSRF-TOKEN", token)
                .param("email", "teste@example.test").param("senha", "senha-exclusiva-teste"))
            .andExpect(status().isOk());
        assertThat(sessao.getId()).isNotEqualTo(idAnterior);
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isOk())
            .andExpect(jsonPath("nome").value("Funcionário de demonstração"))
            .andExpect(jsonPath("senhaHash").doesNotExist());
        mvc.perform(post("/api/produtos").session(sessao).header("X-CSRF-TOKEN", token)
                .contentType("application/json").content("{\"codigo\":\"CT-ANTIGO\",\"nome\":\"Teste\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("codigo").value("CSRF_INVALIDO"));
        var renovado = mvc.perform(get("/api/auth/csrf").session(sessao)).andExpect(status().isOk()).andReturn();
        String novo = json.readTree(renovado.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/auth/logout").session(sessao).header("X-CSRF-TOKEN", novo))
            .andExpect(status().isNoContent());
        assertThat(sessao.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk());
    }

    @Test
    void rejeitaCredenciaisInvalidasSemRedirecionamento() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).param("email", "teste@example.test").param("senha", "errada"))
            .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Location"))
            .andExpect(jsonPath("codigo").value("CREDENCIAIS_INVALIDAS"));
        mvc.perform(post("/api/auth/login").with(csrf()).param("email", "ausente@example.test").param("senha", "errada"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void exigeAutenticacaoNasConsultasEDocumentacao() throws Exception {
        for (String rota : new String[]{"/api/auth/me", "/api/produtos", "/api/produtos/1", "/api/lojas", "/api/lojas/1/estoque", "/v3/api-docs", "/swagger-ui/index.html"}) {
            mvc.perform(get(rota)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("codigo").value("NAO_AUTENTICADO"));
        }
        mvc.perform(post("/api/produtos").with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfProtegeLoginCadastroELogout() throws Exception {
        mvc.perform(post("/api/auth/login").param("email", "teste@example.test").param("senha", "senha-exclusiva-teste"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("codigo").value("CSRF_INVALIDO"));
        var sessao = login();
        long antes = produtos.count();
        mvc.perform(post("/api/produtos").session(sessao).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/produtos").session(sessao).with(csrf().useInvalidToken())
                .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        assertThat(produtos.count()).isEqualTo(antes);
        mvc.perform(post("/api/auth/logout").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(sessao)).andExpect(status().isOk());
    }

    @Test
    void cadastraNormalizaEConsultaProduto() throws Exception {
        var sessao = login();
        var resultado = mvc.perform(post("/api/produtos").session(sessao).with(csrf())
                .contentType("application/json").content("{\"codigo\":\" ct-001 \",\"nome\":\" Produto teste \"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("codigo").value("CT-001"))
            .andExpect(jsonPath("nome").value("Produto teste")).andReturn();
        long id = json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
        assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/api/produtos/" + id);
        mvc.perform(get("/api/produtos/{id}", id).session(sessao)).andExpect(status().isOk())
            .andExpect(jsonPath("codigo").value("CT-001"));
        mvc.perform(get("/api/produtos").session(sessao)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        assertThat(produtos.findByCodigo("CT-001")).isPresent();
    }

    @Test
    void rejeitaCodigoDuplicadoNormalizado() throws Exception {
        mvc.perform(post("/api/produtos").session(login()).with(csrf()).contentType("application/json")
                .content("{\"codigo\":\" prod-001 \",\"nome\":\"Outro\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("codigo").value("CODIGO_DUPLICADO"));
        assertThat(produtos.count()).isEqualTo(2);
    }

    @Test
    void validaCamposLimitesEJson() throws Exception {
        var sessao = login();
        for (String entrada : new String[]{"{}", "{\"codigo\":\" \",\"nome\":\" \"}",
                json.writeValueAsString(java.util.Map.of("codigo", "X".repeat(61), "nome", "Nome")),
                json.writeValueAsString(java.util.Map.of("codigo", "CT-LIMITE", "nome", "X".repeat(151)))}) {
            mvc.perform(post("/api/produtos").session(sessao).with(csrf()).contentType("application/json").content(entrada))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("campos").isMap());
        }
        mvc.perform(post("/api/produtos").session(sessao).with(csrf()).contentType("application/json").content("{"))
            .andExpect(status().isBadRequest());
        assertThat(produtos.count()).isEqualTo(2);
    }

    @Test
    void listaLojasEIncluiEstoqueZeroSemCriarRegistro() throws Exception {
        var sessao = login();
        var b = lojas.findFirstByNomeOrderByIdAsc("Loja B").orElseThrow();
        long antes = estoques.count();
        mvc.perform(get("/api/lojas").session(sessao)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/lojas/{id}/estoque", b.getId()).session(sessao)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].quantidade").value(5))
            .andExpect(jsonPath("$[1].codigo").value("PROD-002")).andExpect(jsonPath("$[1].quantidade").value(0));
        assertThat(estoques.count()).isEqualTo(antes);
    }

    @Test
    void recursoInexistenteEIdInvalidoRetornamErrosClaros() throws Exception {
        var sessao = login();
        mvc.perform(get("/api/produtos/-1").session(sessao)).andExpect(status().isNotFound())
            .andExpect(jsonPath("codigo").value("PRODUTO_NAO_ENCONTRADO"));
        mvc.perform(get("/api/lojas/-1/estoque").session(sessao)).andExpect(status().isNotFound())
            .andExpect(jsonPath("codigo").value("LOJA_NAO_ENCONTRADA"));
        mvc.perform(get("/api/produtos/abc").session(sessao)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/inexistente").session(sessao)).andExpect(status().isNotFound());
    }

    @Test
    void openApiIncluiEndpointsEAutenticacao() throws Exception {
        mvc.perform(get("/v3/api-docs").session(login())).andExpect(status().isOk())
            .andExpect(jsonPath("paths['/api/auth/login'].post").exists())
            .andExpect(jsonPath("paths['/api/auth/logout'].post").exists())
            .andExpect(jsonPath("paths['/api/produtos'].post").exists())
            .andExpect(jsonPath("paths['/api/lojas/{id}/estoque'].get").exists())
            .andExpect(jsonPath("components.securitySchemes.sessao.name").value("JSESSIONID"))
            .andExpect(jsonPath("servers[0].url").value("/"));
    }

    @Test
    void aceitaTamanhosMaximosPermitidos() throws Exception {
        mvc.perform(post("/api/produtos").session(login()).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("codigo", "X".repeat(60), "nome", "N".repeat(150)))))
            .andExpect(status().isCreated());
    }

    @Test
    void logoutAnonimoELoginHtmlNaoSaoPublicos() throws Exception {
        mvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/login")).andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Location"));
    }
}
