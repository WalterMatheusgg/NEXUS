#!/usr/bin/env bash
set +x
set -euo pipefail
umask 077
. "$(dirname -- "$0")/common.sh"
oracle_container=nexus-crm-oracle
attachments_dir="$NEXUS_ROOT/var/attachments"
backend_container=''
writes_paused=false
while (($#)); do
  case "$1" in
    --writes-paused) writes_paused=true; shift ;;
    --oracle-container) oracle_container="${2:?Informe o container}"; shift 2 ;;
    --attachments-dir) attachments_dir="${2:?Informe o diretório}"; shift 2 ;;
    --backend-container) backend_container="${2:?Informe o container}"; shift 2 ;;
    *) fail 'Uso: ./scripts/backup.sh --writes-paused [--attachments-dir DIR | --backend-container NAME] [--oracle-container NAME]' ;;
  esac
done
[[ "$writes_paused" == true ]] || fail 'Pause as gravações e o backend; depois informe --writes-paused. Consulte docs/backup-restauracao.md.'
[[ "$oracle_container" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]+$ ]] || fail 'Nome de container inválido.'
[[ -z "$backend_container" || "$backend_container" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]+$ ]] || fail 'Nome de container inválido.'
require_command python3
require_command tar
require_command gzip
require_command sha256sum
require_command flock
use_docker
mkdir -p "$NEXUS_ROOT/var"
exec 9>"$NEXUS_ROOT/var/backup.lock"
flock -n 9 || fail 'Outra operação de backup/restauração está em andamento.'
[[ "$("${NEXUS_DOCKER[@]}" inspect --format '{{.State.Health.Status}}' "$oracle_container")" == healthy ]] || fail 'Oracle não está saudável.'
schema="$("${NEXUS_DOCKER[@]}" exec "$oracle_container" printenv APP_USER)"
oracle_image="$("${NEXUS_DOCKER[@]}" inspect --format '{{.Config.Image}}' "$oracle_container")"
[[ "$schema" =~ ^[A-Z][A-Z0-9_]{0,29}$ ]] || fail 'APP_USER inválido.'
backup_id="nexus-$(date -u +%Y%m%dT%H%M%SZ)-$RANDOM"
backup_root="${NEXUS_BACKUP_ROOT:-$NEXUS_ROOT/var/backups}"
bundle="$backup_root/$backup_id"
mkdir -p "$backup_root"
mkdir -m 700 "$bundle"
if [[ -n "$backend_container" ]]; then
  # docker cp also supports a stopped backend; archive is streamed to this user's file.
  "${NEXUS_DOCKER[@]}" cp "${backend_container}:/app/var/attachments/." - | gzip -c >"$bundle/attachments.tar.gz"
else
  [[ -d "$attachments_dir" ]] || fail "Diretório de anexos ausente: $attachments_dir. Crie-o vazio se ainda não houve anexos."
  tar -C "$attachments_dir" -czf "$bundle/attachments.tar.gz" .
fi
"${NEXUS_DOCKER[@]}" exec -i --user oracle "$oracle_container" bash -s -- export "$backup_id" <"$NEXUS_ROOT/scripts/oracle-datapump.sh"
remote_dir="/opt/oracle/oradata/nexus-datapump/$backup_id"
"${NEXUS_DOCKER[@]}" exec --user oracle "$oracle_container" cat "$remote_dir/nexus.dmp" >"$bundle/oracle.dmp"
"${NEXUS_DOCKER[@]}" exec --user oracle "$oracle_container" cat "$remote_dir/export.log" >"$bundle/export.log"
python3 - "$bundle" "$backup_id" "$schema" "$oracle_container" "$oracle_image" <<'PY'
from datetime import datetime, timezone
from pathlib import Path
import json
import sys
directory, backup_id, schema, container, image = sys.argv[1:]
Path(directory, 'manifest.json').write_text(json.dumps({
    'formatVersion': 1, 'id': backup_id, 'schema': schema,
    'createdAt': datetime.now(timezone.utc).isoformat(),
    'oracleImage': image,
    'sourceContainer': container, 'writesPausedByOperator': True,
    'scope': 'application schema plus attachments; excludes credentials, wallet and instance configuration'
}, indent=2) + '\n')
PY
(cd "$bundle" && sha256sum oracle.dmp attachments.tar.gz manifest.json >SHA256SUMS)
python3 "$NEXUS_ROOT/scripts/verify-backup.py" "$bundle"
printf 'Backup completo: %s\nCopie-o para armazenamento externo criptografado e valide a restauração antes de retomá-lo como garantia.\n' "$bundle"
