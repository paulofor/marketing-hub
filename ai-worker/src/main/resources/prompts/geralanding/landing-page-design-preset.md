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
8. `theme.typography` é obrigatório e detalhado por tokens semânticos: `display`, `h1`, `h2`, `h3`, `body`, `lead`, `caption`, `overline`, `button`, `legal`.
9. Para cada token tipográfico, declarar explicitamente todos os atributos CSS abaixo (sem omissões): `font`, `font-family`, `font-size`, `font-weight`, `font-style`, `font-variant`, `line-height`, `letter-spacing`, `word-spacing`, `text-align`, `text-decoration`, `text-decoration-line`, `text-decoration-color`, `text-decoration-style`, `text-transform`, `text-shadow`, `white-space`.
10. Cada atributo tipográfico deve ter valor CSS concreto e válido; não usar vazio/null e evitar valores genéricos (`inherit`, `unset`, `initial`, `revert`) quando não houver intenção explícita.
11. Reforçar hierarquia tipográfica: diferença perceptível entre títulos, subtítulos, corpo, apoio e CTA (escala, peso, espaçamento e contraste), mantendo legibilidade em desktop e mobile.
12. Gerar variações `desktop` e `mobile` para cada token tipográfico, preservando a mesma hierarquia visual entre breakpoints.
13. Em `consistencyChecks`, validar explicitamente: presença de todos os atributos tipográficos obrigatórios, legibilidade, contraste e força tipográfica de `display/h1/button`.
14. Se faltar qualquer atributo obrigatório em qualquer token, registrar `FAIL` em `consistencyChecks` com detalhes objetivos; não omitir erro silenciosamente.
15. Evitar geração de presets redundantes do LHM: em `elementPresets`, incluir somente elementos que realmente existem no `landingPageWireframe` e que exigem override para conversão/legibilidade; não criar entradas extras apenas para repetir defaults já cobertos por `theme`/`sectionPresets` e não inventar classes/tokens fora do contrato.

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
    ],
    "consistencyChecks": [
      {
        "check": "TYPOGRAPHY_REQUIRED_ATTRIBUTES",
        "status": "PASS|WARN|FAIL",
        "details": "string"
      }
    ]
  }
}
