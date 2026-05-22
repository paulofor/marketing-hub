package com.marketinghub.geralanding;

import com.marketinghub.geralanding.copy.CopyProvisionalHtmlProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CopyProvisionalHtmlProcessorNewWireframeFormatTest {

    private final CopyProvisionalHtmlProcessor processor = new CopyProvisionalHtmlProcessor();

    @Test
    void shouldGenerateHtmlFromPaginaWireframeAndApplyCopyItems() {
        String wireframeJson = """
                {
                  "pagina": {
                    "head": { "texto": "Wireframe novo" },
                    "corpo": {
                      "estilos": [{ "nome": "max-width", "valor": "680px" }],
                      "secoes": [
                        {
                          "id": "s1-hero",
                          "tag": "section",
                          "estilos": [{ "nome": "padding", "valor": "16px" }],
                          "elementosSeccao": [
                            {"id":"headline","tag":"h1","texto":{"conteudo":""},"estilos":[{"nome":"margin","valor":"0"}]},
                            {"id":"subheadline","tag":"p","texto":{"conteudo":""},"estilos":[{"nome":"margin","valor":"8px 0 0"}]}
                          ]
                        }
                      ]
                    }
                  }
                }
                """;

        String copyJson = """
                {
                  "bodySections": [
                    {
                      "items": [
                        {"item":"headline","copy":"Transforme sua captação"},
                        {"item":"subheadline","copy":"Receba uma prévia em poucos minutos"}
                      ]
                    }
                  ]
                }
                """;

        String html = processor.process(wireframeJson, copyJson);

        assertTrue(html.contains("Transforme sua captação"));
        assertTrue(html.contains("Receba uma prévia em poucos minutos"));
        assertTrue(html.contains("headline"));
    }


    @Test
    void shouldApplyTextoFieldAndSetPlaceholderForInputElements() {
        String wireframeJson = """
                {
                  "sectionOrder": [
                    {
                      "uiTags": "<section><h2 id=\\"title\\">Titulo</h2><input id=\\"email-input\\" type=\\"email\\" placeholder=\\"\\"/></section>",
                      "uiSizes": ""
                    }
                  ]
                }
                """;

        String copyJson = """
                {
                  "bodySections": [
                    {
                      "items": [
                        {"id": "title", "texto": "Novo título"},
                        {"id": "email-input", "texto": "seuemail@exemplo.com"}
                      ]
                    }
                  ]
                }
                """;

        String html = processor.process(wireframeJson, copyJson);

        assertTrue(html.contains("Novo título"));
        assertTrue(html.contains("id=\"email-input\""));
        assertTrue(html.contains("placeholder=\"seuemail@exemplo.com\""));
    }

    @Test
    void shouldComposeHtmlUsingCopyImagesAndDesignPreset() {
        String wireframeJson = """
                {
                  "sectionOrder": [
                    {
                      "uiTags": "<section><h1 id=\\"title\\"></h1><img id=\\"hero-img\\" src=\\"about:blank\\"/></section>",
                      "uiSizes": ""
                    }
                  ]
                }
                """;

        String copyJson = """
                {
                  "bodySections": [
                    {
                      "items": [
                        {"id": "title", "copy": "Landing final"}
                      ]
                    }
                  ]
                }
                """;

        String imagePlanningJson = """
                {
                  "images": [
                    {"imageUrl": "https://cdn.example.com/hero.png"}
                  ]
                }
                """;

        String designPresetJson = """
                {
                  "landingPageDesignPreset": {
                    "presetId": "preset-77",
                    "lhmRuntime": {
                      "baseCss": "body{background:#111;color:#fff;}"
                    }
                  }
                }
                """;

        String html = processor.process(wireframeJson, copyJson);

        assertTrue(html.contains("Landing final"));
    }

    @Test
    void shouldApplyCopyWhenBodySectionsAreProvidedInsidePaginaSchema() {
        String wireframeJson = """
                {
                  "pagina": {
                    "corpo": {
                      "secoes": [
                        {
                          "id": "s1-hero",
                          "tag": "section",
                          "elementosSeccao": [
                            {"id":"headline","tag":"h1","texto":{"conteudo":""}}
                          ]
                        }
                      ]
                    }
                  }
                }
                """;

        String copyJson = """
                {
                  "landingPageCopy": {
                    "pagina": {
                      "corpo": {
                        "secoes": [
                          {
                            "elementosSeccao": [
                              {"id":"headline","texto":{"conteudo":"Título via pagina"}}
                            ]
                          }
                        ]
                      }
                    }
                  }
                }
                """;

        String html = processor.process(wireframeJson, copyJson);

        assertTrue(html.contains("Título via pagina"));
    }

    @Test
    void shouldRenderResponsiveCssAndClassReferencesFromNewWireframeDefinitions() {
        String wireframeJson = """
                {
                  "definicoes": {
                    "layout": {
                      "desktop": [
                        {"nome":"section-flex-col","atributoCss":"display","valor":"flex"}
                      ],
                      "mobile": [
                        {"nome":"section-flex-col","atributoCss":"display","valor":"block"}
                      ]
                    }
                  },
                  "pagina": {
                    "corpo": {
                      "secoes": [
                        {
                          "id": "s1",
                          "tag": "section",
                          "estilos": [{"desktop":["section-flex-col"],"mobile":["section-flex-col"]}],
                          "elementosSeccao": []
                        }
                      ]
                    }
                  }
                }
                """;

        String copyJson = "{\"bodySections\":[]}";

        String html = processor.process(wireframeJson, copyJson);

        assertTrue(html.contains(".section-flex-col {display:flex;}"));
        assertTrue(html.contains("@media (max-width: 768px)"));
        assertTrue(html.contains(".section-flex-col {display:block;}"));
        assertTrue(html.contains("class=\"section-flex-col\""));
    }
}
