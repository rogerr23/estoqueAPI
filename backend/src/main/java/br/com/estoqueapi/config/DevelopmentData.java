package br.com.estoqueapi.config;

import br.com.estoqueapi.entity.*;
import br.com.estoqueapi.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevelopmentData implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final LojaRepository lojas;
    private final ProdutoRepository produtos;
    private final EstoqueRepository estoques;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public DevelopmentData(UsuarioRepository usuarios, LojaRepository lojas,
            ProdutoRepository produtos, EstoqueRepository estoques, PasswordEncoder encoder,
            @Value("${app.demo.email}") String email,
            @Value("${app.demo.password}") String password) {
        this.usuarios = usuarios;
        this.lojas = lojas;
        this.produtos = produtos;
        this.estoques = estoques;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Configure DEMO_EMAIL e DEMO_PASSWORD para o perfil dev");
        }
        usuarios.findByEmail(email).orElseGet(() ->
            usuarios.save(new Usuario("Funcionário de demonstração", email, encoder.encode(password))));
        Loja origem = lojas.findFirstByNomeOrderByIdAsc("Loja A")
            .orElseGet(() -> lojas.save(new Loja("Loja A")));
        Loja destino = lojas.findFirstByNomeOrderByIdAsc("Loja B")
            .orElseGet(() -> lojas.save(new Loja("Loja B")));
        Produto caderno = produtos.findByCodigo("PROD-001")
            .orElseGet(() -> produtos.save(new Produto("PROD-001", "Caderno")));
        Produto caneta = produtos.findByCodigo("PROD-002")
            .orElseGet(() -> produtos.save(new Produto("PROD-002", "Caneta")));
        criarEstoque(origem, caderno, 20);
        criarEstoque(destino, caderno, 5);
        criarEstoque(origem, caneta, 12);
        // Caneta na Loja B permanece sem registro para testar saldo ausente.
    }

    private void criarEstoque(Loja loja, Produto produto, int quantidade) {
        if (estoques.findByLojaIdAndProdutoId(loja.getId(), produto.getId()).isEmpty()) {
            estoques.save(new Estoque(loja, produto, quantidade));
        }
    }
}
