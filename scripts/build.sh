#!/usr/bin/env bash
set -euo pipefail
. "$(dirname -- "$0")/common.sh"
target="${1:-all}"
[[ "$target" =~ ^(all|backend|frontend)$ ]] || fail 'Uso: ./scripts/build.sh [all|backend|frontend]'
if [[ "$target" != frontend ]]; then
  use_java
  (cd "$NEXUS_ROOT/backend" && ./mvnw -B -DskipTests package)
fi
if [[ "$target" != backend ]]; then
  use_node
  (cd "$NEXUS_ROOT/frontend" && npm ci && npm run build)
fi
printf 'Build concluído. Este comando não substitui ./scripts/test.sh.\n'
