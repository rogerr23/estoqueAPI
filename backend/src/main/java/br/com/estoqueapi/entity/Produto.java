package br.com.estoqueapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "produtos")
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cadastrado_por_id")
    private Usuario cadastradoPor;

    @Column(name = "cadastrado_em")
    private Instant cadastradoEm;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Produto() {}

    public Produto(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    public Produto(String codigo, String nome, Usuario cadastradoPor, Instant cadastradoEm) {
        this(codigo, nome);
        this.cadastradoPor = cadastradoPor;
        this.cadastradoEm = cadastradoEm;
    }

    public void atualizar(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    public void excluir() { this.ativo = false; }
    public boolean isAtivo() { return ativo; }
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public Usuario getCadastradoPor() { return cadastradoPor; }
    public Instant getCadastradoEm() { return cadastradoEm; }
}
