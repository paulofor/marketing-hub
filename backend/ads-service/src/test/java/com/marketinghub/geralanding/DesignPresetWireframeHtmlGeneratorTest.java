package com.marketinghub.geralanding;

import com.marketinghub.geralanding.designpreset.DesignPresetWireframeHtmlGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida que o gerador de wireframe do preset preserva atributos do contrato sem injeções extras.
 */
class DesignPresetWireframeHtmlGeneratorTest {

    /**
     * Garante que classes e estilos vindos em props/atributos sejam preservados e combinados com tokens responsivos.
     */
    @Test
    void shouldMergeExistingAndResponsiveClassAndStyleWithoutDroppingContractFields() {
        DesignPresetWireframeHtmlGenerator generator = new DesignPresetWireframeHtmlGenerator();

        String json = """
                {
                  "definicoes": {
                    "tipografia": {
                      "desktop": [
                        {"nome":"section-token","atributoCss":"padding","valor":"20px"},
                        {"nome":"title-token","atributoCss":"font-size","valor":"32px"}
                      ],
                      "mobile": []
                    }
                  },
                  "pagina": {
                    "head": {"texto": "Teste"},
                    "corpo": {
                      "secoes": [
                        {
                          "id": "sec-1",
                          "props": {"class": "existing-section", "style": "background:#fafafa", "data-x": "1"},
                          "estilos": [{"desktop": ["section-token"], "mobile": []}],
                          "elementosSeccao": [
                            {
                              "id": "title",
                              "tag": "h1",
                              "props": {"class": "existing-title", "style": "color:#111"},
                              "texto": {"conteudo": "Titulo"},
                              "estilos": [{"desktop": ["title-token"], "mobile": []}],
                              "elementosInternos": []
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
                """;

        String html = generator.generateFromJson(json);

        assertThat(html).contains("id=\"sec-1\"");
        assertThat(html).contains("class=\"existing-section section-token\"");
        assertThat(html).contains("style=\"background:#fafafa\"");
        assertThat(html).contains("data-x=\"1\"");

        assertThat(html).contains("id=\"title\"");
        assertThat(html).contains("class=\"existing-title title-token\"");
        assertThat(html).contains("style=\"color:#111\"");
    }

    /**
     * Garante que o gerador não injeta alt/width/height automaticamente quando não vierem no contrato.
     */
    @Test
    void shouldNotInjectImageAttributesWhenMissingFromContract() {
        DesignPresetWireframeHtmlGenerator generator = new DesignPresetWireframeHtmlGenerator();

        String json = """
                {
                  "definicoes": {},
                  "pagina": {
                    "head": {"texto": "Teste"},
                    "corpo": {
                      "secoes": [
                        {
                          "id": "sec-img",
                          "elementosSeccao": [
                            {"id": "hero-image", "tag": "img", "props": {"src": "https://img.local/x.png"}, "elementosInternos": []}
                          ]
                        }
                      ]
                    }
                  }
                }
                """;

        String html = generator.generateFromJson(json);

        assertThat(html).contains("<img id=\"hero-image\" src=\"https://img.local/x.png\">");
        assertThat(html).doesNotContain(" alt=");
        assertThat(html).doesNotContain(" width=");
        assertThat(html).doesNotContain(" height=");
    }
}
