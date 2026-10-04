# EstoqueAPI

Sistema acadêmico para consultar estoque por loja e transferir produtos entre lojas.

## Estado atual

Fases 1 a 5 concluídas para o escopo do MVP: base executável, autenticação com
sessão e CSRF, produtos, estoque, transferências, histórico e interface React.
Validação final: 39 testes JUnit/MockMvc/integração, 30 cenários da coleção
Postman (44 asserções), lint, build e fluxo completo pela interface aprovados.
Plano, resultados sanitizados e oito capturas estão em `docs/`, mantidos localmente
fora dos commits conforme a organização deste projeto. A coleção executável e
seu exemplo de ambiente estão em `postman/`, sem credenciais.

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
| PROD-002 — Caneta | 12 | Sem registro (consulta retorna saldo zero) |

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

Validação em 03/10/2026: `./mvnw verify` passou com **39 testes**, sem falhas,
erros ou testes ignorados: 6 da base, 24 de API/integração e 9 unitários.
Inclui os testes anteriores e transferência válida, quantidade 1, saldo exato,
saldo insuficiente, destino ausente, entradas inválidas, usuário da sessão,
histórico ordenado, limite do inteiro, rollback e concorrência com PostgreSQL.

O teste de rollback instala um trigger temporário **somente no banco de testes**:
ele confirma que o débito já chegou ao banco e provoca falha na gravação do
histórico. A suíte verifica a restauração dos saldos, a ausência de histórico
e a remoção do estoque de destino recém-criado. O trigger é removido ao final.

Os testes concorrentes iniciam transações em threads separadas para disputar o
mesmo saldo, criar um destino em comum e movimentar em sentidos opostos. Asserções
verificam os saldos finais, o total preservado e o número de registros criados.


## Organização

- `backend/`: aplicação Java, entidades, repositories, configuração e testes.
- `backend/src/main/resources/db/migration/`: esquema versionado com Flyway.
- `frontend/`: aplicação React e proxy de desenvolvimento.
- `docs/`: plano de testes e evidências.
- `postman/`: coleção executável com cenários positivos/negativos e exemplo de ambiente.

## Marcos concluídos

As cinco fases do plano foram concluídas. Funcionalidades adicionais ou critérios
específicos da disciplina devem ser alinhados antes de ampliar o escopo.

## API — Fases 2 e 3

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
| POST | `/api/transferencias` | 201 — transferência concluída |
| GET | `/api/transferencias` | 200 — histórico do mais recente para o mais antigo |

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
independente do cookie jar do Postman. Entre pela interface e use **Abrir Swagger**.
O contrato documenta o formulário de login, a sessão, CSRF e erros.

## Transferências — Fase 3

Depois de autenticar e obter o novo CSRF, envie POST `/api/transferencias` com
Content-Type application/json, o header `X-CSRF-TOKEN` e:

```json
{
  "produtoId": 1,
  "lojaOrigemId": 1,
  "lojaDestinoId": 2,
  "quantidade": 10
}
```

Use os IDs retornados pelas consultas; IDs não são reiniciados automaticamente.
Com os dados originais, o caderno passa de 20/5 para 10/15 nas lojas A/B.
A resposta contém `id`, `produto`, `lojaOrigem`, `lojaDestino`, `quantidade`,
`dataHora` (Instant em UTC) e `usuario` (id, nome, email; sem senha/hash).
Responsável e horário são definidos pelo servidor. Campos extras e quantidades
fracionárias são rejeitados; envie apenas os quatro campos do contrato.

GET `/api/transferencias` retorna apenas operações concluídas, ordenadas por
horário decrescente e, em caso de empate, ID decrescente. Não há edição,
cancelamento ou paginação neste MVP.

### Regras e atomicidade

- IDs e quantidade devem ser inteiros positivos; origem e destino são distintos.
- Produto e lojas precisam existir; estoque ausente significa zero.
- Saldo insuficiente retorna 409 `SALDO_INSUFICIENTE`, sem movimentação/histórico.
- O saldo exato pode ser transferido; a origem fica com zero.
- Destino ausente é criado dentro da mesma transação.
- Débito, crédito e histórico são confirmados juntos; qualquer falha desfaz tudo.
- Saldos usam inteiro de 32 bits; ultrapassar 2.147.483.647 no destino retorna
  409 `LIMITE_ESTOQUE`, sem alterações.

### Concorrência

Cada transferência bloqueia o registro do produto com `PESSIMISTIC_WRITE` antes
de ler os saldos. Movimentações do mesmo produto aguardam a transação anterior,
inclusive quando precisam criar um destino inexistente. Produtos diferentes
podem ser movimentados em paralelo. É uma escolha simples para o volume deste
MVP; movimentações do mesmo produto entre lojas independentes também são
serializadas. Futuras operações que alterem saldos precisam seguir esse protocolo.

