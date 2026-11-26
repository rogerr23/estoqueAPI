package br.com.estoqueapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Uma edição de produto: quem fez, quando e o que mudou (texto "antes → depois"). */
@Entity
@Table(name = "produto_edicoes")
public class ProdutoEdicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @Column(nullable = false, length = 2000)
    private String alteracoes;

    /** EDICAO, EXCLUSAO_PRODUTO ou EXCLUSAO_TRANSFERENCIA. */
    @Column(nullable = false, length = 30)
    private String acao = "EDICAO";

    protected ProdutoEdicao() {}

    public ProdutoEdicao(Produto produto, Usuario usuario, Instant dataHora, String alteracoes) {
        this.produto = produto;
        this.usuario = usuario;
        this.dataHora = dataHora;
        this.alteracoes = alteracoes;
    }

    public ProdutoEdicao(Produto produto, Usuario usuario, Instant dataHora, String alteracoes, String acao) {
        this(produto, usuario, dataHora, alteracoes);
        this.acao = acao;
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public Usuario getUsuario() { return usuario; }
    public Instant getDataHora() { return dataHora; }
    public String getAlteracoes() { return alteracoes; }
    public String getAcao() { return acao; }
}
