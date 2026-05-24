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
4. Em `pagina.body`, incluir obrigatoriamente a lista `estilos` com classes globais do body.
5. Em cada elemento de `pagina` (`secoes`, `elementosSeccao`, `elementosInternos`), usar `estilos` como lista simples de classes (array de string), sem objetos `desktop/mobile`.
6. Cada item de `definicoes.desktop/mobile` deve seguir exatamente:
   - `nome`: nome da classe utilitária
   - `atributoCss`: propriedade CSS
   - `valor`: valor CSS válido
7. Não usar JSON serializado em string.
8. Usar exclusivamente propriedades CSS permitidas em `docs/gera-landing/listas-css-estrutura-acabamento.md`.
9. Manter foco de conversão: contraste legível, CTA destacado e consistência entre seção e elementos.
10. Criar tokens de cor de texto dedicados e não reutilizar `opacity` para simular cor de texto.
11. Garantir estados interativos reais (ex.: `:hover`) por combinação consistente de tokens base + tokens de hover.

Checklist obrigatório de consistência visual (deve ser atendido no JSON):
- Cores de texto obrigatórias: criar classes para `textPrimary`, `textMuted`, `textSubtle`, `textOnButtonPrimary`, `textOnInput`, `placeholderText`.
- Body global obrigatório: criar preset `pageRoot` com `bgBody`, `fontBase`, `textPrimary` para herança consistente.
- Tipografia não fragmentada: `h1`, `h2`, `h3` devem herdar `font-family` e cor do body, ou receber classes completas equivalentes.
- Botão primário completo: além de `bgButtonPrimary`, `radiusButton`, `shadowButton`, incluir classes para `padding`, `display:inline-flex`, `align-items:center`, `justify-content:center`, `font-weight`, `color` (`textOnButtonPrimary`).
- Input completo: além de `bgInput`, `radiusInput`, `borderSoft`, `caretAccent`, incluir classes para `padding`, `color` (`textOnInput`), `::placeholder` (`placeholderText`), `font` e `min-height`.
- Hover real obrigatório: tokens `bgButtonPrimaryHover` e `bgButtonSecondaryHover` só são válidos quando existirem classes utilitárias preparadas para uso de seletor `:hover` na etapa de HTML/CSS final.
- Opacidade: não usar `opacityMuted` para resolver cor de texto; preferir `color` com valores RGBA/HEX com contraste controlado.
- Contraste obrigatório em tema escuro: assegurar WCAG mínimo de 4.5:1 para texto normal e 3:1 para texto grande.

Estrutura obrigatória de `definicoes` (substitui a lista anterior):

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
    "body": { "estilos": ["pageRoot", "bgBody", "fontBase", "textPrimary"] },
    "corpo": { "secoes": [ { "id": "sec-hero", "estilos": ["sectionHero", "surfaceBand"], "elementosSeccao": [] } ] }
  }
}
