# Cânone — Campos temporais Liquibase/MySQL 5.7

## Regra obrigatória

Changelogs Liquibase executados em MySQL 5.7 não devem criar colunas `TIMESTAMP NOT NULL` sem `DEFAULT` explícito.

Para campos obrigatórios de data/hora que são preenchidos pela aplicação, use preferencialmente `DATETIME NOT NULL`.

Para campos preenchidos automaticamente pelo banco, declare explicitamente `DEFAULT CURRENT_TIMESTAMP` e, quando necessário, `ON UPDATE CURRENT_TIMESTAMP` via SQL compatível com MySQL 5.7.

## Motivo

Algumas configurações reais de MySQL 5.7 rejeitam `TIMESTAMP NOT NULL` sem default automático com erro `Invalid default value`, bloqueando o bootstrap do backend durante o Liquibase.

## Aplicação

Esta regra vale para tabelas de auditoria, rastreio, execução de pipeline, prompts/schemas, métricas e qualquer changelog novo que introduza campos como `created_at`, `updated_at`, `used_at`, `started_at` ou `completed_at`.

Antes de finalizar changelog para MySQL 5.7, revisar mentalmente e no diff se existe `TIMESTAMP NOT NULL` sem default explícito.

## Validação no GitHub Actions

O workflow de Liquibase executa somente verificações estáticas do contrato. Ele não deve iniciar uma instância MySQL nem executar `liquibase:update`. A aplicação física deve ser homologada de forma controlada antes da publicação em produção, sem usar deploy como mecanismo de descoberta de erros.
