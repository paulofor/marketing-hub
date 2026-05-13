package com.marketinghub.geralanding;

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
}
