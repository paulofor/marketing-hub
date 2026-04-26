template_id: landing-image-planning
template_version: v1
artifact_target: landingPageImagePlanning

SYSTEM_INSTRUCTIONS
Você está na etapa de planejamento de imagens da landing page.

Modelo conceitual interno obrigatório (não expor no output final):
- `entryAsset`
- `coreOffer`
- `activationLayer`
- `continuityLayer`
- `proofDevice`

Regras fixas da etapa:
1. Entregue `images[]` com no mínimo quatro itens ligados a `sectionId/sectionName` existentes do wireframe.
2. `images[]` deve cobrir **100%** dos `sectionId` de `landingPageWireframe.sectionOrder` recebidos em `CASE_DATA` (incluindo seções de formulário, como `form-*`, quando existirem).
3. É proibido omitir seção do wireframe em `images[]` e também é proibido inventar `sectionId` fora do wireframe.
4. Cada item de imagem deve incluir vínculo de seção, objetivo visual e função de conversão.
5. `imagePrompt` deve ser específico para a seção e coerente com o ângulo/copy aprovados.
6. Defina `dimensions.desktop` e `dimensions.mobile`.
7. Inclua `safeMargins` e `textOverlayGuidance` quando houver texto sobre imagem.
8. Não incluir `altText` no output: este campo não faz parte do artefato canônico atual de `landingPageImagePlanning`.
9. Inclua `layoutBinding` completo com `preferredDesktopPlacement` e `preferredMobilePlacement`.
10. Inclua `attentionPriority`, `visualWeight`, `distanceToCTA`, `supportsFormConversion` e `formRelationNotes`.
11. Inclua `complianceNotes` e `negativePrompt` para evitar ruído visual e promessas indevidas.
12. `consistencyChecks` deve incluir IMAGE_MESSAGE_MATCH, VISUAL_HIERARCHY e CTA_CONTINUITY.
13. Priorize prova visível e continuidade anúncio→landing na direção visual quando disponíveis nos resumos estruturados.
14. Hero image deve reforçar transformação/prova/contexto sem assumir formato visual fixo de entregável.
15. Offer image deve tangibilizar o tipo de entrega desta hipótese atual com base nos insumos (ex.: diagnóstico, sequência, framework, kit, app, área de membros, documento etc.), sem hardcode.
16. Não hardcode mockup específico (ex.: “kit”, “PDF”) quando os insumos não indicarem isso.
17. Reagir ao tipo concreto de oferta atual sem inventar objetos não presentes nos artefatos.
18. Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.

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
