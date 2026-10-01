# Ambiente local e escolhas técnicas

## Escopo do ponto 1

- Backend NestJS 11 com TypeScript em modo estrito.
- PostgreSQL 17 no Docker Compose, com volume e healthcheck.
- `.env.example` compartilhado por backend e Compose.
- TypeORM para acesso ao banco e migrations explícitas.
- Endpoint `/health` que verifica o banco.
- Swagger UI em `/docs` e OpenAPI JSON em `/docs-json`.
- Scripts de lint, verificação de tipos e build.

O ponto 1 não incluía migrations de negócio, cadastros, testes automatizados ou CI. A consulta e as primeiras migrations foram adicionadas na [entrega 2](CONSULTA-DE-ESTOQUE.md); testes automatizados e CI são as próximas entregas.

## Por que TypeORM

A integração com Nest disponibiliza a conexão por injeção de dependência. Isso permitirá usar o banco real nos testes de integração e substituir dependências em testes isolados quando necessário. A ferramenta também oferece migrations e transações. O SQL das migrations pode ser escrito explicitamente, aproveitando o conhecimento existente.

Não usar `synchronize: true`: o esquema deve evoluir por arquivos versionados. API e CLI compartilham as opções de conexão, evitando diferenças escondidas de configuração.

## Como validar esta etapa

1. Executar lint, verificação de tipos e build.
2. Validar o Compose e subir PostgreSQL até ficar saudável.
3. Iniciar a API e consultar `/health`.
4. Conferir `migration:show` conectando ao banco, mesmo sem migrations ainda.

Essas verificações são de instalação e conexão. Os testes automatizados de comportamento serão construídos nas próximas entregas, com banco isolado.

## Verificação realizada nesta entrega

- Dependências instaladas e versões fixadas no `backend/package-lock.json`.
- Lint, verificação de tipos e build executados com sucesso.
- Configuração Compose validada; PostgreSQL iniciado e saudável.
- CLI `migration:show` conectou ao banco, sem migrations de negócio nesta etapa.
- API compilada iniciou e `GET /health` retornou `{"status":"ok","database":"up"}`.
- `.env`, dependências e arquivos compilados confirmados como ignorados pelo Git.

## Referências oficiais

- [Primeiros passos no NestJS](https://docs.nestjs.com/first-steps).
- [Integrações de banco no NestJS](https://docs.nestjs.com/techniques/database).
- [Migrations no TypeORM](https://typeorm.io/docs/advanced-topics/migrations/).
- [Imagem oficial PostgreSQL](https://hub.docker.com/_/postgres).
- [Variáveis de ambiente no Compose](https://docs.docker.com/compose/how-tos/environment-variables/set-environment-variables/).
