template_id: landing-wireframe
template_version: v1
artifact_target: landingPageWireframe

Variáveis disponíveis:
- NICHE_NAME: {{NICHE_NAME}}
- PERSONA_NAME: {{PERSONA_NAME}}
- HYPOTHESIS_TITLE: {{HYPOTHESIS_TITLE}}
- PRIMARY_PAIN_SUMMARY: {{PRIMARY_PAIN_SUMMARY}}
- PRIMARY_PROMISE_SUMMARY: {{PRIMARY_PROMISE_SUMMARY}}
- MECHANISM_SUMMARY: {{MECHANISM_SUMMARY}}
- PROOF_SUMMARY: {{PROOF_SUMMARY}}
- OFFER_NAME: {{OFFER_NAME}}
- PRIMARY_CTA_ACTION: {{PRIMARY_CTA_ACTION}}
- PRIMARY_CTA_LABEL: {{PRIMARY_CTA_LABEL}}
- PRODUCT_ENVELOPE: {{PRODUCT_ENVELOPE}}
- DELIVERABLES_JSON: {{DELIVERABLES_JSON}}
- PROOF_ASSET_JSON: {{PROOF_ASSET_JSON}}
- CASE_NOTES: {{CASE_NOTES}}

Objetivo:
Converter o copy aprovado em wireframe textual, mobile-first e com message match obrigatório.

Regras:
1. pageGoal precisa deixar explícito qual ação a página deve gerar.
2. variantLayoutId deve ser form-first, proof-first ou story-first.
3. sectionOrder deve mapear cada bloco com sectionId, sectionName, objective, contentType, copySource, uiNotes, messageMatchDependency e sectionDependsOn.
4. Cada bloco precisa informar mobilePriorityScore (1 a 10), dropOffRisk, mediaSlot e compositionNotes.
5. Se houver CTA no bloco, preencher ctaSlot com hasCta=true, ctaLabel, ctaVariant, matchAdCta e notes.
6. formPlacementNotes deve informar em quantos scrolls o formulário aparece e se há versão sticky.
7. ctaPlacementNotes garante repetição literal do CTA aprovado.
8. consistencyChecks precisa incluir CTA_MATCH e EXPERIENCE_CONTINUITY.
9. Cada bloco deve preencher surfaceSpec com surfaceToken, style, contrastMode e notes.
10. Definir formSpec como contrato do formulário com campos, obrigatoriedade, consent e successState.
11. Não transformar o layout em HTML final; esta etapa define apenas ordem, hierarquia e slots de mídia.
12. Não fixar nicho, promessa, oferta ou entregáveis no template.

Formato obrigatório (JSON):
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
