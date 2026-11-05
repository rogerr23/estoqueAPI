package br.com.estoqueapi;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.Set;
import java.util.UUID;

/** Configuração compartilhada somente pelos testes que acessam PostgreSQL. */
abstract class DatabaseTestSupport {
    static final String PASSWORD = UUID.randomUUID().toString();

    @DynamicPropertySource
    static void configurarBancoExclusivo(DynamicPropertyRegistry registry) {
        String url = System.getenv().getOrDefault("TEST_DATABASE_URL",
            "jdbc:postgresql://localhost:5434/estoque_test");
        if (!Set.of("jdbc:postgresql://localhost:5434/estoque_test",
                "jdbc:postgresql://127.0.0.1:5434/estoque_test").contains(url)) {
            throw new IllegalStateException("A suíte exige estoque_test local na porta 5434");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "estoque_test");
        registry.add("spring.datasource.password", () -> System.getenv("TEST_DATABASE_PASSWORD"));
        registry.add("app.demo.password", () -> PASSWORD);
    }
}
