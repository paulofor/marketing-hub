Você está na etapa `landing-page-design-preset` do pipeline Gera Landing.

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


Nicho: {{NICHE_NAME}}

Dor: {{PAIN_JSON}}

Resultado: {{RESULT_JSON}}


{prompt-regras-globais}

Ângulo da Campanha que vai ser publicada:
{dados-campaignAngle}

Copy do Anuncio:
{dados-adCopy}

Briefing das Imagens dos Anuncios:
{dados-adImageBriefing}





Objetivo:
- Retornar um JSON no mesmo formato estrutural da etapa wireframe (objeto raiz com `definicoes` e `pagina`).
- Usar o JSON de `landingPageWireframe` como base da página (`pagina`) e aplicar somente acabamento visual de preset.
- Trocar a lista antiga de `definicoes` pelos 12 grupos abaixo.
- Transformar o wireframe em uma landing com aparência de produto comercial acabado, bonita, confiável e atraente, sem alterar a hierarquia, ids ou intenção das seções.

landingPageWireframe:
{dados-landingPageWireframe}


Regras obrigatórias:
1. Responda somente JSON válido.
2. Preserve a estrutura de `pagina` recebida no wireframe (mesmos ids, mesma hierarquia, sem inventar seções/elementos).
3. Aplique variação `desktop/mobile` somente em `definicoes`; em `pagina` use classes diretas sem separar por device.
4. Em `pagina`, é proibido criar o campo `body`; declare as classes globais do elemento HTML `<body>` somente em `pagina.corpo.estilos`.
5. Em cada elemento de `pagina` (`corpo`, `secoes`, `elementosSeccao`, `elementosInternos`), usar `estilos` como lista simples de classes (array de string), sem objetos `desktop/mobile`.
5.1. Em qualquer `estilos[]` de `pagina` (`corpo`, `secoes`, `elementosSeccao`, `elementosInternos`), usar exclusivamente nomes existentes em `definicoes.*.desktop[].nome` ou `definicoes.*.mobile[].nome`.
6. Cada item de `definicoes.desktop/mobile` deve seguir exatamente:
   - `nome`: nome da classe utilitária
   - `atributoCss`: propriedade CSS
   - `valor`: valor CSS válido
7. Não usar JSON serializado em string.
8. Usar exclusivamente propriedades CSS permitidas em `docs/gera-landing/listas-css-estrutura-acabamento.md`.
9. Manter foco de conversão: contraste legível, CTA destacado e consistência entre seção e elementos.
10. Criar tokens de cor de texto dedicados e não reutilizar `opacity` para simular cor de texto.
11. Garantir estados interativos reais (ex.: `:hover`) por combinação consistente de tokens base + tokens de hover.
12. É proibido criar em `definicoes` qualquer classe (`nome`) que já exista no `landingPageWireframe.definicoes`; os nomes do preset de design devem ser sempre inéditos em relação ao wireframe.

Direção de arte obrigatória:
- A página deve parecer uma landing moderna de produto digital premium, não um formulário técnico em fundo escuro.
- Crie sensação de profundidade e acabamento com: hero visual forte, cards com camadas, bordas sutis, sombras controladas, gradientes discretos, containers centralizados e respiro generoso.
- Use uma paleta coerente e comercial: fundo escuro ou claro sofisticado, contraste alto, um accent principal forte para CTA e um accent secundário para brilho/realce. Evite visual “verde neon solto” sem sistema visual.
- O design deve guiar o olhar: headline dominante, subtítulo confortável, bullets escaneáveis, CTA primário evidente, prova visual com moldura/card e formulário com destaque próprio.
- Diferencie blocos: hero, dor/contraste, mecanismo, prova visual, entregáveis, formulário e FAQ não podem parecer todos iguais. Use variações de background, grid, cards e espaçamento.
- O resultado precisa funcionar em desktop e mobile: no desktop, use largura real de landing e colunas; no mobile, preserve leitura vertical, botões grandes, cards compactos e imagens controladas.
- Visual de produto/prova deve receber tratamento premium: moldura, borda, sombra, radius e tamanho limitado. Se a imagem não carregar, o layout ainda deve parecer controlado, sem quebrar a página.

