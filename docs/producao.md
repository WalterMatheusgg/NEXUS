# Implantação e limites operacionais

O repositório fornece empacotamento e configuração para homologação em uma única máquina. Não houve publicação, implantação externa, ensaio de carga ou restauração do banco atual como parte da criação desses arquivos. A presença de Dockerfiles não certifica prontidão para produção.

## Imagens e configuração

O backend usa build Java 21 com Maven Wrapper e runtime Java 21 como UID `10001`. O frontend usa Node fixado em `.nvmrc` para compilar Angular e Nginx como UID `101` para servir os arquivos. Build de imagem ignora testes que precisam de Oracle; execute a suíte separadamente antes de promover uma imagem.

Não copie `.env` para imagens. `.dockerignore` exclui segredos, runtime e dependências locais. A configuração full-stack repassa explicitamente apenas as variáveis necessárias ao backend, sem a senha administrativa do Oracle. Variáveis de ambiente ficam disponíveis para administradores Docker; uma implantação empresarial deve substituir esse armazenamento por um cofre/secret store com rotação. As tags de imagem estão versionadas; fixe também os digests após homologar e estabeleça atualização de dependências/imagens.

O Compose base permanece dedicado ao Oracle e ao volume atual. O override full-stack acrescenta aplicação e volume `attachment-data`, sem renomear o volume `infra_oracle-data`. Todos os comandos deste projeto usam `-p infra`. Alterar o nome do projeto pode selecionar outro volume e dar a impressão de perda de dados. Oracle mantém a configuração anterior; limites de recursos, retenção de logs e permissões do servidor de banco precisam de revisão específica antes de produção.

O frontend é a única porta da aplicação publicada, no loopback `8081`. O backend é acessível apenas na rede interna do Compose. A publicação loopback é intencional; exposição externa requer DNS, TLS, firewall e revisão de acesso, não apenas trocar `127.0.0.1` por `0.0.0.0`.

## Full stack local

```bash
./scripts/up.sh all
./scripts/health.sh http://127.0.0.1:8081/api
```

Esse comando compila as duas imagens, mantém Oracle no volume existente e inicia Mailpit de desenvolvimento. Reiniciar o backend invalida suas sessões em memória; os dados empresariais persistem no Oracle e os anexos no volume próprio.

Para homologação sem Mailpit, combine apenas os arquivos base e full-stack e configure SMTP externo em uma configuração privada. **Não execute esse exemplo com configurações padrão de e-mail:**

```bash
sudo docker compose -p infra --env-file .env.production \
  -f infra/docker-compose.yml -f infra/docker-compose.fullstack.yml \
  config --quiet
```

Antes do `up`, configure ao menos `NEXUS_DEPLOY_FRONTEND_URL`, `NEXUS_DEPLOY_SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH=true`, `SMTP_STARTTLS=true`, e `NEXUS_COOKIE_SECURE=true` para HTTPS. O `.env.production` não deve ir ao Git. O setup token é um segredo de provisionamento inicial; remova-o do ambiente depois de provisionar e reinicie o serviço de forma controlada. A aplicação só pode executar setup enquanto não houver organização provisionada.

## TLS local opcional

