# EstoqueAPI

Sistema acadêmico, desenvolvido para a disciplina de Qualidade de Software, para
controlar o estoque de várias lojas: cadastro de produtos, entrada de mercadoria,
transferências entre lojas, histórico de movimentações e registro de quem fez cada
operação. Funciona no computador e no celular.

Para publicar no Render, siga [o guia de publicação](RENDER.md). O `render.yaml` da
raiz cria um Web Service (interface + API) e um PostgreSQL.

## Sumário

- [Funcionalidades](#funcionalidades)
- [Perfis de acesso](#perfis-de-acesso)
- [Tecnologias e requisitos](#tecnologias-e-requisitos)
- [Como executar](#como-executar)
- [Dados de demonstração](#dados-de-demonstração)
- [Testes e qualidade](#testes-e-qualidade)
- [API](#api)
- [Regras de negócio](#regras-de-negócio)
- [Banco de dados e migrações](#banco-de-dados-e-migrações)
- [Organização do código](#organização-do-código)
- [Limitações conhecidas](#limitações-conhecidas)

## Funcionalidades

As abas seguem o fluxo de trabalho: visão geral, operação do dia, registros e cadastros.

| Aba | O que faz |
| --- | --- |
| **Painel** | Números principais (produtos, unidades por loja, entradas e transferências do dia), alerta de **estoque baixo** e últimas atividades. Cada cartão leva à aba correspondente já filtrada. |
| **Estoque** | Saldos de **todas as lojas** lado a lado (uma coluna por loja e o total) ou de uma loja só. |
| **Entradas** | Registra mercadoria que chegou a uma loja (produto, loja, quantidade e observação, como a nota fiscal). Mostra as últimas 5 entradas. |
| **Transferências** | Move unidades entre lojas. Mostra o saldo atual da origem e do destino e o máximo permitido, e impede o envio se a origem não tiver saldo. Mostra as últimas 5 transferências. |
| **Movimentações** | Histórico único de **entradas e transferências**, com filtros por tipo, período (hoje, 7 ou 30 dias) e loja, mais busca, ordenação, paginação e **exportação para CSV**. |
| **Alterações** | Registro de edições e exclusões feitas por administradores: quem fez, quando e o que mudou ("antes → depois"). Visível a todos. |
| **Produtos** | Catálogo com estoque total, **estoque mínimo** e situação (OK/Baixo). Cadastro com estoque inicial opcional numa loja. |
| **Gestão** | Somente admin. Sub-abas **Lojas** (cadastrar e renomear) e **Funcionários** (cadastrar, editar, desativar/reativar e ver as atividades de cada um). |

**Recursos comuns a todas as listas:** busca sem diferenciar maiúsculas e acentos;
ordenação clicando no título da coluna; filtros, busca e página guardados por aba;
mensagens de sucesso que somem em 4 s e de erro em 8 s.

**Acesso à API:** o botão **API** no cabeçalho abre o Swagger e copia o token CSRF.

**Celular:** as tabelas viram cartões com o nome de cada campo; um seletor
"Ordenar por" substitui o clique nas colunas; os formulários de cadastro começam
recolhidos para a lista aparecer primeiro; campos com fonte de 16 px (sem zoom
automático no iPhone) e botões de 44 px; filtros dois por linha; paginação compacta.

## Perfis de acesso

| Ação | Funcionário | Admin |
| --- | :---: | :---: |
| Consultar painel, estoque, movimentações e alterações | ✓ | ✓ |
| Registrar entradas e transferências | ✓ | ✓ |
| Cadastrar produtos | ✓ | ✓ |
| Editar produto (código, nome, mínimo e saldo por loja) | | ✓ |
| Excluir produto, entrada ou transferência | | ✓ |
| Gestão de lojas e funcionários | | ✓ |

- Cada pessoa entra com o próprio e-mail e senha; toda operação registra o responsável.
- Excluir um funcionário o **desativa**: ele não entra mais, mas o histórico mantém o nome.
- Ao **desativar** alguém ou **mudar o perfil**, a sessão aberta dessa pessoa é encerrada
  na hora (o sistema confere o banco a cada requisição).
- O admin não pode desativar a si mesmo nem retirar o próprio perfil de administrador.
- O usuário de `DEMO_EMAIL` é criado (ou promovido) como admin ao iniciar com o perfil
  `dev`. Em bancos antigos, a migração V2 promove o primeiro usuário cadastrado.

## Tecnologias e requisitos

- **Backend:** Java 21, Spring Boot 3.5, Spring Security (sessão + CSRF), JPA, Flyway e PostgreSQL 17.
- **Frontend:** React e Vite (versões em `frontend/package-lock.json`).
- **Para desenvolver:** Java 21, Node.js 22.14+, Docker com Compose e Git. O Maven
  Wrapper está incluído; não é preciso instalar Maven.
- **Só para usar:** apenas Docker (veja [Rodar tudo com Docker](#rodar-tudo-com-docker)).

## Como executar

### Configuração

Na raiz, copie o exemplo e defina senhas locais (o `.env` fica fora do Git e não
deve ser sobrescrito se já existir):

```sh
cp .env.example .env
```

O backend lê esse arquivo ao rodar a partir da raiz ou de `backend/`; variáveis de
ambiente têm precedência.

### Desenvolvimento (IntelliJ IDEA ou terminal)

1. Abra a pasta `estoqueAPI`; em `backend/pom.xml`, use **Add as Maven Project** e selecione o JDK 21.
2. Inicie o banco, na raiz:

```sh
docker compose up -d --wait
```

3. Inicie o backend (porta 8080). A configuração **EstoqueAPI dev** em `.run/` faz o mesmo pela IDE:

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

4. Em outro terminal, inicie o frontend:

```sh
cd frontend
npm ci
npm run dev
```

Acesse `http://127.0.0.1:5173` e entre com `DEMO_EMAIL` e `DEMO_PASSWORD` do `.env`.
O Vite encaminha `/api`, `/swagger-ui` e `/v3/api-docs` ao backend.

### Rodar tudo com Docker

Sem Java, Node ou IDE: com o Docker Desktop aberto e o `.env` configurado (senhas
diferentes em `POSTGRES_PASSWORD` e `DEMO_PASSWORD`), na raiz:

```sh
docker compose -f compose.app.yaml up -d --build --wait
```

Acesse `http://127.0.0.1:8090`. Para parar mantendo os dados:

```sh
docker compose -f compose.app.yaml down
```

Não use `down -v` para parar: ele apaga o volume de dados. Para outra porta, defina
`APP_PORT` no `.env`. Este modo usa volume próprio, separado do desenvolvimento.

### Publicação no Render

O `Dockerfile` da raiz compila o React e o inclui no JAR, servindo interface, API e
Swagger no mesmo endereço. O deploy aplica as migrações automaticamente. Detalhes,
limites do plano gratuito e conferência em [RENDER.md](RENDER.md).

## Dados de demonstração

O perfil `dev` cria os dados numa transação, somente quando ausentes (reiniciar não
duplica nem restaura saldos):

| Produto | Loja A | Loja B |
| --- | ---: | ---: |
| PROD-001 — Caderno | 20 | 5 |
| PROD-002 — Caneta | 12 | sem registro (saldo zero) |

A senha do admin é gravada com BCrypt; alterar `DEMO_PASSWORD` depois não muda a
senha de um usuário já criado. Para recomeçar do zero, **apagando todo o banco local**,
pare o backend e rode:

```sh
docker compose down -v
docker compose up -d --wait
```

## Testes e qualidade

### Testes automatizados do backend

Os testes usam um PostgreSQL exclusivo na porta 5434, com armazenamento temporário.
Crie `.env.test` na raiz com `TEST_DATABASE_PASSWORD=uma-senha-local-de-teste`
(fora do Git). Na raiz:

```sh
set -a
source .env.test
set +a
docker compose -p estoqueapi-test -f compose.test.yaml up -d --wait
cd backend
./mvnw verify
```

Para repetir desde um banco limpo, remova o contêiner de testes (o banco de
desenvolvimento não é afetado):

```sh
docker compose -p estoqueapi-test -f compose.test.yaml down
```

Relatórios em `backend/target/surefire-reports/`. A suíte só conecta ao banco
`estoque_test`, na porta 5434 de `localhost`; outras URLs são rejeitadas. A senha do
usuário de teste é gerada em memória a cada execução.

**Resultado atual (26/11/2026): 62 testes, 0 falhas, 0 erros, 0 ignorados.**

| Classe | Testes | O que cobre |
| --- | ---: | --- |
| `AcessoConsultasTests` | 12 | Login, sessão, CSRF, logout, consultas, validações e 401 sem sessão |
| `TransferenciaIntegrationTests` | 12 | Transferências, saldo exato/insuficiente, limite, **rollback**, **concorrência** e contrato OpenAPI |
| `TransferenciaServiceTests` | 7 | Regras de transferência (unitários com Mockito) |
| `EstoqueApiApplicationTests` | 6 | Dados de demonstração (senha protegida, sem duplicar) e restrições do banco |
| `UsuarioAdminTests` | 5 | CRUD de funcionários, permissões, e-mail duplicado e **sessão encerrada ao desativar/mudar perfil** |
| `ProdutoAtualizacaoTests` | 5 | Edição pelo admin, estoque mínimo, registro de alterações e 403 para funcionário |
| `ExclusaoAdminTests` | 4 | Exclusão de produto e transferência, devolução de estoque e 403 |
| `EntradaTests` | 3 | Entradas, soma no estoque, atividades e exclusão pelo admin |
| `LojaAdminTests` | 2 | Cadastro e renomeação de lojas, nome duplicado e 403 |
| `ProdutoEstoqueInicialTests` | 2 | Cadastro com estoque inicial e validações |
| `ProdutoServiceTests` | 2 | Código duplicado (unitários) |
| `SecurityConfigTests` | 2 | Rotas públicas e protegidas |

- **Rollback:** um trigger temporário, criado só no banco de testes, força uma falha
  depois do débito; a suíte confirma que saldos e histórico voltam ao estado anterior.
- **Concorrência:** transações em threads separadas disputam o mesmo saldo, criam um
  destino em comum e movimentam em sentidos opostos; os saldos finais e o total são conferidos.

### Frontend

```sh
cd frontend
npm run lint
npm run build
```

Ambos passam sem avisos.

### Verificação manual (QA)

Em 26/11/2026 todas as abas foram verificadas no navegador, como admin e como
funcionário, em desktop e em tela de celular (375 px), num ambiente isolado: API na
porta 8081, banco de teste na 5434 e interface na 5174. Para repetir:

```sh
cd frontend
ESTOQUE_API_PROXY_TARGET=http://127.0.0.1:8081 npm run dev -- --port 5174
```

Defeitos encontrados nessa verificação e corrigidos:

1. Sessão de usuário desativado ou rebaixado continuava ativa (segurança).
2. Tabela de funcionários escondia os botões de ação em telas menores.
3. Código do produto quebrava em duas linhas na tabela de alterações.
4. Transferência acima do saldo só era recusada pelo servidor; agora a tela avisa antes.

### Coleção Postman

`postman/EstoqueAPI.postman_collection.json` cobre o **escopo original do MVP**: 30
cenários e 44 asserções (sessão, CSRF, produtos, lojas, estoque, transferências e
logout). Importe-a com `postman/local.postman_environment.example.json` e preencha
`baseUrl`, `email` e `password` apenas no ambiente local, com um cookie jar vazio.
A coleção ainda **não** inclui usuários, entradas, lojas, edição e exclusões; esses
recursos estão cobertos pela suíte JUnit acima. Para rodar pelo terminal:

```sh
npm exec --yes --package=newman@6.2.2 -- newman run postman/EstoqueAPI.postman_collection.json -e postman/local.postman_environment.json
```

## API

Todos os endpoints exigem sessão, exceto obter o CSRF e fazer login. Respostas em
JSON, sem redirecionamentos. Operações de escrita exigem o header `X-CSRF-TOKEN`;
obtenha um token novo após login e logout. O contrato completo está no Swagger
(`/swagger-ui/index.html`) e em `/v3/api-docs`, também protegidos por sessão.

### Autenticação

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Token CSRF |
| POST | `/api/auth/login` | Formulário `email` e `senha`; inicia sessão |
| POST | `/api/auth/logout` | 204; encerra a sessão |
| GET | `/api/auth/me` | Usuário da sessão (id, nome, email, perfil) |

### Operação (qualquer usuário autenticado)

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | `/api/produtos` | Produtos ativos (com `estoqueMinimo`) |
| GET | `/api/produtos/{id}` | Um produto |
| POST | `/api/produtos` | `{codigo, nome, lojaId?, quantidade?, estoqueMinimo?}` |
| GET | `/api/produtos/edicoes` | Registro de alterações |
| GET | `/api/lojas` | Lojas |
| GET | `/api/lojas/{id}/estoque` | Saldo de todos os produtos na loja (ausente = 0) |
| POST | `/api/entradas` | `{produtoId, lojaId, quantidade, observacao?}` |
| GET | `/api/entradas` | Entradas, da mais recente |
| POST | `/api/transferencias` | `{produtoId, lojaOrigemId, lojaDestinoId, quantidade}` |
| GET | `/api/transferencias` | Transferências, da mais recente |

### Somente administrador

| Método | Rota | Resultado |
| --- | --- | --- |
| PUT | `/api/produtos/{id}` | `{codigo, nome, estoqueMinimo?, estoques?: [{lojaId, quantidade}]}` |
| DELETE | `/api/produtos/{id}` | Exclui (some das listas; histórico preservado) |
| DELETE | `/api/entradas/{id}` | Exclui e retira as unidades da loja |
| DELETE | `/api/transferencias/{id}` | Exclui e devolve as unidades à origem |
| POST | `/api/lojas` | `{nome}` |
| PUT | `/api/lojas/{id}` | `{nome}` |
| GET | `/api/usuarios` | Funcionários com resumo de atividades |
| POST | `/api/usuarios` | `{nome, email, senha, perfil}` |
| PUT | `/api/usuarios/{id}` | `{nome, email, perfil, ativo, senha?}` (senha vazia mantém a atual) |
| DELETE | `/api/usuarios/{id}` | Desativa |
| GET | `/api/usuarios/{id}/atividades` | Tudo o que o funcionário fez |

### Erros

Formato: `{codigo, mensagem, campos?}`. **400** dados inválidos · **401** sem sessão ou
credenciais inválidas · **403** CSRF inválido ou perfil sem permissão · **404** recurso
inexistente · **409** conflito (código, e-mail ou loja duplicados; saldo insuficiente;
limite do estoque) · **500** falha interna, sem detalhes do banco.

### Exemplo pelo Postman

1. GET `/api/auth/csrf` e copie `token` (mantenha o cookie de sessão).
2. POST `/api/auth/login` com o header `X-CSRF-TOKEN` e body *x-www-form-urlencoded* (`email`, `senha`).
3. GET `/api/auth/csrf` de novo: o token anterior é invalidado no login.
4. POST `/api/produtos` com o novo token e `{"codigo":" prod-003 ","nome":" Borracha "}`
   gera o código `PROD-003` e o nome `Borracha`.

Pela interface é mais simples: entre, clique em **API → Abrir Swagger** e use
**Copiar token CSRF** para as operações de escrita.

## Regras de negócio

- **Quantidades:** inteiros positivos (estoque inicial e mínimo aceitam zero); frações e
  campos fora do contrato são rejeitados. Saldo máximo: 2.147.483.647 (409 `LIMITE_ESTOQUE`).
- **Produtos:** código único, normalizado para maiúsculas e sem espaços nas pontas.
  O código de um produto excluído não pode ser reutilizado.
- **Estoque baixo:** quando a soma de todas as lojas fica abaixo do mínimo (mínimo 0 = sem alerta).
- **Transferências:** origem e destino diferentes; saldo insuficiente retorna 409
  `SALDO_INSUFICIENTE`; o destino sem registro é criado na mesma transação. Débito,
  crédito e histórico são gravados juntos: qualquer falha desfaz tudo.
- **Exclusões pelo admin:** desfazem o efeito no estoque. Uma exclusão é recusada se a
  loja já não tiver as unidades (por exemplo, se já foram transferidas), para o estoque
  nunca ficar negativo. Cada exclusão e edição fica registrada em Alterações.
- **Responsável e horário** de cada operação são definidos pelo servidor.
- **Concorrência:** toda operação que altera saldos bloqueia o produto
  (`PESSIMISTIC_WRITE`) antes de ler os saldos. Operações do mesmo produto acontecem
  uma de cada vez; produtos diferentes seguem em paralelo.

## Banco de dados e migrações

O Flyway cria e atualiza o esquema a cada inicialização (`backend/src/main/resources/db/migration/`):

| Versão | Conteúdo |
| --- | --- |
| V1 | Usuários, lojas, produtos, estoques e transferências |
| V2 | Perfis ADMIN/FUNCIONARIO, usuário ativo e responsável pelo cadastro do produto |
| V3 | Registro de edições de produto |
| V4 | Exclusão lógica de produtos e tipo de ação no registro |
| V5 | Entradas de mercadoria |
| V6 | Estoque mínimo por produto |
| V7 | Exclusão de entradas no registro de alterações |

## Organização do código

- `backend/src/main/java/br/com/estoqueapi/`
  - `controller/` endpoints REST · `service/` regras de negócio · `repository/` acesso a dados
  - `entity/` tabelas · `dto/` contratos de entrada e saída · `config/` segurança, sessão, OpenAPI e dados de demonstração
  - `exception/` tratamento padronizado de erros
- `backend/src/test/` testes unitários e de integração.
- `frontend/src/`
  - `App.jsx` login e sessão · `Dashboard.jsx` abas e operações · `Painel.jsx`, `Lojas.jsx`, `Funcionarios.jsx`, `ProdutoEditor.jsx`
  - `tableTools.jsx` e `tableUtils.js` busca, ordenação, paginação e CSV · `services/api.js` chamadas à API
- `postman/` coleção e exemplo de ambiente, sem credenciais.
- `compose.yaml` banco de desenvolvimento · `compose.test.yaml` banco de testes · `compose.app.yaml` sistema completo.

## Limitações conhecidas

- Projeto acadêmico: sem recuperação de senha, sem cadastro público e sem separação de
  dados por empresa. Todos os usuários compartilham as mesmas lojas e produtos.
- Lojas não podem ser excluídas (têm estoque e histórico ligados a elas).
- Busca, filtros e paginação são feitos no navegador; para volumes muito grandes, o ideal
  seria paginar na API.
- A coleção Postman cobre só o escopo original do MVP (ver [Coleção Postman](#coleção-postman)).
- Não há suíte E2E automatizada nem pipeline de CI; a interface foi verificada manualmente.
- Não foi feito teste de carga; o bloqueio por produto serializa operações do mesmo produto.
- No plano gratuito do Render o serviço "dorme" após 15 minutos sem acesso e o banco
  expira em 30 dias (ver [RENDER.md](RENDER.md)).
