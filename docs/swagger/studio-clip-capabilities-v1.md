# Estúdio — capacidades e custo integral v1

O controller canônico existente `SalesVideoController` e a fachada `SalesVideoService` expõem:

| Método | Caminho | Comportamento |
| --- | --- | --- |
| GET | `/api/sales-videos/provider-models` | Cadastro persistido e `capabilityRevision`/`capabilities` do contrato compartilhado. Não ativa adapter. |
| POST | `/api/sales-videos/studio/clip-plan` | Simula tomadas e custo público vigente; não reserva nem produz. |
| PUT | `/api/sales-videos/projects/{projectId}/production-cost-plan` | Registra previsão restante auditável no tenant do projeto. Não amplia teto nem inicia produção. |
| GET | `/api/sales-videos/projects/{projectId}` | Inclui `productionCostPlanJson`; ausência é lacuna, não zero. |

A simulação recebe `providerName`, `finalSeconds` (1–3600), `resolution`, `audio`,
`imageReferences` e `referenceVideoSeconds`. Retorna revisão, duração final/gerada/excedente,
`scenes` com modelo e segundos pedidos/usados/cobrados, custo por tomada, `clipsCostUsd` anulável,
`blockers` e `productionAllowed=false`. O endpoint não verifica saldo privado; seu custo é parcial.
DRAFT/HOMOLOGATION e modalidades não implementadas permanecem bloqueados.

A previsão integral recebe `cycleId`, `productId`, `experimentId`, `capabilityRevision`,
`providerPlan` exato, `finalSeconds`, `expiresAt`, `remainingCosts` e `unresolvedAttemptIds`.
`remainingCosts` exige AGENTS, REFERENCES, VOICE, MUSIC, POST_PRODUCTION, FEES e RETRIES,
cada qual com `maximumUsd` não negativo e `evidence` preenchida, inclusive para zero.
São custos **restantes**, sem duplicar consumo já lançado. A reserva consulta o ledger real,
confere revisão/escopo, soma o máximo dos clipes e o incorrido e aplica o teto existente.

Não colocar JSON dentro de um campo arbitrário: `productionCostPlanJson` é o envelope JSON
versionado explicitamente aceito no projeto/metadados e interpretado pelo backend. Uma mudança
no plano, duração, ciclo ou revisão exige outra previsão compatível. Registrar previsão não
renova parecer encerrado, preflight vencido ou autorização de produção.

Erros de duração, preço vencido, modalidade/adapter não implementados ou custo desconhecido
bloqueiam antes da geração. A falta de previsão integral no callback gera
`READY_WITH_BLOCKER/PROVIDER_FULL_COST_PLAN_REQUIRED`, com raw snapshots preservados e replay
idempotente, sem enfileirar parecer pago por lacuna determinística. Depois de registrar custos
compatíveis, é possível reaplicar o callback original durante a validade existente, sem nova
consulta ao provider ou extensão do snapshot; isso permite reserva e gate de Plutus pelo backend.
Snapshot vencido, parecer encerrado e ciclo somente de consulta não recebem autorização retroativa.
`PROVIDER_RESULT_UNCERTAIN` e custos incertos mantêm a reserva protegida em CONSUMING,
com pendência `PROVIDER_RECONCILIATION_REQUIRED`; conciliar o ID anterior antes de repetir.
