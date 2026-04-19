template_id: landing-wireframe
template_version: v1
artifact_target: landingPageWireframe

SYSTEM_INSTRUCTIONS
Você está na etapa de wireframe textual (sem HTML final), mobile-first.

Regras fixas da etapa:
1. `pageGoal` deve explicitar a ação principal esperada da página.
2. `variantLayoutId` deve ser um entre: form-first, proof-first, story-first.
3. `sectionOrder` deve mapear ordem, objetivo, dependências de message match e variação intencional de seção via `surfaceSpec` + `uiNotes`.
4. Cada seção deve incluir todos os campos canônicos de `sectionOrder`, incluindo `surfaceSpec` e `ctaSlot`.
5. Se houver CTA na seção, preencher `ctaSlot` com `hasCta`, `ctaLabel`, `ctaVariant`, `matchAdCta` e `notes`.
6. `formPlacementNotes` deve informar momento de exposição do formulário e estratégia sticky quando aplicável.
7. Não exija nem produza campos fora do schema canônico atual (ex.: `mediaSlot`, `compositionNotes`, `messageMatchSummary`, `backgroundColorStrategy`, `textImageBalanceNotes`).
8. `consistencyChecks` deve validar continuidade comercial e aderência estrutural sem exigir campos fora do canônico.
9. Defina `formSpec` como contrato funcional do formulário (campos, consentimento e successState).
10. Não converter para HTML final nesta etapa.
11. Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato `landingPageWireframe`.
Campos obrigatórios:
- pageGoal
- variantLayoutId
- sectionOrder[] com sectionId, sectionName, objective, contentType, copySource, uiNotes, messageMatchDependency, sectionDependsOn, mobilePriorityScore, dropOffRisk, surfaceSpec, ctaSlot
- mobilePriorityNotes
- ctaPlacementNotes
- formPlacementNotes
- consistencyChecks[]
- formSpec
