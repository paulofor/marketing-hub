# Cânone — Campos temporais Liquibase/MySQL 5.7

## Regra obrigatória

Changelogs Liquibase executados em MySQL 5.7 não devem criar colunas `TIMESTAMP NOT NULL` sem `DEFAULT` explícito.

Para campos obrigatórios de data/hora que são preenchidos pela aplicação, use preferencialmente `DATETIME NOT NULL`.

Para campos preenchidos automaticamente pelo banco, declare explicitamente `DEFAULT CURRENT_TIMESTAMP` e, quando necessário, `ON UPDATE CURRENT_TIMESTAMP` via SQL compatível com MySQL 5.7.

Quando o contrato compara a ordem exata de autorização, publicação ou auditoria, preserve a precisão
dos instantes: `DATETIME(6)` para preenchimento pela aplicação e, para preenchimento pelo banco,
`TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`. No MySQL 5.7, uma coluna sem fração pode
arredondar o instante para o segundo seguinte. Não acrescente esperas artificiais nem afrouxe o gate
para compensar perda de precisão.

## Motivo

Algumas configurações reais de MySQL 5.7 rejeitam `TIMESTAMP NOT NULL` sem default automático com erro `Invalid default value`, bloqueando o bootstrap do backend durante o Liquibase.

## Aplicação

Esta regra vale para tabelas de auditoria, rastreio, execução de pipeline, prompts/schemas, métricas e qualquer changelog novo que introduza campos como `created_at`, `updated_at`, `used_at`, `started_at` ou `completed_at`.

Antes de finalizar changelog para MySQL 5.7, revisar mentalmente e no diff se existe `TIMESTAMP NOT NULL` sem default explícito.

## Validação no GitHub Actions

Conforme a orientação operacional vigente, a validação começa na sandbox com MySQL 5.7 isolado,
aplicação, idempotência e reversão quando aplicável. O workflow `liquibase-mysql57.yml` executa
verificações estáticas e contratos físicos em instâncias efêmeras no PR, como proteção adicional.
Não usar commit, PR ou deploy para descobrir o próximo defeito antes de terminar a homologação local.

O validador temporal verifica o `DEFAULT` da própria declaração, inclusive em múltiplas linhas e com
precisão fracionária. Comentários, literais e defaults de outra coluna não satisfazem esse contrato.
