template_id: landing-page-copy
template_version: v4
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
- Gerar copy somente para os pontos textuais que o wireframe já definiu.
- Preservar exatamente a estrutura textual definida pelo wireframe: mesmas seções, mesma ordem, mesmos ids de elementos textuais.
- Não criar, sugerir ou devolver nenhum conteúdo fora do que foi solicitado pelo wireframe.

Regra central de contrato:
- O wireframe é a única fonte de verdade estrutural.
- A etapa copy não decide estrutura, não adiciona seções, não adiciona blocos e não cria metadados.
- A etapa copy apenas escreve o valor `texto` para ids textuais existentes no wireframe.

Elementos de contexto (usar apenas para escolher palavras, sem mudar a estrutura do wireframe):
- `nicho`: define com quem a landing está falando.
- `pain`: dores reais do público que devem aparecer com clareza quando o wireframe pedir texto de dor/promessa.
- `result`: resultado esperado por essas pessoas quando o wireframe pedir texto de resultado/benefício.
- `campaignAngle`: direcionamento-base do fluxo (promessa e framing principal da campanha).
- Esses elementos são contexto estratégico. Eles orientam tom, linguagem e argumentos, mas nunca autorizam criar novos campos, blocos, FAQs, CTAs, checks, notas ou seções.

Como identificar o que deve receber texto:
1. Percorra `pagina.corpo.secoes[]` do wireframe na ordem original.
2. Para cada seção, percorra `elementosSeccao` e `elementosInternos` preservando a ordem original.
3. Inclua em `bodySections[].items[]` somente elementos finais de texto que já existem no wireframe e que representam texto visível ou acionável da página, como `h1`, `h2`, `h3`, `p`, `li`, `a` e `button`.
4. Não inclua containers estruturais (`div`, `form`, `ul`, grids, rows, cards ou wrappers) quando eles apenas agrupam outros elementos.
5. Não inclua imagens (`img`) nem gere `altText`; imagens e assets já pertencem ao wireframe/planejamento visual.
6. Não inclua inputs (`input`) quando o wireframe não solicitar label/placeholder textual. Se o wireframe trouxer placeholder explicitamente preenchível para um input, use o id desse input somente para o texto do placeholder.
7. Se uma seção do wireframe não tiver nenhum ponto textual solicitado, omita essa seção do `bodySections`.

Prioridade de decisão para cada texto:
1. Limites de tamanho do próprio elemento no wireframe (`texto.tamMinimo` e `texto.tamMaximo`).
2. Tag/componente/interação do próprio elemento no wireframe (`tag`, `componente`, `interacao`).
3. Objetivo e nome da seção no wireframe.
4. Contexto estratégico (`nicho`, `pain`, `result`, `campaignAngle`) apenas como apoio de linguagem.

Regras obrigatórias:
1. `sectionId` deve ser exatamente um `id` existente em `pagina.corpo.secoes[]` do wireframe.
2. Cada `items[].id` deve ser exatamente um id de elemento existente dentro da seção correspondente do wireframe.
3. É proibido criar ids, aliases, novas seções ou itens que não existam no wireframe.
4. Cada item deve conter somente `id` e `texto`.
5. Cada seção deve conter somente `sectionId` e `items`.
6. A raiz do JSON deve conter somente `bodySections`.
7. É proibido devolver `faq`, `ctaBlocks`, `formMicrocopy`, `imageAccessibilityPlan`, `consistencyChecks`, `complianceNotes`, `pageGoal`, `primaryCTA`, `messageMatchSource`, `messageMatchNotes` ou qualquer outro campo não previsto no schema.
8. É proibido criar FAQ, objeções, notas de compliance, planos de acessibilidade, checks internos ou metadados, mesmo que pareçam úteis para marketing.
9. Se o wireframe não definiu uma área para determinado conteúdo, esse conteúdo não deve aparecer na resposta.
10. Respeite `texto.tamMaximo`; quando houver dúvida, prefira texto mais curto, claro e vendável.
11. Não vazar termos técnicos/metainstruções no texto final exibido ao usuário.
12. Manter continuidade com promessa e CTA do anúncio somente dentro dos campos textuais que o wireframe já pediu.
13. Se houver conflito entre contexto e wireframe, priorize sempre o wireframe.
14. Responder somente com JSON válido aderente ao schema da etapa.

CASE_DATA
{{CASE_DATA_BLOCK}}

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato `landingPageCopy`.

Formato obrigatório e único:
- `bodySections[]`
  - `sectionId`: id literal da seção no wireframe.
  - `items[]`: lista na ordem do wireframe.
    - `id`: id literal do elemento textual no wireframe.
    - `texto`: copy final para esse elemento.

Critérios mínimos de aceite no próprio output:
- A raiz do JSON contém apenas `bodySections`.
- Todas as seções listadas em `bodySections` existem no wireframe.
- Todos os ids em `items[]` existem dentro da respectiva seção no wireframe.
- Nenhum campo extra é retornado.
- Nenhuma FAQ, bloco de CTA, microcopy extra, plano de imagem, nota ou check é retornado se não estiver definido como elemento textual no wireframe.
- Textos respeitam os limites `texto.tamMinimo` e `texto.tamMaximo` de cada elemento sempre que esses limites existirem.