Qualidade visual mínima da landing (obrigatório):
- `body` deve ter `margin: 0`, fonte legível, background consistente e texto com contraste suficiente para leitura imediata.
- Layout desktop deve parecer landing comercial acabada: usar containers com largura entre 1040px e 1200px, hero em duas colunas quando houver conteúdo visual/card, espaçamento vertical generoso e cards com contraste/sombra leve. Evite aparência de página mobile esticada ou coluna estreita no desktop.
- Primeira dobra deve destacar uma ação principal, uma prova visual e uma hierarquia clara (headline maior, subtítulo legível, bullets escaneáveis, CTA evidente).
- Seções e containers principais devem usar `max-width` e centralização com `margin-left: auto` e `margin-right: auto` quando o conteúdo não precisar ocupar toda a largura.
- Hero deve ficar em layout de duas colunas no desktop e uma coluna no mobile, preservando hierarquia clara entre promessa, prova, CTA e visual.
- CTA primário deve ter aparência real de botão: `padding`, `background`, `border-radius`, `font-weight`, `display: inline-flex`, estados `:hover` e contraste claro entre texto e fundo.
- CTA secundário deve parecer intencional: botão ghost, pill ou link-card discreto; nunca link padrão de navegador.
- Imagens devem usar `max-width: 100%`, altura controlada, `object-fit`, `border-radius` e nunca ocupar a dobra inteira sem contexto textual, CTA ou container de suporte.
- Listas e bullets devem ter espaçamento controlado (`margin`, `padding`, `gap` ou `line-height`) para leitura escaneável sem amontoar textos.
- Formulário deve aparecer em card visual separado, com campos e botão claros, espaçamento interno, borda/sombra/contraste e hierarquia de ação evidente.
- Inputs devem ter altura confortável, placeholder legível, borda clara, estado de foco e contraste suficiente. Campos vazios sem rótulo visual não são aceitáveis.
- FAQ deve parecer seção final organizada, com cards ou linhas bem separadas; não pode parecer texto despejado.

Critérios negativos obrigatórios (não aceitar no JSON):
- Não gerar texto colado na borda da tela.
- Não deixar link com aparência padrão de navegador; todo link/CTA acionável deve receber classes visuais intencionais.
- Não permitir imagem gigante sem container.
- Não usar título que quebre a primeira dobra de forma agressiva.
- Não deixar todos os cards com a mesma aparência quando cumprem papéis diferentes.
- Não deixar o formulário visualmente mais fraco que cards informativos.
- Não usar apenas fundo escuro + card azul + botão verde como único sistema visual; crie hierarquia, realce e acabamento.
- Não gerar desktop com largura de mobile. Se houver espaço, use grid/colunas e max-width comercial.

Checklist obrigatório de consistência visual (deve ser atendido no JSON):
- Cores de texto obrigatórias: criar classes para `textPrimary`, `textMuted`, `textSubtle`, `textOnButtonPrimary`, `textOnInput`, `placeholderText`.
- Corpo global obrigatório: aplicar `bgBody`, `fontBase`, `textPrimary` e `marginReset` exclusivamente em `pagina.corpo.estilos`, sem `pagina.body` nem preset duplicado `pageRoot`.
- Tipografia não fragmentada: `h1`, `h2`, `h3` devem herdar `font-family` e cor do body, ou receber classes completas equivalentes.
- Botão primário completo: além de `bgButtonPrimary`, `radiusButton`, `shadowButton`, incluir classes para `padding`, `display:inline-flex`, `align-items:center`, `justify-content:center`, `font-weight`, `color` (`textOnButtonPrimary`).
- Input completo: além de `bgInput`, `radiusInput`, `borderSoft`, `caretAccent`, incluir classes para `padding`, `color` (`textOnInput`), `::placeholder` (`placeholderText`), `font` e `min-height`.
- Hover real obrigatório: tokens `bgButtonPrimaryHover` e `bgButtonSecondaryHover` só são válidos quando existirem classes utilitárias preparadas para uso de seletor `:hover` na etapa de HTML/CSS final.
- Opacidade: não usar `opacityMuted` para resolver cor de texto; preferir `color` com valores RGBA/HEX com contraste controlado.
- Contraste obrigatório em tema escuro: assegurar WCAG mínimo de 4.5:1 para texto normal e 3:1 para texto grande.

Composição visual esperada por tipo de elemento:
- Seção hero: aplique classe de background de alto impacto, container centralizado, grid desktop 2 colunas, gap grande, alinhamento vertical central, padding generoso, headline grande e visual com card/moldura.
- Containers internos: aplique max-width e centralização; quando forem grids de cards, use 2 ou 3 colunas no desktop e 1 coluna no mobile.
- Cards de mecanismo/prova/FAQ: use superfície diferente do fundo, border-radius, border sutil, padding, sombra leve e espaçamento entre título e texto.
- Cards de prova visual: podem usar borda accent, sombra mais forte e background levemente contrastado para parecer demonstração/produto.
- Formulário: use card com max-width controlado, padding maior, borda accent sutil, campos full-width, botão full-width ou muito evidente no mobile.
- Listas: bullets podem ser simples, mas devem ter line-height e indentação controlada; quando forem benefícios no hero, prefira aparência de checklist/pill se o schema permitir por classes.
- Imagens: além de `object-fit` e `object-position`, aplique `width: 100%`, `max-width`, `max-height`, radius e shadow. Não aplique altura que corte informação importante de mockups/prints.

Estratégia de classes:
- Crie classes reutilizáveis suficientes para compor aparência premium, mas sem excesso desnecessário.
- Prefira nomes semânticos e diretos: `sectionHeroPremium`, `containerWide`, `heroGridPremium`, `cardGlass`, `cardProof`, `formShell`, `mediaFrame`, `buttonPrimaryPremium`, `buttonSecondaryGhost`.
- Classes geradas neste preset devem ser inéditas em relação ao wireframe, mas podem substituir/acompanhar classes do wireframe em `pagina.*.estilos` desde que todos os nomes existam em `definicoes`.
- Preserve ids e hierarquia; altere apenas `definicoes` e listas `estilos[]` para melhorar a apresentação.

