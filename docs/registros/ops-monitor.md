# Registros do monitor operacional

## 2026-06-23 — Ativação do heartbeat periódico do monitor operacional

- Causa-raiz: a tela de operação mostrava todos os módulos como `Sem verificação recente` porque não havia registros em `ops_module_health_check`.
- Ajuste: o `ops-monitor-worker` passou a consumir o endpoint canônico de pendências, executar health check periódico e registrar heartbeat no backend.
- Prevenção: teste unitário garante que o runner consome pendências e chama o contrato de heartbeat.
