package br.com.estoqueapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transferencias")
public class Transferencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_origem_id", nullable = false)
    private Loja lojaOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_destino_id", nullable = false)
    private Loja lojaDestino;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    protected Transferencia() {}

    public Transferencia(Produto produto, Loja lojaOrigem, Loja lojaDestino, int quantidade, Instant dataHora, Usuario usuario) {
        this.produto = produto;
        this.lojaOrigem = lojaOrigem;
        this.lojaDestino = lojaDestino;
        this.quantidade = quantidade;
        this.dataHora = dataHora;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public Loja getLojaOrigem() { return lojaOrigem; }
    public Loja getLojaDestino() { return lojaDestino; }
    public int getQuantidade() { return quantidade; }
    public Instant getDataHora() { return dataHora; }
    public Usuario getUsuario() { return usuario; }
}
