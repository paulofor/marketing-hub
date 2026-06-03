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

Use `landingPageWireframe.pagina` como base. Preserve ids, tags, hierarquia, assets, interações, contratos de campo e intenção comercial. Altere somente `definicoes` e as listas `estilos[]` para transformar o wireframe em uma landing page comercial, premium, confiável, responsiva e visualmente menos monótona.

O assembler final é determinístico e não inventa estilos. Tudo que a página precisa visualmente deve estar declarado neste JSON. Se um botão, input, label, grid, card, imagem ou container precisa de altura, padding, alinhamento, largura, coluna ou quebra mobile, esses estilos precisam existir em `definicoes` e também precisam estar aplicados em `estilos[]` do elemento correto.

# Regras obrigatórias

1. Responda somente JSON válido.
2. Não crie, remova ou renomeie seções e elementos.
3. Não altere `id`, `tag`, `texto`, `briefingVisual`, `interacao`, `asset`, `contratoCampo` ou `componente`, exceto para preservar exatamente o que veio do wireframe.
4. Em `pagina.head.texto`, gere um título final publicável, curto e comercial para a landing; é proibido devolver “Wireframe provisório”, “HTML provisório”, “rascunho”, “debug” ou qualquer marcador técnico.
5. Em `pagina`, é proibido criar o campo `body`. Classes globais do `<body>` ficam somente em `pagina.corpo.estilos`.
6. Em `pagina.corpo.estilos`, use exatamente: `["bgBody", "fontBase", "textPrimary", "marginReset"]`.
7. Em todos os elementos, `estilos` deve ser uma lista simples de strings.
8. Todo nome usado em qualquer `estilos[]` precisa existir em `definicoes.*.desktop[].nome` ou `definicoes.*.mobile[].nome`.
9. A variação desktop/mobile deve existir somente em `definicoes.desktop` e `definicoes.mobile`.
10. Não use JSON serializado em string.
11. Não crie classes com nomes já existentes em `landingPageWireframe.definicoes`; os nomes de preset devem ser inéditos em relação ao wireframe.
12. Não use opacidade para simular cor de texto. Crie tokens de cor dedicados.
13. Não dependa de hover ou focus para legibilidade. Estados podem existir, mas a aparência base precisa funcionar sozinha.
14. É permitido repetir o mesmo `nome` em vários itens de `definicoes` para compor uma classe com múltiplas propriedades CSS. Use isso para classes semânticas como botões, inputs, labels, cards, grids e overrides mobile.

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

Cada item deve ter exatamente `nome`, `atributoCss`, `valor`.

# Propriedades CSS permitidas

Use somente estas propriedades:

`display`, `width`, `height`, `min-width`, `min-height`, `max-width`, `max-height`, `box-sizing`, `overflow`, `overflow-x`, `overflow-y`, `grid`, `grid-template`, `grid-template-columns`, `grid-template-rows`, `grid-template-areas`, `grid-column`, `grid-column-start`, `grid-column-end`, `grid-row`, `grid-row-start`, `grid-row-end`, `grid-area`, `flex`, `flex-direction`, `flex-wrap`, `flex-flow`, `justify-content`, `align-items`, `align-content`, `align-self`, `place-items`, `place-content`, `justify-items`, `gap`, `row-gap`, `column-gap`, `order`, `flex-grow`, `flex-shrink`, `flex-basis`, `margin`, `margin-top`, `margin-right`, `margin-bottom`, `margin-left`, `padding`, `padding-top`, `padding-right`, `padding-bottom`, `padding-left`, `color`, `background`, `background-color`, `background-image`, `background-size`, `background-position`, `background-repeat`, `background-attachment`, `background-clip`, `background-origin`, `font`, `font-family`, `font-size`, `font-weight`, `font-style`, `font-variant`, `line-height`, `letter-spacing`, `word-spacing`, `text-align`, `text-decoration`, `text-decoration-line`, `text-decoration-color`, `text-decoration-style`, `text-transform`, `text-shadow`, `white-space`, `border`, `border-width`, `border-style`, `border-color`, `border-top`, `border-right`, `border-bottom`, `border-left`, `border-radius`, `outline`, `outline-width`, `outline-style`, `outline-color`, `outline-offset`, `box-shadow`, `opacity`, `filter`, `backdrop-filter`, `mix-blend-mode`, `isolation`, `cursor`, `appearance`, `caret-color`, `accent-color`, `list-style`, `list-style-type`, `list-style-position`, `list-style-image`, `object-fit`, `object-position`, `transition`, `transition-property`, `transition-duration`, `transition-timing-function`, `transition-delay`, `animation`, `animation-name`, `animation-duration`, `animation-timing-function`, `animation-delay`, `animation-iteration-count`, `animation-direction`, `animation-fill-mode`, `animation-play-state`.


