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
