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

Nicho: {{NICHE_NAME}}

Dor: {{PAIN_JSON}}

Resultado: {{RESULT_JSON}}

Contrato de promessa única:
- Dor única: {dados-singlePain}
- Prova/preview ou recompensa única: {dados-freeReward}
- Promessa do funil: {dados-funnelPromise}
- CTA principal: {dados-primaryCta}
- Objetivo da campanha: {dados-campaignObjective}

{{prompt-regras-globais}}

# Insumos MOIS de páginas vencedoras

Use os padrões abaixo apenas como referência abstrata de mercado para estruturar a landing. Não copie texto, layout, marca, URL, claims específicos nem identidade de páginas de terceiros. Preserve sempre o contrato do experimento atual.

{{geralandingReferenceInsights}}


Ângulo da Campanha que vai ser publicada:
{{dados-campaignAngle}}

Copy do Anuncio:
{{dados-adCopy}}

Briefing das Imagens dos Anuncios:
{{dados-adImageBriefing}}


template_id: landing-wireframe
template_version: v7
artifact_target: landingPageWireframe

# Objetivo da etapa

Gerar somente o wireframe estrutural da landing em JSON válido, aderente ao schema `landing-page-wireframe-schema.json`.

O wireframe define seções, hierarquia, elementos, intenção comercial, assets esperados, formulário e navegação. O acabamento visual responsivo final é responsabilidade da etapa `landing-page-design-preset`.

# Contrato obrigatório

- Se houver contrato de promessa única no contexto, o wireframe deve estruturar hero, prova, entregáveis, formulário e CTAs para a mesma dor, recompensa gratuita, promessa e CTA.
- O wireframe não pode trocar a recompensa gratuita por diagnóstico, prévia genérica, amostra vaga, consultoria, sistema completo ou outro ativo fora do contrato.
- Se `campaignObjective` for `SALES`, substitua mentalmente “recompensa gratuita” por prova/preview da oferta paga e “formulário” por CTA de checkout. Não crie formulário de captura como etapa obrigatória antes do checkout.

- Entregar somente JSON válido com raiz obrigatória: `definicoes` e `pagina`.
- `definicoes` deve conter exatamente: `estrutura`, `posicao`, `layout`, `mistas`.
- Cada categoria de `definicoes` deve conter `desktop[]` e `mobile[]`.
- Cada item de definição deve conter somente: `nome`, `atributoCss`, `valor`.
- Em `pagina` e em todos os elementos, toda classe aplicada deve aparecer somente em `estilos[]`.
- Em `estilos[]`, use exclusivamente nomes existentes em `definicoes.*.desktop[].nome` ou `definicoes.*.mobile[].nome`.
- Não invente campos fora do schema.
- Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.
- `pagina.corpo.estilos` obrigatório: `bgBody`, `fontBase`, `textPrimary`, `marginReset`.
- Em toda `img`, declarar `asset`: `src`, `alt`, `width`, `height`.
- Em todo `input`, declarar `contratoCampo`: `type`, `name`, `autocomplete`, `required`, `placeholder`.
- Em todo elemento interativo, declarar `interacao` completa.
- `texto.conteudo` deve ser sempre `""` nesta etapa.

# Matriz oficial de grupos CSS do wireframe

Use apenas os atributos permitidos por grupo:

- `definicoes.posicao`: `position`, `top`, `right`, `bottom`, `left`, `z-index`.
- `definicoes.layout`: `display`, `float`, `clear`, `visibility`, `overflow`, `overflow-x`, `overflow-y`, `flex`, `flex-direction`, `flex-wrap`, `flex-flow`, `justify-content`, `align-items`, `align-content`, `align-self`, `gap`, `row-gap`, `column-gap`, `order`, `flex-grow`, `flex-shrink`, `flex-basis`, `grid`, `grid-template`, `grid-template-columns`, `grid-template-rows`, `grid-template-areas`, `grid-column`, `grid-column-start`, `grid-column-end`, `grid-row`, `grid-row-start`, `grid-row-end`, `grid-area`, `justify-items`, `place-items`, `place-content`.
- `definicoes.estrutura`: `width`, `height`, `min-width`, `min-height`, `max-width`, `max-height`, `box-sizing`, `margin`, `margin-top`, `margin-right`, `margin-bottom`, `margin-left`, `padding`, `padding-top`, `padding-right`, `padding-bottom`, `padding-left`.
- `definicoes.mistas`: `transform`, `translate`, `scale`, `rotate`, `transform-origin`.

