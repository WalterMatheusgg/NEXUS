# Backup e restauração

O Oracle em volume Docker persiste dados entre reinicializações. Um volume no mesmo disco não é backup. O bundle produzido pelos scripts contém exportação lógica Data Pump do schema da aplicação, anexos e checksums; não inclui `.env`, configuração da instância, wallets, senhas externas nem recuperação ponto a ponto por redo/RMAN.

**Os scripts não foram executados sobre o banco atual durante a implementação.** Validação sintática e checksums não comprovam restaurabilidade; registre um ensaio completo em ambiente descartável antes de confiar no procedimento para uma empresa.

## Preparação

1. Defina responsável, janela, RPO/RTO e retenção conforme o contrato da empresa. Como ponto de partida operacional sujeito a aprovação: backup diário, cópia externa criptografada e ensaio mensal. Não são garantias já oferecidas pela aplicação.
2. Reserve espaço para dump, arquivo de anexos e cópia externa. Identifique a conta/schema; os scripts exigem `APP_USER` simples em maiúsculas, como `NEXUS_APP`.
3. Suspenda gravações e pare o backend antes do backup. Isso inclui scheduler, e-mails e anexos. Não basta avisar usuários para não clicar; não execute dois backends contra o schema durante a janela.
4. Preserve a versão de código e imagem que gerou o bundle. Verifique a integridade do `.env` em cofre separado. Não coloque segredos no bundle ou no Git.

No desenvolvimento com terminais, interrompa o backend com `Ctrl+C`. Em Compose, pare apenas o serviço backend, usando exatamente os mesmos arquivos e projeto empregados ao iniciá-lo:

```bash
sudo docker compose -p infra --env-file .env \
  -f infra/docker-compose.yml -f infra/docker-compose.mail.yml \
  -f infra/docker-compose.fullstack.yml --profile dev stop backend
```

O banco permanece em execução. `FLASHBACK_TIME=SYSTIMESTAMP` gera uma visão consistente para o dump; a pausa de gravações mantém também os arquivos de anexos coerentes com ele. Não confundir esse mecanismo com backup físico da instância.

## Gerar um bundle

Na raiz do projeto, para anexos do backend executado no host:

```bash
mkdir -p var/attachments
./scripts/backup.sh --writes-paused --attachments-dir "$PWD/var/attachments"
```

Se `NEXUS_STORAGE_DIR` foi alterado, informe o diretório real. O script não tenta inferir segredos ou executar `.env` como shell.

Para o backend em container, já parado:

```bash
./scripts/backup.sh --writes-paused --backend-container infra-backend-1
```

Confira o nome real com `sudo docker compose -p infra -f infra/docker-compose.yml -f infra/docker-compose.fullstack.yml ps -a`. O nome padrão é consequência do projeto `infra`; não o presuma se sua implantação usa outro projeto. O script usa `docker cp` em streaming, que funciona também com container parado.

O destino padrão é `var/backups/nexus-<data UTC>-<identificador>/`, com permissão restrita. Para outro disco, defina `NEXUS_BACKUP_ROOT=/diretorio/privado`. O script:

- Obtém autenticação pelo ambiente **já existente no container**. A senha entra em `expdp` por stdin, sem argumento de processo ou arquivo de parâmetros. Não use `bash -x`, `set -x`, nem `docker inspect` sem filtros.
- Usa autenticação local do sistema operacional como SYSDBA somente para criar um `DIRECTORY` e conceder acesso temporário a ele; exporta como a própria conta da aplicação, sem executar Data Pump como SYSDBA. O diretório Oracle é removido ao fim, revogando seu acesso.
- Não cria usuários, não muda senhas, não altera tabelas da aplicação e não substitui o volume.
- Verifica SHA-256 e rejeita links, caminhos absolutos e `..` no arquivo de anexos. Uma falha deixa um bundle incompleto para diagnóstico; só bundles que passam em `verify-backup.py` devem ser copiados como backup concluído.

O dump e os logs intermediários permanecem em `/opt/oracle/oradata/nexus-datapump/<id>` dentro do container. Eles contêm dados pessoais e consomem disco. Depois de confirmar a cópia externa e a restauração, o operador deve remover somente o diretório correspondente, durante a manutenção; não automatizamos uma exclusão do volume principal. Logs de Data Pump e `export.log` devem ser tratados como confidenciais.

O formato Data Pump usado aqui não cifra o dump. Use armazenamento externo cifrado com controle de acesso, ou uma ferramenta de cifragem com senha solicitada interativamente, por exemplo:

