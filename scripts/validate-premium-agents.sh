#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 - "$repo_root" "$repo_root/config/agents/premium-agent-compliance.json" <<'PY'
import json, pathlib, sys
root=pathlib.Path(sys.argv[1]); agents=json.loads(pathlib.Path(sys.argv[2]).read_text())['agents']; errors=[]
for agent in agents:
    if not agent['operational']:
        if not agent.get('blockedReason'): errors.append(f"{agent['key']}: bloqueio sem causa")
        continue
    module=root/agent['module']; resources=module/'src/main/resources'
    required=[module/'Dockerfile',module/'docker-compose.yml',module/'pom.xml',resources/agent['mcp']]
    java='\n'.join(p.read_text(errors='ignore') for p in (module/'src/main/java').rglob('*.java'))
    compose=(module/'docker-compose.yml').read_text() if (module/'docker-compose.yml').exists() else ''
    docker=(module/'Dockerfile').read_text() if (module/'Dockerfile').exists() else ''
    for path in required:
        if not path.exists(): errors.append(f"{agent['key']}: ausente {path.relative_to(root)}")
    if not list((resources/'prompts').rglob('*.md')) or not list((resources/'prompts').rglob('*schema.json')): errors.append(f"{agent['key']}: prompt/schema ausente")
    for marker in ('read_only: true','no-new-privileges:true','tmpfs:'):
        if marker not in compose: errors.append(f"{agent['key']}: Compose sem {marker}")
    if 'codex@' not in docker: errors.append(f"{agent['key']}: Codex não instalado")
    if '--sandbox' not in java or 'read-only' not in java: errors.append(f"{agent['key']}: sandbox read-only ausente")
    if 'mcp_servers.' not in java: errors.append(f"{agent['key']}: MCP não registrado")
    if 'pending' not in java or ('complete' not in java and '/result' not in java): errors.append(f"{agent['key']}: pending/callback ausente")
    if 'CodexTelemetryReporter' not in java: errors.append(f"{agent['key']}: telemetria ausente")
    if agent.get('browser') and 'playwright' not in docker.lower(): errors.append(f"{agent['key']}: Playwright ausente")
if errors:
    print('\n'.join(f"[ARQUITETURA] {e}" for e in errors),file=sys.stderr); raise SystemExit(1)
print(f"[ARQUITETURA] {sum(a['operational'] for a in agents)} agentes conformes; {sum(not a['operational'] for a in agents)} bloqueado(s) com causa explícita.")
PY
