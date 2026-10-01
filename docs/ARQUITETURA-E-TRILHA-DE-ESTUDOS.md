# Arquitetura e Trilha de Estudos — Sistema de Varejo

## 1. Objetivo deste documento

Este documento organiza as tecnologias que serão usadas no projeto e o motivo de cada uma.

A ideia é desenvolver o sistema aos poucos e, em cada etapa, estudar os conceitos antes ou durante a implementação.

**Foco de aprendizado aprovado:** Roger já conhece SQL. O desenvolvimento usará esse conhecimento; o estudo se concentrará em testes automatizados, AWS e Cloud. Testes acompanham cada funcionalidade relevante desde o início.

> Regra do projeto: nenhuma tecnologia será adicionada apenas para aparecer no currículo. Cada ferramenta deve resolver um problema real do sistema.

---

## 2. Arquitetura inicial da V1

A primeira versão será propositalmente simples:

```text
React
  |
  | HTTP / REST
  v
NestJS
  |
  v
PostgreSQL
```

Durante o desenvolvimento local:

```text
Docker Compose
|-- Frontend React
|-- Backend NestJS
`-- PostgreSQL
```

Nesta fase ainda não serão necessários Lambda, SQS, Redis ou microserviços.

---

## 3. Tecnologias principais

### React + TypeScript — Frontend

Responsável pelas telas utilizadas por funcionários e administradores.

Exemplos:

- Login;
- busca de produtos;
- estoque por loja;
- cadastro de clientes;
- solicitação de transferência;
- acompanhamento das transferências;
- relatórios simples.

### NestJS + TypeScript — Backend

Será a API principal e concentrará as regras de negócio.

Exemplos:

- validar estoque disponível;
- criar transferências;
- reservar mercadorias;
- controlar os status das transferências;
- validar permissões;
- registrar movimentações de estoque.

Organização inicial sugerida:

```text
src/
|-- auth/
|-- usuarios/
|-- clientes/
|-- produtos/
|-- lojas/
|-- estoques/
|-- transferencias/
`-- movimentacoes/
```

### PostgreSQL — Banco de dados

Armazenará os dados persistentes do sistema.

Principais informações:

- produtos;
- lojas;
- estoque por loja;
- clientes;
- colaboradores;
- transferências;
- itens das transferências;
- histórico de movimentações.

Também será importante para estudar transações, integridade e concorrência.

### Docker e Docker Compose — Ambiente local

Serão usados para executar os componentes do projeto de maneira reproduzível.

Objetivos de estudo:

- imagens e containers;
- Dockerfile;
- volumes;
- networks;
- portas;
- variáveis de ambiente;
- comunicação entre containers.

---

## 4. Qualidade e testes

Os testes serão adicionados desde as primeiras regras importantes.

### Testes unitários

Testam uma regra isoladamente.

Exemplo:

```text
Estoque disponível: 5
Quantidade solicitada: 8
Resultado esperado: transferência recusada
```

### Testes de integração

Verificam se diferentes partes trabalham corretamente juntas.

Exemplo:

```text
Confirmar recebimento
        |
        +--> estoque do destino atualizado uma única vez
        +--> status RECEBIDA e histórico registrados
        `--> falha em qualquer gravação desfaz a operação
```

### Testes de validação

Verificam se o sistema atende aos requisitos definidos.

Exemplo:

```text
Requisito: CPF não pode ser alterado após cadastro.
Teste: tentar modificar o CPF.
Resultado esperado: operação recusada.
```

### Testes de sistema

Testam um fluxo completo do ponto de vista do usuário.

Exemplo:

```text
Login
  -> buscar produto
  -> visualizar estoque
  -> solicitar transferência
  -> aprovar
  -> enviar
  -> receber
  -> verificar estoque final
```

---

## 5. CI/CD

Depois que aplicação e testes estiverem funcionando, será criado um pipeline.

Inicialmente:

```text
git push
   |
   v
