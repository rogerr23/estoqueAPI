package br.com.estoqueapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Mercadoria que chegou a uma loja, registrada por um funcionário. */
@Entity
@Table(name = "entradas")
public class Entrada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @Column(nullable = false)
    private int quantidade;

    @Column(length = 200)
    private String observacao;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    protected Entrada() {}

    public Entrada(Produto produto, Loja loja, int quantidade, String observacao, Instant dataHora, Usuario usuario) {
        this.produto = produto;
        this.loja = loja;
        this.quantidade = quantidade;
        this.observacao = observacao;
        this.dataHora = dataHora;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public Loja getLoja() { return loja; }
    public int getQuantidade() { return quantidade; }
    public String getObservacao() { return observacao; }
    public Instant getDataHora() { return dataHora; }
    public Usuario getUsuario() { return usuario; }
}
