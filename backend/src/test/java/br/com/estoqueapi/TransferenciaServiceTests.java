package br.com.estoqueapi;

import br.com.estoqueapi.dto.TransferenciaRequest;
import br.com.estoqueapi.entity.*;
import br.com.estoqueapi.exception.NegocioException;
import br.com.estoqueapi.repository.*;
import br.com.estoqueapi.service.TransferenciaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TransferenciaServiceTests {
    @Mock ProdutoRepository produtos;
    @Mock LojaRepository lojas;
    @Mock EstoqueRepository estoques;
    @Mock UsuarioRepository usuarios;
    @Mock TransferenciaRepository transferencias;
    @InjectMocks TransferenciaService service;
    Produto produto;
    Loja a, b;
    Usuario usuario;

    @BeforeEach
    void entidades() {
        produto = id(new Produto("ABC","Produto"),1L);
        a = id(new Loja("A"),1L); b = id(new Loja("B"),2L);
        usuario = id(new Usuario("Funcionário","teste@example.test","hash"),1L);
    }
    <T> T id(T entidade, Long id) { ReflectionTestUtils.setField(entidade,"id",id); return entidade; }
    TransferenciaRequest entrada(int quantidade) { return new TransferenciaRequest(1L,1L,2L,quantidade); }
    void referencias() {
        when(produtos.buscarParaTransferencia(1L)).thenReturn(Optional.of(produto));
        when(lojas.findById(1L)).thenReturn(Optional.of(a));
        when(lojas.findById(2L)).thenReturn(Optional.of(b));
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
    }
    void gravaHistorico() {
        when(transferencias.saveAndFlush(any())).thenAnswer(call -> id(call.getArgument(0),10L));
    }

    @ParameterizedTest
    @ValueSource(ints = {0,-1})
    void rejeitaQuantidadeInvalidaAntesDeAcessarBanco(int quantidade) {
        assertThatThrownBy(() -> service.transferir(entrada(quantidade),1L))
            .isInstanceOfSatisfying(NegocioException.class,e -> assertThat(e.getStatus().value()).isEqualTo(400));
        verifyNoInteractions(produtos,lojas,estoques,usuarios,transferencias);
    }

    @Test
    void rejeitaLojasIguais() {
        assertThatThrownBy(() -> service.transferir(new TransferenciaRequest(1L,1L,1L,1),1L))
            .isInstanceOfSatisfying(NegocioException.class,e -> assertThat(e.getCodigo()).isEqualTo("LOJAS_IGUAIS"));
        verifyNoInteractions(produtos,lojas,estoques,usuarios,transferencias);
    }

    @Test
    void saldoInsuficienteNaoGravaDestinoNemHistorico() {
        referencias();
        when(estoques.findByLojaIdAndProdutoId(1L,1L)).thenReturn(Optional.of(new Estoque(a,produto,5)));
        assertThatThrownBy(() -> service.transferir(entrada(6),1L))
            .isInstanceOfSatisfying(NegocioException.class,e -> assertThat(e.getCodigo()).isEqualTo("SALDO_INSUFICIENTE"));
        verify(estoques,never()).save(any()); verifyNoInteractions(transferencias);
    }

    @ParameterizedTest
    @ValueSource(ints = {10,20})
    void transfereSaldoParcialOuExatoEGravaAposSaldos(int quantidade) {
        referencias(); gravaHistorico();
        var origem = new Estoque(a,produto,20); var destino = new Estoque(b,produto,5);
        when(estoques.findByLojaIdAndProdutoId(1L,1L)).thenReturn(Optional.of(origem));
        when(estoques.findByLojaIdAndProdutoId(2L,1L)).thenReturn(Optional.of(destino));
        var resposta = service.transferir(entrada(quantidade),1L);
        assertThat(origem.getQuantidade()).isEqualTo(20-quantidade);
        assertThat(destino.getQuantidade()).isEqualTo(5+quantidade);
        assertThat(resposta.usuario().id()).isEqualTo(1L);
        var ordem = inOrder(estoques,transferencias);
        ordem.verify(estoques).save(destino); ordem.verify(estoques).flush();
        ordem.verify(transferencias).saveAndFlush(any());
    }

    @Test
    void destinoAusenteComecaComZero() {
        referencias(); gravaHistorico();
        when(estoques.findByLojaIdAndProdutoId(1L,1L)).thenReturn(Optional.of(new Estoque(a,produto,20)));
        when(estoques.findByLojaIdAndProdutoId(2L,1L)).thenReturn(Optional.empty());
        service.transferir(entrada(3),1L);
        var captor = ArgumentCaptor.forClass(Estoque.class);
        verify(estoques).save(captor.capture());
        assertThat(captor.getValue().getQuantidade()).isEqualTo(3);
        assertThat(captor.getValue().getLoja().getId()).isEqualTo(2L);
    }
}
