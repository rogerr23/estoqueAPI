# EstoqueAPI

Sistema acadêmico para consultar estoque por loja e transferir produtos entre lojas.
A especificação está em [EstoqueAPI.md](EstoqueAPI.md).

## Estado atual

Fases 1 e 2 concluídas: base executável, autenticação com sessão e CSRF,
cadastro e consulta de produtos, listagem de lojas e estoque, erros JSON e OpenAPI.
Transferências/histórico entram na Fase 3; telas funcionais, na Fase 4.
O frontend ainda apresenta a página inicial do projeto.

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

Validação em 03/10/2026: `./mvnw verify` passou com 20 testes, sem falhas,
erros ou testes ignorados. Inclui 6 testes da base, 12 testes de API/integração
com MockMvc e PostgreSQL e 2 testes unitários com Mockito. Verificados: sessão,
proteção contra fixação de sessão, renovação de CSRF, logout, códigos HTTP,
normalização, duplicidade, limites dos campos, estoque ausente e contrato OpenAPI.

## Organização

- `backend/`: aplicação Java, entidades, repositories, configuração e testes.
- `backend/src/main/resources/db/migration/`: esquema versionado com Flyway.
- `frontend/`: aplicação React e proxy de desenvolvimento.
- `docs/`: plano de testes e evidências.
- `postman/`: reservado para a coleção das próximas fases.

## Próximo marco

Fase 3: transferência com transação única, proteção contra concorrência, criação
do estoque de destino, histórico e testes de saldo exato/insuficiente e rollback.

## API — Fase 2

Todos os endpoints de negócio e a documentação exigem sessão. Somente a obtenção
de CSRF e o processamento do login são públicos. As respostas são JSON, sem
redirecionamento para uma página de login.

| Método | Endpoint | Resultado |
| --- | --- | --- |
| GET | `/api/auth/csrf` | 200 — token, headerName, parameterName |
| POST | `/api/auth/login` | 200 — id, nome e email; inicia sessão |
| POST | `/api/auth/logout` | 204 — encerra sessão |
| GET | `/api/auth/me` | 200 — funcionário da sessão, sem senha/hash |
| POST | `/api/produtos` | 201 — produto criado; cabeçalho Location |
| GET | `/api/produtos` | 200 — produtos em ordem de ID |
| GET | `/api/produtos/{id}` | 200 — produto |
| GET | `/api/lojas` | 200 — lojas em ordem de ID |
| GET | `/api/lojas/{id}/estoque` | 200 — todos os produtos e saldos, incluindo zero |

### Como testar com Postman

1. Envie GET `http://127.0.0.1:8080/api/auth/csrf`. Preserve o cookie de sessão
   no cookie jar e copie `token` da resposta.
2. Envie POST `/api/auth/login` com header `X-CSRF-TOKEN` contendo esse token.
   Use body **x-www-form-urlencoded**, campos `email` e `senha`, com os valores
   locais do `.env`. O login usa o filtro padrão do Spring Security.
3. Obtenha um **novo token** via GET `/api/auth/csrf` após autenticar. O token
   anterior é invalidado pelo Spring Security.
4. Consulte `/api/auth/me`, `/api/produtos` e `/api/lojas`. Para cadastrar, envie
   POST `/api/produtos` com o novo token no header, Content-Type application/json
   e body `{"codigo":" prod-003 ","nome":" Borracha "}`.
   O resultado usa código `PROD-003` e nome `Borracha`.
5. Para logout, envie POST `/api/auth/logout` com CSRF válido. A sessão é
   invalidada; uma nova consulta autenticada retorna 401. Obtenha novamente
   CSRF antes de iniciar outro login.

Não envie credenciais reais; use exclusivamente o funcionário de demonstração.
O cadastro aceita código de até 60 caracteres e nome de até 150, após remover
espaços nas extremidades. O código é normalizado para maiúsculas antes da
verificação de unicidade; o banco também protege contra cadastros simultâneos.

### Erros

Erros contêm `codigo` e `mensagem`; validações podem incluir `campos`.
400 indica dados inválidos, 401 autenticação ausente/credenciais inválidas,
403 CSRF ausente/inválido, 404 recurso inexistente e 409 código duplicado.
Falhas internas retornam 500 sem detalhes do banco na resposta.

### Swagger / OpenAPI

Com sessão autenticada, consulte `/v3/api-docs` para o contrato JSON e
`/swagger-ui/index.html` para o Swagger. A documentação também é protegida;
sem sessão retorna 401. O navegador precisa da sua própria sessão autenticada,
independente do cookie jar do Postman. O login pela interface será adicionado na
Fase 4. O contrato documenta o formulário de login, a sessão, CSRF e erros.
