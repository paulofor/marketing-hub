template_id: landing-copy
template_version: v1
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
3. `hero.ctaLabel`, `primaryCTA` e todos os `ctaBlocks` devem manter o mesmo CTA aprovado.
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

Diretriz obrigatória para HERO:
14. `hero.headline`: foco na transformação principal (curta, direta, comercial).
15. `hero.subheadline`: explicar ativo inicial/prova/primeiro passo quando existir, sem presumir formato fixo.
16. `hero.supportingCopy`: contextualizar dor + urgência + ponte para a oferta principal atual.
17. Não presumir formatos universais (kit, PDF, plano fixo, ciclo com duração fixa, regeneração etc.).

Diretriz obrigatória para seção de OFERTA:
18. Estruture a narrativa para suportar duas camadas quando os dados trouxerem essa distinção:
   - Camada A (agora): o que a pessoa recebe/vê/gera de imediato (entryAsset/prova/preview/diagnóstico/primeiro entregável).
   - Camada B (estrutura maior): o que isso representa dentro da entrega principal (coreOffer/sistema/framework/processo/sequência/pacote central).
19. Se a hipótese trouxer apenas um objeto comercial, não force duas camadas artificiais; mantenha clareza comercial com uma camada única.
20. Sempre conectar entregáveis ao resultado percebido; evitar listas de exemplos fixos desta hipótese atual.

Diretriz obrigatória para títulos de seção:
21. Títulos devem ser comerciais e específicos ao caso atual, porém reutilizáveis para hipóteses futuras.
22. Não usar rótulos internos de taxonomia no output.
23. Não fixar nomenclaturas da oferta atual como padrão universal.

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
- hero { eyebrow, headline, subheadline, promise, supportingCopy, proofBadge, microcopy, ctaLabel, ctaUrl, ctaMatchNotes }
- bodySections[] com sectionId, sectionType, title, summary, bullets, copy, ctaSupport, sectionDependsOn, messageMatchNotes
- ctaBlocks[] com placement, ctaVariant, ctaLabel, ctaUrl, matchAdCta, ctaSupport, messageMatchNotes
- faq[] com question, answer, objectionTag
- consistencyChecks[] com check, status (PASS/FAIL/WARNING), details
