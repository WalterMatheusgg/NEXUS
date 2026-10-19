#!/usr/bin/env bash
set -euo pipefail
. "$(dirname -- "$0")/common.sh"
require_env
use_docker
case "${1:-db}" in
  db)
    compose_base config --quiet
    compose_base up -d --wait oracle
    ;;
  mail)
    "${NEXUS_DOCKER[@]}" compose -p infra --env-file "$NEXUS_ROOT/.env" \
      -f "$NEXUS_ROOT/infra/docker-compose.yml" -f "$NEXUS_ROOT/infra/docker-compose.mail.yml" \
      --profile dev up -d --wait mailpit
    ;;
  all)
    compose_all config --quiet
    compose_all up -d --build --wait
    printf 'Aplicação local: http://127.0.0.1:8081; Mailpit: http://127.0.0.1:8025\n'
    ;;
  *) fail 'Uso: ./scripts/up.sh db|mail|all' ;;
esac
