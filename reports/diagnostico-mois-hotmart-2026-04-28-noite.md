# Diagnóstico — execução noturna do robô MOIS (Hotmart)

Data da análise: 2026-04-28 (UTC)
Solicitação: verificar a primeira execução noturna do robô do MOIS acessando dados da Hotmart, via MCP Server (logs + banco).

## 1) Saúde do MCP e ferramentas disponíveis

- Endpoint MCP respondeu corretamente em `https://mcpserverdigi.shop/mcp`.
- `initialize` e `tools/list` funcionando.
- Ferramentas relevantes confirmadas:
  - `java_module_logs`
  - `db_health`
  - `db_list_tables`
  - `db_query`

## 2) Logs do módulo MOIS

Consulta executada via `tools/call -> java_module_logs` com `module=mois`.

### Evidências principais

1. O MOIS iniciou às **2026-04-28T04:10:05.802Z** e finalizou bootstrap às **2026-04-28T04:10:13.130Z**.
2. Houve inicialização do DispatcherServlet às **2026-04-28T07:10:54Z** (primeiro tráfego HTTP observado).
3. Não aparecem, nas últimas linhas disponíveis, logs de execução do robô Hotmart (ex.: start/fim de coleta).
4. Há um evento de requisição malformada em **2026-04-28T07:16:04.020Z**: `Invalid character found in method name`, típico de tráfego TLS enviado para porta HTTP.

## 3) Banco de dados via MCP

### Conectividade

- `db_health`: status `ok`, database `marketinghubdb`.

### Tabelas MOIS presentes

- `mois_collection_job_state`
- `mois_discovery_request`
- `mois_source_snapshot`
- e tabelas de oferta MOIS (`mois_offer_*`).

### Resultado de dados

- `mois_collection_job_state`: **0 linhas** (consulta dos últimos 10 registros por `updated_at`).
- `mois_discovery_request`: **0 linhas** (consulta simples de amostra).

## 4) Conclusão objetiva

Com base no que o MCP retornou nesta análise:

- Não há evidência de persistência de execução do robô MOIS no banco compartilhado consultado pelo MCP (`marketinghubdb`).
- Também não há, no recorte de logs disponível do módulo `mois`, mensagens que indiquem uma coleta Hotmart concluída ou falhada.
- O que existe de concreto é: inicialização do serviço MOIS e uma tentativa de acesso HTTP inválida.

## 5) Hipóteses técnicas mais prováveis

1. O robô não executou nesta janela (ou ainda não executou desde esse boot).
2. O robô executou em outro ambiente/instância/banco não observado por este MCP.
3. O log remoto do MOIS exposto no endpoint atual não contém histórico suficiente da janela noturna completa.

## 6) Próximos passos recomendados

1. Coletar janela maior/histórica de logs do MOIS (arquivo completo, não apenas tail curto).
2. Confirmar timezone e cron efetivo no ambiente de produção do MOIS.
3. Confirmar se a execução noturna grava estado no `marketinghubdb` ou em outro schema/instância.
4. Se necessário, acionar execução manual (`/api/v1/mois/automation/hotmart/run`) e acompanhar logs em tempo real para validar ponta a ponta.

## 7) Observação de configuração no repositório

No código-fonte versionado do módulo MOIS, a flag padrão do robô é:

- `mois.robot.hotmart.enabled=${MOIS_ROBOT_HOTMART_ENABLED:false}`

Ou seja, sem variável de ambiente habilitando explicitamente, o robô fica desativado por padrão.
