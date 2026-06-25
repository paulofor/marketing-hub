# Registros do monitor operacional

## 2026-06-23 — Ativação do heartbeat periódico do monitor operacional

- Causa-raiz: a tela de operação mostrava todos os módulos como `Sem verificação recente` porque não havia registros em `ops_module_health_check`.
- Ajuste: o `ops-monitor-worker` passou a consumir o endpoint canônico de pendências, executar health check periódico e registrar heartbeat no backend.
- Prevenção: teste unitário garante que o runner consome pendências e chama o contrato de heartbeat.

## 2026-06-24 — Incidente sintético para fila GeraLanding parada no AI Worker

- Problema observado: job de GeraLanding em `INICIADO` sem `processing_started_at` não aparecia como problema específico na tela `/ops-monitor`.
- Causa-raiz tratada: o Ops Monitor dependia apenas de heartbeats/incidentes reportados pelo worker; quando o próprio consumo da fila falhava, a tela não destacava a fila parada.
- Ajuste: o backend passou a expor incidente sintético do `ai-worker` quando houver execução GeraLanding antiga em `INICIADO`, mantendo o backend apenas como leitura/relatório de estado persistido.
- Prevenção: teste unitário cobre a criação do incidente sintético `GERALANDING_QUEUE_STALE`.

## 2026-06-24 — Observabilidade e publicação do ops-monitor-worker

- Causa-raiz tratada: o `ops-monitor-worker` não tinha exposição de logfile própria no Actuator, não estava listado na tool `java_module_logs` do MCP Server e não havia workflow dedicado de publicação.
- Ajuste: o worker passou a expor `/actuator/logfile`, o MCP passou a aceitar o módulo `ops-monitor-worker` e foi criado workflow de teste, build, push e deploy para publicação do executor.
- Prevenção: teste de contrato do MCP valida leitura dos logs do `ops-monitor-worker`.

## 2026-06-24 — Correção dos endpoints monitorados

- Problema observado: a tela `/ops-monitor` marcava módulos como fora do ar com erro `404 Not Found` porque alguns registros de `ops_monitored_module` apontavam para `/actuator/health` na porta 80, enquanto os módulos expõem Actuator em portas e base paths próprios.
- Causa-raiz tratada: cadastro inicial do monitor usou endpoints genéricos em vez dos contratos reais de observabilidade dos módulos.
- Ajuste: criado changelog para corrigir `base_url`, `health_path` e `log_path` de backend, AI Worker, Facebook Ads Worker, OPRM Coletor MEI, MOIS Sales Library Worker e Email Service.
- Prevenção: a correção fica versionada em Liquibase para manter o banco alinhado após deploy e evitar reincidência em novos ambientes.

## 2026-06-25 — Monitor operacional passa a usar URLs públicas

- Decisão operacional: o Ops Monitor deve acessar os módulos sempre pela URL pública oficial, sem usar `host.docker.internal` como atalho interno.
- Causa-raiz: o monitor podia marcar um módulo como `ONLINE` pela rede interna do Docker mesmo quando a rota pública estava recusando conexão, criando divergência entre saúde operacional exibida e disponibilidade real percebida fora do host.
- Ajuste: criado changelog incremental para restaurar URLs públicas de AI Worker, Facebook Ads Worker, OPRM Coletor MEI, MOIS Sales Library Worker e Email Service no cadastro `ops_monitored_module`.
- Prevenção: a regra foi registrada na arquitetura do Ops Monitor para impedir novos ajustes que voltem a usar rotas internas como fonte de verdade de disponibilidade.