```bash
tar -C var/backups -czf - NOME_EXATO_DO_BUNDLE | gpg --symmetric --cipher-algo AES256 --output /midia/privada/nexus-backup.tar.gz.gpg
```

Não passe a senha em `--passphrase` e não grave a chave junto com o backup. O comando é um exemplo manual: substitua os caminhos e valide a instalação do GnuPG. Checksums detectam corrupção; não provam autoria de um backup obtido de terceiros.

## Ensaio em Oracle separado

Suba um destino **novo**, na porta `1522`, com projeto e volume próprios:

```bash
sudo docker compose -p nexus-restore --env-file .env \
  -f infra/docker-compose.restore.yml config --quiet
sudo docker compose -p nexus-restore --env-file .env \
  -f infra/docker-compose.restore.yml up -d --wait
```

Esse arquivo não monta `infra_oracle-data`. Se o volume `nexus-restore_oracle-restore-data` já existe e contém um ensaio anterior, o script recusa a importação em vez de apagar objetos. Revise esse destino separado antes de recriá-lo; não use `down -v` no projeto `infra`.

Verifique primeiro o plano, sem alterações:

```bash
./scripts/restore.sh var/backups/NOME_EXATO_DO_BUNDLE \
  --target-container nexus-crm-oracle-restore \
  --attachments-dir "$PWD/var/attachments-restore"
```

Após conferir os caminhos e o destino, execute a importação explicitamente:

```bash
./scripts/restore.sh var/backups/NOME_EXATO_DO_BUNDLE \
  --target-container nexus-crm-oracle-restore \
  --attachments-dir "$PWD/var/attachments-restore" \
  --confirm-empty-target
```

O script recusa o container `nexus-crm-oracle`, qualquer destino que monte `infra_oracle-data`, schemas com objetos, schema de nome diferente e diretório de anexos não vazio. Não usa `TABLE_EXISTS_ACTION=REPLACE`, `DROP USER` ou truncamento. A importação mantém os privilégios e a conta criados no destino; não importa senhas e privilégios do usuário original. Se falhar, preserve logs e diagnostique o banco de ensaio, que pode ter objetos parciais. Não aponte o script para o banco principal como tentativa de correção.

A extração de anexos limita o conteúdo total a 20 GiB e 100 mil entradas por padrão. Para um bundle legítimo maior, revise o arquivo e configure `NEXUS_RESTORE_MAX_BYTES`; extrações recusam links e arquivos especiais. Não restaure bundles não confiáveis.

## Validar a restauração

Com a aplicação normal parada, inicie uma instância isolada do backend com variáveis exportadas **não sensíveis** sobrescrevendo apenas o destino, storage e porta:

```bash
ORACLE_JDBC_URL=jdbc:oracle:thin:@127.0.0.1:1522/FREEPDB1 \
NEXUS_STORAGE_DIR="$PWD/var/attachments-restore" \
SERVER_PORT=18081 ./scripts/dev.sh backend
```

O `.env` continua fornecendo credenciais; use o mesmo schema/credenciais no destino ou uma configuração privada específica. Mantenha disparos de e-mail para Mailpit durante o ensaio. O Flyway deve validar a versão sem tentar reparar checksums ou recriar a estrutura.

Confira `/actuator/health/readiness`, contagem de registros por organização, login de uma conta conhecida, bloqueio entre organizações, ao menos um download de anexo comparado ao original, integridade de históricos e perfis. Uma validação somente de saúde é insuficiente. Para validar pela UI, configure temporariamente o proxy Angular para a porta de ensaio, ou use uma instância de homologação com sua configuração própria; não confunda essa instância com o banco principal.

Registre data, responsável, código/imagens usados, tamanho, hashes, duração de backup/importação e evidências da verificação funcional, sem dados pessoais. Só então retome os serviços normais e marque o ensaio aprovado. A promoção de um banco restaurado para principal é uma operação planejada com janela, validação e possibilidade de reversão; este script não faz essa promoção.

## Referências

- [Oracle Data Pump Export](https://docs.oracle.com/en/database/oracle/oracle-database/21/sutil/oracle-data-pump-export-utility.html): usuário sem senha na linha de comando e prompt de autenticação.
- [Oracle Data Pump parameters](https://docs.oracle.com/en/database/oracle/oracle-database/26/sutil/parameters-available-command-line-mode-export-oracle-data-pump.html): diretórios, schema export e flashback.
- [Imagem Oracle Free](https://github.com/gvenzl/oci-oracle-free): variáveis de inicialização, usuários e persistência.
