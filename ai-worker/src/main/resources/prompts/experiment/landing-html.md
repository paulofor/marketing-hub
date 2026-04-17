template_id: landing-html
template_version: v1
artifact_target: landingPageHtml

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
Unificar copy, wireframe e planejamento de imagens aprovados em landing final pronta para uso no formulário do experimento.

Regras:
1. Entregar documento HTML completo com CSS e JavaScript embutidos.
2. O CTA principal deve ser idêntico ao CTA aprovado nas etapas anteriores.
3. O formulário deve ser mobile-first e renderizado exatamente a partir de wireframe.formSpec.
4. Incluir validação de campos obrigatórios no JavaScript.
5. Incluir bloco de compliance reforçando entrega digital via IA e sem consultoria.
6. Consumir explicitamente os artefatos anteriores (copy, wireframe e planejamento de imagens).
7. Cada seção renderizada deve incluir data-section-id e aplicar wireframe.sectionOrder[i].surfaceSpec.
8. Não inventar estrutura visual fora do layout/plano de imagens sem justificar nos consistencyChecks.
9. Não usar bibliotecas externas.
10. Renderizar imagens somente para itens listados em landingPageImagePlanning.images[].
11. Toda tag <img> permitida deve usar src absoluto válido e altText do planejamento.
12. No mobile (<=768px), respeitar preferredMobilePlacement e impedir overlap de texto/imagem.
13. Após envio do formulário, exibir mensagem clara orientando o usuário a aguardar o e-mail com a prévia.

Formato obrigatório (JSON):
- htmlDocument
- summary
- consistencyChecks[] com CTA_MATCH, PROMISE_MATCH, IMAGE_PLAN_BINDING, SURFACE_SPEC_BINDING, FORM_SPEC_BINDING e FORM_USABILITY
