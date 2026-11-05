package br.com.estoqueapi;

import br.com.estoqueapi.dto.ProdutoRequest;
import br.com.estoqueapi.exception.NegocioException;
import br.com.estoqueapi.repository.ProdutoRepository;
import br.com.estoqueapi.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTests {
    @Mock ProdutoRepository repository;
    @InjectMocks ProdutoService service;

    @Test
    void traduzDuplicacaoMesmoQuandoPreConsultaNaoEncontraCodigo() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicado", new SQLException("unique", "23505")));
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest("ABC", "Nome")))
            .isInstanceOfSatisfying(NegocioException.class, erro -> {
                assertThat(erro.getStatus().value()).isEqualTo(409);
                assertThat(erro.getCodigo()).isEqualTo("CODIGO_DUPLICADO");
            });
    }

    @Test
    void naoDisfarcaOutrasFalhasDePersistenciaComoDuplicacao() {
        var falha = new DataIntegrityViolationException("outro erro", new SQLException("check", "23514"));
        when(repository.saveAndFlush(any())).thenThrow(falha);
        assertThatThrownBy(() -> service.cadastrar(new ProdutoRequest("ABC", "Nome"))).isSameAs(falha);
    }
}
