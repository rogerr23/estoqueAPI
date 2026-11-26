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

    protected ProdutoEdicao() {}

    public ProdutoEdicao(Produto produto, Usuario usuario, Instant dataHora, String alteracoes) {
        this.produto = produto;
        this.usuario = usuario;
        this.dataHora = dataHora;
        this.alteracoes = alteracoes;
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public Usuario getUsuario() { return usuario; }
    public Instant getDataHora() { return dataHora; }
    public String getAlteracoes() { return alteracoes; }
}
