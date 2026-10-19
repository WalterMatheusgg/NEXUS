#!/usr/bin/env bash
# Runs INSIDE the Oracle container, as its oracle OS user. Passwords use stdin.
set +x
set -euo pipefail
umask 077
mode="${1:?export, import or check}"
backup_id="${2:?backup identifier}"
[[ "$mode" =~ ^(export|import|check)$ ]] || exit 2
[[ "$backup_id" =~ ^nexus-[0-9]{8}T[0-9]{6}Z-[0-9]+$ ]] || exit 2
[[ "${APP_USER:-}" =~ ^[A-Z][A-Z0-9_]{0,29}$ ]] || { printf 'APP_USER deve ser um identificador Oracle simples em maiúsculas.\n' >&2; exit 2; }
[[ -n "${APP_USER_PASSWORD:-}" ]] || { printf 'APP_USER_PASSWORD não está disponível no container.\n' >&2; exit 2; }
backup_dir="/opt/oracle/oradata/nexus-datapump/$backup_id"

objects_count() {
  sqlplus -s / as sysdba <<SQL
whenever oserror exit failure
whenever sqlerror exit failure rollback
set heading off feedback off pagesize 0 verify off echo off
alter session set container=FREEPDB1;
select count(*) from dba_objects where owner = '$APP_USER';
exit
SQL
}

if [[ "$mode" == check || "$mode" == import ]]; then
  count="$(objects_count | tr -d '[:space:]')"
  [[ "$count" =~ ^[0-9]+$ ]] || { printf 'Não foi possível conferir os objetos do destino.\n' >&2; exit 1; }
  [[ "$count" == 0 ]] || { printf 'Destino recusado: o schema já contém objetos. Use um Oracle de restauração novo.\n' >&2; exit 1; }
  [[ "$mode" != check ]] || { printf 'Schema de destino vazio.\n'; exit 0; }
fi

mkdir -p "$backup_dir"
chmod 700 "$backup_dir"
sqlplus -s / as sysdba >"$backup_dir/prepare.log" <<SQL
whenever oserror exit failure
whenever sqlerror exit failure rollback
set echo off feedback off verify off
alter session set container=FREEPDB1;
create or replace directory NEXUS_BACKUP_DIR as '$backup_dir';
grant read, write on directory NEXUS_BACKUP_DIR to "$APP_USER";
exit
SQL

cleanup_directory() {
  sqlplus -s / as sysdba >/dev/null <<'SQL' || true
set echo off feedback off
alter session set container=FREEPDB1;
drop directory NEXUS_BACKUP_DIR;
exit
SQL
}
trap cleanup_directory EXIT

if [[ "$mode" == export ]]; then
  [[ ! -e "$backup_dir/nexus.dmp" ]] || { printf 'Export recusado: dump já existe.\n' >&2; exit 1; }
  cat >"$backup_dir/export.par" <<PAR
DIRECTORY=NEXUS_BACKUP_DIR
DUMPFILE=nexus.dmp
LOGFILE=export.log
SCHEMAS=$APP_USER
FLASHBACK_TIME=SYSTIMESTAMP
COMPRESSION=METADATA_ONLY
PARALLEL=1
REUSE_DUMPFILES=NO
PAR
  if ! printf '%s\n' "$APP_USER_PASSWORD" | expdp "$APP_USER@//127.0.0.1:1521/FREEPDB1" \
      parfile="$backup_dir/export.par" >"$backup_dir/export-console.log" 2>&1; then
    printf 'Data Pump falhou. Consulte os logs privados em %s dentro do container.\n' "$backup_dir" >&2
    exit 1
  fi
else
  [[ -s "$backup_dir/nexus.dmp" ]] || { printf 'Dump de restauração ausente.\n' >&2; exit 1; }
  cat >"$backup_dir/import.par" <<PAR
DIRECTORY=NEXUS_BACKUP_DIR
DUMPFILE=nexus.dmp
LOGFILE=import.log
SCHEMAS=$APP_USER
EXCLUDE=USER,ROLE_GRANT,SYSTEM_GRANT,DEFAULT_ROLE
PARALLEL=1
PAR
  if ! printf '%s\n' "$APP_USER_PASSWORD" | impdp "$APP_USER@//127.0.0.1:1521/FREEPDB1" \
      parfile="$backup_dir/import.par" >"$backup_dir/import-console.log" 2>&1; then
    printf 'Import falhou. Preserve o destino para diagnóstico; consulte os logs privados em %s.\n' "$backup_dir" >&2
    exit 1
  fi
fi
printf 'Data Pump %s concluído.\n' "$mode"