# Correção obrigatória das fraquezas apontadas pelo Quality Review

A etapa de qualidade penaliza fortemente CTA quebrado, layout desktop sem primeira dobra forte, conflito de classes, navegação empilhada e aparência de wireframe. Portanto, antes de responder:

- Reforce o hero desktop como composição premium: container amplo, grid/colunas comerciais apenas no desktop, coluna textual com promessa + bullets + CTAs, coluna visual com prova/mockup em moldura.
- Aplique em todo CTA principal classes completas de botão; nunca deixe `a` ou `button` com aparência de link padrão, barra fina, seleção de texto ou área de clique pequena.
- Trate navegação/topo como elemento secundário e profissional: centralizada ou alinhada em container, com espaçamento consistente, sem competir com o CTA principal e sem parecer lista empilhada.
- Evite conflito visual entre classes do wireframe e do preset: quando aplicar classe de preset para botão, card, grid, input ou container, aplique também classes de reset/override suficientes para vencer estilos antigos que causem link azul, padding ausente, largura errada ou layout estreito.
- Se o wireframe trouxer `componente = buttonPrimary` ou `buttonSecondary`, a saída é inválida se o elemento não receber a classe premium correspondente e todos os tokens mínimos definidos neste prompt.
- Se o formulário existir, o card do formulário deve ser visualmente mais forte que cards informativos e o submit deve parecer a ação mais segura e óbvia da página.

# Regra crítica de cascade e responsividade mobile

O HTML final emite o CSS do preset e também o CSS do wireframe. Como o CSS do wireframe pode aparecer depois, o preset precisa blindar o mobile contra classes antigas/agressivas do wireframe.

Obrigatório:
- Todo elemento que receber layout desktop em colunas (`gridDesktopTwo`, `gridDesktopThree`, `gridDesktopTwoEqual`, `heroDesktopGrid`, `cardsDesktopGrid`, ou equivalente) DEVE receber também uma classe mobile com override forte para uma coluna.
- Crie e aplique classes mobile específicas com valores `!important` quando necessário para vencer CSS posterior do wireframe.
- Use `!important` apenas em overrides mobile críticos de layout/largura: `display`, `grid-template-columns`, `flex-direction`, `width`, `max-width`, `box-sizing`, `overflow`, `padding` quando necessário.
- No mobile, todo grid principal deve virar uma coluna: `grid-template-columns: 1fr !important`.
- No mobile, toda linha de CTA deve virar coluna: `flex-direction: column !important`.
- No mobile, todo card deve ocupar a largura disponível: `width: 100% !important`, `max-width: 100% !important`, `box-sizing: border-box !important`.
- No mobile, todo input deve ocupar a largura disponível: `width: 100% !important`, `max-width: 100% !important`, `box-sizing: border-box !important`.
- No mobile, imagens e mockups nunca podem criar coluna estreita: `width: 100% !important`, `max-width: 100% !important`, `height: auto` e `object-fit: contain`.

Crie e aplique estas classes quando houver layout equivalente:
- `mobileOneColumn`: em `mobile`, `display:grid !important` e `grid-template-columns:1fr !important`.
- `mobileStack`: em `mobile`, `flex-direction:column !important`.
- `mobileFullWidth`: em `mobile`, `width:100% !important`, `max-width:100% !important`, `box-sizing:border-box !important`.
- `mobileSafeCard`: em `mobile`, `width:100% !important`, `max-width:100% !important`, `box-sizing:border-box !important`, `overflow:hidden`.
- `mobileSafeMedia`: em `mobile`, `width:100% !important`, `max-width:100% !important`, `height:auto`, `object-fit:contain`.
- `mobileSafeInput`: em `mobile`, `width:100% !important`, `max-width:100% !important`, `box-sizing:border-box !important`, `min-height:48px`.
- `mobileReadablePad`: em `mobile`, padding lateral confortável e nunca inferior a `16px` para seções/containers.

