# V1 — Sistema de Varejo / Rede de Lojas

Documento simples de requisitos e qualidade para apresentação em aula e como base futura do projeto.

## 1. Visão geral e objetivo

**Segmento:** varejo, com uma rede de lojas ou franquias.

**Problema:** funcionários perdem tempo procurando mercadorias em outras unidades. A empresa também precisa centralizar informações de estoque, clientes e movimentações.

**Objetivo da V1:** permitir a consulta do estoque de produtos em todas as lojas e controlar transferências entre unidades, com permissões, consistência dos saldos e histórico.

O foco principal é **buscar mercadorias e transferi-las entre lojas**. Cadastros e relatórios simples apoiam esse fluxo.

## 2. Fluxo principal de busca e transferência

1. O funcionário entra no sistema.
2. Pesquisa um produto pelo nome ou código.
3. O sistema mostra a quantidade disponível em cada loja, inclusive lojas sem estoque.
4. O funcionário escolhe lojas de origem e destino diferentes, produtos e quantidades.
5. O sistema valida os dados e registra a transferência como **SOLICITADA**.
6. Um administrador aprova a transferência. O sistema verifica novamente o estoque e reserva as unidades na origem.
7. Um colaborador da loja de origem registra o envio de todos os itens e quantidades. A transferência fica **EM_TRANSITO** e as unidades saem fisicamente da origem.
8. Um colaborador da loja destino confirma o recebimento de todos os itens e quantidades. A transferência fica **RECEBIDA** e as unidades entram no estoque do destino.
9. Um administrador encerra o processo como **CONCLUIDA**, preservando o histórico e sem movimentar estoque novamente.

**Exemplo:** a loja Barra precisa de 2 notebooks. O funcionário encontra 8 disponíveis na loja Centro e solicita a transferência de Centro para Barra.

Uma transferência pode conter vários produtos, como 2 notebooks, 5 mouses e 3 teclados.

## 3. Entidades da V1

As entidades abaixo descrevem os conceitos do sistema. Os detalhes de campos e relacionamentos serão refinados no Passo 2 — Modelagem de Dados.

| Entidade | Para que serve | Informações principais |
|---|---|---|
| **Loja** | Identifica cada unidade da rede. | Identificação, nome e dados da unidade. |
| **Produto** | Mantém o cadastro comum das mercadorias. | ID, código, nome, descrição, categoria, preço e status. |
| **Estoque** | Controla as quantidades de um produto em uma loja. | Loja, produto, quantidade física e quantidade reservada. A disponível é calculada. |
| **Cliente** | Centraliza o cadastro dos clientes. | ID, nome, CPF, telefone, e-mail, endereço, data de cadastro e status ATIVO/INATIVO. |
| **Colaborador** | Identifica quem utiliza o sistema e suas permissões. | Identificação, vínculo com uma loja e perfil FUNCIONÁRIO ou ADMINISTRADOR. |
| **Transferência** | Registra o processo de envio entre unidades. | ID, origem, destino, solicitante, data de solicitação, data de conclusão, status e itens. |
| **ItemTransferência** | Representa cada produto de uma transferência. | Transferência, produto e quantidade. |
| **MovimentaçãoEstoque** | Explica as alterações do estoque. | Tipo, produto, quantidade, loja, data e origem da movimentação, incluindo a transferência relacionada quando houver. |

**Relacionamentos principais:** uma loja possui estoques e colaboradores; um produto pode ter estoque em várias lojas; uma transferência possui origem, destino e um ou mais itens; cada item aponta para um produto; as movimentações registram as alterações de estoque.

**A quantidade não pertence ao cadastro de Produto.** O mesmo notebook pode ter 5 unidades na Barra, 12 no Centro e nenhuma em Botafogo.

### Estoque físico, reservado e disponível

- **Físico:** unidades presentes na loja.
- **Reservado:** parte do estoque físico comprometida com transferências aprovadas que ainda não foram enviadas.
- **Disponível:** unidades que ainda podem ser comprometidas.

**Disponível = Físico − Reservado.**

Exemplo: físico 10, reservado 3 e disponível 7. A reserva faz parte do físico; não é uma quantidade adicional.

### Critério de baixo estoque

Na V1, o critério é fixo para todos os produtos e lojas e usa a quantidade **disponível**:

| Quantidade disponível | Classificação |
|---|---|
| 0 | Sem estoque disponível. |
| 1 | Baixo estoque. |
| Maior que 1 | Acima do limite de baixo estoque. |

