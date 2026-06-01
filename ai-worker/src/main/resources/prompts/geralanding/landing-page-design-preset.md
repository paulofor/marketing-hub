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


Qualidade visual mínima da landing (obrigatório):
- `body` deve ter `margin: 0`, fonte legível, background consistente e texto com contraste suficiente para leitura imediata.
- Layout desktop deve parecer landing comercial acabada: usar containers com largura entre 1040px e 1200px, hero em duas colunas quando houver conteúdo visual/card, espaçamento vertical generoso e cards com contraste/sombra leve. Evite aparência de página mobile esticada ou coluna estreita no desktop.
- Primeira dobra deve destacar uma ação principal, uma prova visual e uma hierarquia clara (headline maior, subtítulo legível, bullets escaneáveis, CTA evidente).
- Seções e containers principais devem usar `max-width` e centralização com `margin-left: auto` e `margin-right: auto` quando o conteúdo não precisar ocupar toda a largura.
- Hero deve ficar em layout de duas colunas no desktop e uma coluna no mobile, preservando hierarquia clara entre promessa, prova, CTA e visual.
- CTA primário deve ter aparência real de botão: `padding`, `background`, `border-radius`, `font-weight`, `display: inline-flex`, estados `:hover` e contraste claro entre texto e fundo.
- Imagens devem usar `max-width: 100%`, altura controlada, `object-fit`, `border-radius` e nunca ocupar a dobra inteira sem contexto textual, CTA ou container de suporte.
- Listas e bullets devem ter espaçamento controlado (`margin`, `padding`, `gap` ou `line-height`) para leitura escaneável sem amontoar textos.
- Formulário deve aparecer em card visual separado, com campos e botão claros, espaçamento interno, borda/sombra/contraste e hierarquia de ação evidente.

Critérios negativos obrigatórios (não aceitar no JSON):
- Não gerar texto colado na borda da tela.
- Não deixar link com aparência padrão de navegador; todo link/CTA acionável deve receber classes visuais intencionais.
- Não permitir imagem gigante sem container.
- Não usar título que quebre a primeira dobra de forma agressiva.

Checklist obrigatório de consistência visual (deve ser atendido no JSON):
- Cores de texto obrigatórias: criar classes para `textPrimary`, `textMuted`, `textSubtle`, `textOnButtonPrimary`, `textOnInput`, `placeholderText`.
- Corpo global obrigatório: aplicar `bgBody`, `fontBase`, `textPrimary` e `marginReset` exclusivamente em `pagina.corpo.estilos`, sem `pagina.body` nem preset duplicado `pageRoot`.
- Tipografia não fragmentada: `h1`, `h2`, `h3` devem herdar `font-family` e cor do body, ou receber classes completas equivalentes.
- Botão primário completo: além de `bgButtonPrimary`, `radiusButton`, `shadowButton`, incluir classes para `padding`, `display:inline-flex`, `align-items:center`, `justify-content:center`, `font-weight`, `color` (`textOnButtonPrimary`).
- Input completo: além de `bgInput`, `radiusInput`, `borderSoft`, `caretAccent`, incluir classes para `padding`, `color` (`textOnInput`), `::placeholder` (`placeholderText`), `font` e `min-height`.
- Hover real obrigatório: tokens `bgButtonPrimaryHover` e `bgButtonSecondaryHover` só são válidos quando existirem classes utilitárias preparadas para uso de seletor `:hover` na etapa de HTML/CSS final.
- Opacidade: não usar `opacityMuted` para resolver cor de texto; preferir `color` com valores RGBA/HEX com contraste controlado.
- Contraste obrigatório em tema escuro: assegurar WCAG mínimo de 4.5:1 para texto normal e 3:1 para texto grande.

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
          "estilos": ["sectionHero", "surfaceBand"],
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