GitHub Actions
   |
   +--> lint
   +--> testes
   +--> build
   `--> build da imagem Docker
```

O objetivo inicial não será fazer deploy, mas automatizar a validação do projeto.

Conceitos para estudar:

- Continuous Integration;
- Continuous Delivery/Deployment;
- pipelines;
- jobs;
- stages;
- artifacts;
- secrets;
- automação de testes.

---

## 6. Cloud local sem custo — LocalStack

Antes de utilizar AWS real, alguns serviços serão estudados localmente com LocalStack.

```text
Projeto
   |
   v
LocalStack
   |
   +-- SQS
   +-- S3
   `-- Lambda
```

Isso permite experimentar conceitos da AWS sem manter infraestrutura paga funcionando na internet.

---

## 7. SQS — Mensageria

SQS será introduzido somente quando houver necessidade de processamento assíncrono.

Exemplo futuro:

```text
Transferência aprovada
        |
        v
      NestJS
        |
        | evento
        v
       SQS
        |
        v
      Worker
```

Possíveis eventos:

```text
TRANSFERENCIA_SOLICITADA
TRANSFERENCIA_APROVADA
TRANSFERENCIA_RECEBIDA
TRANSFERENCIA_CANCELADA
```

Conceitos para estudar:

- mensageria;
- processamento síncrono x assíncrono;
- producer;
- consumer;
- filas;
- retry;
- Dead Letter Queue (DLQ);
- idempotência;
- consistência eventual.

---

## 8. S3 — Armazenamento de objetos

S3 poderá ser usado posteriormente para arquivos e relatórios.

Exemplo:

```text
Administrador
      |
      v
Gerar relatório
      |
      v
    NestJS
      |
      v
     S3
```

Conceitos para estudar:

- buckets;
- objetos;
- upload e download;
- permissões;
- URLs;
- armazenamento de arquivos.

---

## 9. Lambda — Serverless

O backend principal continuará sendo NestJS.

Lambda será utilizado somente para pequenas tarefas específicas ou orientadas a eventos.

Exemplo:

```text
SQS
 |
 v
Lambda
 |
 v
Processamento secundário
```

Conceitos para estudar:

- serverless;
- funções;
- triggers;
- stateless;
- timeout;
- concorrência;
- escalabilidade;
- logs.

---

## 10. Observabilidade

Quando o sistema possuir vários componentes, será necessário entender o que está acontecendo em cada um deles.

Na AWS, o principal serviço estudado será o CloudWatch.

```text
NestJS -----+
Lambda -----+--> CloudWatch
SQS --------+
```

Conceitos:

- logs;
- métricas;
- monitoramento;
- alarmes;
- observabilidade.

---

## 11. Terraform — Infrastructure as Code

Depois de entender os serviços individualmente, a infraestrutura passará a ser definida por código.

Exemplo:

```text
infra/
|-- main.tf
|-- variables.tf
|-- sqs.tf
|-- s3.tf
`-- lambda.tf
```

Fluxo:

```text
Terraform
   |
   v
Infraestrutura
   |
   +-- SQS
   +-- S3
   `-- Lambda
```

Primeiro poderá ser utilizado com LocalStack e posteriormente com AWS real.

Conceitos para estudar:

- Infrastructure as Code (IaC);
- providers;
- resources;
- variables;
- outputs;
- state;
- terraform plan;
- terraform apply;
- terraform destroy.

---

## 12. AWS real

A AWS real entra depois que os conceitos principais já tiverem sido praticados localmente.

O objetivo será criar laboratórios pequenos e controlados, evitando manter recursos desnecessários ativos.

Alguns conceitos que fazem mais sentido estudar no ambiente real:

- IAM;
- roles e policies;
- princípio do menor privilégio;
- VPC;
- Security Groups;
- CloudWatch;
- deploy;
- custos e billing.

A infraestrutura poderá ser criada para estudo e destruída depois dos testes.

```text
terraform apply
      |
      v
Laboratório AWS
      |
   testes
      |
      v
