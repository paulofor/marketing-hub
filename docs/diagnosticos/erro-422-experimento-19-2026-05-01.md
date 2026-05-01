# Diagnóstico 422 — Experimento 19 (etapa texto da landing)

- **Data/hora do erro:** 01/05/2026 14:06:26 (BRT)
- **Endpoint:** `/api/internal/experiment-pipeline/jobs/f8629983-915f-42e4-88c2-37f05bde1125/complete`
- **Status:** `422 Unprocessable Entity`
- **Mensagem literal:** `Copy da landing inválida em bodySections: slotId 'headline' não pertence aos copySlots da sectionId 's1-hero'`

## O que o modelo entregou (literal)
No payload de `landingPageCopy.bodySections`, foi enviado um item com:
- `sectionId: "s1-hero"`
- `slotId: "headline"`

## O que a especificação esperava (literal)
Pelo contrato canônico, quando existe `landingPageWireframe.sectionOrder[].copySlots`, cada item de `landingPageCopy.bodySections` deve usar:
- `sectionId` existente no wireframe;
- `slotId` **literalmente igual** a um dos valores de `copySlots` da mesma seção.

## Diferença entre entregue e esperado
- **Entregue:** `slotId` semântico/genérico (`headline`).
- **Esperado:** `slotId` técnico/canônico da seção `s1-hero` (valor exato declarado em `copySlots`, por exemplo `hero-headline`, `slot-hero-01`, etc., conforme wireframe real).

## Validação correspondente que rejeitou
Validação de compatibilidade estrutural `sectionId + slotId` no backend da etapa de copy: `slotId` deve pertencer à lista `copySlots` da `sectionId` correspondente.

## Causa raiz
O modelo tratou `slotId` como rótulo funcional (`headline`) em vez de identificador técnico canônico do wireframe.

## Evidência confirmada em log/telemetria
Consulta em `experiment_pipeline_generation_job` para `experiment_id=19` confirmou o erro literal salvo pelo worker/backend, sem inferência:

- `created_at: 2026-05-01T17:03:18`
- `error_message`: `Rejeitado pelo backend ao completar o job (422). Motivo: {"timestamp":"2026-05-01T14:06:26.567559167-03:00","status":422,"error":"Unprocessable Entity","message":"Copy da landing inválida em bodySections: slotId 'headline' não pertence aos copySlots da sectionId 's1-hero'","path":"/api/internal/experiment-pipeline/jobs/f8629983-915f-42e4-88c2-37f05bde1125/complete"}`

Também foram encontrados erros equivalentes no mesmo experimento com variações de seção/casing:

- `slotId 'Headline'` em `sectionId 's1_hero_preview'` (timestamp do backend `2026-05-01T14:04:38.548722482-03:00`)
- `slotId 'headline'` em `sectionId 's1-hero-proof'` (timestamp do backend `2026-04-30T23:57:48.867073312-03:00`)

Isso confirma que o padrão recorrente é uso de alias semântico (`headline`) no lugar do `copySlots[].slotId` canônico.

## Ação corretiva recomendada
1. Reforçar prompt da etapa `landing-copy` para proibir uso de `purpose`/alias como `slotId` (ex.: `headline`, `subheadline`, `promise`).
2. Exigir uso literal de `copySlots[].slotId` do wireframe em `bodySections[].slotId`.
3. Manter fallback canônico: se faltar slot válido, registrar `FAIL` em `consistencyChecks` em vez de inventar valor.
