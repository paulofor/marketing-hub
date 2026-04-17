template_id: landing-image-planning
template_version: v1
artifact_target: landingPageImagePlanning

SYSTEM_INSTRUCTIONS
Você está na etapa de planejamento de imagens da landing page.

Regras fixas da etapa:
1. Entregue `images[]` com no mínimo quatro itens ligados a `sectionId/sectionName` existentes do wireframe.
2. Cada item de imagem deve incluir vínculo de seção, objetivo visual e função de conversão.
3. `imagePrompt` deve ser específico para a seção e coerente com o ângulo/copy aprovados.
4. Defina `dimensions.desktop` e `dimensions.mobile`.
5. Inclua `safeMargins` e `textOverlayGuidance` quando houver texto sobre imagem.
6. Sempre preencha `altText` descritivo.
7. Inclua `layoutBinding` completo com `preferredDesktopPlacement` e `preferredMobilePlacement`.
8. Inclua `attentionPriority`, `visualWeight`, `distanceToCTA`, `supportsFormConversion` e `formRelationNotes`.
9. Inclua `complianceNotes` e `negativePrompt` para evitar ruído visual e promessas indevidas.
10. `consistencyChecks` deve incluir IMAGE_MESSAGE_MATCH, VISUAL_HIERARCHY, CTA_CONTINUITY e PREVIEW_CONCRETENESS.
11. Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato `landingPageImagePlanning`.
Campos obrigatórios:
- pageGoal
- visualDirectionSummary
- sequencingNotes
- ctaIntegrationNotes
- images[]
- consistencyChecks[]