A suíte confirmou duas solicitações de 8 com saldo 10: uma conclui, a outra recebe
409; os saldos ficam 2/13 e há somente um registro no histórico. Confirmou também
créditos simultâneos num destino ausente e transferências em sentidos opostos.

No backend iniciado pelo IntelliJ, também foram conferidos autenticação,
histórico, contrato OpenAPI e uma tentativa de saldo insuficiente: 409, com
estoque e histórico preservados. Os dados de demonstração permaneceram intactos.

## Login pela interface e acesso ao Swagger

1. Acesse `http://127.0.0.1:5173/`.
2. Informe o e-mail de `DEMO_EMAIL` e a senha de `DEMO_PASSWORD` do `.env` local.
3. Clique em **Entrar**. Credenciais inválidas exibem uma mensagem; durante o
   envio, o formulário fica desabilitado para evitar solicitações repetidas.
4. Após entrar, selecione **API** e clique em **Abrir Swagger**. Ele abre no endereço do frontend,
   usando a mesma sessão. Não é necessário usar o console ou preencher Authorize.
5. Para operações POST, clique em **Copiar token CSRF** no módulo **API** e cole
   no campo `X-CSRF-TOKEN` do Swagger. Se a cópia automática não estiver disponível,
   o token será exibido para seleção manual. Um novo login exige um novo token.
6. Use **Sair** para encerrar a sessão. Recarregar a página enquanto autenticado
   preserva o acesso; depois de sair, a tela pede login novamente.

O Vite encaminha `/api`, `/swagger-ui` e `/v3/api-docs` ao backend. O OpenAPI usa
um endereço de servidor relativo, permitindo executar chamadas pelo frontend ou
pelo backend sem depender de uma porta fixa. Isso preserva o uso de cookies da
sessão sem ampliar a configuração de CORS.

Validação deste marco: lint e build do frontend passaram; os 39 testes do backend
passaram, incluindo a asserção do endereço relativo do OpenAPI. No navegador,
foram verificados senha incorreta, login válido, persistência ao recarregar,
cópia do token, consulta de produtos pelo Swagger com resposta 200 e logout.
O funcionário temporário usado na verificação foi removido.


## Interface de gestão de estoque

Após o login, use os módulos no menu:

- **Produtos**: cadastre código e nome; o catálogo inclui os produtos cadastrados.
  O código é normalizado pela API e duplicidades são rejeitadas.
- **Estoque**: escolha uma loja para consultar todos os produtos e quantidades.
  Produtos sem registro de estoque aparecem com zero.
- **Transferências**: selecione produto, origem, destino e quantidade inteira
  positiva. Confira os saldos exibidos e clique em **Confirmar transferência**.
  A API valida o saldo atual e registra o funcionário autenticado e o horário.
  Após a conclusão, saldos e histórico são consultados novamente.
- **Histórico**: consulte operações concluídas, com produto, lojas, quantidade,
  data/hora no fuso do navegador e responsável.
- **API**: abra o Swagger e copie o token CSRF para operações manuais.

**Atualizar** repete apenas as consultas. Se uma operação concluir e a consulta
seguinte falhar, a confirmação permanece visível junto ao erro de atualização;
não reenvie a transferência para atualizar a tela. Os formulários bloqueiam novos
envios enquanto uma gravação está em andamento. Uma sessão expirada retorna ao
login. Tabelas largas têm rolagem horizontal em telas menores.

Validação deste marco: lint e build do frontend, mais verificação no navegador
com usuário e produto temporários. Foram conferidos cadastro, código duplicado,
estoque ausente como zero, transferência de 3 unidades (10/0 para 7/3), rejeição
por saldo insuficiente e histórico com responsável e horário. Os registros de
teste foram removidos sem alterar os produtos de demonstração.


## Validação final e coleção Postman

Importe `postman/EstoqueAPI.postman_collection.json` e
`postman/local.postman_environment.example.json` no Postman. Preencha `baseUrl`,
`email` e `password` somente no ambiente local. Use um cookie jar vazio para a
primeira requisição (consulta anônima), depois execute a coleção inteira em ordem.
O script obtém um token CSRF antes de cada escrita e o cookie da sessão é mantido
pelo cliente. Não há senha, cookie ou token salvo nos arquivos distribuídos.

