# Evidências — Fase 1

Execução em 03/10/2026, com Java 21.0.5, Node.js 22.14.0, Docker 27.4.0,
Spring Boot 3.5.16 e PostgreSQL 17.

## Resultado

- Backend: `./mvnw verify` — BUILD SUCCESS.
- JUnit 5 com PostgreSQL exclusivo: 6 testes, 0 falhas, 0 erros, 0 ignorados.
- Frontend: `npm run lint` e `npm run build` — ambos concluídos com sucesso.
- Backend em perfil `dev`: migração aplicada e servidor iniciado na porta 8080.
- `GET /api/produtos` sem autenticação: 401 com JSON, conforme o bloqueio provisório da Fase 1.
- Frontend na porta 5173: página inicial renderizada e conferida no navegador.
- IntelliJ: `../../backend/pom.xml` importado e módulo `estoque-api` reconhecido com JDK 21.
- Configuração **EstoqueAPI dev** executada pela IDE: perfil `dev` ativo, Tomcat na porta 8080 e inicialização concluída; resposta 401 confirmada novamente.

## Cenários automatizados

Os testes CT-BASE-01 a CT-BASE-06 verificaram dados iniciais, hash de senha,
idempotência da carga, preservação de saldo alterado ao reiniciar a carga,
unicidade de produto e estoque, saldo não negativo, chaves estrangeiras e
restrições de quantidade/lojas na tabela de transferências.

Relatório resumido do Maven:

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Os relatórios completos são gerados em `../../backend/target/surefire-reports`.
Para reproduzir, siga os comandos de banco temporário e testes no README.

## Limites deste marco

Login, operações HTTP de negócio, OpenAPI, regras do service de transferência,
rollback de transferência e concorrência serão implementados e testados nas fases
seguintes. O frontend atual é a página inicial, sem operações de negócio.
