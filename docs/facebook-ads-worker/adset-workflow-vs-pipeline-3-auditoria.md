# Auditoria: jobs da tela `/experiments/:id/adset-workflow` vs pipeline 3 públicos

Data: 2026-02-12

## Evidências utilizadas

- Documento de referência: `docs/facebook-ads-worker/pipeline-3-publicos-meta-ads-api-ia-worker.md`
- Enum de tipos de job do workflow: `backend/ads-service/src/main/java/com/marketinghub/facebookads/playbook/ExperimentAdSetJobType.java`
- Orquestração e encadeamento dos jobs: `backend/ads-service/src/main/java/com/marketinghub/facebookads/playbook/service/ExperimentAdSetWorkflowJobCoordinator.java`
- Renderização dos jobs na tela (ordenação por ID): `frontend/src/pages/experiment/ExperimentAdSetWorkflowPage.tsx`

## Ordem real do pipeline exibido na tela

A tela mostra os jobs do backend ordenados por `id` ascendente. A sequência efetiva do orquestrador é:

1. `AI_PREPARE_SEED`
2. `FACEBOOK_SEED_LOOKUP`
3. `FACEBOOK_TARGETING_SUGGESTIONS`
4. `FACEBOOK_SOCIAL_POSITIONS` (opcional, quando há `positionQueries`)
5. `AI_BUILD_SPECS`
6. `FACEBOOK_VALIDATE_SPEC` (1 job por spec)
7. `FACEBOOK_REACH_ESTIMATE` (1 job por spec)

## Comparativo com o documento `pipeline-3-publicos-meta-ads-api-ia-worker.md`

| Fase do documento | Cobertura no workflow atual | Observação |
|---|---|---|
| Definir ICP e gerar seeds (IA) | ✅ Sim | Coberto por `AI_PREPARE_SEED`. |
| `targetingsearch` para obter IDs | ✅ Sim | Coberto por `FACEBOOK_SEED_LOOKUP`. |
| `targetingsuggestions` para expandir seed | ✅ Sim | Coberto por `FACEBOOK_TARGETING_SUGGESTIONS`. |
| Planejar 3 públicos e montar `targeting_spec` com `flexible_spec` | ✅ Sim | Coberto por `AI_BUILD_SPECS` com persistência de specs. |
| `targetingvalidation` | ✅ Sim (e obrigatório no fluxo atual) | No documento é opcional; no workflow atual acontece antes de reach para cada spec. |
| `reachestimate` | ✅ Sim | Coberto por `FACEBOOK_REACH_ESTIMATE` para cada spec. |
| Loop de calibração automática por faixa de reach | ⚠️ Parcial | O documento descreve loop explícito com ajustes; no workflow atual há validação+reach e conclusão quando todas specs ficam `READY`, sem job dedicado de recalibração automática após reach fora da faixa. |
| Artefatos de trilha (`decision_log`, `seed_candidates`, `suggestions_curated`, `audience_plan`) | ⚠️ Parcial | O fluxo persiste estado e payloads de jobs/specs, porém não segue literalmente os mesmos artefatos/nomes de arquivo do documento. |

## Conclusão

- **Os jobs da tela seguem o núcleo do pipeline do documento** (seed → search → suggestions → build specs → validate → reach).
- Existem **diferenças de implementação**:
  - o fluxo atual inclui etapa opcional extra de cargos (`FACEBOOK_SOCIAL_POSITIONS`);
  - `targetingvalidation` é tratado como etapa obrigatória no encadeamento atual;
  - não há etapa explícita de recalibração iterativa por threshold de reach com job dedicado.
- Portanto, o status recomendado é: **aderente no fluxo principal, com divergências parciais nos detalhes operacionais e de auditoria/artefatos**.
