# Registro de aplicação — Protocolo Monitor

## 2026-06-25 — NichoCNAE v3 / OPRM Coletor MEI

- Pipeline/versionamento: NichoCNAE v3.
- Módulo executor monitorado: `oprm-coletor-mei`.
- Sinal operacional: execução v3 em `PENDING` por mais de 6 minutos.
- Comportamento no Ops Monitor: módulo `DEGRADED` e incidente `OPRM_NICHO_CNAE_V3_QUEUE_STALE` com job, CNAE e etapa parada.
- Objetivo: evitar que container saudável mas fila v3 sem consumo apareça como operação normal.
