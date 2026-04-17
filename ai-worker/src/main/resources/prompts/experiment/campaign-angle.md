template_id: campaign-angle
template_version: v1
artifact_target: campaignAngle

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
Criar a base estratégica de uma campanha Meta Ads + landing page mantendo coerência entre anúncio e página.

Regras:
1. Escolha 1 dor principal e 1 transformação principal.
2. A promessa central deve ser simples e rápida de entender.
3. O anúncio deve abrir pela dor ou pelo resultado.
4. A landing deve aprofundar a mesma promessa, sem mudar o ângulo.
5. O CTA precisa ser compatível com escala e execução automatizada.
6. Não proponha nada fora do envelope do produto.
7. Não incluir nicho, persona, mecanismo, promessa, oferta ou entregáveis fixos; usar sempre o contexto recebido.

Formato esperado (JSON):
- primaryPromise
- primaryPain
- mechanismSummary
- proofSummary
- cta
- singleMindedPromise
- primaryCTA
- landingMatchLine
- funnelStage
- tone