# Regra crítica de responsividade

O HTML final recebe CSS do wireframe e do preset. Portanto, o wireframe NÃO deve criar/aplicar classes desktop agressivas que possam quebrar o mobile antes do preset responder.

Regras obrigatórias:
- Não aplique em `pagina.*.estilos[]` classes de `grid-template-columns` desktop como `heroTwoCol`, `gridTwoCol`, `gridThreeCol`, `grid2col`, `grid3col`, `grid2colHero`, `gridDesktopTwo`, `gridDesktopThree` ou equivalentes.
- Não aplique em `pagina.*.estilos[]` classes de `flex-direction: row` para blocos principais da página.
- No wireframe, prefira classes neutras e mobile-safe: container, padding, `display`, `gap`, `flex-direction: column`, `grid-template-columns: 1fr`, `width: 100%`, `max-width`, `position`, `z-index`.
- O desktop em duas/três colunas deve ser apenas descrito no `objetivo`, `papelComercial`, `posicaoDesejada` e `briefingVisual`, para o preset design aplicar depois.
- O wireframe pode declarar classes desktop em `definicoes`, mas evite aplicá-las nos elementos quando elas puderem forçar colunas no mobile.
- Se precisar aplicar layout no wireframe, use sempre uma versão mobile-first: uma coluna por padrão. Nunca dependa de uma classe desktop para depois ser corrigida no mobile.
- Cards, hero, prova, entregáveis e FAQ precisam renderizar aceitavelmente em uma coluna mesmo antes do preset.

# Direção comercial obrigatória

- Gere uma landing de venda/captura com percepção de produto real, não uma página técnica de gerador de arquivo.
- Se `campaignObjective` for `SALES`, gere página curta de venda direta low-ticket: promessa clara, prova visual, entregáveis, preço/garantia quando disponíveis, CTA de checkout e recuperação por eventos. Não peça nome/e-mail antes da compra.
- Se `campaignObjective` for `LEADS`, gere página de captura com formulário simples e entrega da isca.
- A página precisa parecer desejável antes de parecer operacional: venda a transformação, mostre prova visual e só depois explique formato, PDF, amostra, marca d’água ou entrega.
- A promessa principal deve ser baseada em `pain`, `result`, `mecanismo` e `campaignAngle`; amostra/PDF/mini-kit é prova ou redução de risco, nunca o centro da primeira dobra.
- O usuário deve entender em poucos segundos: qual problema resolve, por que é diferente, o que verá antes de comprar e qual é o próximo passo.
- Priorize seções com contraste narrativo: antes/depois, dor concreta, mecanismo simples, prova visual da entrega, captura com baixo risco e FAQ de objeções.

# Padrão universal de qualidade comercial

A landing deve funcionar para qualquer produto digital validado pelo Marketing Hub, sem ficar presa a um caso, nicho ou formato específico. Use sempre a narrativa:

**Dor → Resultado → Mecanismo → Prova → Oferta → Ação**

Regras obrigatórias para estruturar a página:
- **Dor**: criar seção que mostre sintomas concretos e custo de manter o problema.
- **Resultado**: deixar claro o avanço prático que o público quer alcançar.
- **Mecanismo**: reservar espaço para explicar por que o produto digital resolve a dor de forma plausível, normalmente em 3 passos.
- **Prova**: reservar pelo menos uma seção de preview/demonstração aplicada da entrega, não apenas uma imagem decorativa.
- **Oferta**: listar entregáveis pelo benefício que geram, não apenas pelo formato do arquivo.
- **Ação**: em `SALES`, conduzir para checkout com CTA de compra; em `LEADS`, conduzir para formulário com CTA orientado ao benefício imediato.