terraform destroy
```

---

## 13. Possível arquitetura futura

O projeto pode chegar futuramente a algo parecido com:

```text
                    React
                      |
                      v
                    NestJS
                  /    |    \
                 /     |     \
                v      v      v
          PostgreSQL   SQS     S3
                       |
                       v
                    Worker
                       |
                       +--> Lambda

                 CloudWatch
                      ^
                      |
                logs e métricas

                  Terraform
                      |
                      v
                infraestrutura
```

Essa não é a arquitetura inicial. Ela representa uma possível evolução conforme novas necessidades forem estudadas.

---

## 14. Ordem de implementação e estudo

### Etapa 1 — Fundamentos do sistema

- [x] Revisar requisitos e regras de negócio
- [x] Modelar banco de dados — aprovado em MODELAGEM-DE-DADOS-V1.md
- [x] Criar projeto NestJS — base local configurada
- [x] Configurar PostgreSQL — Docker Compose e conexão verificados
- [ ] Implementar API REST
- [ ] Implementar autenticação e permissões
- [ ] Criar frontend React

**Aplicar:** REST, módulos, DTOs, validação, autenticação, autorização e o conhecimento existente de SQL.

**Foco de estudo:** testes desde as primeiras regras e integração com PostgreSQL; não é necessária uma introdução a SQL.

### Etapa 2 — Qualidade

Esta etapa acompanha a implementação da Etapa 1, em vez de começar somente depois da aplicação pronta. Priorizar testes de transação, concorrência, repetição de envio/recebimento e permissões.

- [ ] Testes unitários
- [ ] Testes de integração
- [ ] Testes de validação
- [ ] Testes dos fluxos principais
- [ ] Testar cenários de erro e concorrência

**Estudar:** pirâmide de testes, casos de teste, mocks, integração, cobertura e critérios de aceite.

### Etapa 3 — Docker

- [ ] Dockerizar backend
- [ ] Dockerizar frontend
- [ ] PostgreSQL em container
- [ ] Criar Docker Compose

**Estudar:** containers, imagens, volumes, networks e configuração por ambiente.

### Etapa 4 — CI/CD

- [ ] Pipeline de testes
- [ ] Pipeline de build
- [ ] Build automático da imagem Docker

**Estudar:** CI/CD, pipelines, jobs, artifacts e secrets.

### Etapa 5 — Cloud local

- [ ] Adicionar LocalStack
- [ ] Criar SQS local
- [ ] Criar producer e consumer
- [ ] Implementar retry/DLQ
- [ ] Experimentar S3
- [ ] Experimentar Lambda

**Estudar:** cloud, mensageria, eventos, object storage e serverless.

### Etapa 6 — Infrastructure as Code

- [ ] Introduzir Terraform
- [ ] Criar recursos locais por código
- [ ] Versionar infraestrutura

**Estudar:** IaC e gerenciamento de infraestrutura.

### Etapa 7 — AWS real

- [ ] Criar laboratório controlado
- [ ] Configurar IAM
- [ ] Testar SQS/S3/Lambda reais
- [ ] Estudar CloudWatch
- [ ] Criar infraestrutura com Terraform
- [ ] Destruir recursos após os testes quando apropriado

**Estudar:** segurança, observabilidade, networking, deploy e custos.

---

## 15. Regra de aprendizado

Para cada tecnologia nova, seguir este processo:

```text
1. Qual problema temos?
        |
        v
2. Qual conceito resolve esse problema?
        |
        v
3. Estudar o conceito
        |
        v
4. Implementar no projeto
        |
        v
5. Testar cenários normais e de falha
        |
        v
6. Conseguir explicar por que usamos aquilo
```

O objetivo final não é apenas dizer que o projeto utiliza AWS, SQS, Lambda ou Terraform.

O objetivo é conseguir explicar qual problema existia, por que determinada tecnologia foi escolhida, como foi implementada e quais riscos ou vantagens ela trouxe.
