# Squad Backend Forms — Checkpoint 1

- **Owner:** ChatGPT (AI dev)
- **Objetivo:**
  - Estender colunas `lead_portal_flow_question` para LONGTEXT e evitar erros de truncamento.
  - Disponibilizar APIs para atualização dos formulários simples quando não estiverem vinculados a experimentos.
  - Garantir entrega automática (sem steps manuais) por meio das pipelines existentes.
- **Riscos:**
  - Migração Liquibase precisar rodar em bases grandes (tempo de lock) — mitigar usando preconditions e escopo coluna a coluna.
  - Regras de edição podem conflitar com fluxos já aprovados — validar com constraints no serviço.
- **Custos Estimados:**
  - Engenharia: ~6h (inclui migração + ajustes em serviços/tests).
  - Deploy: embutido no pipeline atual (sem custo adicional manual).