O relatório de baixo estoque lista combinações de produto e loja com disponível igual a **1**. O relatório de produtos sem estoque considera disponível igual a **0**. Não haverá configuração de limite por produto ou loja na V1.

Exemplo: físico 5 e reservado 4 resultam em disponível 1, portanto baixo estoque. Físico 5 e reservado 5 resultam em disponível 0, portanto sem estoque disponível, embora ainda existam unidades físicas reservadas.

## 4. Requisitos funcionais principais

| ID | O sistema deve permitir... |
|---|---|
| RF01 | Autenticar colaboradores e respeitar seus perfis de acesso. |
| RF02 | Cadastrar e editar lojas, produtos e colaboradores, conforme as permissões. |
| RF03 | Consultar, cadastrar e editar clientes, mantendo o CPF imutável e permitindo inativação para preservar histórico. |
| RF04 | Buscar produtos por nome ou código e consultar seu estoque por loja. |
| RF05 | Solicitar transferências com vários produtos e validar origem, destino e quantidades. |
| RF06 | Consultar transferências e acompanhar seu status. |
| RF07 | Aprovar transferências e reservar estoque na origem. |
| RF08 | Registrar envio, recebimento e conclusão, atualizando os estoques nos momentos corretos. |
| RF09 | Cancelar transferências antes do envio e liberar suas reservas, quando existirem. |
| RF10 | Duplicar uma transferência como nova solicitação, com confirmação do funcionário e novas validações. |
| RF11 | Registrar e consultar movimentações rastreáveis de estoque. |
| RF12 | Consultar relatórios simples de estoque atual, estoque por loja, produtos sem estoque, produtos com baixo estoque, transferências realizadas, movimentações e clientes cadastrados. |

Os tipos de movimentação aprovados são **ENTRADA, SAIDA, TRANSFERENCIA_ENTRADA, TRANSFERENCIA_SAIDA, AJUSTE, RESERVA e LIBERACAO_RESERVA**. Os dois últimos registram alterações da reserva, sem mudar o físico.

### Perfis e permissões

**FUNCIONÁRIO:** consultar produtos, estoque e clientes; cadastrar e editar clientes; solicitar, consultar e duplicar transferências; registrar envio na sua loja de origem e recebimento na sua loja de destino.

**ADMINISTRADOR:** todas as ações do funcionário; cadastrar e editar produtos, lojas e colaboradores; aprovar, cancelar e concluir transferências; registrar entradas e ajustes manuais com motivo obrigatório; consultar relatórios.

Cada colaborador pertence a uma loja. As permissões operacionais ficam definidas assim:

| Ação | Quem pode executar | Condição |
|---|---|---|
| Aprovar | Administrador. | Transferência SOLICITADA e saldo suficiente em todos os itens. |
| Cancelar | Administrador. | Transferência SOLICITADA ou APROVADA. |
| Registrar envio | Funcionário ou administrador vinculado à loja de origem. | Transferência APROVADA; envio integral. |
| Confirmar recebimento | Funcionário ou administrador vinculado à loja de destino. | Transferência EM_TRANSITO; recebimento integral. |
| Concluir | Administrador. | Transferência RECEBIDA; sem nova alteração de saldo. |
| Registrar entrada ou ajuste manual | Administrador. | Motivo obrigatório, registrado no histórico. |

O perfil de administrador não dispensa o vínculo com a loja correta nas ações de envio e recebimento. As permissões devem ser verificadas na API.

## 5. Fluxo e status das transferências

**SOLICITADA → APROVADA → EM_TRANSITO → RECEBIDA → CONCLUIDA**

| Status | Significado e efeito no estoque |
|---|---|
| **SOLICITADA** | Pedido registrado e validado. Ainda não reserva estoque; por isso, a aprovação precisa verificar o saldo novamente. |
| **APROVADA** | Administrador autorizou. A quantidade fica reservada na origem, sem diminuir o físico. |
| **EM_TRANSITO** | Mercadoria enviada. Diminuem o físico e a reserva da origem pela quantidade enviada. O destino ainda não recebe saldo. |
| **RECEBIDA** | Destino confirmou o recebimento. Seu estoque físico aumenta uma única vez. |
| **CONCLUIDA** | Processo encerrado e protegido contra alterações. Não movimenta o mesmo estoque novamente. |
| **CANCELADA** | Pedido encerrado antes do envio. Se estava aprovado, libera a reserva; o estoque físico permanece igual. |

