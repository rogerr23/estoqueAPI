package br.com.estoqueapi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil = Perfil.FUNCIONARIO;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Usuario() {}

    public Usuario(String nome, String email, String senhaHash) {
        this(nome, email, senhaHash, Perfil.FUNCIONARIO);
    }

    public Usuario(String nome, String email, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    public void atualizar(String nome, String email, Perfil perfil, boolean ativo) {
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    public void alterarSenha(String senhaHash) { this.senhaHash = senhaHash; }
    public void desativar() { this.ativo = false; }
    public void tornarAdmin() { this.perfil = Perfil.ADMIN; }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getSenhaHash() { return senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public boolean isAtivo() { return ativo; }
}
