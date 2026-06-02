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

{{prompt-regras-globais}}

Ângulo da Campanha que vai ser publicada:
{{dados-campaignAngle}}

Copy do Anuncio:
{{dados-adCopy}}

Briefing das Imagens dos Anuncios:
{{dados-adImageBriefing}}


template_id: landing-wireframe
template_version: v6
artifact_target: landingPageWireframe

# Objetivo da etapa

Gerar somente o wireframe estrutural da landing em JSON válido, aderente ao schema `landing-page-wireframe-schema.json`.

O wireframe define seções, hierarquia, elementos, intenção comercial, assets esperados, formulário e navegação. O acabamento visual responsivo final é responsabilidade da etapa `landing-page-design-preset`.

# Contrato obrigatório

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
- A página precisa parecer desejável antes de parecer operacional: venda a transformação, mostre prova visual e só depois explique formato, PDF, amostra, marca d’água ou entrega.
- A promessa principal deve ser baseada em `pain`, `result`, `mecanismo` e `campaignAngle`; amostra/PDF/mini-kit é prova ou redução de risco, nunca o centro da primeira dobra.
- O usuário deve entender em poucos segundos: qual problema resolve, por que é diferente, o que verá antes de comprar e qual é o próximo passo.
- Priorize seções com contraste narrativo: antes/depois, dor concreta, mecanismo simples, prova visual da entrega, captura com baixo risco e FAQ de objeções.

# Estrutura comercial mínima

Gerar no mínimo 5 seções quando houver dados suficientes:

1. Hero: promessa + prova visual + CTA primário para formulário.
2. Contraste/dor: antes/depois ou problema/novo caminho.
3. Mecanismo: 3 passos/cards com `h3` + `p`.
4. Prova/preview: imagem ou print conceitual demonstrando a entrega.
5. Entregáveis/recebe: lista de 3 a 5 itens concretos.
6. Formulário: nome + email + submit.
7. FAQ: 4 a 6 dúvidas essenciais.

# Regras de hero e H1

- Primeira dobra forte obrigatória: abrir com resultado comercial desejado + dor removida + mecanismo plausível.
- Critério eliminatório do H1: não começar com “Gere uma amostra”, “Baixe um PDF”, “Receba um material”, “Crie seu mini-kit”, “Preencha um briefing” ou variações.
- O H1 deve vender dor removida + resultado desejado.
- PDF/amostra só aparece no subtítulo, bullets, legenda visual, seção de prova ou formulário.
- Hero deve conter H1, subtítulo, 3 bullets, CTA primário e prova visual.
- O desktop de hero em duas colunas deve ser descrito, mas não aplicado com classe desktop agressiva no wireframe.

# Regras para CTAs e navegação

- CTAs de navegação interna devem ser `tag: "a"`, não `button`.
- Use `tag: "button"` somente para submit real dentro de formulário.
- Todo `targetSectionId` deve começar com exatamente um `#`, nunca `##` e nunca sem `#`.
- `targetSectionId` deve apontar para id real de seção existente.
- CTA primário do hero: `tag: "a"`, `componente: "buttonPrimary"`, `targetSectionId: "#sec-form"`.
- CTA secundário relevante: `tag: "a"`, `componente: "buttonSecondary"`.
- Links discretos de baixa prioridade podem usar `componente: "none"`.
- Não crie mais de 2 ações visíveis no hero antes da microcopy.
- O formulário deve ter um botão `button` de submit com texto curto e direto.

# Regras para imagens

- Planejar normalmente entre 2 e 4 elementos `img` úteis no total da página.
- Inserir `img` somente quando cumpre função explícita de prova, demonstração do produto, antes/depois, explicação do mecanismo ou redução de objeção.
- Pelo menos uma imagem deve representar visualmente a entrega que a pessoa receberá.
- Para cada `img`, preencher `briefingVisual` completo.
- Não usar imagem decorativa genérica.
- Imagens de prova devem pedir mockups/prints conceituais de tela ou páginas com elementos legíveis e úteis.
- Hero com imagem controlada: declarar proporção e altura máxima, sem bloco full-width desproporcional.

# Formulário obrigatório

- Incluir seção/formulário de captura com somente os campos `nome` e `email`.
- Não incluir telefone, WhatsApp, CPF, empresa ou outros campos.
- Campos devem ter rótulos ou microcopy visível.
- Inputs não podem depender apenas de campos vazios para o usuário entender.

# Heurísticas de composição

- Hero bullets: ~3 itens.
- Lista de entregáveis: 3 a 5 itens.
- Antes/depois: 3 itens de antes e 3 de depois.
- Como funciona: 3 passos.
- FAQ: 4 a 6 perguntas.
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
