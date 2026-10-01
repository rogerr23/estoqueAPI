# CRUD de produtos

## Entrega

Cadastro, consulta individual, edição parcial e inativação de produtos via HTTP e Swagger, com persistência no PostgreSQL. Usa o esquema existente; não exige migration adicional.

DTOs definem o contrato e validam dados com class-validator/class-transformer. ValidationPipe rejeita campos desconhecidos. SQL usa parâmetros para valores; na edição, os nomes das colunas vêm de uma lista fixa.

Código é normalizado em maiúsculas e permanece único entre ativos/inativos. O preço é recebido como texto decimal não negativo, evitando arredondamento silencioso; até dez dígitos inteiros e duas casas decimais. Descrição e categoria podem ser limpas com null. A API atualiza `atualizado_em` nas alterações efetivas.

DELETE implementa inativação, conforme o modelo aprovado. Não libera código, não apaga estoque e não gera movimentação de saldo. Bloqueia reservas e pedidos abertos; o teste de pedidos usou tabelas mínimas de cenário, pois o fluxo de transferências ainda não existe. Quando esse fluxo for implementado, solicitação e inativação deverão compartilhar o bloqueio da linha do produto para evitar concorrência.

Autenticação e autorização de administrador ainda são pendentes; estas operações estão disponíveis somente na API de desenvolvimento em localhost. Não há reativação nesta entrega.

## Verificações realizadas

- Lint, tipos e build.
- Banco temporário independente do banco de desenvolvimento, criado com todas as migrations e removido ao final.
- Cadastro e consulta verificando gravação direta no PostgreSQL.
- Normalização de código e nome.
- Edição parcial preservando os campos omitidos; limpeza de opcionais com null.
- Campos obrigatórios, nulos indevidos, objeto vazio, array e campos proibidos recusados.
- Preço negativo, numérico ou com mais de duas casas recusado.
- Conflitos de código no cadastro e edição, inclusive dois cadastros simultâneos.
- ID inválido e produto ausente retornando 400/404.
- Inativação bloqueada por reserva e pelos quatro status abertos de transferência.
- Inativação repetida e conservação do cadastro e saldo físico.

Essas verificações pontuais ainda não constituem uma suíte versionada. Na próxima etapa, transformar os cenários em testes automatizados reproduzíveis e executáveis no CI.

## Commit sugerido

`feat: implementa cadastro, edição e inativação de produtos`

O commit é executado pelo Roger.

Referência: [validação de DTOs no NestJS](https://docs.nestjs.com/techniques/validation).

## Remoção da unidade de medida

O campo foi removido dos contratos de entrada/saída e do banco por uma nova migration. Quantidades continuam inteiras. A migration antiga permanece intacta; a reversão da nova migration restaura o campo com UN, único valor anteriormente permitido. Produtos, IDs, saldos e histórico são preservados.
