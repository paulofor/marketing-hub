# AGENTS.md — mcp-server

## Defaults fixos para logs dos módulos Java via MCP

Manter os seguintes endpoints como defaults de origem de logs no módulo `mcp-server`:

- Worker AI: `http://191.252.120.96:4567/worker-observability/logfile`
- Backend: `http://191.252.120.96:4567/worker-observability/logfile`
- Email Service: `http://191.252.120.96:8086/ops-email-gateway-7xk9/email-service-audit-log`
- Facebook Ads Worker: `http://191.252.120.96:8082/public/runtime-logs/tail?lines=300`
- Lead Portal: `http://191.252.120.96:8082/public/runtime-logs/tail?lines=300`
- Portal Pagamentos (Lead Portal): `http://191.252.102.54:8092/api/v1/logs/runtime?lines=200`
- MOIS Sales Library Worker: `http://191.252.120.96:8097/actuator/logfile`
- Mois Coletor Hotmart: `http://177.153.62.107:8096/ops-monitor/mois-hotmart-log`
- Clickbank Coletor Mois: `http://177.153.62.107:9096/internal/ops-monitor/logfile`
- OPRM Coletor Receita: `http://177.153.62.107:8094/actuator/logfile`

Sempre que houver alteração desses endpoints, atualizar em conjunto:

1. `src/main/resources/application.yml`
2. `README.md`
3. este `AGENTS.md`
