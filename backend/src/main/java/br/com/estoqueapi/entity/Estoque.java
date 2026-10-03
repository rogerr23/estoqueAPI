package br.com.estoqueapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "estoques", uniqueConstraints = @UniqueConstraint(name = "uk_estoque_loja_produto", columnNames = {"loja_id", "produto_id"}))
public class Estoque {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    protected Estoque() {}

    public Estoque(Loja loja, Produto produto, int quantidade) {
        this.loja = loja;
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public void debitar(int unidades) {
        if (unidades <= 0 || unidades > quantidade) throw new IllegalArgumentException("Débito inválido");
        quantidade -= unidades;
    }

    public void creditar(int unidades) {
        if (unidades <= 0) throw new IllegalArgumentException("Crédito inválido");
        quantidade = Math.addExact(quantidade, unidades);
    }

    public Long getId() { return id; }
    public Loja getLoja() { return loja; }
    public Produto getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }
}
