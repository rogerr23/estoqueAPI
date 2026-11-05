# Entrega 2 do EstoqueAPI

Execução atual em 24/10/2026, America/Sao_Paulo. Resultado consolidado: **25 CTs aprovados de 25**, incluindo 18 críticos. Backend: **39 execuções**, sem falhas/erros/ignorados. Postman: **30 cenários, 43 chamadas com CSRF auxiliar e 44 asserções aprovadas**. São contagens diferentes.

## O que enviar e revisar

- `Relatório de Execução e Gestão de Testes.pdf`: documento da Entrega 2.
- `Relatório de Execução e Gestão de Testes.md`: versão editável do conteúdo.
- `matriz-CT01-CT25.csv` e `resultados-CT01-CT25.json`: rastreabilidade e roteiro completo.
- `evidencias`: capturas novas, JUnit e Newman sanitizados, leituras SQL e registro do ambiente.
- `codigo-testes`: testes Java, apoio de configuração, coleção Postman e proxy de observação do CT25.
- `defeitos.csv`: registro vazio de defeitos funcionais novos; não foram inventados BUGs. O impedimento resolvido está em `evidencias/impedimentos.txt`.

O grupo ainda deve revisar o material, validar a execução assistida por ferramentas e enviar os arquivos pelo sistema acadêmico. O relatório identifica isso explicitamente. A URL do Git é a referência do projeto; as alterações locais não foram publicadas. Não houve commit nem push.

## Como localizar as provas

`junit-baseline.xml` registra a suíte original; `junit-regressao.xml` registra a versão com asserções ampliadas e proteção do banco. Ambos têm 39 execuções. Seus resumos `.txt` identificam os horários de término. Propriedades, logs brutos e dados de autenticação foram removidos dos XMLs. O JSON Postman contém somente contagens, nomes, status e asserções; não contém respostas, variáveis nem cookie jar.

As referências AC, TI, TU, PU e BASE do roteiro correspondem às classes Java explicadas na seção 5 do PDF. Cada método deve ser conferido no código junto do XML final. As expressões [0], [-1], [10] e [20] descrevem o parâmetro do teste; a nomenclatura exata das instâncias aparece no XML.

| Prefixo da captura | Conteúdo |
| --- | --- |
| 01 | Catálogo e cadastro normalizado |
| 02 e 03 | Estoques A20/B5; Caneta ausente em B apresentada como zero |
| 04 | Rejeição por saldo insuficiente |
| 05 | Transferência de dez e prévia 10/15 |
| 06 e 07 | Consultas separadas de origem e destino |
| 08 | Histórico da transferência com responsável e data |
| 09 e 10 | Logout e acesso protegido negado |
| 11 | CT25 durante o envio, controles desabilitados |
| 12 e 13 | CT25 concluído e histórico das operações |

No CT25 houve um envio exploratório de uma unidade (10/15 → 9/16, #18) e uma repetição controlada de uma unidade (9/16 → 8/17, #19). Na segunda execução foram realizados cinco cliques adicionais no botão enquanto estava desabilitado. O log `ct25-rede.jsonl` contém **dois POSTs porque foram dois envios independentes**, um por envio. `banco-antes-ct25.txt` e `banco-depois-ct25.txt` demonstram saldos e históricos. O proxy atrasou cada POST em oito segundos e registrou apenas metadados sem segredos. Depois do logout, o mesmo log registrou `/api/auth/me` com 401.

`banco-apos-regressao.txt` pertence à nova base limpa da regressão: dois produtos, duas lojas, um usuário, três estoques, zero histórico e zero trigger de falha. Não deve ser comparado como continuação da base do navegador, que foi descartada depois de coletar as provas.

## Reprodução segura

Pré-requisitos: Docker Desktop iniciado, Java 21, Node compatível e dependências já instaladas. Use o projeto local com os fontes desta versão. Os arquivos em `codigo-testes/backend/src/test` preservam o caminho para copiar em uma cópia do repositório. Não são um projeto Java autônomo: usam o backend existente. O código de produção Java e React é o do commit-base informado; use também o `../../frontend/vite.config.js` e README locais para o proxy de QA.

1. Não inicie o Compose de desenvolvimento para testar. Prepare `.env.test` com `TEST_DATABASE_PASSWORD` exclusivo; o arquivo não integra a entrega.
2. Pare APIs de QA ligadas à porta 5434. O script a seguir recria **apenas a base temporária de testes**, por isso conclua ou registre qualquer outra execução que a esteja usando:

```sh
bash output/entrega-2/codigo-testes/executar-regressao.sh /caminho/estoqueAPI
```

3. O suporte dos testes aceita apenas `estoque_test` em localhost/127.0.0.1:5434 e usuário `estoque_test`. A senha do funcionário é gerada em memória e muda entre processos JUnit. Não é a senha de desenvolvimento.
4. Para Postman/interface, inicie novamente a API na porta 8081 apontada explicitamente para o banco de testes. Use as instruções da seção “Validação final e coleção Postman” do README; escolha `DEMO_EMAIL` e `DEMO_PASSWORD` fictícios e exclusivos. Não reutilize nem exporte credenciais de desenvolvimento.
5. Execute a coleção completa, em ordem, com cookie jar vazio. Preencha somente um ambiente Postman local, nunca a coleção distribuída. A coleção cria produto e histórico de teste, apesar de devolver as dez unidades à origem. Não aponte para desenvolvimento.
6. Para a interface normal de QA:

```sh
cd frontend
ESTOQUE_API_PROXY_TARGET=http://127.0.0.1:8081 npm run dev -- --port 5174
```

7. Para repetir CT25, inicie `node output/entrega-2/codigo-testes/proxy-observacao.mjs` na raiz; em outro terminal, execute o frontend com `ESTOQUE_API_PROXY_TARGET=http://127.0.0.1:8082` e `--port 5175`. O proxy é somente para QA. Registre saldos e histórico antes/depois; faça um clique e repita cliques no botão durante os oito segundos. Conte POSTs em `/private/tmp/estoque-entrega2/proxy.jsonl`, separando cada envio intencional. Preserve o log fora do pacote atual para não misturar rodadas.
8. Use o roteiro CT01–CT25 e restaure as pré-condições para cada caso; capture evidências com data, versão e ambiente. Saldos de rollback, concorrência e limite são preparados pelos testes de integração. Não provoque essas falhas em desenvolvimento.
9. Encerre os processos temporários. Para remover somente a base de testes, na raiz, use `docker compose -p estoqueapi-test -f compose.test.yaml down`. Não use `down -v` no Compose de desenvolvimento.

A reprodução manual não é pendência desta execução: o Chrome estava acessível e o fluxo foi executado. Estas instruções permitem a conferência do grupo. Não foram testados alta carga, segurança completa nem o Compose integral do backend/frontend. A API mantém a limitação conhecida de reenvios HTTP sem idempotência.

## Histórico e autoria dos resultados

Os registros de 03/10 e os nove unitários recebidos no pacote de preparação são antecedentes. Nenhum foi contabilizado como nova execução. O pacote atual tem seus próprios registros e hashes. Executor técnico: Codex nesta sessão; revisão humana pelos alunos ainda pendente.
