# Registro de validação da entrega

Este registro distingue verificações executadas de instruções para o operador. A [validação da fundação](validacao-fundacao.md) é histórica e não comprova autenticação ou os módulos posteriores.

## Infraestrutura e documentação

Compose, Dockerfiles, Nginx e scripts de backup/restauração foram preparados. Sua criação não iniciou containers, não substituiu o volume Oracle e não executou backup/importação. Build de imagens, TLS, SMTP e Data Pump precisam de resultados reais antes de receberem aprovação. Não houve implantação externa ou validação em produção.

## Evidências

Verificações executadas em 17/10/2026 no workspace local:

- O repositório Git não tem commits-base e apresenta os arquivos do projeto como não rastreados; por isso não foi possível verificar por diff uma lista anterior de 51 arquivos alterados.
- `./scripts/test.sh backend`: **44 testes aprovados** contra Oracle Free 23.26, sem falhas ou erros. Inclui 6 testes HTTP de autenticação/sessão/permissões/isolamento, 23 testes da fundação, 9 testes de módulos de negócio, 3 testes de sessão/senha e 3 de validação. Flyway validou as três migrations; schema `NEXUS_APP` permaneceu na versão 3.
- `./scripts/test.sh frontend`: **12 testes aprovados** em 5 arquivos.
- `./scripts/build.sh frontend`: build de produção Angular concluído.
- `./scripts/test.sh infra`: **7 testes aprovados**.
- Teste focado temporário de recuperação do administrador local: **1 aprovado**; arquivo auxiliar removido após restaurar a senha e liberar tentativas de login.
- `docker compose ... config --quiet`: configuração base e full-stack validadas sem iniciar nem recriar containers.
- Backend iniciado com Java 21 e conectado ao Oracle existente. Actuator reportou `UP`; chamadas anônimas a clientes receberam 401. Login com CSRF criou sessão; `/auth/me`, dashboard, clientes e chamados responderam 200. O hash de senha não aparece na resposta da API.
- Proxy padrão Angular em `http://127.0.0.1:4200/api` encaminhou login com CSRF e sessão; `/auth/me`, dashboard, clientes e chamados responderam 200 com dados do Oracle.
- Smoke test no navegador: login, dashboard, clientes e chamados abriram; dashboard exibiu contagens reais em zero e as listas mostraram estado vazio. Nenhum registro foi criado. O único 401 ocorreu antes do login ao verificar sessão anônima; não houve erros de console/rede após autenticar.
- Foi provisionada a organização local fictícia `NEXUS-DEMO-2026` e seu administrador inicial pelo setup protegido. Após a configuração, somente `NEXUS_SETUP_TOKEN` foi removido de `.env` (permissão 600 preservada); o backend reiniciado reportou `setupEnabled:false`. A senha não é registrada neste arquivo nem no repositório.

O ambiente foi validado localmente de ponta a ponta até a renderização das telas e leitura das APIs. Persistência de CRUD HTTP foi coberta pelos testes de integração Oracle; não houve criação de clientes ou chamados artificiais para preencher o dashboard.

| Área | Estado |
| --- | --- |
| Backend, migrations e Oracle | Verificado; 44 testes passaram e schema está na versão 3. |
| Angular | 12 testes e build de produção aprovados. |
| Autenticação HTTP | Login/CSRF/sessão e endpoints protegidos verificados; sem sessão, cliente retorna 401. |
| Isolamento e permissões | Cobertos pelos testes HTTP/domínio com Oracle, inclusive outra organização e perfil limitado. |
| Integração Angular/API | Proxy padrão e telas de login, dashboard, clientes e chamados verificadas no navegador. |
| Compose | Configuração base e full-stack válidas; imagens/serviços da aplicação não foram iniciados. |
| E-mail e recuperação | Fluxo de recuperação é coberto por teste; entrega SMTP real e uso do link no navegador não foram ensaiados. |
| HTTPS | Certificado, cookies Secure e reset sob TLS não foram ensaiados. |
| Backup/restauração | Procedimentos documentados; nenhum dump ou restore foi executado. |
| Produção | Sem implantação, teste de carga ou homologação. |

Os itens sem ensaio explícito permanecem pendentes e não são atestados pelo resultado dos testes automatizados.
