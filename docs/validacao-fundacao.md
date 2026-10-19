# Validação da fundação — 16/10/2026

Escopo: conferir a implementação existente antes de começar o MVP. Produto: Nexus CRM. Repositório GitHub: CodeX.

**Resultado do checkpoint inicial:** a fundação passou nos testes e na integração real descritos abaixo. **A árvore atual não está integralmente validada:** novos módulos, migrações e mudanças de configuração surgiram durante as verificações. Os resultados do checkpoint não devem ser atribuídos automaticamente a essas adições.

## Ambiente confirmado

| Componente | Versão / estado |
| --- | --- |
| Java | OpenJDK 21.0.12.1, x86_64 |
| Maven local / Wrapper | 3.9.16 / 3.10.0 |
| Node / npm via NVM | 24.22.0 / 11.19.1 |
| Angular / CLI / Material | 22.2.3 |
| TypeScript | 6.0.3 |
| Spring Boot | 4.1.1 |
| Docker Engine / Compose | 29.9.1 / 5.6.0, serviço ativo |
| Oracle | Imagem `gvenzl/oracle-free:23.26.3-slim-faststart`, container saudável |

O Node do sistema é 12.22.9 e não atende ao Angular instalado. O `.nvmrc` indica a versão correta; carregue o NVM e execute `nvm use` antes dos comandos npm.

## Docker, Oracle e configuração

- Os pacotes Docker já estavam instalados; não foi necessário instalar novamente ou mudar grupos do usuário.
- `docker compose --env-file .env -f infra/docker-compose.yml config --quiet` passou.
- O container existente `nexus-crm-oracle` está saudável. A porta 1521 está publicada somente em `127.0.0.1`.
- A PDB `FREEPDB1` está em `READ WRITE`; a conta `NEXUS_APP` está aberta. A aplicação usa essa conta, não `SYS` ou `SYSTEM`.
- O `.env` existente está ignorado pelo Git, com permissão `600`, sem marcadores de exemplo e com senhas distintas. Seu conteúdo e as credenciais do banco foram preservados.
- O volume existente `infra_oracle-data` foi preservado. Não foram executados `down -v`, remoção de volumes, redefinição de senhas ou recriação do banco.

## Backend, migração e testes

`cd backend && ./mvnw -B -Ddebug=false verify` passou, executando **23 testes**, com zero falhas, erros ou testes ignorados. O parâmetro `debug=false` evita o logging de diagnóstico ativado pela variável externa `DEBUG` deste ambiente; ele não altera as credenciais ou a validação do banco.

O build gerou `backend/target/nexus-crm-0.0.1-SNAPSHOT.jar`. O relatório local detalhado fica em `backend/target/surefire-reports/` e é ignorado pelo Git.

Os testes usam o Oracle real e um servidor HTTP em porta aleatória:

- Conexão Oracle com conta de aplicação e validação Flyway sem migrações pendentes ou falhas; V1 aplicada.
- Tabela `ORGANIZATIONS` válida e trigger `TRG_ORGANIZATIONS_UPDATED_AT` válida e habilitada.
- Geração de identidade, status e datas padrão; atualização da data pelo trigger e preservação da data de criação.
- Rejeição de código duplicado, status inválido e ausência dos campos obrigatórios `CODE`, `NAME` e `STATUS`.
- `GET /actuator/health` público, JSON com apenas `status`; oito rotas adicionais e seis métodos HTTP diferentes de GET bloqueados com 403.

Os testes que inserem dados usam transações com rollback. A sequência de identidade pode avançar mesmo com rollback, comportamento normal do Oracle. Não foram criados registros permanentes de demonstração.

A migração V1 existente não foi modificada nem reaplicada à força. A inicialização valida seu checksum e reutiliza o esquema na versão 1. `ddl-auto=validate` está habilitado; como ainda não há entidades JPA, as verificações de `ORGANIZATIONS` são feitas explicitamente com JDBC.

Foram corrigidas duas configurações locais: o backend escuta no loopback por padrão e as probes foram desabilitadas para que o health público exponha somente o status agregado. As rotas de probes já eram bloqueadas pela política de segurança.