Estrutura obrigatória de `definicoes` (substitui a lista anterior; inclua os grupos de layout/estrutura necessários para largura máxima, grid, flex, espaçamento e imagens):

- `estrutura-layout`
  - display
  - width
  - height
  - min-width
  - min-height
  - max-width
  - max-height
  - box-sizing
  - overflow
  - overflow-x
  - overflow-y
  - grid
  - grid-template
  - grid-template-columns
  - grid-template-rows
  - grid-template-areas
  - grid-column
  - grid-column-start
  - grid-column-end
  - grid-row
  - grid-row-start
  - grid-row-end
  - grid-area
  - flex
  - flex-direction
  - flex-wrap
  - flex-flow
  - justify-content
  - align-items
  - align-content
  - align-self
  - place-items
  - place-content
  - justify-items
  - gap
  - row-gap
  - column-gap
  - order
  - flex-grow
  - flex-shrink
  - flex-basis

- `espacamento`
  - margin
  - margin-top
  - margin-right
  - margin-bottom
  - margin-left
  - padding
  - padding-top
  - padding-right
  - padding-bottom
  - padding-left

- `cores-fundo`
  - color
  - background
  - background-color
  - background-image
  - background-size
  - background-position
  - background-repeat
  - background-attachment
  - background-clip
  - background-origin

- `tipografia`
  - font
  - font-family
  - font-size
  - font-weight
  - font-style
  - font-variant
  - line-height
  - letter-spacing
  - word-spacing

- `texto`
  - text-align
  - text-decoration
  - text-decoration-line
  - text-decoration-color
  - text-decoration-style
  - text-transform
  - text-shadow
  - white-space

- `bordas`
  - border
  - border-width
  - border-style
  - border-color
  - border-top
  - border-right
  - border-bottom
  - border-left
  - border-radius

- `contorno`
  - outline
  - outline-width
  - outline-style
  - outline-color
  - outline-offset

- `sombras-transparencia`
  - box-shadow
  - opacity

- `filtro-efeitos`
  - filter
  - backdrop-filter
  - mix-blend-mode
  - isolation

- `cursor`
  - cursor
  - appearance
  - caret-color
  - accent-color

- `listas`
  - list-style
  - list-style-type
  - list-style-position
  - list-style-image

- `imagens`
  - object-fit
  - object-position

- `transições`
  - transition
  - transition-property
  - transition-duration
  - transition-timing-function
  - transition-delay

- `animações`
  - animation
  - animation-name
  - animation-duration
  - animation-timing-function
  - animation-delay
  - animation-iteration-count
  - animation-direction
  - animation-fill-mode
  - animation-play-state

Quality gate interno antes de responder:
- Releia o JSON final como se fosse renderizado em uma página real.
- Rejeite mentalmente se parecer página simples demais, estreita, sem contraste entre blocos, sem hierarquia de CTA, com imagem solta ou formulário fraco.
- Garanta que o desktop use largura e colunas quando possível.
- Garanta que o mobile tenha padding suficiente, botões grandes e imagens controladas.
- Garanta que o design transmita confiança antes do usuário chegar ao formulário.

Formato esperado de saída:
{
  "definicoes": {
    "estrutura-layout": { "desktop": [{ "nome": "string", "atributoCss": "display", "valor": "grid" }], "mobile": [] },
    "espacamento": { "desktop": [{ "nome": "string", "atributoCss": "padding", "valor": "24px" }], "mobile": [] },
    "cores-fundo": { "desktop": [{ "nome": "string", "atributoCss": "background-color", "valor": "#FFFFFF" }], "mobile": [] },
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
      "secoes": [
        {
          "nome": "Hero",
          "objetivo": "Apresentar a promessa central",
          "oQueQuerProvocarNoUsuario": "percepção de valor imediato",
          "papelComercial": "atração",
          "fasePersuasao": "atenção",
          "objeçãoQueRemove": "falta de clareza",
          "prioridadeConversao": 10,
          "acaoEsperada": "avançar para o CTA",
          "fonteContexto": ["landingPageWireframe"],
          "id": "sec-hero",
          "estilos": ["sectionHeroPremium", "surfaceHeroGlow"],
          "elementosSeccao": [
            {
              "id": "hero-title",
              "tag": "h1",
              "texto": { "tamMaximo": 90, "tamMinimo": 45, "conteudo": "" },
              "estilos": ["headlineHero", "textPrimary"],
              "briefingVisual": null,
              "elementosInternos": [],
              "interacao": { "intencaoAcao": "", "targetSectionId": null, "hrefEsperado": null },
              "asset": null,
              "contratoCampo": null,
              "componente": "none"
            }
          ]
        }
      ]
    }
  }
}
