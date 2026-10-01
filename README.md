# Estoque API

Sistema de consulta de estoque e transferência entre lojas. Requisitos e modelo aprovado em [docs](docs/).

## Ambiente local — ponto 1

NestJS 11 com TypeScript, PostgreSQL 17 no Docker Compose e TypeORM para acesso ao banco e migrations. A API roda no computador; o banco roda em container. A consulta de produtos e estoque está implementada. A suíte de testes automatizados e o pipeline de CI são as próximas entregas.

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
npm run migration:run
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

As migrations iniciais criam lojas, produtos e estoques, além da base de colaboradores e movimentações para rastrear entradas de demonstração. Histórico e tipos de movimentação relacionados às transferências serão acrescentados quando esse fluxo for implementado. `migration:revert` desfaz a última migration e só deve ser usado quando essa reversão for desejada.

### Dados de demonstração e consulta de estoque

Depois de aplicar migrations, na pasta `backend`:

```sh
npm run seed:demo
```

O seed é permitido apenas em `NODE_ENV=development`. Cria lojas Centro e Barra e produtos Notebook (5 unidades no Centro) e Camisa (1 unidade no Centro). Na Barra, os pares sem registro aparecem como zero. Os códigos têm prefixo `DEMO-`. Reexecutar não repete entradas nem redefine saldos de produtos já existentes.

Cada saldo inicial tem uma movimentação ENTRADA, gravada na mesma transação. O autor da carga é um colaborador inativo com senha aleatória não disponibilizada, usado apenas para rastreabilidade; não é uma conta de login. Não há autenticação ou endpoints de alteração de estoque nesta etapa; a API permanece vinculada a localhost.

No Swagger:

1. Execute `GET /produtos` com `busca=Notebook` ou `busca=DEMO-NOTE-001`.
2. Copie o `id` do produto retornado.
3. Execute `GET /produtos/{id}/estoques` com esse ID.
4. Confira Centro com físico 5, reservado 0 e disponível 5; Barra com zero.
5. Repita com Camisa: Centro aparece como BAIXO_ESTOQUE, pois disponível é exatamente 1.

Busca é por trecho, sem distinção de maiúsculas/minúsculas. `%` e `_` são tratados como caracteres literais. Sem `busca`, a rota lista produtos, até 50 por página, com parâmetro `pagina`. Inclui cadastros inativos; a consulta de estoque também inclui todas as lojas e identifica seu status.

Parâmetros inválidos retornam 400; produto inexistente retorna 404; busca sem correspondência retorna lista vazia. Preço é texto decimal, preservando as duas casas do banco. Disponível é calculado na consulta e não armazenado.

### Identificadores UUID

Produtos, lojas e colaboradores usam UUIDv4 gerado no PostgreSQL. Na consulta, `produto.id` e `lojaId` são strings UUID. Copie o ID retornado pela busca e use-o em `/produtos/{id}/estoques`; IDs inteiros antigos agora retornam 400. UUID válido de produto inexistente retorna 404. Os códigos, como `DEMO-NOTE-001`, continuam disponíveis para busca.

Novas migrations convertem os dados e vínculos existentes sem recriar o banco. `id_legado` preserva internamente os números anteriores para permitir reversão, mas não aparece na API. Estoques e movimentações mantêm IDs internos inteiros. Clientes e transferências usarão UUID quando forem implementados. UUID não substitui autorização.

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