Para validar cookies `Secure` em HTTPS sem expor o sistema, use certificados de desenvolvimento confiáveis, como os gerados por [mkcert](https://github.com/FiloSottile/mkcert). Instale a ferramenta e a CA conforme o projeto oficial, fora deste script; a instalação de uma CA muda a confiança da máquina.

```bash
mkdir -p var/tls
mkcert -cert-file var/tls/localhost.pem -key-file var/tls/localhost-key.pem localhost 127.0.0.1
chmod 600 var/tls/localhost-key.pem
```

O Nginx executa como UID `101`. Em Linux, dê a ele leitura apenas da chave por ACL, mantendo o acesso dos outros usuários bloqueado:

```bash
setfacl -m u:101:r var/tls/localhost-key.pem
```

Use o override TLS após os demais. Ele monta somente certificados locais e configura o backend para cookies Secure e links HTTPS:

```bash
sudo docker compose -p infra --env-file .env \
  -f infra/docker-compose.yml -f infra/docker-compose.mail.yml \
  -f infra/docker-compose.fullstack.yml -f infra/docker-compose.tls.yml \
  --profile dev up -d --build --wait
```

Abra `https://localhost:8443`. A porta HTTP `8081` redireciona para HTTPS. Cookies de `127.0.0.1` e `localhost` têm escopos diferentes; finalize sessões antigas e mantenha o mesmo hostname durante o teste. O certificado local não serve para domínio público e sua chave é ignorada pelo Git. O healthcheck HTTP do frontend verifica o listener; a verificação de TLS e da API deve ser feita separadamente em `https://localhost:8443/api/actuator/health/readiness`.

Em produção, use certificado de uma CA adequada, renovação monitorada e domínio oficial. Substitua o arquivo de servidor TLS e seu redirecionamento fixo `:8443`; configure HSTS após validar HTTPS e a política de subdomínios. A aplicação precisa receber `X-Forwarded-Proto` verdadeiro do proxy confiável. O Nginx fornecido termina TLS diretamente e o calcula por `$scheme`; se colocar outro proxy à frente, ajuste a cadeia e a confiança explicitamente. Não aceite cabeçalhos de proxy arbitrários da Internet.

## Sessões e segurança de borda

- A aplicação foi desenhada inicialmente para uma instância. Sessões e rate limits locais não são compartilhados entre réplicas. Antes de escalar, use armazenamento de sessão central e limitador distribuído, além de testar invalidação e concorrência.
- `NEXUSSESSION` é HttpOnly/SameSite; `XSRF-TOKEN` deve ser legível para o Angular enviar `X-XSRF-TOKEN`. Não aplique HttpOnly a todos os cookies no proxy. Com HTTPS, configure cookies Secure no backend.
- O proxy preserva cookies e o header CSRF, retira `/api/` do caminho e limita upload HTTP a 11 MiB; a aplicação limita cada arquivo a 10 MiB. Não reescreva `/api` duas vezes.
- No container, Spring usa `SERVER_FORWARD_HEADERS_STRATEGY=framework` para reconhecer `/api` e a origem externa. Nginx substitui os cabeçalhos de encaminhamento e remove `Forwarded`/port/SSL enviados pelo cliente. O backend permanece sem porta publicada. Não ative essa confiança em um backend exposto diretamente.
- O CSP permite estilos inline necessários ao Material, e mantém scripts e conexões na própria origem. O Nginx não registra query strings, cookies, tokens ou corpos. Não habilite debug HTTP/SQL com bind values em produção.
- Inclua antivírus/quarentena, revisão de arquivos e bloqueios adicionais conforme o risco da empresa. Validação de extensão e assinatura não substitui análise de malware.

## Observabilidade, disponibilidade e dados

`/actuator/health` expõe somente saúde agregada. Liveness verifica a aplicação e readiness deve refletir a conexão do banco; detalhes e outros endpoints Actuator permanecem protegidos. Readiness não prova permissões, disponibilidade de SMTP, integridade do storage ou qualidade de um backup. Monitore esses itens separadamente com alertas e ensaios reais. `/healthz` do frontend verifica apenas Nginx.

Backend e frontend usam logs Docker com rotação de 5 arquivos de até 10 MiB. Mailpit tem rotação menor e mantém no máximo 200 mensagens/um dia; sua caixa pode conter links de redefinição e permanece somente no loopback. Revise retenção do Oracle e do Docker daemon, agregação, mascaramento, acesso e alertas. Configure observabilidade externa com correlação de requisições e métricas sem dados pessoais antes de assumir SLAs.

Implemente e ensaie o [runbook de backup](backup-restauracao.md), incluindo o volume de anexos, cópia externa cifrada e proteção das chaves. Não faça backup consistente copiando arquivos de um Oracle em execução. O armazenamento de anexos precisa de capacidade, monitoramento e política própria. Oracle Database Free tem limites de edição e capacidade que devem ser confrontados com a carga e os termos oficiais antes do uso empresarial.

## Liberação empresarial

Uma liberação deve registrar, no mínimo, resultado dos testes unitários e integração Oracle, isolamento entre empresas e permissões negativas; revisão de segredos/dependências, cookies/CSRF e upload; acessibilidade e fluxos completos no navegador; carga representativa; entrega e recuperação por e-mail; TLS/renovação; backup restaurado e RPO/RTO medidos; retenção e responsabilidades LGPD; plano de atualização/rollback.

Uma migration aplicada não deve ser editada para corrigir produção. Crie a próxima versão. Rollback de código que depende de schema novo exige compatibilidade planejada ou restauração coordenada; não use `flyway repair` para ocultar uma alteração de checksum. A implantação externa e publicação GitHub continuam operações separadas e não foram realizadas.

Referências de configuração: [Spring Boot atrás de proxy](https://docs.spring.io/spring-boot/how-to/webserver.html), [proxy Nginx](https://nginx.org/en/docs/http/ngx_http_proxy_module.html), [herança de headers Nginx](https://nginx.org/en/docs/http/ngx_http_headers_module.html) e [Mailpit Docker](https://mailpit.axllent.org/docs/install/docker/).
