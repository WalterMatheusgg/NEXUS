# Operação local e diagnóstico

## Subir os serviços

Execute sempre na raiz do projeto. Preserve o `.env` existente.

```bash
./scripts/doctor.sh
./scripts/up.sh db
./scripts/up.sh mail
```

Em terminais separados:

```bash
./scripts/dev.sh backend
```

```bash
./scripts/dev.sh frontend
```

Aplicação: `http://127.0.0.1:4200`; API direta: `http://127.0.0.1:18080`; Mailpit: `http://127.0.0.1:8025`. Use o mesmo host em todo o fluxo para os cookies. O modo full-stack usa `./scripts/up.sh all` e aplicação em `http://127.0.0.1:8081`; evite operar dois backends sobre o mesmo banco durante backup/manutenção.

## Diagnóstico sem revelar segredos

```bash
./scripts/health.sh
./scripts/health.sh http://127.0.0.1:4200/api
sudo docker compose -p infra --env-file .env -f infra/docker-compose.yml ps
sudo docker compose -p infra --env-file .env -f infra/docker-compose.yml logs --tail=100 oracle
```

Use `config --quiet`, não `config` sem opção: a saída expandida pode conter senhas. Não compartilhe `.env`, saída completa de `docker inspect`, cookies do navegador, tokens de reset ou corpos de login. Antes de anexar logs a uma issue, remova dados pessoais e credenciais. Erros HTTP têm `status`, `title`, `detail`, `instance` e, quando aplicável, `errors` por campo.

| Sintoma | Conferência e ação |
| --- | --- |
| Node incompatível | `./scripts/dev.sh frontend` ativa NVM. Em comandos manuais, carregue `~/.nvm/nvm.sh` e execute `nvm use` na raiz. |
| Maven não encontra Java 21 | `java -version`; configure `JAVA_HOME` e `PATH` para seu JDK 21. |
| Oracle inicializando | Aguarde `healthy`; veja logs e espaço em disco, sem apagar volume. |
| ORA-01017 após editar `.env` | Variáveis de inicialização não redefinem senha de um banco existente. Restaure a configuração anterior ou faça rotação DBA controlada. |
| Flyway checksum mismatch | Verifique histórico de migration e versão do código. Não edite migration aplicada e não execute `repair` por tentativa. |
| HTTP 401 | Sessão ausente, expirada ou invalidada; entre novamente. Reinícios invalidam sessões em memória. |
| HTTP 403 ao gravar | Confira permissão e CSRF. Obtenha token novo após login e use cookie/header corretos. Não desabilite CSRF. |
| HTTP 409 ao salvar | Registro alterado por outro usuário ou unicidade violada. Recarregue e revise os campos. |
| HTTP 429 em login/reset | Aguarde a janela do limitador. Investigue se há repetição indevida; não desative o limitador como correção permanente. |
| Recuperação não chega | Confira Mailpit/configuração SMTP. A fila Oracle é consultada a cada 10 s e tenta novamente com espera progressiva; após oito tentativas, exige investigação operacional. Saúde da API não monitora entrega SMTP. A mensagem na UI é genérica para impedir enumeração de contas. |
| Setup indisponível | Confira `/auth/status`; setup só provisiona a instalação inicial e exige token local forte. Não recrie organização apagando tabelas. |
| Cookie Secure em HTTP | Use TLS ou configure `NEXUS_COOKIE_SECURE=false` somente no desenvolvimento local. |
| Falha em upload | Confira limite de 10 MiB, formato permitido, assinatura e permissão/capacidade do storage. |
| Interface abre mas `/api` falha | Verifique saúde direta, destino do proxy e backend. O build estático não inclui o proxy do `ng serve`. |
| Porta 8080 encerra conexão nesta máquina | O desenvolvimento usa 18080; altere proxy junto com `SERVER_PORT` se necessário. |

## Parar, preservar e recuperar

No desenvolvimento, use `Ctrl+C` em cada terminal. Parar/remover o container Oracle preservando volume:

```bash
sudo docker compose -p infra --env-file .env -f infra/docker-compose.yml down
```

No modo full-stack, use os mesmos arquivos usados no `up`, para encerrar também aplicação e Mailpit:

```bash
sudo docker compose -p infra --env-file .env \
  -f infra/docker-compose.yml -f infra/docker-compose.mail.yml \
  -f infra/docker-compose.fullstack.yml --profile dev down
```

Se usou TLS, inclua também `-f infra/docker-compose.tls.yml`. **Não acrescente `-v`**: isso apaga os volumes e os dados. Backend e frontend podem ser reconstruídos; banco, anexos e segredos precisam de preservação e backup. Recuperação está em [backup-restauracao.md](backup-restauracao.md).

## Incidente de segurança

Restrinja acesso de rede e preserve evidências antes de alterações destrutivas. Revogue sessões/credenciais afetadas e chaves conforme a exposição, mantenha registro de ações e envolva responsáveis da empresa. Uma troca de senha administrativa Oracle não atualiza automaticamente os outros ambientes. Comunicação a titulares/autoridade, bases legais e prazos devem ser definidos pelo controlador com assessoria adequada; o sistema não automatiza nem garante conformidade legal.
