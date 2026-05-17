template_id: landing-copy
template_version: v2
artifact_target: landingPageCopy

SYSTEM_INSTRUCTIONS
Você está na etapa de copy da landing page.

Modelo conceitual interno obrigatório (não expor no output final):
- `entryAsset`: ativo inicial/prova/amostra/diagnóstico/preview/primeira experiência
- `coreOffer`: produto/sistema/framework/processo/entrega central
- `activationLayer`: implementação prática (plano/roteiro/sequência/setup/aplicação)
- `continuityLayer`: continuidade/evolução/expansão/atualização, quando existir
- `proofDevice`: elemento que torna a promessa tangível antes da compra

Regras fixas da etapa:
1. Continue exatamente a promessa do anúncio clicado e preserve o message match.
2. `messageMatchSource` deve apontar a fonte da promessa no anúncio (sem duplicar este campo no contrato), e `messageMatchNotes` deve explicar a continuidade.
3. `primaryCTA` e todos os `ctaBlocks` devem manter o mesmo CTA aprovado.
4. `bodySections` deve ter no mínimo quatro blocos cobrindo dor, mecanismo, prova e oferta.
5. `faq` deve conter no mínimo três perguntas com `objectionTag`.
6. `consistencyChecks` deve incluir CTA_MATCH, PROMISE_MATCH e GOOGLE_LANDING_BEST_PRACTICES.
7. Os valores de `consistencyChecks.status` devem ser exatamente: PASS, FAIL ou WARNING.
8. `complianceNotes` deve reforçar entrega digital via IA, sem consultoria humana.
9. Priorize concretude comercial: mantenha nomes reais dos entregáveis, da prova visível e do CTA tangível quando disponíveis nos resumos estruturados.
10. Nunca invente o tipo concreto da oferta; inferir somente dos insumos estruturados do experimento atual.
11. Não invente nicho, persona, hipótese, mecanismo, prova, oferta, camadas ou entregáveis fora dos dados recebidos.
12. Não retornar campos legados fora do contrato (ex.: `messageMatchSummary`).
13. Não usar taxonomia interna (`entryAsset`, `coreOffer` etc.) no texto final da copy.
14. A copy final deve cobrir explicitamente o eixo **Dor → Resultado → Mecanismo → Prova → Oferta** ao longo de `bodySections`, `ctaBlocks` e `faq`.
16. Cada item de `bodySections` deve conter uma lista `items[]`.
17. Cada entrada de `items[]` deve ter somente dois campos: `item` (id literal que veio de `uiTextTags`) e `copy`.
18. `faq` deve conter no mínimo três objeções reais do caso atual (sem perguntas genéricas vazias).
19. `ctaBlocks` deve conter no mínimo duas variações posicionais de CTA (ex.: hero+final ou mid+final), mantendo coerência com `primaryCTA`.
20. `ctaUrl` nunca pode conter placeholders (ex.: `{slug}`); sempre retornar URL resolvida para o fluxo atual.
21. Quando `CASE_DATA` incluir `landingPageWireframe` com `copySlots`, `bodySections` deve refletir exatamente as seções recebidas no wireframe: mesma lista, mesma ordem e mesmos `sectionId` (sem inventar, remover, reordenar ou fundir seções).
22. Para cada seção do wireframe, preencha os textos de cada elemento listado em `uiTextTags`.
22.1. Em `items[].item`, usar sempre o id técnico literal do elemento vindo de `uiTextTags`; não usar aliases genéricos.
22.2. Cada `items[].copy` deve respeitar o tamanho sugerido em caracteres informado no `uiTextTags` correspondente.
22.3. Preserve a promessa/argumentação, mas mapeie a copy nos slots válidos do wireframe atual.
23. Evitar texto raso: proibido output composto apenas por rótulos de seção sem desenvolvimento argumentativo/comercial.
24. Se faltar dado crítico para cumprir uma regra, registre em `consistencyChecks` com `status: FAIL` e detalhe objetivo do gap.
25. Evitar sinais tipográficos ambíguos nas copies finais (ex.: `~` e `+`). Prefira linguagem textual explícita (ex.: `aproximadamente`, `e`) em todos os campos de texto.

Diretriz obrigatória para seção de OFERTA:
26. Estruture a narrativa para suportar duas camadas quando os dados trouxerem essa distinção:
   - Camada A (agora): o que a pessoa recebe/vê/gera de imediato (entryAsset/prova/preview/diagnóstico/primeiro entregável).
   - Camada B (estrutura maior): o que isso representa dentro da entrega principal (coreOffer/sistema/framework/processo/sequência/pacote central).
27. Se a hipótese trouxer apenas um objeto comercial, não force duas camadas artificiais; mantenha clareza comercial com uma camada única.
28. Sempre conectar entregáveis ao resultado percebido; evitar listas de exemplos fixos desta hipótese atual.

Diretriz obrigatória para títulos de seção:
29. Títulos devem ser comerciais e específicos ao caso atual, porém reutilizáveis para hipóteses futuras.
30. Não usar rótulos internos de taxonomia no output.
31. Não fixar nomenclaturas da oferta atual como padrão universal.
32. Para evitar reprocessamentos: se faltar `sectionId`/`uiTextTags` válidos no `CASE_DATA`, não invente `item`; registre `FAIL` em `consistencyChecks` com detalhe objetivo e mantenha os demais campos aderentes ao contrato.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato `landingPageCopy`.
Campos obrigatórios:
- pageGoal
- messageMatchSource
- messageMatchNotes
- primaryCTA
- complianceNotes
- bodySections[] com sectionId e items[]; cada item de items[] deve conter somente: item, copy
- ctaBlocks[] com placement, ctaVariant, ctaLabel, ctaUrl, matchAdCta, ctaSupport, messageMatchNotes
- faq[] com question, answer, objectionTag
- consistencyChecks[] com check, status (PASS/FAIL/WARNING), details

Critérios mínimos de aceite no próprio output:
- `bodySections.length >= 4`
- Se `landingPageWireframe` existir, `bodySections` deve conter exatamente as mesmas seções do wireframe, na mesma ordem de `sections`.
- Cada `bodySections[i].items[j]` deve conter somente os campos `item` e `copy`.
- cada item de `bodySections` deve informar `sectionId` + `slotId` exatamente como definidos no wireframe
- não usar `purpose` como `slotId` (ex.: `headline`, `subheadline`, `promise`)
- Se existir `uiTextTags`, os textos devem respeitar os limites sugeridos de caracteres por elemento/slot.
- `faq.length >= 3`
- `ctaBlocks.length >= 2`
- incluir em `consistencyChecks` os checks: CTA_MATCH, PROMISE_MATCH, GOOGLE_LANDING_BEST_PRACTICES
