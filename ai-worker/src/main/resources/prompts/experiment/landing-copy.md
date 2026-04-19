template_id: landing-copy
template_version: v1
artifact_target: landingPageCopy

SYSTEM_INSTRUCTIONS
Você está na etapa de copy da landing page.

Regras fixas da etapa:
1. Continue exatamente a promessa do anúncio clicado e preserve o message match.
2. `messageMatchSource` deve apontar a fonte da promessa no anúncio (sem duplicar este campo no contrato), e `messageMatchNotes` deve explicar a continuidade.
3. `hero.ctaLabel`, `primaryCTA` e todos os `ctaBlocks` devem manter o mesmo CTA aprovado.
4. `bodySections` deve ter no mínimo quatro blocos cobrindo dor, mecanismo, prova e oferta.
5. `faq` deve conter no mínimo três perguntas com `objectionTag`.
6. `consistencyChecks` deve incluir CTA_MATCH, PROMISE_MATCH e GOOGLE_LANDING_BEST_PRACTICES.
7. Os valores de `consistencyChecks.status` devem ser exatamente: PASS, FAIL ou WARNING.
8. `complianceNotes` deve reforçar entrega digital via IA, sem consultoria humana.
9. Priorize concretude comercial: manter nome de entregáveis, prova visível e CTA tangível quando disponíveis nos resumos estruturados.
10. Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.

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
