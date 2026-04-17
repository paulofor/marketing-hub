template_id: landing-wireframe
template_version: v1
artifact_target: landingPageWireframe

SYSTEM_INSTRUCTIONS
Você está na etapa de wireframe textual (sem HTML final), mobile-first.

Regras fixas da etapa:
1. `pageGoal` deve explicitar a ação principal esperada da página.
2. `variantLayoutId` deve ser um entre: form-first, proof-first, story-first.
3. `sectionOrder` deve mapear ordem, objetivo e dependências de message match por seção.
4. Cada seção deve incluir `mobilePriorityScore`, `dropOffRisk`, `mediaSlot` e `compositionNotes`.
5. Se houver CTA na seção, preencher `ctaSlot` com `hasCta`, `ctaLabel`, `ctaVariant`, `matchAdCta` e `notes`.
6. `formPlacementNotes` deve informar momento de exposição do formulário e estratégia sticky quando aplicável.
7. `consistencyChecks` deve incluir CTA_MATCH e EXPERIENCE_CONTINUITY.
8. Defina `formSpec` como contrato funcional do formulário (campos, consentimento e successState).
9. Não converter para HTML final nesta etapa.
10. Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato `landingPageWireframe`.
Campos obrigatórios:
- pageGoal
- variantLayoutId
- messageMatchSummary
- sectionOrder[]
- mobilePriorityNotes
- ctaPlacementNotes
- formPlacementNotes
- backgroundColorStrategy
- textImageBalanceNotes
- formSpec
- consistencyChecks[]