Rejeite qualquer saída onde no mobile:
- cards apareçam em colunas estreitas lado a lado;
- imagem vire uma faixa estreita ou muito alta por causa de grid desktop;
- seção hero fique em duas colunas;
- cards de mecanismo/entregáveis/FAQ fiquem com largura menor que o container;
- formulário fique espremido;
- inputs fiquem pequenos, sem padding, sem borda clara ou com aparência de campo padrão do navegador;
- CTA principal não ocupe largura confortável.

# Direção de arte

A landing deve parecer um produto digital premium, não um formulário técnico em fundo escuro.

# Hierarquia comercial universal

O design deve materializar visualmente a sequência **Dor → Resultado → Mecanismo → Prova → Oferta → Ação** sem alterar a estrutura do wireframe. Para isso:
- Hero deve ser a área de maior impacto e comunicar transformação com CTA evidente.
- Dor/antes-depois deve ter contraste suficiente para o usuário reconhecer o problema rapidamente.
- Mecanismo deve parecer organizado, simples e plausível, normalmente em steps/cards.
- Prova/preview deve parecer vitrine real do produto digital, com mockup funcional maior e mais concreto que imagens decorativas.
- Oferta/entregáveis deve ser escaneável e conectada a benefícios práticos.
- Formulário deve ser o ponto de ação mais forte, com superfície própria e confiança visual.

Use profundidade e acabamento com:
- hero visual forte;
- cards em camadas;
- bordas sutis;
- sombras controladas;
- gradientes discretos;
- containers centralizados;
- respiro generoso;
- CTA evidente;
- formulário mais forte que cards informativos;
- inputs premium, claros e confiáveis.

Evite o sistema visual repetitivo: fundo escuro + card azul + botão verde em todas as seções.

# Tratamento visual por seção

## Hero
- Background de alto impacto, gradiente ou brilho sutil.
- Container centralizado.
- Desktop com duas colunas quando houver visual.
- Mobile sempre em uma coluna, com `mobileOneColumn` e `mobileFullWidth` nos blocos principais.
- Headline dominante.
- CTA primário com aparência de botão real.
- Mockup com moldura, sombra e tamanho controlado.

## Dor / Problema / Antes e Depois
- Contraste claro em relação ao Hero.
- Cards de antes e depois não devem parecer idênticos.
- Desktop pode usar duas colunas, mas mobile obrigatoriamente uma coluna.

## Preview / Prova / Entregáveis
- Deve parecer vitrine do produto.
- Mockups com frame premium.
- Listas de entregáveis escaneáveis.
- Mobile nunca pode renderizar mockup em coluna estreita; use `mobileSafeMedia`.

## Como Funciona / Mecanismo
- Deve parecer organizado e didático.
- Use cards ou steps com espaçamento consistente.
- Desktop pode usar 3 colunas, mas mobile obrigatoriamente 1 coluna.

## Formulário / Captura
- Deve ser o bloco de conversão mais evidente.
- Use superfície própria, padding maior, borda/sombra e hierarquia clara.
- Campos full-width, confortáveis e legíveis.
- Labels visíveis precisam parecer labels de formulário, não parágrafos soltos.
- Inputs devem parecer campos premium: altura confortável, borda clara, fundo limpo, sombra sutil, padding real, texto legível e largura total.
- Botão submit mais forte que botões secundários.
- Não invente script, endpoint, mensagem de sucesso ou comportamento.

## FAQ
- Deve ser calmo, organizado e leve.
- Mobile deve ser uma coluna, com cards legíveis e largura total.

# Regras rígidas de qualidade para inputs e formulário

Os inputs são elementos críticos de confiança. Não podem parecer campos pequenos, padrão do navegador, colados, desalinhados ou sem respiro.

## Mapeamento obrigatório para inputs

Para todo elemento `input`, `textarea` ou `select`, especialmente quando `componente = formInput`:
- aplique `formInputPremium`, `inputFullWidth`, `inputTextReadable`, `inputBg`, `textOnInput`, `borderStrong`, `radiusSm`, `inputShadowSoft`, `mobileSafeInput`.
- se o input estiver dentro de `form`, ele deve ter largura total e `box-sizing:border-box`.
- não use apenas `inputBg`, `borderSoft` e `bodyText`; isso deixa o campo fraco.

