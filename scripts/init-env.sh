#!/usr/bin/env bash
set -euo pipefail
. "$(dirname -- "$0")/common.sh"
require_command python3
python3 - "$NEXUS_ROOT" <<'PY'
from pathlib import Path
import os
import secrets
import sys

root = Path(sys.argv[1])
target = root / '.env'
replacements = {
    'replace-with-a-generated-secret': secrets.token_hex(32),
    'replace-with-a-different-generated-secret': secrets.token_hex(32),
    'replace-with-a-generated-setup-token': secrets.token_hex(32),
}
content = (root / '.env.example').read_text()
for marker, value in replacements.items():
    content = content.replace(marker, value)
try:
    fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
except FileExistsError:
    raise SystemExit('.env já existe: preservado. Não substitua as credenciais de um volume inicializado.')
with os.fdopen(fd, 'w') as stream:
    stream.write(content)
print('.env criado com permissão 600 e segredos aleatórios. Nenhum segredo foi impresso.')
PY
