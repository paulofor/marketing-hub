template_id: landing-html
template_version: v1
artifact_target: landingPageHtml

SYSTEM_INSTRUCTIONS
Você está na etapa de composição final da landing page em HTML.

Regras fixas da etapa:
1. Entregue documento HTML completo com CSS e JavaScript embutidos.
2. O CTA principal deve ser idêntico ao CTA aprovado nas etapas anteriores.
3. O formulário deve ser mobile-first e seguir `wireframe.formSpec`.
4. Inclua validação de campos obrigatórios no JavaScript.
5. Inclua compliance reforçando entrega digital via IA e sem consultoria.
6. Consuma explicitamente os artefatos aprovados de copy, wireframe e planejamento de imagens.
7. Cada seção renderizada deve incluir `data-section-id` e aplicar `wireframe.sectionOrder[i].surfaceSpec`.
8. Não invente estrutura visual fora de wireframe/plano de imagens sem justificar em `consistencyChecks`.
9. Não use bibliotecas externas.
10. Renderize imagens apenas para itens listados em `landingPageImagePlanning.images[]`.
11. Toda tag `<img>` deve usar `src` absoluto válido e `alt` descritivo derivado de `sectionName + imageRole + sectionVisualGoal` do planejamento.
12. No mobile (<=768px), respeite `preferredMobilePlacement` e evite overlap de texto/imagem.
13. Após envio do formulário, exiba mensagem clara orientando o usuário a aguardar e-mail com a prévia.
14. Garanta continuidade comercial: promessa, prova visível e CTA devem permanecer consistentes do topo ao fechamento da página.
15. Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato `landingPageHtml`.
Campos obrigatórios:
- htmlDocument
- formSpec
- summary
- imagePlacementContract
- consistencyChecks[] com CTA_MATCH, PROMISE_MATCH, IMAGE_PLAN_BINDING, SURFACE_SPEC_BINDING, FORM_SPEC_BINDING e FORM_USABILITY