Pré-condições: ambiente de teste local com carga `dev`, lojas **Loja A** e
**Loja B**, produto **PROD-001** e saldo inicial de pelo menos 10 na origem.
A coleção cria um produto por execução e duas transferências; a segunda devolve
as 10 unidades à origem. Saldos são restaurados ao terminar com sucesso, mas os
produtos e o histórico permanecem. Use uma base descartável para a entrega.
Os testes de rollback e concorrência são executados pelo JUnit, não pelo Postman.

Para repetir a validação HTTP em um ambiente separado:

1. Execute a suíte JUnit seguindo a seção **Testes e verificações**.
2. Na raiz, carregue `.env.test` e exporte os parâmetros para a API temporária:

```sh
set -a
source .env.test
set +a
export SPRING_DATASOURCE_URL="${TEST_DATABASE_URL:-jdbc:postgresql://localhost:5434/estoque_test}"
export SPRING_DATASOURCE_USERNAME="${TEST_DATABASE_USER:-estoque_test}"
export SPRING_DATASOURCE_PASSWORD="$TEST_DATABASE_PASSWORD"
# Defina DEMO_EMAIL e DEMO_PASSWORD com valores exclusivos de teste.
cd backend
java -jar target/estoque-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=8081
```

3. Use `http://127.0.0.1:8081` como `baseUrl` no ambiente local do Postman.
   Para usar Newman no terminal, salve esse ambiente preenchido como
   `postman/local.postman_environment.json` (ignorado pelo Git) e execute na raiz:

```sh
npm exec --yes --package=newman@6.2.2 -- newman run postman/EstoqueAPI.postman_collection.json -e postman/local.postman_environment.json
```

4. Ao terminar, encerre a API temporária e remova o contêiner de testes usando
   o comando `down` da seção de testes. Isso não remove o banco de desenvolvimento.

A execução final usou API na porta 8081, PostgreSQL temporário na 5434 e uma cópia
local da interface na 5174 com proxy para essa API. O ambiente habitual 5173/8080
não foi modificado. Foram aprovadas 44 asserções Postman, em 30 cenários com
43 chamadas HTTP contando as consultas auxiliares de CSRF. Resultados sanitizados:
`docs/evidencias/fase-5/`; rastreabilidade: `docs/plano-de-testes.md`.

### Limitações conhecidas

O escopo é um MVP acadêmico local. Listagens não têm paginação; funcionários
possuem as mesmas permissões. Lojas, funcionários e saldos iniciais vêm da carga
de desenvolvimento. Não há entrada de estoque pela interface, administração de
usuários, recuperação de senha, edição/exclusão de produtos ou cancelamento de
transferência. O fluxo de ponta a ponta foi verificado pelo navegador com
capturas; não existe uma suíte E2E reutilizável nem pipeline de CI neste marco.
O bloqueio por produto serializa transferências do mesmo produto. Não foi definida
meta de cobertura, carga máxima ou requisito de publicação pela disciplina.

## Rodar tudo com Docker, sem Java ou IntelliJ

Para quem só precisa usar o sistema, instale e abra o Docker Desktop. Extraia o
projeto e, na raiz, copie `.env.example` para `.env` se esse arquivo ainda não
existir. Defina senhas locais diferentes em `POSTGRES_PASSWORD` e `DEMO_PASSWORD`.

```sh
docker compose -f compose.app.yaml up -d --build --wait
```

Acesse `http://127.0.0.1:8090` e entre com `DEMO_EMAIL`/`DEMO_PASSWORD` do `.env`.
O Docker constrói backend e frontend e cria o PostgreSQL com dados de demonstração.
Não é necessário instalar Java, Maven, Node.js, PostgreSQL ou uma IDE no computador.
O primeiro build depende de internet para baixar imagens e dependências.

O frontend Nginx encaminha API e Swagger ao backend na rede interna do Compose,
preservando sessão e CSRF. Somente a interface é publicada, em loopback. Esse modo
usa o projeto Compose `estoqueapi-app` e volume próprios, separados do ambiente
local de desenvolvimento. Cada computador tem seu banco, sem sincronização.

Para parar mantendo os dados:

```sh
docker compose -f compose.app.yaml down
```

Para iniciar novamente, use `up -d --wait`; acrescente `--build` quando houver
alterações no código. Não use `down -v` para parar: ele apaga o volume de dados.
Para outra porta, defina `APP_PORT=8091` no `.env` e use a nova porta no navegador.
Banco e API não são publicados em portas do computador neste modo.

O guia detalhado para iniciantes fica localmente em
`docs/guia-para-rodar-o-sistema.md`; a versão PDF e o ZIP para compartilhar ficam
em `output/`. Documentos e pacotes gerados permanecem fora do Git. Envie o ZIP
junto do PDF, ou disponibilize esta configuração no repositório antes de orientar
alguém a baixar pelo GitHub. Não envie `.env`, senhas ou o banco local.