Caminhos de cancelamento permitidos: **SOLICITADA → CANCELADA** e **APROVADA → CANCELADA**, por administrador.

A partir de **EM_TRANSITO**, não é permitido simplesmente cancelar. Uma ocorrência nessa etapa exige tratamento que preserve os saldos e o histórico; o procedimento detalhado não foi fechado na V1.

### Operações integrais e ocorrências

A aprovação, o envio e o recebimento devem abranger todos os itens e as quantidades solicitadas. A V1 não permite aprovação, envio ou recebimento parcial. Se qualquer item falhar na validação, a operação inteira deve ser recusada, preservando status, saldos e histórico anteriores.

Perda de mercadoria ou divergência no recebimento não devem ser registradas como recebimento integral nem resolvidas por cancelamento simples. O fluxo de resolução dessas ocorrências permanece pendente de definição; a V1 ainda não oferece um procedimento para encerrá-las.

### Exemplo de saldos em cada etapa

Transferência de 7 notebooks do Centro para a Barra. Inicialmente, Centro tem 10 físicos, sem reservas, e Barra tem zero.

| Etapa | Centro: físico | Centro: reservado | Centro: disponível | Barra: físico |
|---|---:|---:|---:|---:|
| SOLICITADA | 10 | 0 | 10 | 0 |
| APROVADA | 10 | 7 | 3 | 0 |
| EM_TRANSITO | 3 | 0 | 3 | 0 |
| RECEBIDA | 3 | 0 | 3 | 7 |
| CONCLUIDA | 3 | 0 | 3 | 7 |

Durante o transporte, as 7 unidades estão vinculadas à transferência, fora do estoque físico das duas lojas.

## 6. Regras de negócio RN01–RN21

| ID | Regra fechada |
|---|---|
| **RN01** | Um produto pode possuir estoque em várias lojas. |
| **RN02** | A quantidade disponível corresponde à quantidade física menos a quantidade reservada. |
| **RN03** | Uma transferência deve possuir lojas de origem e destino diferentes. |
| **RN04** | Não pode ser solicitada ou aprovada quantidade superior ao estoque disponível. |
| **RN05** | Quando uma transferência for aprovada, sua quantidade deverá ser reservada na loja de origem. |
| **RN06** | Quando a mercadoria entrar em trânsito, a quantidade deverá sair fisicamente do estoque da origem. |
| **RN07** | O estoque da loja destino somente deverá aumentar após confirmação do recebimento. |
| **RN08** | Transferências concluídas não poderão ser alteradas. |
| **RN09** | Uma transferência duplicada será sempre uma nova transferência e deverá passar novamente pelas validações. |
| **RN10** | O CPF do cliente deve ser único e não poderá ser alterado após o cadastro. |
| **RN11** | Produtos que possuam histórico não deverão ser excluídos fisicamente; deverão ser inativados. |
| **RN12** | Clientes deverão ser inativados em vez de excluídos quando houver histórico associado. |
| **RN13** | Toda alteração de estoque deverá gerar uma movimentação rastreável. |
| **RN14** | Cada colaborador deverá estar associado a uma loja. |
| **RN15** | Somente administradores poderão aprovar transferências. |
| **RN16** | Envio só poderá ser registrado por colaborador da loja de origem, e recebimento por colaborador da loja de destino. |
| **RN17** | Somente administradores poderão concluir transferências, e apenas a partir de RECEBIDA, sem nova movimentação de estoque. |
| **RN18** | Entradas e ajustes manuais só poderão ser registrados por administradores, com motivo obrigatório no histórico. |
| **RN19** | Aprovação, envio e recebimento deverão ser integrais: todos os itens e quantidades, sem atualização parcial. |
| **RN20** | A inativação de um produto deverá ser bloqueada enquanto ele participar de transferência SOLICITADA, APROVADA, EM_TRANSITO ou RECEBIDA. |
| **RN21** | Baixo estoque corresponde a disponível igual a 1 por produto e loja; disponível igual a 0 corresponde a sem estoque disponível. |

**Cuidados para aplicar essas regras:**

