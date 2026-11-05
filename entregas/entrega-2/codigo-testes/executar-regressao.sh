#!/usr/bin/env bash
# Reexecução somente no contêiner temporário de testes; não usa compose.yaml.
set -euo pipefail
project_root="${1:?Informe a pasta raiz do projeto EstoqueAPI}"
cd "$project_root"
test -f compose.test.yaml
test -f .env.test
test -f backend/src/test/java/br/com/estoqueapi/DatabaseTestSupport.java
set -a
source .env.test
set +a
: "${TEST_DATABASE_PASSWORD:?Configure a senha exclusiva em .env.test}"
export TEST_DATABASE_URL=jdbc:postgresql://127.0.0.1:5434/estoque_test
export TEST_DATABASE_USER=estoque_test
# Pare antes qualquer API de QA ligada a esse banco.
docker compose -p estoqueapi-test -f compose.test.yaml down
docker compose -p estoqueapi-test -f compose.test.yaml up -d --wait
(cd backend && ./mvnw verify)
(cd frontend && npm run lint && npm run build)
# Mantém a base exclusiva disponível para a próxima verificação Postman/interface.
