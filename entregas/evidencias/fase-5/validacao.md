# Validação final do MVP — 03/10/2026

Base validada: commit `5f2eb40`. Plataforma macOS, Java 21.0.5, Node 22.14.0,
npm 10.9.2, Newman 6.2.2. Escopo: RF01–RF06 e RN01–RN10.

## Resultados

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Maven verify, PostgreSQL real exclusivo | 39 testes; 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS | backend-resumo.txt; junit-resultados.xml |
| React lint | Saída sem avisos/erros, exit 0 | frontend-lint.txt |
| Vite build | Bundle gerado, exit 0 | frontend-build.txt |
| Coleção Postman / Newman | 30 cenários, 43 chamadas incluindo CSRF, 44 asserções; 0 falhas | postman-resultados.json |
| Fluxo integrado pela interface | Cadastro, consulta, rejeição por saldo, transferência, dois saldos, histórico, sessão/reload e logout | Capturas 01–08 abaixo |

A execução usa base temporária estoque_test/5434, API 8081 e frontend 5174.
O banco de desenvolvimento e a interface habitual não foram alterados. A API e
o frontend de teste foram encerrados, e o contêiner temporário removido após a
verificação. Tokens, cookies, senhas e variáveis de ambiente não estão nos
relatórios arquivados. XML preserva apenas contagens e identificação dos testes;
relatório Postman preserva nomes, status HTTP e resultados das asserções.

## Fluxo e dados observados

1. Senha incorreta: mensagem “E-mail ou senha inválidos”. Login correto com
   funcionário sintético validacao@example.test inicia a sessão.
2. Código `  e2e-fase5  `, nome “Produto de evidência E2E”: cadastro concluído
   como E2E-FASE5, listado no catálogo. [Captura 01](01-cadastro.jpg).
3. Loja A: novo produto com zero; Caderno PROD-001 com 20 e Caneta com 12.
   [Captura 02](02-estoque-inicial.jpg).
4. Caderno A→B, quantidade 21: “Saldo insuficiente na loja de origem”,
   prévia permanece 20/5. [Captura 03](03-saldo-insuficiente.jpg).
5. Mesma seleção, quantidade 10: transferência #17 concluída; prévia 10/15,
   confirmação em fundo azul claro e borda marinho. [Captura 04](04-transferencia-concluida.jpg).
6. Consulta de estoque A: Caderno 10. [Captura 05](05-saldo-origem.jpg).
7. Consulta de estoque B: Caderno 15. [Captura 06](06-saldo-destino.jpg).
8. Histórico: #17 primeiro, 10 unidades de A→B, 03/10/2026 21:28:29 no fuso
   do navegador, Funcionário de demonstração como responsável. Os registros #15
   e #16 são a ida e retorno da coleção Postman executada antes do E2E.
   [Captura 07](07-historico.jpg).
9. Recarregar mantém acesso aos módulos; Sair retorna ao formulário de login
   com “Você saiu do sistema”. [Captura 08](08-logout.jpg).

IDs são observados nesta execução, não devem ser fixados como pré-condição de
novas execuções. A coleção resolve IDs pelas consultas. A API gera responsável
por sessão e data/hora. O E2E não foi exportado como teste automatizado reutilizável.

## Defeitos e ajustes registrados

| ID | Encontrado | Correção aplicada | Reteste / status |
| --- | --- | --- | --- |
| DEF-01 | Confirmação com cinza semelhante ao fundo podia passar despercebida; apontada pelo usuário | Fundo azul suave, texto marinho com peso 600 e borda esquerda forte no CSS | Confirmação visível na captura 04; aprovado |
| DEF-02 | Tabela larga expandia o painel e deixava Sair fora da área visível em tela menor durante Fase 4 | min-width:0 no painel e rolagem horizontal na tabela | Menu/Sair dentro do painel nas capturas desta execução; aprovado |
| DEF-03 | README dizia que as telas ainda seriam implementadas e checklist não marcava Fase 4 concluída | Estado atual e checklist revisados; resultados da Fase 5 consolidados | Conferência textual com funcionalidades e relatórios; corrigido neste marco |

Não foram encontrados novos defeitos funcionais nos cenários executados na
validação final. Isso não demonstra ausência de defeitos em cenários não cobertos.
Rollback e concorrência são comprovados pelos testes de integração, não pelas
capturas. A cobertura percentual e os requisitos de carga/produção não foram medidos.

## Limitações e entrega

Sem paginação, administração de usuários/lojas/saldos, entrada de estoque pela
interface, recuperação de senha, perfis de acesso, edição/exclusão ou cancelamento.
Bloqueio serializa o mesmo produto. Sem pipeline CI ou suíte E2E permanente.
Sem enunciado/rubrica da disciplina para confirmar requisitos externos ao plano.

MVP e validação previstos nas cinco fases concluídos. Documentos e evidências
ficam na pasta local ignorada pelo Git; README e coleção executável podem ser
versionados. Evidências estão disponíveis para organizar a entrega acadêmica
separadamente, conforme a preferência do usuário.
