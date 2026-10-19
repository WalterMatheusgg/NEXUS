#!/usr/bin/env bash
# Shared helpers; do not source .env as executable shell code.
set -euo pipefail
NEXUS_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

fail() { printf 'Erro: %s\n' "$*" >&2; exit 1; }
require_command() { command -v "$1" >/dev/null 2>&1 || fail "Instale $1 antes de continuar."; }
require_env() {
  [[ -f "$NEXUS_ROOT/.env" ]] || fail 'Crie .env com ./scripts/init-env.sh em uma instalação nova; preserve um .env existente.'
  require_command python3
  python3 - "$NEXUS_ROOT/.env" <<'PY'
from pathlib import Path
import stat
import sys
p = Path(sys.argv[1])
if stat.S_IMODE(p.stat().st_mode) != 0o600:
    raise SystemExit('Permissão insegura: execute chmod 600 .env.')
values = {}
for line in p.read_text().splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        key, value = line.split('=', 1)
        values[key.strip()] = value.strip()
for key in ('ORACLE_SYS_PASSWORD', 'ORACLE_APP_USER', 'ORACLE_APP_PASSWORD', 'ORACLE_JDBC_URL'):
    if not values.get(key) or values[key].startswith('replace-with-'):
        raise SystemExit(f'Configure {key}; nenhum valor sensível foi exibido.')
if values.get('NEXUS_SETUP_TOKEN', '').startswith('replace-with-'):
    raise SystemExit('Substitua o marcador NEXUS_SETUP_TOKEN; nenhum valor foi exibido.')
if values['ORACLE_SYS_PASSWORD'] == values['ORACLE_APP_PASSWORD']:
    raise SystemExit('As senhas Oracle administrativa e da aplicação devem ser distintas.')
PY
}

use_java() {
  require_command java
  local actual
  actual="$(java -version 2>&1 | head -n 1)"
  [[ "$actual" =~ \"21[\.-] ]] || fail "Java 21 é obrigatório. Ajuste JAVA_HOME/PATH. Encontrado: $actual"
  if [[ -n "${JAVA_HOME:-}" ]]; then
    [[ -x "$JAVA_HOME/bin/java" ]] || fail 'JAVA_HOME não aponta para um JDK executável.'
    actual="$("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1)"
    [[ "$actual" =~ \"21[\.-] ]] || fail 'JAVA_HOME também precisa selecionar Java 21; Maven usa esse caminho antes do PATH.'
  fi
}

use_node() {
  local nvm_script="${NVM_DIR:-$HOME/.nvm}/nvm.sh"
  if [[ -s "$nvm_script" ]]; then
    set +u
    # shellcheck source=/dev/null
    . "$nvm_script"
    nvm use --silent "$(cat "$NEXUS_ROOT/.nvmrc")"
    set -u
  fi
  require_command node
  require_command npm
  [[ "$(node --version)" == "v$(cat "$NEXUS_ROOT/.nvmrc")" ]] || fail "Use a versão de .nvmrc: carregue o NVM e execute nvm install e nvm use na raiz."
}

use_docker() {
  require_command docker
  if docker info >/dev/null 2>&1; then
    NEXUS_DOCKER=(docker)
  else
    require_command sudo
    NEXUS_DOCKER=(sudo docker)
  fi
  "${NEXUS_DOCKER[@]}" compose version >/dev/null
}

compose_base() {
  "${NEXUS_DOCKER[@]}" compose -p infra --env-file "$NEXUS_ROOT/.env" -f "$NEXUS_ROOT/infra/docker-compose.yml" "$@"
}

compose_all() {
  "${NEXUS_DOCKER[@]}" compose -p infra --env-file "$NEXUS_ROOT/.env" \
    -f "$NEXUS_ROOT/infra/docker-compose.yml" \
    -f "$NEXUS_ROOT/infra/docker-compose.mail.yml" \
    -f "$NEXUS_ROOT/infra/docker-compose.fullstack.yml" --profile dev "$@"
}
