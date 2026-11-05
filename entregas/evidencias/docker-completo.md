# Execução completa em Docker

Validação em 03/10/2026: build multi-stage do backend e frontend passou;
PostgreSQL, backend e frontend saudáveis com `up -d --build --wait`.
Acesso pelo navegador na porta 8090: login sintético, estoque inicial 20/12,
cadastro DOCKER-CHECK com sessão/CSRF e Swagger com endpoints carregados.
Após `down` e `up -d --wait`, DOCKER-CHECK permaneceu no banco, comprovando
persistência no volume. Produto e funcionário temporários removidos ao terminar.
Serviços de validação encerrados com down; volume mantido. Ambiente original
5173/8080 e banco de desenvolvimento não foram alterados.

PDF renderizado e três páginas inspecionadas sem cortes/sobreposições. ZIP
verificado quanto à integridade e ausência de .env, .git e artefatos de build.
Docker validado no Mac desta máquina; instalação Windows/Linux descrita com
links oficiais, não executada em máquinas desses sistemas.
