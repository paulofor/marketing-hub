# AGENTS.md — mcp-server

## Defaults fixos para logs dos módulos Java via MCP

Manter os seguintes endpoints como defaults de origem de logs no módulo `mcp-server`:

- Worker AI: `http://191.252.120.96:4567/worker-observability/logfile`
- Backend: `http://191.252.181.168:8099/ops-mh-observability-v2/backend-log-stream-x9k`
- Email Service: `http://191.252.120.96:8086/ops-email-gateway-7xk9/email-service-audit-log`
- Facebook Ads Worker: `http://191.252.120.96:8082/public/runtime-logs/tail?lines=300`
- Lead Portal: `http://191.252.120.96:8082/public/runtime-logs/tail?lines=300`
- Portal Pagamentos (Lead Portal): `http://163.245.200.7:8092/api/v1/logs/runtime?lines=200`
- MOIS: `http://191.252.120.96:8097/actuator/logfile`
- MOIS Sales Library Worker (`mois-sales-library-worker`): `http://191.252.120.96:8097/actuator/logfile`
- Mois Coletor Hotmart: `http://177.153.62.107:8096/ops-monitor/mois-hotmart-log`
- Clickbank Coletor Mois: `http://177.153.62.107:9096/internal/ops-monitor/logfile`
- OPRM Coletor Receita/MEI: `http://191.252.120.96:8094/actuator/logfile`
- Ops Monitor Worker: `http://191.252.120.96:8098/actuator/logfile`
- PDE Platform Backend: `http://163.245.200.7:8096/actuator/logfile`
- Video Management Service: `http://177.153.62.107:8095/actuator/logfile`
- Customer Agent Worker: `http://163.245.202.80:8099/ops-customer-agent-observability-v1/customer-agent-worker-log`
- Financial Agent Worker: `http://163.245.202.80:8095/ops-financial-agent-observability-v1/financial-agent-worker-log`
- Experiment Strategist Worker: `http://163.245.202.80:8096/ops-experiment-strategist-observability-v1/logfile`
- Meta Ad Approver Worker: `http://163.245.202.80:8097/ops-meta-ad-approver-observability-v1/logfile`
- Landing Generator Agent Worker: `http://163.245.202.80:8100/ops-landing-generator-observability-v1/logfile`

Sempre que houver alteração desses endpoints, atualizar em conjunto:

1. `src/main/resources/application.yml`
2. `README.md`
3. este `AGENTS.md`

## VPS_HOST_INVENTORY_MCP

Quando precisar descobrir CPU, memória, disco, portas ou containers dos VPS, procure primeiro a tool MCP `vps_host_inventory`.

- Implementação: `src/main/java/com/marketinghub/mcpserver/service/VpsHostInventoryService.java`.
- Contrato JSON-RPC: `src/main/java/com/marketinghub/mcpserver/controller/McpController.java`.
- Configuração: bloco `mcp.vps-host-inventory` em `src/main/resources/application.yml`.
- Documentação operacional: seção "Inventário físico de VPS via SSH restrito" em `README.md`.

Não criar shell genérico no MCP para esse caso. A consulta deve usar host em allowlist e comando remoto fixo. A chave privada SSH do MCP deve ficar somente no host do MCP, fora do Git, montada no container em `/opt/marketinghub/mcp/ssh/id_ed25519`.

Para o Lead Portal público, usar `vps_docker_logs` com host `191.252.120.96` e alvo fixo
`lead-portal-stack`; esse contrato retorna estado e logs dos containers canônicos backend,
frontend e proxy sem aceitar nomes ou comandos arbitrários.

## RUNTIME_BUILD_INFO_MCP

Quando precisar confirmar qual versão/commit de um módulo está rodando em produção, procure primeiro a tool MCP `runtime_build_info`.

- Implementação: `src/main/java/com/marketinghub/mcpserver/service/RuntimeBuildInfoService.java`.
- Contrato JSON-RPC: `src/main/java/com/marketinghub/mcpserver/controller/McpController.java`.
- Configuração: bloco `mcp.build-info` em `src/main/resources/application.yml`.
- Módulos configurados por padrão:
  - `backend`, apontando para `http://191.252.181.168/actuator/info`.
  - `pde-platform-backend`, apontando para `http://163.245.200.7:8096/actuator/info`.

O backend principal deve manter uma rota compatível em `/actuator/info` para essa consulta, mesmo quando seus demais endpoints do Actuator usarem base path operacional próprio. Não inferir commit produtivo apenas por container recente, tag `latest` ou horário de deploy. Se a tool retornar `buildIdentityPublished=false`, o MCP confirmou que o endpoint respondeu, mas o runtime ainda não publicou commit, branch, build time ou version rastreável. Se a tool retornar HTTP 500, trate como falha do módulo consultado em publicar identidade de build e corrija o Actuator/rota de runtime no próprio módulo antes de usar inferências alternativas.