O mesmo JAR foi iniciado e respondeu `{"status":"UP"}` em `127.0.0.1:18080`. Nesta máquina, conexões a `8080` eram encerradas antes de alcançar o Spring, mesmo com o processo escutando. O comportamento foi reproduzido com curl e Python; a mesma aplicação em `18080` funcionou. Backend e proxy passaram a usar `18080` por padrão, sem alterar regras de rede do sistema. Esse teste isola o problema à porta/caminho local; ele não determina, por si só, a causa do encerramento em `8080`.

## Frontend e build

- `npm test -- --watch=false`: **3 testes passaram**, em dois arquivos.
- `npm run build`: passou, com bundle inicial de aproximadamente 322,49 kB e sem avisos de orçamento.
- O `package.json` e o lock declaram Node `>=24.15.0 <25`, compatível com as dependências Angular existentes. As versões das dependências foram preservadas.
- O build de produção desabilita o inlining de fontes remotas (`optimization.fonts.inline=false`), eliminando a necessidade de consultar Google Fonts durante a compilação. Posteriormente, os links de fontes também foram removidos do HTML por alterações que surgiram durante a validação.
- A tarefa de build frontend no VS Code carrega o NVM e a versão do `.nvmrc`.

O proxy `/api` pertence ao servidor de desenvolvimento; uma implantação de produção precisará configurar seu próprio encaminhamento para o backend.

## Integração real do checkpoint

O Chrome headless acessou `127.0.0.1:4200` e confirmou a tela **Conectada**, com HTTP 200 pelo proxy e resposta `{"status":"UP"}`. Ao parar somente o backend, clicar em **Atualizar** exibiu **Sem conexão**, com HTTP 502. Após reiniciar o JAR, a tela voltou a **Conectada**. Não foram usados dados simulados. O Oracle permaneceu em execução.

Foi preservada uma cópia temporária do JAR aprovado e do relatório de testes em `/tmp/nexus-crm-foundation-validated-8xcdqzqk/`. O JAR contém apenas a migração V1 e as duas classes da fundação; SHA-256: `bce04acc396fa44a8563cc724b211d69db3765cb47e296c5b98812de54a23aa5`. Esse diretório é temporário e não substitui versionamento.

## Alterações encontradas durante a validação

Entre aproximadamente 17h38 e 17h44 (America/Sao_Paulo), apareceram arquivos que não foram criados por esta equipe de validação: autenticação, administração, clientes, comercial, chamados, tarefas, processos, relatórios/dashboard, testes adicionais, migrações V2 e V3, scripts operacionais, Dockerfiles e configurações Nginx. Também mudaram `pom.xml`, `application.properties`, `SecurityConfiguration`, inicialização do frontend e HTML. Os arquivos foram preservados. O usuário informou não ter outro agente ou editor conhecido trabalhando no projeto.

A segurança atual passou a permitir autenticação e probes, com respostas diferentes das esperadas pela suíte inicial. Portanto, os **23 testes aprovados pertencem ao checkpoint da fundação**, não ao conjunto atual de módulos. Os primeiros problemas de dependências e beans observados mudaram durante a leitura e não devem ser tratados como falhas atuais sem nova verificação.

Uma compilação posterior do frontend passou com aproximadamente 325,69 kB e aviso de orçamento CSS do shell; novos arquivos surgiram durante esse build, impedindo atribuir seu resultado a um estado final estável.

Esta validação não iniciou o backend com V2/V3 nem executou scripts novos de instalação, backup ou restauração. Na última consulta somente leitura, o Oracle já registrava V1 e V2 com sucesso, demonstrando que o banco também mudou além do checkpoint. Não é correto afirmar que o banco atual permanece somente na V1.

Python foi usado nesta validação para diagnósticos locais. Os novos scripts operacionais também o utilizam para configuração e backups. O Nginx novo serve o Angular compilado e encaminha `/api` no cenário de implantação; não foi iniciado por esta validação. Nenhum dos dois substitui o backend Java ou o frontend Angular, e o desenvolvimento local validado usa o servidor Angular.

## Limites para a próxima etapa

O checkpoint inicial não implementa login, autorização por organização, entidades JPA de negócio ou módulos do CRM. A tabela inicial não fornece isolamento multiempresa por si só. A árvore atual contém novas implementações desses recursos, que precisam ser revisadas e testadas em um estado estável, reconciliando o contrato de segurança e as migrações antes de considerar o MVP validado.

O Git local ainda não tem commits nem remoto configurado. Os arquivos existentes foram preservados; esta validação não criou commit nem configurou um remoto por inferência.
