# Execução atual da interface

Data: 24/10/2026; fuso: America/Sao_Paulo; Chrome 155.0.8059.85 no macOS 27.0.1. Executor técnico: Codex, assistido pelo navegador nesta sessão. Revisão humana: pendente.
Versão: a84a4f8d3cef994b006afec54c4c2bebf8b9d447; aplicação Java e componentes React sem mudanças; Vite com proxy configurável. Fontes identificadas em fontes-sha256.txt.

API 8081 conectada explicitamente a estoque_test:5434; Vite 5174. PostgreSQL 17.11 com tmpfs em contêiner exclusivo. Usuário fictício qa.entrega2@example.test. A senha foi gerada para esta rodada e não foi incluída nas provas.

1. A interface rejeitou senha incorreta com E-mail ou senha inválidos. Depois, login válido apresentou módulos e catálogo.
2. Cadastro de código com minúsculas e espaços e nome com espaços gerou E2E-ENTREGA2, Produto Entrega 2, ID observado 20. Captura 01.
3. Estoque A apresentou Caderno 20 e Caneta 12. B apresentou Caderno 5 e Caneta zero. Capturas 02 e 03. A consulta não criou novo estoque: o banco manteve três registros.
4. Transferir 21 de Caderno A→B exibiu Saldo insuficiente na loja de origem; prévia 20/5. Captura 04. A preservação da persistência para esse cenário também foi verificada no JUnit.
5. Transferir 10 concluiu como #17 em 24/10/2026 17:30:19, com prévia 10/15. Captura 05. Consultas separadas de A e B mostraram 10 e 15. Capturas 06 e 07.
6. Histórico apresentou #17, produto, lojas, quantidade 10, data/hora e Funcionário de demonstração. Captura 08. As duas operações anteriores #15/#16 são ida e retorno do Postman na mesma rodada.
7. Recarregar manteve a sessão e voltou ao catálogo. Sair mostrou Você saiu do sistema. A consulta protegida em seguida exibiu NAO_AUTENTICADO. Capturas 09 e 10. A repetição instrumentada do logout registrou 204 e /me 401 no log do proxy.
8. CT25: interface adicional 5175, proxy 8082→8081 com atraso de oito segundos somente no POST de transferência. Envio exploratório #18 de uma unidade: 10/15→9/16. Repetição controlada #19 de uma unidade: 9/16→8/17. Na repetição, após iniciar envio, cinco cliques no mesmo botão desabilitado não produziram outro POST. Capturas 11–13 mostram estado ocupado, sucesso e histórico. O log registrou um POST por envio intencional, ambos 201; houve dois envios, e não duplicação. Leituras SQL confirmaram estoque ainda com três registros e histórico de três para cinco.
9. Logout final foi realizado antes de encerrar a API e recriar somente a base de testes para a regressão. Os dados de navegador foram descartados apenas depois das evidências.

Não houve acesso ao banco de desenvolvimento. Nenhum resultado dessa sequência utiliza as capturas antigas de 03/10. Os timestamps individuais das imagens, seus hashes e o ambiente estão no índice de evidências.
