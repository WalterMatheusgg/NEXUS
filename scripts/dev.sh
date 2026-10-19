#!/usr/bin/env bash
set -euo pipefail
. "$(dirname -- "$0")/common.sh"
require_env
case "${1:-}" in
  backend)
    use_java
    cd "$NEXUS_ROOT/backend"
    exec ./mvnw spring-boot:run
    ;;
  frontend)
    use_node
    cd "$NEXUS_ROOT/frontend"
    if [[ ! -d node_modules ]]; then npm ci; fi
    exec npm start -- --host 127.0.0.1
    ;;
  *) fail 'Uso: ./scripts/dev.sh backend|frontend (um terminal para cada processo).' ;;
esac
