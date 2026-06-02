Você está na etapa `landing-page-design-preset` do pipeline Gera Landing.

Contexto:

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
      <landingPresetDesign/>
      <landingHtml/>
    </experimento>
  </hipotese>
</nicho>
```

Nicho: {{NICHE_NAME}}

Dor: {{PAIN_JSON}}

Resultado: {{RESULT_JSON}}

{prompt-regras-globais}

Ângulo da campanha:
{dados-campaignAngle}

Copy do anúncio:
{dados-adCopy}

Briefing das imagens dos anúncios:
{dados-adImageBriefing}

Wireframe da landing:
{dados-landingPageWireframe}

# Objetivo

Retornar um JSON válido no mesmo formato estrutural do wireframe, com objeto raiz contendo `definicoes` e `pagina`.

Use `landingPageWireframe.pagina` como base. Preserve ids, tags, hierarquia, assets, interações, contratos de campo e intenção comercial. Altere somente `definicoes` e as listas `estilos[]` para transformar o wireframe em uma landing page comercial, premium, confiável e visualmente menos monótona.

O assembler final é determinístico e não inventa estilos. Tudo que a página precisa visualmente deve estar declarado neste JSON.

# Regras obrigatórias

1. Responda somente JSON válido.
2. Não crie, remova ou renomeie seções e elementos.
3. Não altere `id`, `tag`, `texto`, `briefingVisual`, `interacao`, `asset`, `contratoCampo` ou `componente`, exceto para preservar exatamente o que veio do wireframe.
4. Em `pagina`, é proibido criar o campo `body`. Classes globais do `<body>` ficam somente em `pagina.corpo.estilos`.
5. Em `pagina.corpo.estilos`, use exatamente: `["bgBody", "fontBase", "textPrimary", "marginReset"]`.
6. Em todos os elementos, `estilos` deve ser uma lista simples de strings.
7. Todo nome usado em qualquer `estilos[]` precisa existir em `definicoes.*.desktop[].nome` ou `definicoes.*.mobile[].nome`.
8. A variação desktop/mobile deve existir somente em `definicoes.desktop` e `definicoes.mobile`.
9. Não use JSON serializado em string.
10. Não crie classes com nomes já existentes em `landingPageWireframe.definicoes`; os nomes de preset devem ser inéditos em relação ao wireframe.
11. Não use opacidade para simular cor de texto. Crie tokens de cor dedicados.
12. Não dependa de hover para legibilidade. Hover pode existir, mas a aparência base precisa funcionar sozinha.

# Grupos obrigatórios em `definicoes`

Use exatamente estes grupos:

- `estrutura-layout`
- `espacamento`
- `cores-fundo`
- `tipografia`
- `texto`
- `bordas`
- `contorno`
- `sombras-transparencia`
- `filtro-efeitos`
- `cursor`
- `listas`
- `imagens`
- `transições`
- `animações`

Cada grupo deve ter:

```json
{
  "desktop": [{ "nome": "classe", "atributoCss": "display", "valor": "grid" }],
  "mobile": []
}
```

Cada item deve ter exatamente:
- `nome`
- `atributoCss`
- `valor`

# Propriedades CSS permitidas

Use somente estas propriedades:

`display`, `width`, `height`, `min-width`, `min-height`, `max-width`, `max-height`, `box-sizing`, `overflow`, `overflow-x`, `overflow-y`, `grid`, `grid-template`, `grid-template-columns`, `grid-template-rows`, `grid-template-areas`, `grid-column`, `grid-column-start`, `grid-column-end`, `grid-row`, `grid-row-start`, `grid-row-end`, `grid-area`, `flex`, `flex-direction`, `flex-wrap`, `flex-flow`, `justify-content`, `align-items`, `align-content`, `align-self`, `place-items`, `place-content`, `justify-items`, `gap`, `row-gap`, `column-gap`, `order`, `flex-grow`, `flex-shrink`, `flex-basis`, `margin`, `margin-top`, `margin-right`, `margin-bottom`, `margin-left`, `padding`, `padding-top`, `padding-right`, `padding-bottom`, `padding-left`, `color`, `background`, `background-color`, `background-image`, `background-size`, `background-position`, `background-repeat`, `background-attachment`, `background-clip`, `background-origin`, `font`, `font-family`, `font-size`, `font-weight`, `font-style`, `font-variant`, `line-height`, `letter-spacing`, `word-spacing`, `text-align`, `text-decoration`, `text-decoration-line`, `text-decoration-color`, `text-decoration-style`, `text-transform`, `text-shadow`, `white-space`, `border`, `border-width`, `border-style`, `border-color`, `border-top`, `border-right`, `border-bottom`, `border-left`, `border-radius`, `outline`, `outline-width`, `outline-style`, `outline-color`, `outline-offset`, `box-shadow`, `opacity`, `filter`, `backdrop-filter`, `mix-blend-mode`, `isolation`, `cursor`, `appearance`, `caret-color`, `accent-color`, `list-style`, `list-style-type`, `list-style-position`, `list-style-image`, `object-fit`, `object-position`, `transition`, `transition-property`, `transition-duration`, `transition-timing-function`, `transition-delay`, `animation`, `animation-name`, `animation-duration`, `animation-timing-function`, `animation-delay`, `animation-iteration-count`, `animation-direction`, `animation-fill-mode`, `animation-play-state`.

# Direção de arte

A landing deve parecer um produto digital premium, não um formulário técnico em fundo escuro.

Use profundidade e acabamento com:
- hero visual forte;
- cards em camadas;
- bordas sutis;
- sombras controladas;
- gradientes discretos;
- containers centralizados;
- respiro generoso;
- CTA evidente;
- formulário mais forte que cards informativos.

Evite o sistema visual repetitivo: fundo escuro + card azul + botão verde em todas as seções.

# Mapa obrigatório de contraste entre seções

Antes de montar o JSON final, atribua uma função visual para cada seção usando `id`, `nome`, `papelComercial`, `fasePersuasao`, `objetivo` e `prioridadeConversao`.

Cada função visual deve gerar combinação própria de:
- superfície/background;
- grid/container;
- card/frame;
- espaçamento;
- tratamento de imagem;
- tratamento de CTA.

Use pelo menos cinco tratamentos visuais diferentes quando a página tiver seções suficientes:

1. `hero`: impacto, promessa principal, visual forte, CTA evidente.
2. `dor/problema/antes-depois`: tensão e identificação, com contraste emocional entre antes e depois.
3. `preview/prova/entregáveis`: demonstração tangível do produto, com moldura visual mais forte.
4. `como-funciona/mecanismo`: clareza, processo, passos numerados ou cards organizados e leves.
5. `formulario/captura`: bloco de conversão destacado, com maior percepção de segurança e ação.
6. `faq/objeções`: bloco mais calmo, legível e organizado, sem competir com o formulário.

Se houver duas seções consecutivas com o mesmo tipo de superfície, diferencie pelo menos dois aspectos: padding, card, borda, imagem, grid, CTA, sombra ou acento.

# Tratamento visual por seção

## Hero
- Background de alto impacto, gradiente ou brilho sutil.
- Container centralizado.
- Desktop com duas colunas quando houver visual.
- Mobile em uma coluna.
- Headline dominante.
- CTA primário com aparência de botão real.
- Mockup com moldura, sombra e tamanho controlado.

## Dor / Problema / Antes e Depois
- Contraste claro em relação ao Hero.
- Cards de antes e depois não devem parecer idênticos.
- Antes pode usar superfície mais densa/fria/alerta.
- Depois pode usar acento positivo ou borda accent.

## Preview / Prova / Entregáveis
- Deve parecer vitrine do produto.
- Mockups com frame premium.
- Listas de entregáveis escaneáveis.
- Pode usar borda accent, sombra mais forte ou superfície iluminada.

## Como Funciona / Mecanismo
- Deve parecer organizado e didático.
- Use cards ou steps com espaçamento consistente.
- Não deve competir visualmente com Hero ou Formulário.

## Formulário / Captura
- Deve ser o bloco de conversão mais evidente.
- Use superfície própria, padding maior, borda/sombra e hierarquia clara.
- Campos full-width, confortáveis e legíveis.
- Botão submit mais forte que botões secundários.
- Não invente script, endpoint, mensagem de sucesso ou comportamento.

## FAQ
- Deve ser calmo, organizado e leve.
- Pode usar cards menores, linhas ou superfície menos intensa.
- CTA final visível, mas não mais forte que o formulário.

# Regras obrigatórias de desktop/mobile

Todo container principal deve ter classes mobile e desktop quando aplicável:
- largura mobile;
- largura desktop ou wide;
- centralização;
- padding mobile;
- padding desktop.

Todo grid principal deve ter classes mobile e desktop quando aplicável:
- mobile: uma coluna;
- desktop: duas ou três colunas quando houver visual/cards;
- gap mobile;
- gap desktop;
- alinhamento vertical quando necessário.

Todo CTA deve ter classes para:
- `display: inline-flex` ou `display: flex`;
- `align-items: center`;
- `justify-content: center`;
- padding mobile e desktop;
- radius;
- font-weight;
- color;
- background;
- cursor;
- `text-decoration: none`;
- largura full no mobile quando for CTA primário de conversão.

# Regras rígidas de qualidade para botões e CTAs

Os botões são elementos críticos da landing. Eles não podem parecer links finos, barras azuis pequenas ou componentes improvisados.

Para todo elemento `a` ou `button` com `componente` igual a `buttonPrimary`, `buttonSecondary` ou com `interacao.intencaoAcao` preenchida:
- aplique classe visual de botão, nunca apenas cor de fundo;
- use `min-height` de pelo menos `48px` no mobile e `46px` no desktop;
- use padding confortável: no mínimo `14px 18px` no mobile e `14px 22px` no desktop;
- use `font-size` entre `15px` e `17px`, `font-weight` entre `700` e `800`, `line-height` entre `1.15` e `1.25`;
- use `border-radius` entre `12px` e `999px`, escolhendo pill ou botão arredondado consistente com o visual da página;
- use `box-shadow` ou borda sutil no CTA primário para dar presença, mas sem exagero;
- no mobile, CTA primário de hero, prova, formulário e fechamento deve ter `width: 100%` quando estiver em coluna estreita;
- no desktop, CTA primário pode ser `inline-flex`, mas deve ter largura mínima visual suficiente; não pode ficar como uma etiqueta estreita;
- o botão submit do formulário deve ser o CTA mais forte da seção, com largura full, altura confortável e contraste alto;
- CTA secundário deve parecer ghost button/pill/link-card, com borda e padding; nunca link azul sublinhado padrão.

Rejeite qualquer CTA que pareça:
- barra fina com pouco padding;
- texto pequeno demais;
- link padrão de navegador;
- botão full-width com altura baixa;
- cor forte sem hierarquia tipográfica;
- CTA primário visualmente mais fraco que um card comum;
- vários CTAs idênticos competindo entre si.

Crie e aplique, quando fizer sentido, classes específicas para qualidade de botão:
- `buttonPrimaryPremium`: botão primário forte, alto contraste, altura confortável e sombra controlada;
- `buttonSecondaryGhost`: botão secundário com borda, fundo discreto, padding real e boa área de toque;
- `buttonFullMobile`: `width: 100%` no mobile para CTAs principais;
- `buttonMinTouch`: garante `min-height` confortável;
- `buttonTextStrong`: define peso, tamanho e line-height do texto do botão;
- `buttonFormSubmit`: variação mais forte para submit do formulário.

Toda imagem de produto/prova deve ter classes para:
- `width: 100%`;
- `max-width: 100%`;
- `height: auto` ou altura máxima controlada;
- `object-fit`;
- `object-position`;
- radius;
- border/sombra/moldura quando for mockup.

# Classes obrigatórias globais mínimas

Crie e aplique, quando apropriado:
- `bgBody`
- `fontBase`
- `textPrimary`
- `marginReset`
- `textMuted`
- `textSubtle`
- `textOnButtonPrimary`
- `textOnInput`
- `placeholderText`
- `containerWide`
- `containerCenter`
- `gridMobileOne`
- `gridDesktopTwo`
- `gridDesktopThree`
- `imgFluid`
- `buttonPrimaryPremium`
- `buttonSecondaryGhost`
- `buttonFullMobile`
- `buttonMinTouch`
- `buttonTextStrong`
- `buttonFormSubmit`
- `formShell`
- `mediaFrame`

# Critérios negativos

Rejeite e gere novamente se o JSON produzir:
- texto colado na borda;
- link com aparência padrão;
- imagem gigante sem container;
- título que quebra a primeira dobra agressivamente;
- todos os cards parecidos;
- formulário mais fraco que cards informativos;
- apenas fundo escuro + card azul + botão verde;
- desktop com largura de mobile;
- seções consecutivas sem contraste;
- Hero sem prova visual forte;
- Preview/prova sem força de produto;
- CTA primário sem aparência de botão;
- CTA com menos de 44px de altura visual;
- botão primário parecendo uma barra fina;
- botão submit menor ou mais fraco que CTAs secundários;
- texto de botão com tamanho visual menor que o corpo da página;
- CTA secundário renderizado como link comum.

# Quality gate interno

Antes de responder, revise o JSON como se fosse renderizado.

Garanta que:
- desktop use largura comercial e colunas quando possível;
- mobile tenha padding suficiente, botões grandes e imagens controladas;
- cada seção tenha papel visual coerente com sua função comercial;
- Hero, Prova e Formulário não usem o mesmo tratamento visual;
- Formulário seja o bloco mais acionável;
- Preview pareça uma vitrine do produto;
- FAQ seja legível e mais calmo;
- todos os CTAs tenham área de toque confortável, contraste alto e aparência de botão real;
- o botão principal da primeira dobra seja imediatamente reconhecível como a próxima ação;
- o botão do formulário seja visualmente mais forte que qualquer link ou botão secundário da mesma seção.

# Formato esperado de saída

{
  "definicoes": {
    "estrutura-layout": { "desktop": [], "mobile": [] },
    "espacamento": { "desktop": [], "mobile": [] },
    "cores-fundo": { "desktop": [], "mobile": [] },
    "tipografia": { "desktop": [], "mobile": [] },
    "texto": { "desktop": [], "mobile": [] },
    "bordas": { "desktop": [], "mobile": [] },
    "contorno": { "desktop": [], "mobile": [] },
    "sombras-transparencia": { "desktop": [], "mobile": [] },
    "filtro-efeitos": { "desktop": [], "mobile": [] },
    "cursor": { "desktop": [], "mobile": [] },
    "listas": { "desktop": [], "mobile": [] },
    "imagens": { "desktop": [], "mobile": [] },
    "transições": { "desktop": [], "mobile": [] },
    "animações": { "desktop": [], "mobile": [] }
  },
  "pagina": {
    "head": { "texto": "" },
    "corpo": {
      "estilos": ["bgBody", "fontBase", "textPrimary", "marginReset"],
      "secoes": []
    }
  }
}
