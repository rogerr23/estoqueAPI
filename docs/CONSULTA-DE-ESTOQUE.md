# Entrega 2 — Consulta de produtos e estoque

## Comportamento

- `GET /produtos?busca=Notebook`: busca por nome ou código, até 50 registros por página.
- `GET /produtos/:id/estoques`: produto e saldos em todas as lojas, incluindo pares ausentes como zero.
- Físico menos reservado determina o disponível.
- Disponível 0: SEM_ESTOQUE; 1: BAIXO_ESTOQUE; acima de 1: NORMAL.
- Erros de entrada: 400; produto inexistente: 404.
- IDs de produto e loja são UUIDv4. A rota de estoque valida o UUID; números antigos são inválidos.
- Documentação e exemplos disponíveis em `/docs`.

## Estrutura

Controller recebe a requisição e documenta seu contrato. Service valida parâmetros e consulta PostgreSQL com parâmetros separados do SQL. DTOs descrevem respostas no Swagger. O módulo registra essas dependências no NestJS.

As duas migrations criam lojas/produtos/estoques e a base necessária para rastrear entradas: colaboradores e movimentações manuais. Essa é uma implementação incremental do modelo aprovado. Ainda não inclui clientes, transferências, eventos nem os tipos de movimentação das transferências; uma migration posterior ampliará as restrições e adicionará os vínculos correspondentes.

Não há alteração automática do esquema pelo ORM. As migrations são explícitas e transacionais. A carga de demonstração também é transacional e registra ENTRADA para cada saldo inicial. Pode ser repetida sem dobrar os saldos.

## Conferência desta entrega

Conferir build, tipos, lint, aplicação das migrations, execução repetida do seed, respostas HTTP e contrato OpenAPI. Verificar também restrições de saldo e unicidade no banco, com alterações de verificação desfeitas.

Verificações realizadas com sucesso nesta entrega:

- Lint, tipos e build.
- As duas migrations aplicadas ao PostgreSQL local.
- Seed executado duas vezes: duas entradas, total de seis unidades, sem duplicação.
- Busca por nome e código; resultado vazio; caracteres de busca tratados literalmente.
- Notebook com disponível 5 no Centro e 0 na Barra; camisa com baixo estoque no Centro.
- IDs e parâmetros inválidos retornando 400; ID inexistente retornando 404.
- Rotas e modelos presentes no documento OpenAPI.
- Saldo negativo, reserva acima do físico e par repetido rejeitados pelo banco.
- Cálculo com físico 5 e reservado 4 retornando 1; alterações de conferência desfeitas por rollback.

Essas verificações de implementação não substituem a suíte de testes automatizados. A próxima entrega ensinará como transformar os cenários abaixo em testes reproduzíveis, com banco de teste separado:

- Busca por nome/código e nenhum resultado.
- Produto inexistente e ID inválido.
- Loja sem registro de estoque.
- Disponível calculado corretamente e baixo estoque exatamente 1.
- Entrada inicial rastreável e seed sem duplicação.
- Quantidades inválidas e estoque duplicado rejeitados pelo banco.

## Commit sugerido

`feat: adiciona consulta de estoque por loja e dados de demonstração`

Roger executa os commits. O assistente apenas indica oportunidades e sugere mensagens.

## Evolução para UUID

Produtos, lojas e colaboradores convertidos por novas migrations; clientes e transferências usarão UUID desde a criação. Estoque, itens e históricos mantêm IDs inteiros internos. O identificador anterior fica em `id_legado` para permitir reversão e não é exposto pela API.

Verificações realizadas: lint, tipos e build; conversão com dados e vínculos; inserção após conversão; reversão e reaplicação em esquema temporário desfeito por rollback; preservação dos saldos e movimentos no banco local; seed repetido; respostas UUID no Swagger e na API; IDs inválidos retornando 400 e UUIDv4 inexistente retornando 404.

Geração usa [gen_random_uuid() do PostgreSQL 17](https://www.postgresql.org/docs/17/functions-uuid.html), sem extensão adicional. A rota usa [ParseUUIDPipe do NestJS](https://docs.nestjs.com/pipes) com versão 4.
