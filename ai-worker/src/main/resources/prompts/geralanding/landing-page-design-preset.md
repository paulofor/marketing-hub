Você está na etapa `landing-page-design-preset` do pipeline Gera Landing.

Estamos Trabalhando nesse contexto:

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

Regras obrigatórias:
1. Responda somente JSON válido.
2. Preserve a estrutura de `pagina` recebida no wireframe (mesmos ids, mesma hierarquia, sem inventar seções/elementos).
3. Aplique definições visuais pelos grupos em `definicoes`, com variações `desktop` e `mobile`.
4. Cada item de `desktop/mobile` deve seguir exatamente:
   - `nome`: nome da classe utilitária
   - `atributoCss`: propriedade CSS
   - `valor`: valor CSS válido
5. Não usar JSON serializado em string.
6. Usar exclusivamente propriedades CSS permitidas em `docs/gera-landing/listas-css-estrutura-acabamento.md`.
7. Manter foco de conversão: contraste legível, CTA destacado e consistência entre seção e elementos.

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
  "pagina": { "...": "estrutura do wireframe aplicada" }
}