Para todo label visual de campo (`tag = label`, ou `p` cujo id contenha `label`, `field-nome-label`, `field-email-label`):
- aplique `fieldLabel`, `textMuted` e espaçamento inferior curto.
- labels não devem parecer parágrafos comuns; devem ter peso 700, tamanho 14px e alinhamento claro.

Para todo formulário (`tag = form`) e card de formulário:
- aplique classes de stack vertical e gap real (`formStack`, `fieldGap`, `mobileFullWidth` quando apropriado).
- o card do formulário deve aplicar `formShell`, `cardSurface`, `borderStrong`, `radiusLg`, `mobileSafeCard`.

## Definições mínimas obrigatórias de input

Crie obrigatoriamente em `definicoes` as classes abaixo, com múltiplas propriedades quando necessário usando o mesmo `nome` repetido:

- `formInputPremium`: `display:block`, `width:100%`, `max-width:100%`, `box-sizing:border-box`, `min-height:50px`, `padding:13px 14px`, `border-radius:12px`, `font-size:16px`, `font-weight:500`, `line-height:1.3`, `appearance:none`.
- `inputFullWidth`: `width:100%`, `max-width:100%`, `box-sizing:border-box`.
- `inputTextReadable`: `font-size:16px`, `line-height:1.3`, `font-weight:500`.
- `inputShadowSoft`: `box-shadow:0 8px 18px rgba(15,23,42,0.06)`.
- `fieldLabel`: `display:block`, `font-size:14px`, `font-weight:700`, `line-height:1.2`, `margin-bottom:6px`.
- `formStack`: `display:flex`, `flex-direction:column`.
- `fieldGap`: `gap:8px` ou `row-gap:8px`.
- `formGap`: `gap:14px` ou `row-gap:14px`.

Observação: não dependa de `placeholderText` para estilo do placeholder, porque o tokenizador atual não gera pseudo-seletor `::placeholder`. O campo precisa ficar bom mesmo sem estilizar placeholder. Use labels visíveis e input base forte.

Rejeite qualquer input que pareça:
- pequeno demais;
- sem padding;
- sem borda clara;
- sem largura total;
- desalinhado em relação ao botão;
- com altura menor que 44px;
- com label parecendo texto solto;
- com aparência de campo nativo sem acabamento.

# Regras rígidas de qualidade para botões e CTAs

Os botões são elementos críticos da landing. Eles não podem parecer links finos, barras pequenas ou componentes improvisados.

O erro mais comum a evitar: aplicar apenas `rowInline`, `rowAlignCenter`, `ctaPrimaryBg`, `radiusMd` e `shadowMd`. Isso NÃO é um botão completo. Sem padding + min-height + justify-content + tipografia de CTA, o resultado é uma barra ruim.

## Mapeamento obrigatório por componente

Para todo elemento `a` ou `button`:

- Se `componente = buttonPrimary`, o elemento DEVE receber: `buttonPrimaryPremium`, `buttonMinTouch`, `buttonTextStrong`, `textOnButtonPrimary`, `ctaPrimaryBg`, `cursorPointer`, `textNoUnderline`.
- Se `componente = buttonSecondary`, o elemento DEVE receber: `buttonSecondaryGhost`, `buttonMinTouch`, `buttonTextStrong`, `cursorPointer`, `textNoUnderline`.
- Se o elemento for submit do formulário (`tag = button`, `type = submit` ou id contendo `submit`), ele DEVE receber: `buttonFormSubmit`, `buttonMinTouch`, `buttonTextStrong`, `textOnButtonPrimary`, `ctaPrimaryBg`, `cursorPointer`.
- Se `interacao.intencaoAcao` estiver preenchida, mas `componente = none`, aplique ao menos `buttonTertiaryLink`, `buttonTextStrong`, `cursorPointer`, `textNoUnderline`.
- Se o CTA principal estiver no hero, prova, entregáveis, FAQ final ou formulário, aplique também `buttonFullMobile`.
- Se houver container de CTAs, aplique classes de layout com gap real (`ctaRowPremium`, `mobileStack`, `mobileFullWidth`) para impedir CTAs colados, barras finas ou links soltos.

## Definições mínimas obrigatórias de botão

Crie obrigatoriamente em `definicoes` as classes abaixo, com múltiplas propriedades quando necessário usando o mesmo `nome` repetido:

