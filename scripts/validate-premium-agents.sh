#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 - "$repo_root" "$repo_root/config/agents/premium-agent-compliance.json" <<'PY'
import json, pathlib, re, sys
root=pathlib.Path(sys.argv[1]); agents=json.loads(pathlib.Path(sys.argv[2]).read_text())['agents']; errors=[]
workflow_by_agent = {
    'customer-agent': root/'.github/workflows/customer-agent-worker-ci.yml',
    'financial-agent': root/'.github/workflows/financial-agent-worker-ci.yml',
    'growth-operator': root/'.github/workflows/growth-operator-worker-ci.yml',
    'experiment-strategist': root/'.github/workflows/experiment-strategist-worker-ci.yml',
    'meta-ad-approver': root/'.github/workflows/meta-ad-approver-worker-ci.yml',
}
codex_home_by_agent = {
    'customer-agent': 'CUSTOMER_AGENT_CODEX_HOME',
    'financial-agent': 'FINANCIAL_AGENT_CODEX_HOME',
    'growth-operator': 'GROWTH_OPERATOR_CODEX_HOME',
    'experiment-strategist': 'EXPERIMENT_STRATEGIST_CODEX_HOME',
    'meta-ad-approver': 'META_AD_APPROVER_CODEX_HOME',
}
mcp_backend_contracts = {
    'customer-agent': (
        '/api/customer-agent/v1/internal/evaluations/',
        root/'backend/ads-service/src/main/java/com/marketinghub/customeragent/controller/CustomerAgentController.java',
        '@GetMapping("/internal/evaluations/{id}")',
    ),
    'financial-agent': (
        '/api/financial-agent/v1/internal/executions/',
        root/'backend/ads-service/src/main/java/com/marketinghub/financialagent/controller/FinancialAgentController.java',
        '@GetMapping("/internal/executions/{id}")',
    ),
    'experiment-strategist': (
        '/api/experiment-strategist/v1/internal/executions/',
        root/'backend/ads-service/src/main/java/com/marketinghub/experimentstrategist/controller/ExperimentStrategistController.java',
        '@GetMapping("/internal/executions/{id}")',
    ),
}
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
    backend_contract = mcp_backend_contracts.get(agent['key'])
    if backend_contract:
        route_prefix, controller_path, controller_mapping = backend_contract
        mcp_text = (resources/agent['mcp']).read_text() if (resources/agent['mcp']).exists() else ''
        controller_text = controller_path.read_text() if controller_path.exists() else ''
        if route_prefix not in mcp_text:
            errors.append(f"{agent['key']}: MCP não consulta a rota interna canônica")
        if controller_mapping not in controller_text:
            errors.append(f"{agent['key']}: backend não expõe a rota de contexto usada pelo MCP")
    workflow = workflow_by_agent.get(agent['key'])
    codex_home = codex_home_by_agent.get(agent['key'])
    if workflow and codex_home:
        workflow_text = workflow.read_text() if workflow.exists() else ''
        exported_blocks = re.findall(r'export\s+(.+?)\\\s*\n\s*&&', workflow_text, re.DOTALL)
        if not any(f'{codex_home}=' in block for block in exported_blocks):
            errors.append(
                f"{agent['key']}: workflow não exporta {codex_home} para toda a sessão remota"
            )
        for marker in (
            '/opt/growth-operator/codex-home/auth.json',
            'install -m 600 -o 10001 -g 10001',
            'codex login status',
        ):
            if marker not in workflow_text:
                errors.append(
                    f"{agent['key']}: workflow sem bootstrap/readiness seguro da identidade Codex ({marker})"
                )
if errors:
    print('\n'.join(f"[ARQUITETURA] {e}" for e in errors),file=sys.stderr); raise SystemExit(1)
print(f"[ARQUITETURA] {sum(a['operational'] for a in agents)} agentes conformes; {sum(not a['operational'] for a in agents)} bloqueado(s) com causa explícita.")
PY
