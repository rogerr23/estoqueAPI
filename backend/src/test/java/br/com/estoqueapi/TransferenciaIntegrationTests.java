package br.com.estoqueapi;

import br.com.estoqueapi.dto.TransferenciaRequest;
import br.com.estoqueapi.entity.*;
import br.com.estoqueapi.exception.NegocioException;
import br.com.estoqueapi.repository.*;
import br.com.estoqueapi.service.TransferenciaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.demo.email=teste@example.test", "app.demo.password=senha-exclusiva-teste"})
@ActiveProfiles({"dev", "test"})
@AutoConfigureMockMvc
class TransferenciaIntegrationTests {
    @Autowired TransferenciaService service;
    @Autowired ProdutoRepository produtos;
    @Autowired LojaRepository lojas;
    @Autowired EstoqueRepository estoques;
    @Autowired UsuarioRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    Produto produto;
    Loja a, b, c;
    Long usuarioId;

    @BeforeEach
    void preparar() {
        produto = produtos.saveAndFlush(new Produto("CT-P3-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT), "Teste transferência"));
        a = lojas.saveAndFlush(new Loja("CT-P3 A"));
        b = lojas.saveAndFlush(new Loja("CT-P3 B"));
        c = lojas.saveAndFlush(new Loja("CT-P3 C"));
        estoques.saveAndFlush(new Estoque(a, produto, 20));
        estoques.saveAndFlush(new Estoque(b, produto, 5));
        usuarioId = usuarios.findByEmail("teste@example.test").orElseThrow().getId();
    }

    @AfterEach
    void limpar() {
        jdbc.execute("DROP TRIGGER IF EXISTS ct_falha_historico ON transferencias");
        jdbc.execute("DROP FUNCTION IF EXISTS ct_falha_historico()");
        jdbc.update("DELETE FROM transferencias WHERE produto_id = ?", produto.getId());
        jdbc.update("DELETE FROM estoques WHERE produto_id = ?", produto.getId());
        produtos.deleteById(produto.getId());
        lojas.deleteAllById(List.of(a.getId(), b.getId(), c.getId()));
    }

    TransferenciaRequest entrada(Loja origem, Loja destino, int quantidade) {
        return new TransferenciaRequest(produto.getId(), origem.getId(), destino.getId(), quantidade);
    }
    int saldo(Loja loja) {
        return estoques.findByLojaIdAndProdutoId(loja.getId(), produto.getId()).map(Estoque::getQuantidade).orElse(0);
    }
    long historicos() {
        return jdbc.queryForObject("SELECT count(*) FROM transferencias WHERE produto_id = ?", Long.class, produto.getId());
    }
    MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                .param("email", "teste@example.test").param("senha", "senha-exclusiva-teste"))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    void verificarIntacto() {
        assertThat(saldo(a)).isEqualTo(20);
        assertThat(saldo(b)).isEqualTo(5);
        assertThat(historicos()).isZero();
    }

    @Test
    void transferenciaValidaPreservaTotalERegistraResponsavelEHorario() {
        Instant antes = Instant.now();
        var registro = service.transferir(entrada(a, b, 10), usuarioId);
        assertThat(saldo(a)).isEqualTo(10);
        assertThat(saldo(b)).isEqualTo(15);
        assertThat(saldo(a) + saldo(b)).isEqualTo(25);
        assertThat(historicos()).isEqualTo(1);
        assertThat(registro.usuario().id()).isEqualTo(usuarioId);
        assertThat(registro.dataHora()).isBetween(antes, Instant.now());
    }

    @Test
    void permiteConsumirSaldoExato() {
        service.transferir(entrada(a, b, 20), usuarioId);
        assertThat(saldo(a)).isZero();
        assertThat(saldo(b)).isEqualTo(25);
    }

    @Test
    void criaDestinoAusenteEAceitaUmaUnidade() {
        service.transferir(entrada(a, c, 1), usuarioId);
        assertThat(saldo(a)).isEqualTo(19);
        assertThat(saldo(c)).isEqualTo(1);
        assertThat(estoques.findByLojaIdAndProdutoId(c.getId(), produto.getId())).isPresent();
    }

    @Test
    void saldoInsuficienteOuOrigemAusenteNaoAlteraNada() {
        for (var request : List.of(entrada(a, b, 21), entrada(c, b, 1))) {
            assertThatThrownBy(() -> service.transferir(request, usuarioId))
                .isInstanceOfSatisfying(NegocioException.class, e -> assertThat(e.getCodigo()).isEqualTo("SALDO_INSUFICIENTE"));
        }
        verificarIntacto();
        assertThat(estoques.findByLojaIdAndProdutoId(c.getId(), produto.getId())).isEmpty();
    }

    @Test
    void limiteDoInteiroNaoDebitaOrigem() {
        jdbc.update("UPDATE estoques SET quantidade = ? WHERE loja_id = ? AND produto_id = ?", Integer.MAX_VALUE, b.getId(), produto.getId());
        assertThatThrownBy(() -> service.transferir(entrada(a, b, 1), usuarioId))
            .isInstanceOfSatisfying(NegocioException.class, e -> assertThat(e.getCodigo()).isEqualTo("LIMITE_ESTOQUE"));
        assertThat(saldo(a)).isEqualTo(20);
        assertThat(saldo(b)).isEqualTo(Integer.MAX_VALUE);
        assertThat(historicos()).isZero();
    }

    @Test
    void apiValidaQuantidadeLojasERecursosSemAlterarSaldos() throws Exception {
        var sessao = login();
        for (var request : List.of(entrada(a,b,0), entrada(a,b,-1), entrada(a,a,1))) {
            mvc.perform(post("/api/transferencias").session(sessao).with(csrf()).contentType("application/json")
                    .content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
        }
        var invalido = new LinkedHashMap<String,Object>();
        invalido.put("produtoId", produto.getId()); invalido.put("lojaOrigemId", a.getId());
        invalido.put("lojaDestinoId", b.getId()); invalido.put("quantidade", 1.5);
        mvc.perform(post("/api/transferencias").session(sessao).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(invalido))).andExpect(status().isBadRequest());
        invalido.put("quantidade", 1); invalido.put("usuarioId", 999);
        mvc.perform(post("/api/transferencias").session(sessao).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(invalido))).andExpect(status().isBadRequest());
        for (var request : List.of(new TransferenciaRequest(Long.MAX_VALUE,a.getId(),b.getId(),1),
                new TransferenciaRequest(produto.getId(),Long.MAX_VALUE,b.getId(),1),
                new TransferenciaRequest(produto.getId(),a.getId(),Long.MAX_VALUE,1))) {
            mvc.perform(post("/api/transferencias").session(sessao).with(csrf()).contentType("application/json")
                    .content(json.writeValueAsString(request))).andExpect(status().isNotFound());
        }
        mvc.perform(post("/api/transferencias").session(sessao).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(entrada(a,b,21))))
            .andExpect(status().isConflict()).andExpect(jsonPath("codigo").value("SALDO_INSUFICIENTE"));
        mvc.perform(post("/api/transferencias").session(sessao).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("campos").isMap());
        verificarIntacto();
    }

    @Test
    void apiExigeSessaoECsrfEDevolveHistoricoSeguroOrdenado() throws Exception {
        mvc.perform(get("/api/transferencias")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/transferencias").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(entrada(a,b,1)))).andExpect(status().isUnauthorized());
        var sessao = login();
        mvc.perform(post("/api/transferencias").session(sessao).contentType("application/json")
                .content(json.writeValueAsString(entrada(a,b,1)))).andExpect(status().isForbidden());
        verificarIntacto();
        mvc.perform(post("/api/transferencias").session(sessao).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(entrada(a,b,10))))
            .andExpect(status().isCreated()).andExpect(jsonPath("quantidade").value(10))
            .andExpect(jsonPath("usuario.id").value(usuarioId))
            .andExpect(jsonPath("usuario.senhaHash").doesNotExist()).andExpect(jsonPath("dataHora").exists());
        var segundo = service.transferir(entrada(b,a,1),usuarioId);
        mvc.perform(get("/api/transferencias").session(sessao)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(segundo.id())).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].usuario.senhaHash").doesNotExist());
        // Datas empatadas têm desempate determinístico pelo ID.
        jdbc.update("UPDATE transferencias SET data_hora = TIMESTAMPTZ '2026-01-01 00:00:00+00' WHERE produto_id = ?",produto.getId());
        assertThat(service.historico().getFirst().id()).isEqualTo(segundo.id());
    }

    @Test
    void falhaNoHistoricoAposDebitoRealDesfazSaldosENovoDestino() throws Exception {
        jdbc.execute("""
            CREATE FUNCTION ct_falha_historico() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
              IF (SELECT quantidade FROM estoques WHERE loja_id = NEW.loja_origem_id AND produto_id = NEW.produto_id) = 10 THEN
                RAISE EXCEPTION 'CT_DEBITO_CONFIRMADO';
              END IF;
              RETURN NEW;
            END $$
            """);
        jdbc.execute("CREATE TRIGGER ct_falha_historico BEFORE INSERT ON transferencias FOR EACH ROW EXECUTE FUNCTION ct_falha_historico()");
        assertThatThrownBy(() -> service.transferir(entrada(a,c,10),usuarioId))
            .hasStackTraceContaining("CT_DEBITO_CONFIRMADO");
        verificarIntacto();
        assertThat(estoques.findByLojaIdAndProdutoId(c.getId(),produto.getId())).isEmpty();
        mvc.perform(post("/api/transferencias").session(login()).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(entrada(a,b,10))))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("codigo").value("ERRO_INTERNO"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("CT_DEBITO_CONFIRMADO"))));
        verificarIntacto();
    }

    List<Integer> executarSimultaneamente(List<TransferenciaRequest> requests) throws Exception {
        var pronto = new CountDownLatch(requests.size());
        var inicio = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(requests.size());
        try {
            var futures = new ArrayList<Future<Integer>>();
            for (var request : requests) futures.add(pool.submit(() -> {
                pronto.countDown();
                if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout na largada");
                try { service.transferir(request,usuarioId); return 201; }
                catch (NegocioException e) { return e.getStatus().value(); }
            }));
            assertThat(pronto.await(10,TimeUnit.SECONDS)).isTrue();
            inicio.countDown();
            var resultados = new ArrayList<Integer>();
            for (var future : futures) resultados.add(future.get(20,TimeUnit.SECONDS));
            return resultados;
        } finally {
            inicio.countDown(); pool.shutdownNow();
            assertThat(pool.awaitTermination(10,TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void concorrenciaNaoPermiteGastarMesmoSaldoDuasVezes() throws Exception {
        jdbc.update("UPDATE estoques SET quantidade = 10 WHERE loja_id = ? AND produto_id = ?",a.getId(),produto.getId());
        assertThat(executarSimultaneamente(List.of(entrada(a,b,8),entrada(a,b,8)))).containsExactlyInAnyOrder(201,409);
        assertThat(saldo(a)).isEqualTo(2); assertThat(saldo(b)).isEqualTo(13);
        assertThat(historicos()).isEqualTo(1);
    }

    @Test
    void concorrenciaCriaUmUnicoDestinoSemPerderCreditos() throws Exception {
        assertThat(executarSimultaneamente(List.of(entrada(a,c,5),entrada(b,c,5)))).containsOnly(201).hasSize(2);
        assertThat(saldo(a)).isEqualTo(15); assertThat(saldo(b)).isZero(); assertThat(saldo(c)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM estoques WHERE loja_id = ? AND produto_id = ?",Long.class,c.getId(),produto.getId())).isEqualTo(1);
        assertThat(historicos()).isEqualTo(2);
    }

    @Test
    void transferenciasEmSentidosOpostosNaoTravemNemPercamAtualizacoes() throws Exception {
        assertThat(executarSimultaneamente(List.of(entrada(a,b,3),entrada(b,a,2)))).containsOnly(201).hasSize(2);
        assertThat(saldo(a)).isEqualTo(19); assertThat(saldo(b)).isEqualTo(6);
        assertThat(historicos()).isEqualTo(2);
    }

    @Test
    void openApiDocumentaTransferenciaHistoricoECsrf() throws Exception {
        mvc.perform(get("/v3/api-docs").session(login())).andExpect(status().isOk())
            .andExpect(jsonPath("paths['/api/transferencias'].post.responses['409']").exists())
            .andExpect(jsonPath("paths['/api/transferencias'].get").exists())
            .andExpect(jsonPath("components.schemas.TransferenciaRequest.properties.quantidade.type").value("integer"));
    }
}