- Quantidades dos itens devem ser inteiras e maiores que zero.
- A verificação do estoque e sua reserva devem ocorrer como uma operação indivisível: outra aprovação não pode usar as mesmas unidades no intervalo entre verificar e reservar.
- Uma consulta anterior não garante saldo na aprovação. O sistema deve verificar todos os itens novamente, considerando o total por produto.
- Reserva, liberação de reserva, saída e entrada precisam deixar histórico rastreável. A modelagem aprovada registra essas alterações com RESERVA e LIBERACAO_RESERVA.
- Repetir a confirmação de envio ou recebimento não pode repetir a movimentação de estoque.
- Inativar um cadastro preserva os vínculos e o histórico. Produtos só podem ser inativados quando todas as suas transferências estiverem CONCLUIDAS ou CANCELADAS.

Esses cuidados explicam como manter as regras consistentes; não acrescentam novos módulos à V1.

## 7. Requisitos não funcionais

| Área | ID | Requisito |
|---|---|---|
| Segurança | RNF01 | Senhas não poderão ser armazenadas em texto puro. |
| Segurança | RNF02 | As operações deverão respeitar as permissões do usuário. |
| Integridade | RNF03 | O sistema não poderá permitir estoque disponível negativo. |
| Integridade | RNF04 | Operações críticas de estoque deverão manter consistência mesmo em caso de erro. |
| Usabilidade | RNF05 | A pesquisa de produtos deverá permitir busca simples por nome ou código. |
| Usabilidade | RNF06 | Ações importantes deverão mostrar confirmação de sucesso ou mensagem de erro compreensível. |
| Rastreabilidade | RNF07 | Movimentações deverão manter data, produto, quantidade, loja e origem da movimentação. |
| Desempenho | RNF08 | Pesquisas comuns de produtos deverão responder em até 2 segundos em condições normais de utilização da V1. |


## 8. Riscos e cenários de falha

| Cenário | O que precisa ser observado |
|---|---|
| Dois usuários comprometem o mesmo saldo ao mesmo tempo. | A reserva deve impedir aprovação acima do disponível. |
| Falha no meio da aprovação, envio ou recebimento. | Status, saldos e histórico não podem ficar parcialmente atualizados. |
| Recebimento ou envio registrado duas vezes. | O estoque deve mudar somente uma vez. |
| Transferência duplicada sem saldo atual. | A nova solicitação deve passar pelas validações e ser impedida se faltar estoque. |
| Usuário tenta aprovar sem permissão. | Operação recusada, sem mudança de status ou saldo. |
| CPF duplicado ou tentativa de alteração do CPF. | Cadastro duplicado ou alteração devem ser impedidos. |
| Origem igual ao destino; quantidade zero ou negativa. | Solicitação recusada com mensagem clara. |
| Cancelamento após aprovação, antes do envio. | Reserva liberada e físico preservado. |
| Cancelamento após envio. | Cancelamento simples bloqueado. |
| Tentativa de inativar produto durante uma transferência aberta. | Bloquear a inativação e preservar cadastro e vínculos. |
| Um item tem saldo e outro não, na mesma transferência. | Recusar a aprovação inteira, sem reservar nenhum item. |
| Colaborador de outra loja tenta registrar envio ou recebimento. | Recusar a ação, inclusive se o colaborador for administrador. |
| Tentativa de envio ou recebimento parcial. | Recusar a operação, sem alterar status, saldos ou histórico. |
| Entrada ou ajuste manual sem motivo. | Recusar a operação, sem alterar estoque. |

### Destaque: estoque 5 e duas transferências simultâneas de 4

**Situação inicial:** físico 5, reservado 0, disponível 5. Dois usuários criam solicitações de 4 unidades do mesmo produto na mesma origem.

As solicitações podem existir ao mesmo tempo, pois **SOLICITADA não reserva saldo**. Porém, **as duas não podem ser aprovadas**.

| Momento | Físico | Reservado | Disponível | Resultado |
|---|---:|---:|---:|---|
| Antes das aprovações | 5 | 0 | 5 | Duas solicitações de 4 aguardam aprovação. |
| Uma aprovação é efetivada | 5 | 4 | 1 | Uma transferência fica APROVADA. |
| A outra tenta ser aprovada | 5 | 4 | 1 | Aprovação recusada: 4 é maior que 1. |

Não importa qual pedido vence a disputa. O resultado aceitável é **uma aprovação e uma recusa por falta de saldo**. A solicitação recusada na aprovação não deve criar reserva e pode permanecer SOLICITADA; não foi definido um status REJEITADA.