- `buttonPrimaryPremium`: `display:inline-flex`, `align-items:center`, `justify-content:center`, `padding:14px 22px`, `min-height:48px`, `border-radius:14px`, `font-size:16px`, `font-weight:800`, `line-height:1.2`, `text-decoration:none`, `box-shadow:0 14px 32px rgba(37,99,235,0.24)`.
- `buttonSecondaryGhost`: `display:inline-flex`, `align-items:center`, `justify-content:center`, `padding:13px 20px`, `min-height:46px`, `border-radius:14px`, `font-size:15px`, `font-weight:750`, `line-height:1.2`, `text-decoration:none`, `background-color:#FFFFFF`, `border:1px solid #DCE6F7`.
- `buttonTertiaryLink`: `display:inline-flex`, `align-items:center`, `justify-content:center`, `padding:10px 12px`, `min-height:42px`, `border-radius:999px`, `font-size:14px`, `font-weight:700`, `line-height:1.2`, `text-decoration:none`.
- `buttonFullMobile`: em `mobile`, `width:100% !important`.
- `buttonMinTouch`: `min-height:48px`.
- `buttonTextStrong`: `font-size:16px`, `font-weight:800`, `line-height:1.2`.
- `buttonFormSubmit`: `display:flex`, `align-items:center`, `justify-content:center`, `width:100%`, `padding:15px 22px`, `min-height:50px`, `border-radius:14px`, `font-size:16px`, `font-weight:800`, `line-height:1.2`.
- `ctaRowPremium`: `display:flex`, `align-items:center`, `gap:12px`, `flex-wrap:wrap`.

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
- `mobileOneColumn`
- `mobileStack`
- `mobileFullWidth`
- `mobileSafeCard`
- `mobileSafeMedia`
- `mobileSafeInput`
- `imgFluid`
- `formInputPremium`
- `inputFullWidth`
- `inputTextReadable`
- `inputShadowSoft`
- `fieldLabel`
- `formStack`
- `fieldGap`
- `formGap`
- `buttonPrimaryPremium`
- `buttonSecondaryGhost`
- `buttonTertiaryLink`
- `buttonFullMobile`
- `buttonMinTouch`
- `buttonTextStrong`
- `buttonFormSubmit`
- `formShell`
- `mediaFrame`
- `ctaRowPremium`

# Critérios negativos

Rejeite e gere novamente se o JSON produzir:
- texto colado na borda;
- link com aparência padrão;
- imagem gigante sem container;
- título que quebra a primeira dobra agressivamente;
- todos os cards parecidos;
- formulário mais fraco que cards informativos;
- desktop com largura de mobile;
- seções consecutivas sem contraste;
- Hero sem prova visual forte;
- Preview/prova sem força de produto;
- CTA primário sem aparência de botão;
- CTA com menos de 44px de altura visual;
- botão primário parecendo uma barra fina;
- botão submit menor ou mais fraco que CTAs secundários;
- container de CTA sem gap real ou com links soltos visualmente;
- navegação/topo empilhado como lista improvisada;
- título/head contendo “Wireframe provisório”, “HTML provisório” ou marcador técnico;
- elemento com `componente = buttonPrimary` sem a classe `buttonPrimaryPremium`;
- elemento com `componente = buttonSecondary` sem a classe `buttonSecondaryGhost`;
- botão submit sem a classe `buttonFormSubmit`;
- input com `componente = formInput` sem a classe `formInputPremium`;
- input sem `width:100%`, `box-sizing:border-box`, `min-height` e padding real;
- label de campo sem classe `fieldLabel`;
- qualquer bloco principal mobile com duas ou três colunas;
- cards mobile estreitos lado a lado;
- imagem mobile espremida em coluna estreita.

# Quality gate interno

Antes de responder, revise o JSON como se fosse renderizado.

Garanta que:
- desktop use largura comercial e colunas quando possível;
- mobile tenha uma coluna real em todos os grids principais;
- mobile tenha padding suficiente, botões grandes e imagens controladas;
- todos os cards no mobile tenham largura total e `box-sizing:border-box`;
- todos os inputs tenham aparência premium, largura total, altura confortável e labels visíveis;
- Hero, Prova e Formulário não usem o mesmo tratamento visual;
- Formulário seja o bloco mais acionável;
- Preview pareça uma vitrine do produto;
- FAQ seja legível e mais calmo;
- nenhum CTA dependa apenas de `background`, `border-radius` e `shadow` para parecer botão;
- nenhum artefato técnico/provisório apareça em `pagina.head.texto` ou em texto visível.

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
