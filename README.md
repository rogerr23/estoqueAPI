# Estoque API

Sistema de consulta de estoque e transferência entre lojas. Requisitos e modelo aprovado em [docs](docs/).

## Ambiente local — ponto 1

Nesta etapa: NestJS 11 com TypeScript, PostgreSQL 17 no Docker Compose e TypeORM para acesso ao banco e migrations. A API roda no computador; o banco roda em container. Ainda não há tabelas de negócio, consulta de estoque, suíte de testes ou pipeline de CI.

### Pré-requisitos

- Node.js 22, versão 22.14 ou superior da linha 22, e npm.
- Docker Engine em execução e Docker Compose v2 (no macOS, normalmente Docker Desktop).
- Portas locais 3000 e 5432 disponíveis, ou valores alternativos no `.env`.

### Preparar e executar

Na raiz do repositório:

```sh
cp .env.example .env
docker compose up -d --wait postgres
cd backend
npm ci
npm run start:dev
```

O `.env` fica na **raiz**, compartilhado entre Compose e API. Não sobrescreva um `.env` existente com suas configurações. As credenciais do exemplo são somente para desenvolvimento local. O arquivo real é ignorado pelo Git.

Em outro terminal:

```sh
curl --fail http://127.0.0.1:3000/health
```

Resposta esperada:

```json
{"status":"ok","database":"up"}
```

Esse endpoint executa `SELECT 1`: confirma conexão real com PostgreSQL. Não é uma suíte de testes e não confirma regras de negócio.

Para reiniciar automaticamente a API ao editar arquivos, use `npm run start:watch`.

### Swagger

Com a API em execução, abra [Swagger UI](http://localhost:3000/docs). A documentação permite visualizar rotas, respostas e executar requisições com **Try it out**. O documento OpenAPI em JSON está em [docs-json](http://localhost:3000/docs-json).

O endpoint `/health` documenta respostas 200 e 503. Swagger ajuda na exploração manual; os testes automatizados serão adicionados nas próximas entregas.

### Verificar o projeto

Na pasta `backend`:

```sh
npm run lint
npm run typecheck
npm run build
npm start
```

Use `npm start` depois de parar a API de desenvolvimento, pois ambos usam a mesma porta. O `package-lock.json` fixa as versões instaladas; `npm ci` reproduz essas versões.

### Migrations

TypeORM usa a mesma configuração de conexão na API e no CLI. `synchronize` e execução automática de migrations ficam desativados: mudanças no esquema serão explícitas e versionadas.

```sh
cd backend
npm run migration:show
npm run migration:create -- src/database/migrations/NomeDaAlteracao
npm run migration:run
```

No ponto 1, a pasta de migrations está vazia; `migration:show` não lista alterações. No próximo ponto serão adicionadas as primeiras tabelas. `migration:revert` desfaz a última migration e só deve ser usado quando essa reversão for desejada.

### Parar o banco e preservar dados

Na raiz:

```sh
docker compose down
```

Os dados persistem no volume. Alterar usuário, senha ou nome de banco no `.env` não reconfigura um volume já inicializado. Não remova o volume para resolver um problema sem considerar os dados existentes.

### Diagnóstico

- **Cannot connect to the Docker daemon:** inicie Docker Desktop/Engine antes de subir o banco.
- **Porta ocupada:** altere `PORT` ou `DB_PORT` no `.env`; recrie o serviço Compose se mudar `DB_PORT`.
- **Falha de conexão da API:** confira `docker compose ps` e `docker compose logs postgres`, além dos valores do `.env`. Com a API local, `DB_HOST` é `127.0.0.1`.
- **Variável obrigatória ausente:** confira se `.env` foi criado na raiz. Variáveis já exportadas no terminal têm precedência sobre o arquivo.

### Aprendizado e próximas entregas

O foco é Testes, CI, AWS e Cloud. A sequência é: consulta de estoque e dados de demonstração; testes unitários e de integração com banco separado; pipeline no GitHub Actions; transferências com testes de rollback, concorrência e repetição; laboratórios de Cloud.

Decisões e referências: [ambiente local](docs/AMBIENTE-LOCAL.md).
