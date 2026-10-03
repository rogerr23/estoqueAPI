# EstoqueAPI

Sistema acadêmico para consultar estoque por loja e transferir produtos entre lojas.
A especificação está em [EstoqueAPI.md](EstoqueAPI.md).

## Estado atual

Fase 1: base executável com backend, frontend, PostgreSQL, migração inicial,
entidades, repositories e dados de demonstração. Login, endpoints de negócio,
Swagger e telas funcionais entram nas próximas fases. Por enquanto o backend
bloqueia as requisições; a página inicial do frontend apresenta o projeto.

## Requisitos

- Java 21.
- Node.js 22.14 ou superior compatível com Vite e npm.
- Docker com Docker Compose e daemon iniciado.
- Git para versionamento.

O Maven Wrapper está incluído; não é necessário instalar Maven.
Backend: Spring Boot 3.5.16, JPA, Spring Security, Flyway e PostgreSQL.
Frontend: React e Vite; versões resolvidas em `frontend/package-lock.json`.

## Configuração local

Na raiz, copie o exemplo e defina senhas locais:

```sh
cp .env.example .env
```

Não sobrescreva um `.env` já configurado. O arquivo `.env` fica fora do Git.
Nesta primeira configuração já foi criado um `.env` local com senhas aleatórias.
O backend carrega esse arquivo ao executar a partir da raiz ou de `backend/`.
As variáveis do ambiente têm precedência.

## Trabalhar no IntelliJ IDEA

1. Abra a pasta `estoqueAPI`.
2. Abra `backend/pom.xml` e use **Add as Maven Project** se ainda não estiver vinculado.
3. Selecione JDK 21 para o projeto e para o importador/runner do Maven.
4. Inicie o banco pelo terminal integrado, na raiz:

```sh
docker compose up -d --wait
```

5. Pelo terminal integrado, inicie o backend:

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A configuração compartilhada **EstoqueAPI dev** em `.run/` já define o perfil e
o diretório de execução. Selecione-a na IDE para executar ou depurar.
Também pode executar `EstoqueApiApplication` pela IDE, usando `backend/` como
working directory e `--spring.profiles.active=dev` como argumento do programa.
O servidor escuta em `http://127.0.0.1:8080`.

6. Em outro terminal integrado, a partir da raiz:

```sh
cd frontend
npm ci
npm run dev
```

Acesse `http://127.0.0.1:5173`. O Vite encaminha `/api` ao backend.

## Dados de demonstração

O perfil `dev` cria os dados em uma transação, somente quando ausentes.
Reiniciar o backend não reinicia saldos nem duplica registros.

| Produto | Loja A | Loja B |
| --- | ---: | ---: |
| PROD-001 — Caderno | 20 | 5 |
| PROD-002 — Caneta | 12 | Sem registro (saldo zero na futura consulta) |

O funcionário usa `DEMO_EMAIL` e `DEMO_PASSWORD` do `.env`. A senha é gravada com
BCrypt. Alterar a variável não altera a senha de um usuário já criado.
A carga não roda fora do perfil `dev`.

### Restaurar os dados de demonstração

A sequência abaixo **apaga todo o banco local deste projeto**, incluindo futuros
produtos e transferências. Use somente quando quiser recomeçar a demonstração.
Pare o backend primeiro:

```sh
docker compose down -v
docker compose up -d --wait
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O Flyway recria as tabelas e a carga restaura os dados iniciais.

## Testes e verificações

Os testes usam PostgreSQL exclusivo, na porta 5434, com armazenamento temporário.
Crie `.env.test` na raiz com `TEST_DATABASE_PASSWORD=uma-senha-local-de-teste`.
Esse arquivo também fica fora do Git. Nesta primeira configuração ele já foi criado.

Na raiz, em um terminal zsh/bash:

```sh
set -a
source .env.test
set +a
docker compose -p estoqueapi-test -f compose.test.yaml up -d --wait
cd backend
./mvnw verify
```

Para repetir a suíte desde um banco limpo, pare o banco de testes antes de iniciá-lo
novamente. Os testes pressupõem a base exclusiva com apenas os dados de teste:

```sh
# Execute na raiz do projeto.
docker compose -p estoqueapi-test -f compose.test.yaml down
```

Esse comando remove o contêiner de testes e seus dados temporários. O volume de
desenvolvimento permanece intacto. Os relatórios ficam em
`backend/target/surefire-reports/`.

No frontend:

```sh
cd frontend
npm run lint
npm run build
```

Consulte [docs/plano-de-testes.md](docs/plano-de-testes.md) para rastreabilidade
e [docs/evidencias/fase-1.md](docs/evidencias/fase-1.md) para o resultado deste marco.

## Organização

- `backend/`: aplicação Java, entidades, repositories, configuração e testes.
- `backend/src/main/resources/db/migration/`: esquema versionado com Flyway.
- `frontend/`: aplicação React e proxy de desenvolvimento.
- `docs/`: plano de testes e evidências.
- `postman/`: reservado para a coleção das próximas fases.

## Próximo marco

Fase 2: autenticação com sessão e CSRF; cadastro e consultas de produtos;
listagem de lojas e estoque; erros padronizados; OpenAPI e testes correspondentes.
