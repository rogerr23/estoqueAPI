# Publicar a demonstração no Render

O `Dockerfile` da raiz compila React e Java e inclui a interface no JAR.
Um Web Service atende interface, API e Swagger no mesmo endereço HTTPS,
preservando cookies de sessão e CSRF. O PostgreSQL é um serviço separado.
Os Dockerfiles de `backend/` e `frontend/` continuam destinados ao Compose local.

## Publicação

1. Envie os arquivos desta configuração ao repositório GitHub
   `https://github.com/rogerr23/estoqueAPI`. Não envie `.env` nem senhas.
2. Entre em https://dashboard.render.com e escolha **New > Blueprint**.
3. Conecte o repositório e selecione a branch que contém `render.yaml`.
4. Informe `DEMO_EMAIL` e `DEMO_PASSWORD` para criar o login da demonstração.
   Escolha credenciais exclusivas para esse ambiente.
5. Confira os dois recursos: Web Service Free e PostgreSQL Free. O Blueprint
   foi configurado para planos gratuitos; confira o resumo antes de criar.
6. Aplique o Blueprint e aguarde o banco e o serviço ficarem disponíveis.
7. Abra o endereço HTTPS mostrado no Web Service e faça login com os valores
   definidos no passo 4. Esse é o link para compartilhar com os visitantes.

Não use apenas `backend/Dockerfile`: ele publica a API sem a interface.
Não é necessário preencher comandos de build/start: o Dockerfile faz isso.
As variáveis de conexão são obtidas automaticamente do banco pelo Blueprint,
e o Flyway cria as tabelas. O banco local não é enviado para o Render.

O perfil `dev` é habilitado deliberadamente junto com `render` para carregar
usuário, lojas, produtos e saldos iniciais da demonstração. A carga é idempotente:
reiniciar não restaura saldos já alterados. Mudar `DEMO_PASSWORD` depois da criação
também não altera a senha salva do usuário existente.

## Como as pessoas usam

Os visitantes precisam somente de navegador, internet, link e login; não precisam
instalar Java, Docker ou PostgreSQL. Após entrar, podem consultar estoque,
cadastrar produtos, transferir e ver o histórico. Em **API**, podem abrir Swagger.
Uma chamada direta pelo Postman exige cookies de sessão e token CSRF, como no README.

O MVP não tem cadastro público nem separação de dados por visitante. Quem recebe
o mesmo login atua como o mesmo funcionário e modifica o mesmo estoque. Mesmo com
contas diferentes, as lojas e os produtos são compartilhados. Use dados fictícios.
Há controle transacional para transferências simultâneas, mas não foi realizado
teste de carga que determine quantos usuários esta instância suporta.

## Limites do plano gratuito

- O Web Service suspende após 15 minutos sem tráfego; a primeira visita seguinte
  pode esperar cerca de um minuto pelo reinício.
- O PostgreSQL Free expira 30 dias após a criação. Para manter a demonstração
  disponível depois disso, providencie outro banco ou migre para um plano pago
  antes da expiração. Não trate esse plano como armazenamento permanente.
- A sessão fica na memória: reiniciar, suspender ou publicar uma versão exige
  novo login. Os dados ficam no PostgreSQL e dependem da disponibilidade dele.
- CPU e memória são limitadas. Para uso contínuo ou mais acessos, dimensione
  serviço e banco com base em medições e configure backups.

Referências oficiais: https://render.com/docs/free,
https://render.com/docs/docker e https://render.com/docs/blueprint-spec.

## Conferência após publicar

Abra o link em janela anônima: a tela de login deve carregar e
`/api/produtos` deve retornar 401 antes de autenticar. Entre, consulte lojas e
estoque, faça uma transferência de demonstração e confira os saldos e histórico.
Abra Swagger pela interface e confirme que consultas funcionam com a mesma sessão.
Teste também o logout. Caso o deploy falhe, veja **Logs** no serviço do Render.
