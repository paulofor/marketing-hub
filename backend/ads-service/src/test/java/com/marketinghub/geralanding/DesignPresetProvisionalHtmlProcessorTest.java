package com.marketinghub.geralanding;

import com.marketinghub.geralanding.designpreset.DesignPresetProvisionalHtmlProcessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida processamento do preset de design no formato novo tokenizado (definicoes + pagina).
 */
class DesignPresetProvisionalHtmlProcessorTest {

    /**
     * Garante aplicação dos tokens CSS e classes de seção/elemento no HTML provisório.
     */
    @Test
    void shouldApplyLegacyTokenizedPresetFormat() {
        DesignPresetProvisionalHtmlProcessor processor = new DesignPresetProvisionalHtmlProcessor();

        String wireframe = """
                {
                  "pagina": {
                    "head": {},
                    "corpo": {
                      "secoes": [
                        {"id":"sec-hero","elementosSeccao":[{"id":"hero-title","tag":"h1","texto":{"conteudo":""},"elementosInternos":[]}]}]
                    }
                  }
                }
                """;

        String copy = "{" +
                "\"bodySections\":[{\"items\":[{\"id\":\"hero-title\",\"texto\":\"Titulo\"}]}]" +
                "}";

        String design = """
                {
                  "definicoes": {
                    "tipografia": {
                      "desktop": [
                        {"nome":"h1-size","atributoCss":"font-size","valor":"44px"}
                      ],
                      "mobile": []
                    }
                  },
                  "pagina": {
                    "corpo": {
                      "estilos": [{"desktop": ["bg-page"], "mobile": []}],
                      "secoes": [
                        {
                          "id": "sec-hero",
                          "estilos": [{"desktop": ["section-desktop"], "mobile": []}],
                          "elementosSeccao": [
                            {"id": "hero-title", "estilos": [{"desktop": ["h1-size"], "mobile": []}], "elementosInternos": []}
                          ]
                        }
                      ]
                    }
                  }
                }
                """;

        String html = processor.process(wireframe, copy, "{}", design);

        assertThat(html).contains("id=\"lhm-legacy-design-preset-css\"");
        assertThat(html).contains(".h1-size{font-size:44px;}");
        assertThat(html).contains("id=\"sec-hero\"").contains("class=\"section-desktop\"");
        assertThat(html).contains("id=\"hero-title\"").contains("class=\"h1-size\"");
    }

    @Test
    void shouldRejectDeprecatedCanonicalPresetFormat() {
        DesignPresetProvisionalHtmlProcessor processor = new DesignPresetProvisionalHtmlProcessor();

        assertThatThrownBy(() -> processor.process(
                "{\"pagina\":{\"head\":{},\"corpo\":{\"secoes\":[]}}}",
                "{\"bodySections\":[]}",
                "{}",
                "{\"landingPageDesignPreset\":{\"theme\":{}}}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("formato tokenizado");
    }
}