**Falha que queremos evitar:** os dois processos consultam 5 disponíveis, ambos aprovam 4 e comprometem 8 unidades de um estoque de 5. A proteção precisa estar na operação que aprova e reserva, inclusive quando as ações são simultâneas.

## 9. Itens fora da V1

- Integração com e-commerce e sistemas externos das franquias.
- Notificações.
- Dashboards avançados, previsão de estoque e inteligência artificial.
- Aplicativo mobile.
- Microserviços.
- Implantação em AWS e uso de SQS, Lambda ou Redis nesta etapa.

A arquitetura inicial acordada para a futura implementação é **React → API REST em NestJS → PostgreSQL**, com **Docker Compose** para frontend, backend e banco. Este documento apenas registra a base; não implementa essa arquitetura.

### 10.1. Teste unitário — uma regra isolada

**O que é:** testa uma pequena parte do sistema, como uma regra de cálculo ou de negócio, isoladamente.

**Exemplo A — calcular disponível:**

- Situação inicial: físico 10 e reservado 3.
- Ação: calcular o disponível.
- Resultado esperado: 7, conforme RN02.

**Exemplo B — verificar saldo suficiente:**

- Situação inicial: disponível 10.
- Ação: avaliar uma transferência de 15.
- Resultado esperado: recusar a quantidade por falta de saldo, conforme RN04.
- Comparação: uma quantidade de 10 deve passar pela regra de saldo; zero e valores negativos devem falhar na regra de quantidade.

**Como demonstrar:** escreva os valores e apresente o cálculo ou a decisão esperada. Na implementação futura, essa regra será verificada isoladamente, sem precisar executar o fluxo completo das telas.

**Como falar à professora:** “Aqui eu testo somente uma regra. Com 10 unidades físicas e 3 reservadas, devem sobrar 7 disponíveis. Também verifico se a regra recusa uma quantidade maior que o saldo.”

### 10.2. Teste de integração — partes trabalhando juntas

**O que é:** verifica se componentes conectados trabalham corretamente, como transferência, estoque e histórico no banco.

**Exemplo — confirmar recebimento:**

- Situação inicial: transferência de 4 unidades EM_TRANSITO; origem já teve a saída registrada; destino possui 2 unidades.
- Ação: confirmar o recebimento.
- Resultado esperado: status RECEBIDA, destino com 6 unidades e uma movimentação TRANSFERENCIA_ENTRADA vinculada à transferência. A origem não sofre outra saída.
- Ação adicional: repetir a mesma confirmação.
- Resultado esperado: destino continua com 6; não existe uma segunda entrada.

**Como demonstrar:** compare o status, o saldo e o histórico antes e depois. No sistema futuro, execute a operação integrada ao banco e confira os registros. Simule também uma falha ao gravar: os dados não devem ficar parcialmente atualizados.

**Como falar à professora:** “Não basta mudar o status na tela. Eu verifico se a transferência, o estoque e o histórico funcionam juntos. Ao receber 4 unidades, o destino passa de 2 para 6, e repetir a confirmação não pode somar mais 4.”

### 10.3. Teste de validação — atender ao requisito acordado

**O que é:** verifica se o comportamento entregue atende à necessidade e às regras definidas. Aqui, validação significa conferir o requisito, não apenas o formato de um campo.

**Exemplo — CPF único e imutável, RN10:**

- Situação inicial: cliente cadastrado com um CPF de teste.
- Ação: tentar alterar esse CPF.
- Resultado esperado: alteração impedida e CPF original preservado.
- Ação adicional: cadastrar outro cliente com o mesmo CPF.
- Resultado esperado: cadastro duplicado impedido, com mensagem compreensível.
- Comparação: editar um dado permitido, como telefone, deve funcionar.

**Como demonstrar:** apresente RN10 e compare cada ação ao comportamento esperado. Quando houver sistema, realize as tentativas no cadastro e confira os dados salvos.

**Como falar à professora:** “Estou validando se o sistema entrega o que foi combinado. O requisito diz que o CPF é único e não muda depois do cadastro, então tento violar essas duas condições e espero que o sistema impeça.”

### 10.4. Teste de sistema — o fluxo completo

**O que é:** testa o sistema inteiro pela perspectiva de seus usuários.

**Exemplo — transferência do início ao fim:**

