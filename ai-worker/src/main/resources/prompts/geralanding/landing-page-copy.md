template_id: landing-page-copy
template_version: v3
artifact_target: landingPageCopy

Estamos Trabalhando nesse contexto:

```xml
<nicho>
	<hipotese>
		<pain/>
		<result/>
		<mecanismo/>
		<proof/>
		<oferta/>
		<experimento>
			<campaignAngle/>
			<adCopy/>
			<adImageBriefing/>
			<landingPageWireframe/>
			<landingCopy/>
			<landingPromptImagem/>
				<listaImagem/>
			<landingPromptImagem/>
			<landingPresetDesign/>
			<landingHtml/>
		</experimento>
	</hipotese>
</nicho>
```

Wireframe da Landing: IMPORTANTE !!
{dados-landingPageWireframe}

SYSTEM_INSTRUCTIONS
Objetivo principal desta etapa:
- Ler o JSON que vem logo após "Wireframe da Landing: IMPORTANTE !!".
- Para cada seção do wireframe, gerar uma lista de itens contendo somente `id` e `texto`, preservando exatamente os ids originais do wireframe.
- Respeitar obrigatoriamente os sinais de `uiTags` e os limites sugeridos em `uiSizeTexts`.

Elementos de contexto (usar para direcionar a argumentação, sem mudar a estrutura do wireframe):
- `nicho`: define com quem a landing está falando.
- `pain`: dores reais do público que devem aparecer com clareza.
- `result`: resultado esperado por essas pessoas (transformação desejada).
- `campaignAngle`: direcionamento-base do fluxo (promessa e framing principal da campanha).
- Esses elementos são contexto estratégico. Eles orientam tom, linguagem e argumentos, mas não substituem `uiSizeTexts`, `uiTags` e `slotId`.

Prioridade de decisão (ordem obrigatória):
1. `uiSizeTexts` (tamanho sugerido por campo textual).
2. `uiTags`/`uiTextTags` (intenção do texto por seção/slot).
3. Contexto estratégico (`nicho`, `pain`, `result`, `campaignAngle`) para escolher linguagem, dor e promessa.
4. Demais dados do CASE_DATA como apoio complementar.

Regras obrigatórias:
1. `sectionId` e `slotId` devem ser exatamente os mesmos do wireframe (sem inventar ids e sem aliases).
2. Cada texto final deve ficar o mais próximo possível do intervalo sugerido em `uiSizeTexts` (mínimo/máximo). Evite extrapolar.
3. Se faltar `uiSizeTexts` em algum slot obrigatório, preencher mesmo assim com base em `uiTags` + contexto estratégico e registrar `consistencyChecks.status = WARN` com detalhe objetivo.
4. Não vazar termos técnicos/metainstruções no texto final exibido ao usuário.
5. Manter continuidade com promessa e CTA do anúncio, preservando o direcionamento de `campaignAngle`.
6. Se houver conflito entre contexto e wireframe, priorize wireframe (`uiTags` + `uiSizeTexts`).
7. Responder somente com JSON válido aderente ao schema da etapa.
8. Campos explicativos (`messageMatchNotes`, `ctaMatchNotes`, `matchAdCta`, `consistencyChecks.details`) também são conteúdo de negócio e devem ser escritos em linguagem natural para time de marketing; nunca referencie nomes de campos/paths técnicos (ex.: `campaignAngle.primaryCTA`, `landingMatchLine`, `mechanismSummary`) e nunca escreva instruções como “usa literalmente”, “idêntico ao anúncio” ou “âncora para o formulário”.
9. Proibido copiar trechos literais do prompt, schema, metadados ou contrato. Se detectar risco de vazamento técnico em qualquer campo textual, reescreva o texto antes de finalizar o JSON.

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
- bodySections[] com sectionId e items[] (cada item contendo apenas id e texto)
- ctaBlocks[] com placement, ctaVariant, ctaLabel, ctaUrl, matchAdCta, ctaSupport, messageMatchNotes
- faq[] com question, answer, objectionTag
- consistencyChecks[] com check, status (PASS/WARN/FAIL), details

Critérios mínimos de aceite no próprio output:
- `bodySections.length >= 4`
- Cada `bodySections[i].items[j].id` deve existir na seção correspondente do wireframe (`elementosSeccao` e filhos), sem renomear ids.
- cada `bodySections[i].items.length >= 3`
- `faq.length >= 3`
- `ctaBlocks.length >= 2`
- incluir em `consistencyChecks` os checks: CTA_MATCH, PROMISE_MATCH, GOOGLE_LANDING_BEST_PRACTICES
- textos devem priorizar `uiTags` e respeitar `uiSizeTexts` do wireframe
