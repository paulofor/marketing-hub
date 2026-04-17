template_id: landing-copy
template_version: v1
artifact_target: landingPageCopy

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

Objetivo da landing:
Continuar exatamente a promessa do anúncio clicado e levar o usuário ao mesmo CTA declarado no anúncio.

Regras:
1. Repita a mesma promessa no hero (hero.headline + hero.promise) e em pageGoal.
2. messageMatchSource deve citar qual headline do anúncio está sendo espelhada e messageMatchNotes precisa explicar a continuidade.
3. hero.ctaLabel, primaryCTA e todos os ctaBlocks devem usar exatamente o mesmo texto do CTA aprovado.
4. bodySections precisa ter no mínimo quatro blocos cobrindo dor, mecanismo, prova e oferta.
5. ctaBlocks deve mapear onde cada CTA aparece (hero, mid, final, sticky ou inline).
6. faq precisa trazer pelo menos três perguntas com objectionTag.
7. consistencyChecks deve listar no mínimo CTA_MATCH, PROMISE_MATCH e GOOGLE_LANDING_BEST_PRACTICES.
8. complianceNotes deve reforçar entrega 100% digital (gerada por IA) e sem consultoria.
9. Não fixar contexto: use apenas informações do caso recebido.

Formato obrigatório (JSON):
- pageGoal
- messageMatchSource
- messageMatchNotes
- primaryCTA
- hero { eyebrow, headline, subheadline, promise, supportingCopy, proofBadge, microcopy, ctaLabel, ctaUrl, ctaMatchNotes }
- bodySections[] com sectionId, sectionType, title, summary, bullets, copy, ctaSupport, sectionDependsOn, messageMatchNotes
- ctaBlocks[] com placement, ctaVariant, ctaLabel, ctaUrl, matchAdCta, ctaSupport, messageMatchNotes
- faq[] com question, answer, objectionTag
- consistencyChecks[] com check, status (PASS/WARN/FAIL), details
- complianceNotes
