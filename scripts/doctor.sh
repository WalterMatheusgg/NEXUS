#!/usr/bin/env bash
set -euo pipefail
. "$(dirname -- "$0")/common.sh"
use_java
use_node
require_command git
require_command curl
require_command python3
java -version
node --version
npm --version
git --version
use_docker
"${NEXUS_DOCKER[@]}" version --format '{{.Server.Version}}'
"${NEXUS_DOCKER[@]}" compose version
require_env
compose_base config --quiet
python3 - "$NEXUS_ROOT/.env" <<'PY'
from pathlib import Path
import stat
import sys
p = Path(sys.argv[1])
values = {}
for line in p.read_text().splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        key, value = line.split('=', 1)
        values[key.strip()] = value.strip()
errors = []
if stat.S_IMODE(p.stat().st_mode) != 0o600:
    errors.append('Defina chmod 600 .env.')
for key in ('ORACLE_SYS_PASSWORD', 'ORACLE_APP_USER', 'ORACLE_APP_PASSWORD', 'ORACLE_JDBC_URL'):
    if not values.get(key) or values[key].startswith('replace-with-'):
        errors.append(f'Configure {key}; nenhum valor foi exibido.')
if values.get('NEXUS_SETUP_TOKEN', '').startswith('replace-with-'):
    errors.append('Substitua o marcador NEXUS_SETUP_TOKEN antes do primeiro provisionamento.')
if errors:
    raise SystemExit('\n'.join(errors))
print('Configuração local presente; valores sensíveis não exibidos.')
if not values.get('NEXUS_SETUP_TOKEN'):
    print('Setup sem token: desabilitado. Uma instalação já provisionada pode continuar funcionando.')
PY
