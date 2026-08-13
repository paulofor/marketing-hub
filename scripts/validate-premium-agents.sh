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
    'landing-generator': root/'.github/workflows/landing-generator-agent-worker-ci.yml',
}
codex_home_by_agent = {
    'customer-agent': 'CUSTOMER_AGENT_CODEX_HOME',
    'financial-agent': 'FINANCIAL_AGENT_CODEX_HOME',
    'growth-operator': 'GROWTH_OPERATOR_CODEX_HOME',
    'experiment-strategist': 'EXPERIMENT_STRATEGIST_CODEX_HOME',
    'meta-ad-approver': 'META_AD_APPROVER_CODEX_HOME',
    'landing-generator': 'LANDING_GENERATOR_CODEX_HOME',
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
    'landing-generator': (
        '/api/internal/geralanding/agent/v1/stage-executions/',
        root/'backend/ads-service/src/main/java/com/marketinghub/geralanding/agent/v1/LandingGenerationAgentController.java',
        '@GetMapping("/{executionId}/context")',
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
    if agent.get('internetAccess') != 'unrestricted': errors.append(f"{agent['key']}: acesso livre à internet não declarado")
    if agent.get('premiumMemory') is not True: errors.append(f"{agent['key']}: memória premium não declarada")
    if '--search' not in java: errors.append(f"{agent['key']}: pesquisa livre na internet não habilitada no Codex")
    if 'mcp_servers.' not in java: errors.append(f"{agent['key']}: MCP não registrado")
    if 'pending' not in java or ('complete' not in java and '/result' not in java): errors.append(f"{agent['key']}: pending/callback ausente")
    if 'CodexTelemetryReporter' not in java: errors.append(f"{agent['key']}: telemetria ausente")
    mcp_text = (resources/agent['mcp']).read_text() if (resources/agent['mcp']).exists() else ''
    for memory_tool in ('recuperar_memoria_especializada', 'registrar_aprendizado_candidato'):
        if memory_tool not in mcp_text: errors.append(f"{agent['key']}: MCP sem {memory_tool}")
    if f'/agents/{agent["key"]}' not in mcp_text:
        errors.append(f"{agent['key']}: MCP sem agentKey fixo na memória premium")
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
        isolated_home = f'/opt/growth-operator/agents/{agent["key"]}/codex-home'
        for marker in (
            f'install -d -o 10001 -g 10001 {isolated_home}',
            f'{codex_home}={isolated_home}',
            'node /app/agent-health-report.mjs',
        ):
            if marker not in workflow_text:
                errors.append(
                    f"{agent['key']}: workflow sem bootstrap/readiness seguro da identidade Codex ({marker})"
                )
        if 'scripts/reconcile-agent-codex-auth.sh' in workflow_text:
            errors.append(f"{agent['key']}: workflow ainda reconcilia a sessão compartilhada legada")
        if re.search(r'install .*auth\.json .*agents/.*/codex-home/auth\.json', workflow_text):
            errors.append(f"{agent['key']}: workflow clona refresh token para identidade isolada")
        if 'group: codex-agent-host-deploy' in workflow_text:
            errors.append(f"{agent['key']}: workflow usa fila compartilhada que cancela deploys pendentes")
        module_sync = f'rsync -az --delete {agent["module"]}/'
        if module_sync not in workflow_text:
            errors.append(f"{agent['key']}: workflow não sincroniza somente o próprio módulo")
if errors:
    print('\n'.join(f"[ARQUITETURA] {e}" for e in errors),file=sys.stderr); raise SystemExit(1)
print(f"[ARQUITETURA] {sum(a['operational'] for a in agents)} agentes conformes; {sum(not a['operational'] for a in agents)} bloqueado(s) com causa explícita.")
PY
bash "$repo_root/scripts/test-isolated-agent-codex-auth.sh"
node "$repo_root/scripts/test-agent-version-deploy-gate.mjs"
