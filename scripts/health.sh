#!/usr/bin/env bash
set -euo pipefail
. "$(dirname -- "$0")/common.sh"
require_command curl
require_command python3
base="${1:-http://127.0.0.1:18080}"
for probe in health health/liveness health/readiness; do
  curl --fail --silent --show-error --connect-timeout 5 --max-time 15 "$base/actuator/$probe" \
    | python3 -c 'import json,sys; p=json.load(sys.stdin); print(json.dumps(p)); sys.exit(0 if p.get("status")=="UP" else 1)'
done