Escolha a prova conforme o tipo de produto digital: roteiro/script pede antes/depois ou simulação; plano de ação pede checklist/mapa/cronograma; produto educacional pede módulo demonstrativo ou transformação aplicada; diagnóstico pede amostra de relatório/indicadores; template/ferramenta pede print conceitual ou fluxo preenchido; biblioteca/kit pede cards de exemplos e modo de uso.

# Estrutura comercial mínima

Gerar no mínimo 5 seções quando houver dados suficientes:

1. Hero: promessa + prova visual + CTA primário para checkout em `SALES` ou formulário em `LEADS`.
2. Contraste/dor: antes/depois ou problema/novo caminho.
3. Mecanismo: 3 passos/cards com `h3` + `p`.
4. Prova/preview: imagem ou print conceitual demonstrando a entrega.
5. Entregáveis/recebe: lista de 3 a 5 itens concretos.
6. Ação: bloco de checkout/compra em `SALES` ou formulário nome + email + submit em `LEADS`.
7. FAQ: 4 a 6 dúvidas essenciais.

# Regras de hero e H1

- Primeira dobra forte obrigatória: abrir com resultado comercial desejado + dor removida + mecanismo plausível.
- Critério eliminatório do H1: não começar com “Gere uma amostra”, “Baixe um PDF”, “Receba um material”, “Crie seu mini-kit”, “Preencha um briefing” ou variações.
- O H1 deve vender dor removida + resultado desejado.
- PDF/amostra só aparece no subtítulo, bullets, legenda visual, seção de prova ou formulário.
- Hero deve conter H1, subtítulo, 3 bullets, CTA primário e prova visual.
- O desktop de hero em duas colunas deve ser descrito no `objetivo`, `papelComercial`, `posicaoDesejada` e `briefingVisual`, mas não aplicado com classe desktop agressiva no wireframe.
- A prova visual do hero deve ficar em container próprio, separado do bloco textual e dos CTAs, para evitar que a renderização final desloque a imagem para baixo sem hierarquia de primeira dobra.
- O hero precisa ter uma intenção clara de composição: coluna textual com promessa/CTA e coluna visual com prova/mockup; se não houver imagem útil, explique no briefing visual que o preset deve manter uma primeira dobra forte sem espaço vazio.

# Regras para CTAs e navegação

- CTAs de navegação interna devem ser `tag: "a"`, não `button`.
- Use `tag: "button"` somente para submit real dentro de formulário.
- Todo `targetSectionId` deve começar com exatamente um `#`, nunca `##` e nunca sem `#`.
- `targetSectionId` deve apontar para id real de seção existente.
- CTA primário do hero: `tag: "a"`, `componente: "buttonPrimary"`. Em `SALES`, apontar para seção real de checkout/oferta, como `#sec-checkout`; em `LEADS`, apontar para `#sec-form`.
- CTA secundário relevante: `tag: "a"`, `componente: "buttonSecondary"`.
- Links discretos de baixa prioridade podem usar `componente: "none"`, mas nunca para a ação principal de conversão.
- Toda ação principal de hero, prova, entregáveis e formulário deve ter `componente` explícito (`buttonPrimary` ou `buttonSecondary`) para o preset design conseguir aplicar aparência real de botão.
- Agrupe CTAs do hero em um container próprio com objetivo/briefing de linha de ações, para que o preset consiga aplicar espaçamento, alinhamento e quebra mobile sem transformar os CTAs em barras soltas.
- Não crie mais de 2 ações visíveis no hero antes da microcopy.
- O formulário deve ter um botão `button` de submit com texto curto e direto.
- Todo CTA principal precisa ser pensado como botão final premium, não como link: no `objetivo`/`briefingVisual` indique área clicável confortável, altura mínima e presença visual forte.
- O submit do formulário deve ser o componente de ação mais evidente da seção de captura e não pode ficar menor que CTAs secundários.

# Regras para imagens

