package br.com.estoqueapi;

import br.com.estoqueapi.config.DevelopmentData;
import br.com.estoqueapi.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "app.demo.email=teste@example.test",
    "app.demo.password=senha-exclusiva-teste"
})
@ActiveProfiles({"dev", "test"})
class EstoqueApiApplicationTests {
    @Autowired UsuarioRepository usuarios;
    @Autowired LojaRepository lojas;
    @Autowired ProdutoRepository produtos;
    @Autowired EstoqueRepository estoques;
    @Autowired TransferenciaRepository transferencias;
    @Autowired DevelopmentData seed;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @Test
    void carregaDadosConhecidosComSenhaProtegida() {
        assertThat(usuarios.count()).isEqualTo(1);
        assertThat(lojas.count()).isEqualTo(2);
        assertThat(produtos.count()).isEqualTo(2);
        assertThat(estoques.count()).isEqualTo(3);
        assertThat(transferencias.count()).isZero();
        var usuario = usuarios.findByEmail("teste@example.test").orElseThrow();
        assertThat(usuario.getSenhaHash()).isNotEqualTo("senha-exclusiva-teste");
        assertThat(encoder.matches("senha-exclusiva-teste", usuario.getSenhaHash())).isTrue();
        var caderno = produtos.findByCodigo("PROD-001").orElseThrow();
        var a = lojas.findFirstByNomeOrderByIdAsc("Loja A").orElseThrow();
        var b = lojas.findFirstByNomeOrderByIdAsc("Loja B").orElseThrow();
        assertThat(estoques.findByLojaIdAndProdutoId(a.getId(), caderno.getId()).orElseThrow().getQuantidade()).isEqualTo(20);
        assertThat(estoques.findByLojaIdAndProdutoId(b.getId(), caderno.getId()).orElseThrow().getQuantidade()).isEqualTo(5);
        var caneta = produtos.findByCodigo("PROD-002").orElseThrow();
        assertThat(estoques.findByLojaIdAndProdutoId(b.getId(), caneta.getId())).isEmpty();
    }

    @Test
    void repetirCargaNaoDuplicaDadosNemReiniciaSaldo() {
        var estoque = estoques.findAll().getFirst();
        int anterior = estoque.getQuantidade();
        try {
            jdbc.update("UPDATE estoques SET quantidade = 3 WHERE id = ?", estoque.getId());
            seed.run();
            assertThat(usuarios.count()).isEqualTo(1);
            assertThat(lojas.count()).isEqualTo(2);
            assertThat(produtos.count()).isEqualTo(2);
            assertThat(estoques.count()).isEqualTo(3);
            assertThat(estoques.findById(estoque.getId()).orElseThrow().getQuantidade()).isEqualTo(3);
        } finally {
            jdbc.update("UPDATE estoques SET quantidade = ? WHERE id = ?", anterior, estoque.getId());
        }
    }

    @Test
    void bancoRejeitaCodigoDuplicadoENaoNormalizado() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO produtos(codigo,nome) VALUES ('PROD-001','Outro')"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO produtos(codigo,nome) VALUES (' codigo ','Outro')"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRejeitaSaldoNegativoEEstoqueDuplicado() {
        var estoque = estoques.findAll().getFirst();
        assertThatThrownBy(() -> jdbc.update("UPDATE estoques SET quantidade = -1 WHERE id = ?", estoque.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO estoques(loja_id,produto_id,quantidade) SELECT loja_id,produto_id,0 FROM estoques WHERE id = ?", estoque.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRejeitaReferenciaInexistente() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO estoques(loja_id,produto_id,quantidade) VALUES (-1,-1,1)"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRejeitaTransferenciaComMesmaLojaOuQuantidadeZero() {
        var a = lojas.findFirstByNomeOrderByIdAsc("Loja A").orElseThrow();
        var b = lojas.findFirstByNomeOrderByIdAsc("Loja B").orElseThrow();
        var produto = produtos.findByCodigo("PROD-001").orElseThrow();
        var usuario = usuarios.findByEmail("teste@example.test").orElseThrow();
        String sql = "INSERT INTO transferencias(produto_id,loja_origem_id,loja_destino_id,quantidade,data_hora,usuario_id) VALUES (?,?,?,?,CURRENT_TIMESTAMP,?)";
        assertThatThrownBy(() -> jdbc.update(sql, produto.getId(), a.getId(), a.getId(), 1, usuario.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(sql, produto.getId(), a.getId(), b.getId(), 0, usuario.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(transferencias.count()).isZero();
    }
}
