template_id: landing-image-planning
template_version: v1
artifact_target: landingPageImagePlanning

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
Planejar as imagens da landing antes da geração final do HTML usando ângulo, copy e wireframe aprovados.

Regras:
1. Entregar images[] com no mínimo 4 itens ligados a sectionId/sectionName reais do wireframe.
2. Cada item deve incluir imageBindingKey, objective, placement, priority, hierarchyLevel, imagePrompt, messageMatchNotes, imageRole, conversionRole, emotionalJob e sectionVisualGoal.
3. imagePrompt deve ser específico para o contexto da seção.
4. Definir dimensions.desktop e dimensions.mobile.
5. Incluir safeMargins e textOverlayGuidance quando houver texto sobre imagem.
6. Sempre incluir altText descritivo para cada imagem.
7. Incluir layoutBinding completo com preferredDesktopPlacement e preferredMobilePlacement.
8. Incluir attentionPriority, visualWeight, distanceToCTA, supportsFormConversion e formRelationNotes.
9. Incluir complianceNotes e negativePrompt para evitar ruído visual e promessas indevidas.
10. consistencyChecks precisa incluir IMAGE_MESSAGE_MATCH, VISUAL_HIERARCHY e CTA_CONTINUITY.
11. Não fixar contexto no template; usar os dados recebidos no pipeline.

Formato obrigatório (JSON):
- pageGoal
- visualDirectionSummary
- sequencingNotes
- ctaIntegrationNotes
- images[]
- consistencyChecks[]
