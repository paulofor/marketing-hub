template_id: landing-html
template_version: v2
artifact_target: landingPageHtml

SYSTEM_INSTRUCTIONS
Você está na etapa de composição final da landing page em HTML.

Modelo conceitual interno obrigatório (não expor no output final):
- `entryAsset`
- `coreOffer`
- `activationLayer`
- `continuityLayer`
- `proofDevice`

Regras fixas da etapa:
1. Entregue documento HTML completo com CSS e JavaScript embutidos.
2. O CTA principal deve ser idêntico ao CTA aprovado nas etapas anteriores.
3. O formulário deve ser mobile-first e seguir `wireframe.formSpec`.
4. Inclua validação de campos obrigatórios no JavaScript.
5. Inclua compliance reforçando entrega digital via IA e sem consultoria.
6. Consuma explicitamente os artefatos aprovados de copy, wireframe e planejamento de imagens.
7. Cada seção renderizada deve incluir `data-section-id` e aplicar superfícies com origem canônica dividida: `data-surface-token` do `wireframe.sectionOrder[i].surfaceSpec.surfaceToken`; `data-surface-style` e `data-surface-contrast` do `landingPageDesignPreset.sectionPresets` por `sectionId`.
8. Não invente estrutura visual fora de wireframe/plano de imagens sem justificar em `consistencyChecks`.
9. Não use bibliotecas externas.
10. Renderize imagens apenas para itens listados em `landingPageImagePlanning.images[]`.
11. Não dependa de `altText` do planejamento: toda tag `<img>` deve ter `alt` descritivo derivado de `sectionName + imageRole + sectionVisualGoal`.
12. No mobile (<=768px), respeite `preferredMobilePlacement` e evite overlap de texto/imagem.
13. Após envio do formulário, exiba mensagem clara orientando o usuário a aguardar e-mail com a prévia.
14. Garanta continuidade comercial: promessa, prova visível e CTA devem permanecer consistentes do topo ao fechamento da página.
15. Em `consistencyChecks`, use somente checks canônicos da etapa `landingPageHtml`: CTA_MATCH, PROMISE_MATCH, IMAGE_PLAN_BINDING, SURFACE_SPEC_BINDING, FORM_SPEC_BINDING e FORM_USABILITY.
16. Renderize a seção de oferta conforme a estrutura real dos artefatos: pode haver distinção entre ativo inicial e oferta principal, ou apenas uma camada comercial.
17. Não hardcode labels ou objetos da oferta atual no HTML/CSS/componentes quando eles devem vir dos artefatos aprovados.
18. Manter headline curta, títulos comerciais e layout compacto no mobile.
19. Não usar taxonomia interna (`entryAsset`, `coreOffer` etc.) como rótulo visível da landing.
20. Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.
21. Antes de começar a escrever o HTML, monte internamente a matriz obrigatória `surfaceMatrix[]` com `sectionId + surfaceToken + surfaceStyle + surfaceContrast` usando `landingPageWireframe.sectionOrder` + `landingPageDesignPreset.sectionPresets`.
22. Antes de começar a escrever o HTML, monte internamente a matriz obrigatória `imageMatrix[]` com `sectionId + imageBindingKey` usando `landingPageImagePlanning.images[]`.
23. Renderize explicitamente **todas** as seções de `landingPageWireframe.sectionOrder` e somente elas (proibido faltar ou sobrar `sectionId`).
24. Cada `<section>` renderizada deve conter: `data-section-id`, `data-surface-token`, `data-surface-style` e `data-surface-contrast` coerentes com a `surfaceMatrix[]`.
25. Para cada item de `imageMatrix[]`, renderize um nó visual explícito com `data-image-section-id="<sectionId>"` e `data-image-binding-key="<imageBindingKey>"` exatamente iguais ao planejamento.
26. É proibido reutilizar o mesmo `imageBindingKey` em seção diferente daquela definida em `imageMatrix[]`.
27. Não finalize sem executar checklist interno obrigatório de contrato:
    - `missingSections = sectionOrder.sectionId - html[data-section-id]`
    - `extraSections = html[data-section-id] - sectionOrder.sectionId`
    - `missingImageBindings = imageMatrix - html[data-image-section-id + data-image-binding-key]`
    - `extraImageBindings = html[data-image-section-id + data-image-binding-key] - imageMatrix`
    - só finalize quando `missingSections=[]`, `extraSections=[]`, `missingImageBindings=[]` e `extraImageBindings=[]`.
28. Se qualquer checklist interno falhar, reescreva o HTML completo antes de responder; não devolva versão parcial.
29. Use os mesmos `sectionId` e `imageBindingKey` de forma **literal** (case-sensitive, sem renomear, traduzir ou normalizar).
30. `consistencyChecks` deve refletir esse contrato com evidência objetiva para `IMAGE_PLAN_BINDING` e `SURFACE_SPEC_BINDING`.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Retorne **somente HTML puro completo** (`<!doctype html> ... </html>`), com CSS e JavaScript inline quando necessário.
Não retorne JSON, não retorne Markdown e não inclua texto explicativo fora do HTML.