1. Preparar origem com 10 unidades, sem reservas, e destino com 2.
2. Entrar como funcionário e pesquisar o produto.
3. Solicitar transferência de 4 unidades.
4. Entrar como administrador e aprovar: origem fica com físico 10, reservado 4 e disponível 6.
5. Registrar envio: origem fica com físico 6, reservado 0 e disponível 6; destino continua com 2.
6. Confirmar recebimento: destino passa para 6.
7. Concluir: saldos permanecem iguais e a transferência fica protegida contra alteração.
8. Conferir histórico e relatórios: saída na origem, entrada no destino e vínculo com a transferência.

**Como demonstrar:** percorra os passos como uma história de uso, mostrando os saldos da tabela ou, futuramente, as telas e registros do sistema.

**Como falar à professora:** “Aqui eu acompanho o processo inteiro: login, busca, solicitação, aprovação, envio e recebimento. Quero verificar se um funcionário consegue resolver o problema da loja e se os estoques terminam corretos.”

### 10.5. Demonstração especial — concorrência

Este cenário pode ser verificado em integração e no sistema completo.

- Preparar físico 5, reservado 0 e duas solicitações de 4.
- Tentar aprová-las simultaneamente, em duas sessões ou com um teste que dispare as duas operações juntas.
- Conferir que apenas uma foi aprovada, a reserva total é 4 e o disponível é 1.
- Conferir que a tentativa recusada não deixou reserva nem atualização parcial.

Uma explicação com cartões ou tabela ajuda na aula; clicar em sequência ilustra a revalidação, mas **não comprova proteção contra concorrência real**. Para isso, as operações precisam disputar o saldo ao mesmo tempo na futura implementação.

**Como falar à professora:** “Se existem 5 unidades e dois pedidos de 4, os dois podem ter visto o mesmo saldo antes. Na aprovação, só um pode reservar 4. O outro encontra apenas 1 disponível e deve ser impedido. Isso evita prometer 8 unidades quando só existem 5.”

### 10.6. Outros casos curtos para mencionar

| Caso | Resultado esperado |
|---|---|
| Solicitar origem igual ao destino. | Bloquear a solicitação — RN03. |
| Funcionário tentar aprovar. | Negar a operação — RN15. |
| Cancelar uma transferência aprovada de 4, com físico 5. | Reserva volta a 0; físico continua 5; disponível volta a 5. |
| Duplicar transferência antiga sem saldo atual. | Criar somente após confirmação e validações; não executar automaticamente — RN09. |
| Alterar transferência concluída. | Impedir a alteração — RN08. |
| Inativar produto com histórico. | Preservar o cadastro e os vínculos históricos — RN11. |
| Pesquisar por nome ou código. | Encontrar o produto e apresentar estoque por loja; medir o tempo conforme RNF08. |
| Inativar produto com transferência aberta. | Bloquear a inativação — RN20. |
| Consultar baixo estoque com físico 5 e reservado 4. | Incluir o produto nessa loja, pois disponível é 1 — RN21. |
| Consultar baixo estoque com disponível 0. | Não incluir em baixo estoque; incluir em sem estoque disponível — RN21. |
| Colaborador de outra loja tentar registrar envio. | Recusar a operação — RN16. |
| Registrar entrada manual sem motivo. | Recusar a operação — RN18. |

**Ideia central para a apresentação:** um requisito gera vários casos de teste. Além do caminho esperado, é preciso pensar em falta de estoque, cancelamento, repetição, falhas e ações simultâneas.

## 11. Base para a próxima etapa

O Passo 1 está fechado: objetivo, escopo, entidades, permissões, regras, riscos e critérios de qualidade.

O próximo passo é **Modelagem de Dados**, aproveitando os cadastros já estudados e acrescentando lojas, estoques por unidade, transferências, itens e movimentações. Fornecedores foram mencionados na modelagem anterior, mas não receberam um fluxo próprio neste escopo fechado.

As decisões operacionais do Passo 1 foram aprovadas: envio pela loja de origem, recebimento pela loja de destino, conclusão por administrador, entradas e ajustes manuais por administrador com motivo, operações integrais, bloqueio de inativação durante transferências abertas e baixo estoque com disponível igual a 1.

Permanecem pendentes o procedimento de resolução de ocorrências após envio e as condições do teste de desempenho. A [modelagem aprovada](MODELAGEM-DE-DADOS-V1.md) define os históricos, quantidades inteiras, sem campo de unidade de medida. Uma caixa é cadastrada como produto próprio e contada por unidade. No cliente, nome, CPF, telefone, e-mail e endereço principal são obrigatórios; complemento é opcional.
