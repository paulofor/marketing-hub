# Registros de execução — Gera Landing

## 2026-05-03 — Separação de marcos de data/hora

Para a tabela `gera_landing_stage_execution`, foram definidos três marcos distintos de data/hora para suportar o ciclo de execução por etapa:

- `created_at` → momento de **criação** do registro.
- `processing_started_at` → momento de **início do processamento**.
- `completed_at` → momento de **conclusão**.

### Implementação desta entrega

Nesta etapa foi implementado apenas o primeiro marco (**criação**):

- inclusão das colunas no schema via Liquibase;
- preenchimento de `created_at` no momento da persistência inicial.

Os marcos de processamento e conclusão ficam preparados no modelo para ativação nas próximas etapas.
