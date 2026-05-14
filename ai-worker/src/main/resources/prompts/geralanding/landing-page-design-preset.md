Você está na etapa `landing-page-design-preset` do pipeline Gera Landing.

Objetivo:
- Gerar o artefato `landingPageDesignPreset` cobrindo tema visual e presets por seção/elemento.
- Produzir atributos CSS explícitos por seção e por elemento.

Regras obrigatórias:
1. Responda somente JSON válido no contrato solicitado.
2. Não inventar `sectionId`: use apenas os ids existentes em `landingPageWireframe`.
3. Para cada seção em `sectionPresets`, preencher `sectionId`, `surfaceStyle`, `contrastMode` e `sectionAttributes`.
4. Para cada elemento em `elementPresets`, preencher `elementId`, `tag`, `attributes` e `tokenBindings`.
5. Todo item de `attributes[].name` deve vir exatamente da whitelist abaixo.
6. Não usar json em string. Campos estruturados devem ser objetos/arrays JSON reais.
7. Manter consistência com objetivo comercial de conversão (clareza visual, contraste, hierarquia e foco no CTA).

Whitelist de atributos CSS permitidos (`attributes[].name`):
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
- font
- font-family
- font-size
- font-weight
- font-style
- font-variant
- line-height
- letter-spacing
- word-spacing
- text-align
- text-decoration
- text-decoration-line
- text-decoration-color
- text-decoration-style
- text-transform
- text-shadow
- white-space
- border
- border-width
- border-style
- border-color
- border-top
- border-right
- border-bottom
- border-left
- border-radius
- outline
- outline-width
- outline-style
- outline-color
- outline-offset
- box-shadow
- opacity
- filter
- backdrop-filter
- mix-blend-mode
- isolation
- cursor
- appearance
- caret-color
- accent-color
- list-style
- list-style-type
- list-style-position
- list-style-image
- object-fit
- object-position
- transition
- transition-property
- transition-duration
- transition-timing-function
- transition-delay
- animation
- animation-name
- animation-duration
- animation-timing-function
- animation-delay
- animation-iteration-count
- animation-direction
- animation-fill-mode
- animation-play-state

Formato de saída:
{
  "landingPageDesignPreset": {
    "presetId": "string",
    "theme": {
      "palette": {},
      "typography": {},
      "spacing": {}
    },
    "sectionPresets": [
      {
        "sectionId": "string",
        "surfaceStyle": "band|solid|gradient-soft|image-tint",
        "contrastMode": "normal|high|soft",
        "sectionAttributes": [
          { "name": "background-color", "value": "#FFFFFF" }
        ]
      }
    ],
    "elementPresets": [
      {
        "sectionId": "string",
        "elementId": "string",
        "tag": "string",
        "attributes": [
          { "name": "font-size", "value": "16px" }
        ],
        "tokenBindings": [
          { "attributeName": "font-size", "tokenPath": "theme.typography.baseSize" }
        ]
      }
    ]
  }
}
