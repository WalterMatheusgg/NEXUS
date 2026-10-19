# Nexus CRM

Aplicação Full Stack para gestão empresarial, com Java 21/Spring Boot 4, Angular 22 e persistência Oracle Database Free. Reúne autenticação, usuários/perfis, clientes, atendimento, dashboard, comercial, tarefas, processos e relatórios. Indicadores e listagens consultam a API e o banco real; não há dados comerciais inventados nem contas com senha fixa.

O projeto usa um monólito modular e um único repositório Git local. Nenhum script publica código ou configura remoto. A entrega local e os arquivos de implantação **não equivalem a homologação para produção**: resultados executados e pendências operacionais estão em [docs/validacao-entrega.md](docs/validacao-entrega.md).

## Tecnologias e estrutura

| Camada | Tecnologias |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.1, Web MVC, Data JPA/Hibernate, Security, Validation, Actuator, Mail, OpenAPI, JUnit/Mockito, Maven Wrapper 3.10.0. |
| Frontend | Angular/CLI/Material 22.2.3, TypeScript 6, standalone, formulários reativos, SCSS, Vitest. |
| Banco | Oracle Free `23.26.3-slim-faststart`, JDBC e Flyway; `ddl-auto=validate`, sem H2. |
| Operação | Docker/Compose, Nginx, Mailpit de desenvolvimento, scripts Bash/Python 3. |

```text
nexus-crm/
├── backend/       # API, segurança, módulos de negócio e migrations
├── frontend/      # Angular, funcionalidades, core e shared
├── infra/         # Oracle, imagens, proxy e overrides Compose
├── scripts/       # ambiente, execução, build, testes e backup/restauração
├── docs/          # arquitetura, API, operação e critérios de aceite
├── .vscode/       # recomendações e tarefas
├── .env.example   # somente marcadores/configuração não secreta
└── .nvmrc         # Node 24.22.0
```

## Preparação do ambiente

