#!/usr/bin/env bash
set -euo pipefail
. "$(dirname -- "$0")/common.sh"
target="${1:-all}"
[[ "$target" =~ ^(all|backend|frontend|infra)$ ]] || fail 'Uso: ./scripts/test.sh [all|backend|frontend|infra]'
if [[ "$target" == all || "$target" == backend ]]; then
  require_env
  use_java
  printf 'Os testes de integração exigem Oracle real disponível.\n'
  (cd "$NEXUS_ROOT/backend" && ./mvnw -B verify)
fi
if [[ "$target" == all || "$target" == frontend ]]; then
  use_node
  (cd "$NEXUS_ROOT/frontend" && npm ci && npm test -- --watch=false)
fi
if [[ "$target" == all || "$target" == infra ]]; then
  require_command python3
  python3 "$NEXUS_ROOT/scripts/test-tooling.py"
fi
