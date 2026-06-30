template_id: landing-page-copy
template_version: v5
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
- O contrato de promessa única em `CASE_DATA` é a fonte de verdade comercial quando trouxer `singlePain`, `freeReward`, `funnelPromise`, `primaryCta` ou `campaignObjective`.
- Qualidade comercial obrigatória: a copy deve vender a transformação antes de explicar o formato da prova/amostra. Evite abrir com frases como “gere uma amostra/PDF”; abra com o resultado que o público quer e o problema que ele quer remover, usando a prova visual como redução de risco.
- Quando `campaignObjective` for `SALES`, escreva como página de venda direta low-ticket: o CTA principal deve comprar/ir ao checkout, `freeReward` deve aparecer como preview/prova visual da oferta e nenhum texto deve prometer entrega gratuita por formulário antes da compra.
- Quando `campaignObjective` for `LEADS`, escreva como página de captura: o CTA principal pode prometer a entrega gratuita definida em `freeReward`.
- Narrativa universal obrigatória: quando o wireframe pedir textos de promessa, dor, mecanismo, prova, oferta e ação, escreva seguindo **Dor → Resultado → Mecanismo → Prova → Oferta → Ação**.
- Especificidade obrigatória: todo texto deve parecer feito para o nicho e para a dor recebidos no contexto; evite frases que serviriam para qualquer mercado.
- Mecanismo plausível: em passos/cards explicativos, mostre por que a solução funciona de forma simples, sem promessa mágica e sem jargão interno.
- CTA orientado ao benefício: botões e links devem repetir a ação principal recebida em `primaryCta`. Em `SALES`, use compra/checkout; em `LEADS`, use a recompensa gratuita, como “Receber as 3 mensagens”.
- Coerência entre CTA e formulário obrigatória: se o formulário pede somente `nome` e `email`, prometa apenas a entrega gratuita concreta de baixo esforço recebida em `freeReward`. Não prometa personalização profunda, briefing completo, diagnóstico detalhado imediato, “prévia” genérica ou sistema completo.
- Clareza de formulário obrigatória: se o wireframe trouxer labels, placeholders, microcopy ou botão do formulário, escreva textos explícitos para `nome`, `email` e CTA, para que o usuário não veja campos vazios sem orientação.
- CTA visualmente curto e forte: textos de botão devem ser curtos, específicos e com verbo de ação; evite frases longas que quebrem o layout, pareçam link comum ou reduzam a percepção de botão premium.
- Prova de valor rápida: em listas e cards, prefira frases que conectem item entregue → benefício prático → redução de esforço/dor, sem depender de termos genéricos como “mini-kit”, “amostra” ou “material” isoladamente.
- A etapa copy não decide estrutura, não adiciona seções, não adiciona blocos e não cria metadados.
- A etapa copy apenas escreve o valor `texto` para ids textuais existentes no wireframe.
- Princípio de pouco esforço obrigatório: o usuário não quer fazer esforço para entender a comunicação da página; portanto, cada texto deve ser claro em leitura rápida, reduzir carga cognitiva, evitar explicação longa sem necessidade e conduzir naturalmente para o próximo CTA ou próximo passo definido pelo wireframe.

Elementos de contexto (usar apenas para escolher palavras, sem mudar a estrutura do wireframe):
- `nicho`: define com quem a landing está falando.
- `pain`: dores reais do público que devem aparecer com clareza quando o wireframe pedir texto de dor/promessa.
- `result`: resultado esperado por essas pessoas quando o wireframe pedir texto de resultado/benefício.
- `campaignAngle`: direcionamento-base do fluxo (promessa e framing principal da campanha).
- `singlePain`, `freeReward`, `funnelPromise`, `primaryCta`, `campaignObjective`: contrato de promessa única que deve ser repetido em anúncio, botão, formulário e entrega.
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
12. Para botão submit de formulário, use CTA compatível com o que o formulário realmente coleta; com apenas nome/e-mail, nunca sugira personalização profunda, briefing completo, diagnóstico detalhado imediato, prévia genérica ou sistema completo.
13. Manter continuidade com promessa e CTA do anúncio somente dentro dos campos textuais que o wireframe já pediu, repetindo `funnelPromise`, `freeReward` e `primaryCta` quando estiverem disponíveis.
14. Antes de responder, revise se H1, subtítulo, CTA do hero e submit contam a mesma história: dor removida, resultado prometido, mecanismo plausível, prova/entrega e ação de baixo esforço.
15. Se houver conflito entre contexto e wireframe, priorize sempre o wireframe.
16. Responder somente com JSON válido aderente ao schema da etapa.

CASE_DATA
{{CASE_DATA_BLOCK}}

Regra para os insumos MOIS em `geralandingReferenceInsights`: use apenas padrões abstratos de copy vencedora; não copie frases, claims, marcas, URLs nem promessas fora do contrato do experimento atual.

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