Use Linux, Bash, JDK 21, Git, Python 3, curl, NVM, Docker Engine e Docker Compose plugin. O desenvolvimento original ocorreu no Linux Mint 21.1, base Ubuntu jammy. O README não instala ferramentas administrativas automaticamente. Para uma nova máquina, siga as instruções oficiais do [Docker Ubuntu](https://docs.docker.com/engine/install/ubuntu/) e [NVM](https://github.com/nvm-sh/nvm#installing-and-updating); Mint precisa respeitar a base Ubuntu e não é distribuição oficialmente suportada pelo instalador Docker.

No terminal integrado do VS Code, a partir de `nexus-crm/`:

```bash
. "$HOME/.nvm/nvm.sh"
nvm install
nvm use
java -version
node --version
npm --version
git --version
sudo docker version
sudo docker compose version
```

Java deve ser 21; Node deve coincidir com `.nvmrc`. A máquina pode também ter Node antigo no sistema: os scripts ativam a versão NVM para impedir seu uso acidental. A CLI Angular é local ao projeto, sem necessidade de instalação global. Se Java estiver incorreto, ajuste `JAVA_HOME` e `PATH` para o JDK instalado; não copie caminhos específicos de outra máquina.

Recomendações VS Code: Java, Spring Boot e Angular Language Service. As tarefas compartilhadas executam os mesmos scripts deste README. A coleção [docs/api.http](docs/api.http) é opcional para a extensão REST Client; não inclui credenciais.

## Configuração local e segredos

**Nesta máquina, preserve o `.env` já existente e as credenciais do Oracle inicializado.** Para uma instalação nova, somente se `.env` ainda não existe:

```bash
./scripts/init-env.sh
```

O comando cria `.env` com modo `600`, senhas Oracle distintas e setup token aleatórios, sem exibi-los. Recusa substituir arquivo existente. Alternativamente, copie `.env.example`, restrinja permissões e substitua os três marcadores com segredos locais distintos. Não adicione `.env` ao Git.

```bash
chmod 600 .env
./scripts/doctor.sh
```

`doctor.sh` verifica ferramentas e presença da configuração sem imprimir valores sensíveis. Pode solicitar sudo para acesso ao Docker; associação ao grupo `docker` equivale a privilégios de root e não é alterada pelos scripts.

O Spring importa explicitamente `../.env` quando executado em `backend/`. Os scripts usam esse diretório. Variáveis já exportadas têm precedência. O Compose recebe o caminho explicitamente por `--env-file`; `.env` não é executado como script Bash. Segredos devem ser valores simples em uma linha; o gerador usa hexadecimal. Alterar senhas no arquivo **não redefine** usuários de um volume Oracle existente.

## Executar no VS Code

Na raiz, suba Oracle e a caixa SMTP de desenvolvimento:

```bash
./scripts/up.sh db
./scripts/up.sh mail
```

O Oracle publica `1521` somente em `127.0.0.1`, usa a PDB `FREEPDB1`, conta `NEXUS_APP` e volume persistente `infra_oracle-data`. Aguarde o healthcheck; primeiro boot pode levar minutos. Os scripts preservam o projeto Compose `infra` para reutilizar o volume atual.

Em um terminal:

```bash
./scripts/dev.sh backend
```

Em outro:

```bash
./scripts/dev.sh frontend
```

O frontend instala dependências com `npm ci` se necessário. Para reinstalação determinística após mudança de lockfile, rode `npm ci` em `frontend/` com NVM ativo. Não use `npm install` apenas para contornar incompatibilidade de versão.

- Aplicação: <http://127.0.0.1:4200>.
- API direta: <http://127.0.0.1:18080>.
- Mailpit: <http://127.0.0.1:8025> — captura e-mails locais, não envia para a Internet.

Angular encaminha `/api/*` para `127.0.0.1:18080/*`, removendo o prefixo. O build estático não inclui esse proxy; a imagem Nginx fornece o equivalente. Mantenha o mesmo hostname durante o uso, pois cookies de `localhost` e `127.0.0.1` são diferentes.

Valide a integração:

```bash
./scripts/health.sh
./scripts/health.sh http://127.0.0.1:4200/api
```

As probes devem indicar `UP`; as rotas de negócio exigem sessão e permissões. Saúde positiva não comprova os fluxos comerciais: siga os [critérios de aceite](docs/escopo-e-validacao.md).

## Primeiro acesso e permissões

Abra a aplicação. Se a instalação ainda não foi provisionada, use a tela de configuração inicial para informar código/nome da organização, nome/e-mail do administrador, senha e o `NEXUS_SETUP_TOKEN` local. Consulte o token **somente no editor privado de `.env`**, sem colá-lo em chats, screenshots, comandos ou logs. Senhas precisam ter no mínimo 12 caracteres e no máximo 72 bytes UTF-8.

Não existe usuário de demonstração. Setup cria a primeira organização e perfis Administrador, Gestor, Vendedor, Atendimento e Funcionário. Administrador inicia com todas as permissões e pode configurar os demais na administração. O código da organização faz parte do login e deve ter 3 a 50 letras/números, hífen ou sublinhado.

`GET /auth/status` informa se o setup é necessário/habilitado sem retornar o segredo. Setup repetido não reprovisiona a instalação. Após concluir, remova o token do ambiente em uma janela apropriada e reinicie o backend; mantenha a configuração privada de recuperação em cofre. A arquitetura considera várias organizações; cadastro público de empresas não faz parte desta versão.

Sessão usa cookie `NEXUSSESSION` HttpOnly/SameSite=Lax, expiração por inatividade de 30 minutos e até cinco sessões por usuário/instância. O Angular envia `X-XSRF-TOKEN` nas mutações; clientes externos devem obter `/auth/csrf` e manter cookie jar. Não desative CSRF em login ou setup. Reiniciar o backend invalida as sessões em memória.

Recuperação de senha retorna mensagem genérica e envia link temporário por SMTP. No ambiente local, leia-o no Mailpit; o link depende de `NEXUS_FRONTEND_URL`. Tokens expiram em 30 minutos por padrão e não podem ser reutilizados. Use a tela de perfil para mudança de senha. SMTP empresarial exige autenticação, TLS e configuração privada; a caixa Mailpit não deve ser exposta.

## Módulos e dados

Clientes PF/PJ têm validação de CPF/CNPJ, contatos e interações. Chamados mantêm histórico, comentários, responsáveis, prazos e anexos privados. Comercial inclui leads, conversão, oportunidades e funil; tarefas e processos mantêm responsáveis/estados. Dashboard e relatórios consultam dados reais da organização autenticada. CSV é a exportação inicial; PDF e tema escuro permanecem evoluções posteriores conforme o escopo original.

Anexos ficam em `var/attachments` no desenvolvimento, fora do frontend/Git, ou no volume próprio em containers. O limite por arquivo é 10 MiB, com PDF, PNG, JPEG, TXT e CSV permitidos conforme validação de conteúdo. Download exige autenticação e vínculo à empresa. Não trate validação de formato como análise de malware.

Todas as regras de empresa/permissão precisam ser impostas no backend; filtros da UI não oferecem isolamento. O banco também usa vínculos/constraints entre organizações. Inativação de cliente preserva histórico e não equivale a exclusão de todos os dados pessoais. Prazos de auditoria/notificações são configuráveis; retenção comercial, direitos do titular e backups exigem política específica, descrita em [privacidade.md](docs/privacidade.md).

## Testes e builds

Com Oracle saudável e `.env` configurado:

```bash
./scripts/test.sh
./scripts/build.sh
```

Também podem ser executados separadamente com `backend` ou `frontend`; `test.sh infra` verifica ferramentas auxiliares sem Oracle/Docker. `test.sh backend` executa `./mvnw -B verify` contra Oracle real; não há H2. A suíte pode criar registros de teste e avançar identities; rode testes apenas em ambiente de desenvolvimento/teste, nunca contra dados de produção. `test.sh frontend` executa Vitest sem watch. `build.sh` empacota Java sem repetir testes e gera o build Angular; ele não substitui a suíte.

Não declare a entrega validada apenas pelo build. Registre testes de autenticação, permissões negativas, duas organizações, CRUD e proxy em [validacao-entrega.md](docs/validacao-entrega.md). Swagger/OpenAPI exige sessão e pode ser desabilitado por configuração; contrato e chamadas estão em [api.md](docs/api.md).

## Containers, implantação e backup

Para executar a pilha local em containers, com o mesmo Oracle/volume:

```bash
./scripts/up.sh all
./scripts/health.sh http://127.0.0.1:8081/api
```

A aplicação fica em <http://127.0.0.1:8081>. O backend não publica porta do host; Nginx serve Angular e encaminha API. Anexos usam volume próprio, e segredos não entram nas imagens. Leia [producao.md](docs/producao.md) para SMTP externo, cookies Secure, TLS local opcional, logs, limites de instância e critérios de liberação. Não houve publicação externa ou configuração de GitHub.

Backup e restauração estão em [backup-restauracao.md](docs/backup-restauracao.md). Pause gravações para manter Oracle e anexos coerentes. O procedimento exporta Data Pump sem senha nos argumentos e exige destino separado/vazio na restauração. **Não foi executada restauração no banco atual.** Um procedimento escrito só vira garantia operacional após ensaio e evidências; mantenha cópia externa cifrada e segredos em cofre separado.

## Parar e solucionar problemas

No desenvolvimento, use `Ctrl+C` nos terminais. Para remover apenas o container Oracle e preservar o volume:

```bash
sudo docker compose -p infra --env-file .env -f infra/docker-compose.yml down
```

No full stack, use todos os arquivos utilizados no `up`, conforme [runbook.md](docs/runbook.md). **`down -v` apaga dados**; não o use no ambiente principal. Alterar `.env`, reinstalar dependências ou reconstruir aplicação não deve substituir o volume do banco.

Falhas de Node, portas, CSRF, sessão, SMTP, Flyway, upload e credenciais estão no [runbook](docs/runbook.md). Use `docker compose ... config --quiet`; a saída expandida de `config` pode revelar segredos.

## Documentação

- [Arquitetura e isolamento por organização](docs/arquitetura.md).
- [API e segurança HTTP](docs/api.md).
- [Escopo e critérios de aceite](docs/escopo-e-validacao.md).
- [Operação e diagnóstico](docs/runbook.md).
- [Empacotamento e implantação](docs/producao.md).
- [Backup e restauração](docs/backup-restauracao.md).
- [Privacidade e retenção](docs/privacidade.md).
- [Registro atual de validação](docs/validacao-entrega.md); [checkpoint histórico da fundação](docs/validacao-fundacao.md).

Referências: [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [Angular compatibility](https://angular.dev/reference/versions), [Oracle Free image](https://github.com/gvenzl/oci-oracle-free), [Mailpit](https://mailpit.axllent.org/docs/install/docker/), [Nginx proxy](https://nginx.org/en/docs/http/ngx_http_proxy_module.html).
