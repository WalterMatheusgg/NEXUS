#!/usr/bin/env bash
set +x
set -euo pipefail
umask 077
. "$(dirname -- "$0")/common.sh"
bundle="${1:?Uso: ./scripts/restore.sh BUNDLE --target-container NAME --attachments-dir DIR [--confirm-empty-target]}"
shift
target_container=''
attachments_dir=''
confirmed=false
while (($#)); do
  case "$1" in
    --target-container) target_container="${2:?Informe o container de teste}"; shift 2 ;;
    --attachments-dir) attachments_dir="${2:?Informe um diretório vazio}"; shift 2 ;;
    --confirm-empty-target) confirmed=true; shift ;;
    *) fail 'Opção inválida. Consulte docs/backup-restauracao.md.' ;;
  esac
done
[[ "$target_container" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]+$ ]] || fail 'Informe --target-container.'
[[ "$target_container" != nexus-crm-oracle ]] || fail 'Restauração no banco local principal é proibida por este script. Use o container de restauração separado.'
[[ -n "$attachments_dir" ]] || fail 'Informe --attachments-dir apontando para um diretório vazio separado.'
require_command python3
require_command flock
python3 "$NEXUS_ROOT/scripts/verify-backup.py" "$bundle"
if [[ "$confirmed" != true ]]; then
  printf 'Plano: importar schema em %s e extrair anexos em %s.\nNenhuma alteração executada. Confira o destino e acrescente --confirm-empty-target.\n' "$target_container" "$attachments_dir"
  exit 0
fi
use_docker
mkdir -p "$NEXUS_ROOT/var"
exec 9>"$NEXUS_ROOT/var/backup.lock"
flock -n 9 || fail 'Outra operação de backup/restauração está em andamento.'
[[ "$("${NEXUS_DOCKER[@]}" inspect --format '{{.State.Health.Status}}' "$target_container")" == healthy ]] || fail 'Oracle de destino não está saudável.'
# Refuse the original Oracle volume even when its container has been renamed.
mounts="$("${NEXUS_DOCKER[@]}" inspect --format '{{range .Mounts}}{{println .Name}}{{end}}' "$target_container")"
[[ "$mounts" != *infra_oracle-data* ]] || fail 'Destino monta o volume principal infra_oracle-data; operação recusada.'
backup_id="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["id"])' "$bundle/manifest.json")"
schema="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["schema"])' "$bundle/manifest.json")"
target_schema="$("${NEXUS_DOCKER[@]}" exec "$target_container" printenv APP_USER)"
[[ "$target_schema" == "$schema" ]] || fail 'APP_USER de destino difere do schema do backup. Remapeamento exige procedimento DBA próprio.'
"${NEXUS_DOCKER[@]}" exec -i --user oracle "$target_container" bash -s -- check "$backup_id" <"$NEXUS_ROOT/scripts/oracle-datapump.sh"
python3 "$NEXUS_ROOT/scripts/verify-backup.py" "$bundle" --extract-attachments "$attachments_dir"
"${NEXUS_DOCKER[@]}" exec -i --user oracle "$target_container" bash -c \
  'set -euo pipefail; umask 077; p="/opt/oracle/oradata/nexus-datapump/$1"; mkdir -p "$p"; test ! -e "$p/nexus.dmp"; cat > "$p/nexus.dmp"' \
  -- "$backup_id" <"$bundle/oracle.dmp"
"${NEXUS_DOCKER[@]}" exec -i --user oracle "$target_container" bash -s -- import "$backup_id" <"$NEXUS_ROOT/scripts/oracle-datapump.sh"
printf 'Importação concluída no destino separado. Valide Flyway, contagens, login, isolamento e anexos conforme o runbook. O banco principal não foi selecionado.\n'