- Planejar normalmente entre 2 e 4 elementos `img` úteis no total da página.
- Inserir `img` somente quando cumpre função explícita de prova, demonstração do produto, antes/depois, explicação do mecanismo ou redução de objeção.
- Pelo menos uma imagem deve representar visualmente a entrega que a pessoa receberá.
- Para cada `img`, preencher `briefingVisual` completo.
- Não usar imagem decorativa genérica.
- Imagens de prova devem pedir mockups/prints conceituais de tela ou páginas com elementos legíveis e úteis.
- Hero com imagem controlada: declarar proporção e altura máxima, sem bloco full-width desproporcional.
- A imagem principal do desktop deve nascer dentro de um wrapper dedicado de mídia/mockup, com intenção explícita de `max-width`, proporção e centralização; nunca deixe a imagem como bloco largo solto que possa ocupar metade da dobra com vazio.
- Em `briefingVisual` da imagem hero, descreva que o preset deve limitar largura visual, aplicar moldura/sombra e manter preenchimento útil, sem áreas vazias grandes ao redor do mockup.

# Formulário ou checkout obrigatório

- Se `campaignObjective` for `SALES`, incluir seção de checkout/oferta com CTA principal para compra e sem inputs de captura antes do checkout. Use link/âncora de checkout como interação principal quando a URL real ainda não estiver disponível.
- Se `campaignObjective` for `LEADS`, incluir seção/formulário de captura com somente os campos `nome` e `email`.
- Não incluir telefone, WhatsApp, CPF, empresa ou outros campos.
- Campos devem ter rótulos ou microcopy visível.
- Inputs não podem depender apenas de campos vazios para o usuário entender.
- O formulário deve ficar dentro de um container/card próprio, com intenção clara de largura controlada no desktop; não desenhe formulário horizontalmente esticado, baixo ou sem hierarquia.
- Em desktop, a intenção do formulário deve ser bloco vertical compacto e confiável, com campos full-width dentro do card, e não uma faixa larga atravessando a seção.
- Em mobile, o formulário deve ocupar largura total segura, com campos e botão grandes para toque.

# Heurísticas de composição

- Hero bullets: ~3 itens.
- Lista de entregáveis: 3 a 5 itens.
- Antes/depois: 3 itens de antes e 3 de depois.
- Como funciona: 3 passos.
- FAQ: 4 a 6 perguntas.
- Formulário: card vertical com título/microcopy, labels, campos nome/e-mail e submit full-width dentro do card.
- Evitar listas grandes no início da página.
- Evitar mais de 5 itens visíveis por bloco quando a pessoa ainda não entendeu a oferta.
- Evitar misturar benefícios, recursos e explicações extensas no mesmo bloco.

# Quality gate interno antes de responder

Releia o wireframe gerado e rejeite mentalmente se:
- parecer página técnica, estreita ou sem prova visual;
- o hero estiver centrado no formato da entrega;
- houver classe desktop de colunas aplicada diretamente em elementos principais;
- o mobile depender do preset para não quebrar;
- houver `targetSectionId` com `##`;
- CTA interno for `button` em vez de `a`;
- CTA principal estiver com `componente = none` ou sem container de ações;
- qualquer CTA principal puder parecer link azul/barra fina por falta de componente ou intenção visual;
- formulário desktop estiver descrito como faixa larga/baixa em vez de card vertical com largura controlada;
- imagem hero estiver solta, gigante ou com áreas vazias sem moldura/limite;
- hero não tiver separação clara entre texto, CTAs e prova visual;
- a página ficar fraca sem as imagens.

# Ajuste de intenção por seção

- `papelComercial`: função comercial da seção no funil.
- `fasePersuasao`: fase predominante.
- `objeçãoQueRemove`: principal objeção que resolve.
- `prioridadeConversao`: inteiro de 1 a 10.
- `acaoEsperada`: ação concreta esperada após consumir a seção.
- `fonteContexto[]`: liste de onde a seção foi derivada.
- `objetivo`: função comercial central.
- `nome` e `id`: coerentes com funil e navegação.

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao schema `landing-page-wireframe-schema.json` da etapa Gera Landing.
