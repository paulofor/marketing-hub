#!/usr/bin/env bash
# Valida o callback da imagem real sem credenciais ou saída para provedores externos.
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${GEN45_IMAGE:?Informe a imagem candidata}"
: "${GEN45_COMPOSE_PROJECT:?Informe o projeto exclusivo da sandbox}"
: "${GEN45_EVIDENCE_DIR:?Informe o diretório de evidências na sandbox}"
mkdir -p "$GEN45_EVIDENCE_DIR"
rm -f "$GEN45_EVIDENCE_DIR/result.json"
compose=(docker compose -p "$GEN45_COMPOSE_PROJECT" -f infra/testing/runway-gen45/compose.yml)
cleanup() {
  "${compose[@]}" logs --no-color > "$GEN45_EVIDENCE_DIR/containers.log" 2>&1 || true
  "${compose[@]}" down --volumes --remove-orphans
}
trap cleanup EXIT
"${compose[@]}" create
"${compose[@]}" cp worker:/app/app.jar "$GEN45_EVIDENCE_DIR/app.jar"
python3 infra/testing/runway-gen45/verify-package.py "$GEN45_EVIDENCE_DIR/app.jar"
"${compose[@]}" cp infra/testing/runway-gen45/mock.mjs mock:/tmp/mock.mjs
"${compose[@]}" cp video-management-service/src/test/resources/runway/gen45 mock:/tmp/fixtures
"${compose[@]}" up -d --wait --wait-timeout 90
for attempt in $(seq 1 45); do
  if "${compose[@]}" cp mock:/tmp/evidence/result.json "$GEN45_EVIDENCE_DIR/result.json" 2>/dev/null; then
    if python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); sys.exit(0 if d.get("callbacks") or d.get("errors") else 1)' "$GEN45_EVIDENCE_DIR/result.json"; then break; fi
  fi
  sleep 1
done
python3 - "$GEN45_EVIDENCE_DIR" <<'PY'
import json, pathlib, sys, time
path = pathlib.Path(sys.argv[1], 'result.json')
for _ in range(45):
    data = json.loads(path.read_text()) if path.exists() else {}
    assert not data.get('errors'), data
    if data.get('callbacks'):
        assert len(data['callbacks']) == 1 and len(data['dryRuns']) == 2, data
        print('PASS imagem: pending → dry runs 10s/5s → callback READY auditável')
        break
    time.sleep(1)
else:
    raise AssertionError('A imagem não concluiu o callback local')
PY
