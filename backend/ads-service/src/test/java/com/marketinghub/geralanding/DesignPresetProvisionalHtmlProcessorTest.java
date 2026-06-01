package com.marketinghub.geralanding;

import com.marketinghub.geralanding.presetdesign.provisorio.DesignPresetProvisionalHtmlProcessor;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida processamento do preset de design no formato novo tokenizado (definicoes + pagina).
 */
@ExtendWith(OutputCaptureExtension.class)
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

        assertThat(html).doesNotContain("id=\"lhm-legacy-design-preset-css\"");
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

    @Test
    void shouldApplyBodyClassesAndNestedSectionClassesFromTokenizedPreset() {
        DesignPresetProvisionalHtmlProcessor processor = new DesignPresetProvisionalHtmlProcessor();

        String wireframe = """
                {
                  "pagina": {
                    "head": {},
                    "corpo": {
                      "secoes": [
                        {
                          "id":"sec-hero",
                          "elementosSeccao":[
                            {
                              "id":"hero-container",
                              "tag":"div",
                              "elementosInternos":[
                                {"id":"hero-title","tag":"h1","texto":{"conteudo":""},"elementosInternos":[]}
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
                """;

        String copy = "{" +
                "\"bodySections\":[{\"items\":[{\"id\":\"hero-title\",\"texto\":\"Titulo\"}]}]" +
                "}";

        String design = """
                {
                  "definicoes": {},
                  "pagina": {
                    "body": {
                      "desktop": ["pageRoot","bgBody","fontBase","textPrimary","textSizeBase","lineHeightBase","marginReset"],
                      "mobile": ["pageRoot","bgBody","fontBase","textPrimary","textSizeBase","lineHeightBase","marginReset"]
                    },
                    "corpo": {
                      "secoes": [
                        {
                          "id": "sec-hero",
                          "estilos": {"desktop": ["section-desktop"], "mobile": []},
                          "elementosSeccao": [
                            {
                              "id": "hero-container",
                              "estilos": {"desktop": ["container-desktop"], "mobile": []},
                              "elementosInternos": [
                                {
                                  "id": "hero-title",
                                  "estilos": {"desktop": ["h1-size"], "mobile": []},
                                  "elementosInternos": []
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
                """;

        String html = processor.process(wireframe, copy, "{}", design);

        List<String> expectedBodyClasses = List.of(
                "pageRoot", "bgBody", "fontBase", "textPrimary", "textSizeBase", "lineHeightBase", "marginReset");
        List<String> bodyClasses = Jsoup.parse(html).body().classNames().stream().toList();
        assertThat(bodyClasses).containsAll(expectedBodyClasses);
        assertThat(html).contains("id=\"sec-hero\"").contains("class=\"section-desktop\"");
        assertThat(html).contains("id=\"hero-container\"").contains("class=\"container-desktop\"");
        assertThat(html).contains("id=\"hero-title\"").contains("class=\"h1-size\"");
    }

    @Test
    void shouldApplyWireframeDefinitionCssAndClassesInDesignPresetAssembly() {
        DesignPresetProvisionalHtmlProcessor processor = new DesignPresetProvisionalHtmlProcessor();

        String wireframe = """
                {
                  "definicoes": {
                    "layout": {
                      "desktop": [{"nome":"wireframe-layout","atributoCss":"display","valor":"grid"}],
                      "mobile": [{"nome":"wireframe-layout","atributoCss":"display","valor":"block"}]
                    }
                  },
                  "pagina": {
                    "corpo": {
                      "secoes": [
                        {"id":"sec-hero","tag":"section","estilos":{"desktop":["wireframe-layout"],"mobile":[]}}
                      ]
                    }
                  }
                }
                """;

        String design = """
                {
                  "definicoes": {},
                  "pagina": { "corpo": { "secoes": [] } }
                }
                """;

        String html = processor.process(wireframe, "{\"bodySections\":[]}", "{}", design);

        assertThat(html).contains(".wireframe-layout{display:grid;}");
        assertThat(html).contains("@media (max-width: 768px)");
        assertThat(html).contains(".wireframe-layout{display:block;}");
        assertThat(html).contains("id=\"sec-hero\"").contains("class=\"wireframe-layout\"");
    }

    @Test
    void shouldKeepWireframeAndDesignCssDefinitionsTogether() {
        DesignPresetProvisionalHtmlProcessor processor = new DesignPresetProvisionalHtmlProcessor();

        String wireframe = """
                {
                  "definicoes": {
                    "layout": {
                      "desktop": [{"nome":"wireframe-layout","atributoCss":"display","valor":"grid"}],
                      "mobile": []
                    }
                  },
                  "pagina": {
                    "corpo": {
                      "secoes": [
                        {"id":"sec-hero","tag":"section","estilos":{"desktop":["wireframe-layout"],"mobile":[]}}
                      ]
                    }
                  }
                }
                """;

        String design = """
                {
                  "definicoes": {
                    "tipografia": {
                      "desktop": [{"nome":"design-title","atributoCss":"font-size","valor":"48px"}],
                      "mobile": []
                    }
                  },
                  "pagina": {
                    "corpo": {
                      "secoes": [
                        {"id":"sec-hero","estilos":{"desktop":["design-title"],"mobile":[]}}
                      ]
                    }
                  }
                }
                """;

        String html = processor.process(wireframe, "{\"bodySections\":[]}", "{}", design);

        assertThat(html).contains(".wireframe-layout{display:grid;}");
        assertThat(html).contains(".design-title{font-size:48px;}");
        assertThat(html).contains("id=\"sec-hero\"").contains("wireframe-layout").contains("design-title");
    }

    /**
     * Garante que CTAs usam targetSectionId de interacao e que botões de formulário viram submit.
     */
    @Test
    void shouldApplyInteractionHrefAndSubmitButtonInsideForm() {
        DesignPresetProvisionalHtmlProcessor processor = new DesignPresetProvisionalHtmlProcessor();

        String html = processor.process(fullWireframe(), fullCopy(), "{}", fullDesign());
        org.jsoup.nodes.Document document = Jsoup.parse(html);

        assertThat(document.getElementById("hero-cta-primary").attr("href")).isEqualTo("#sec-formulario");
        assertThat(document.getElementById("hero-cta-secondary").attr("href")).isEqualTo("#sec-como-funciona");
        assertThat(document.getElementById("problema-anchor-to-prova").attr("href")).isEqualTo("#sec-prova-preview");
        assertThat(document.getElementById("como-cta-to-form").attr("href")).isEqualTo("#sec-formulario");
        assertThat(document.getElementById("prova-cta-to-form").attr("href")).isEqualTo("#sec-formulario");
        assertThat(document.getElementById("faq-cta").attr("href")).isEqualTo("#sec-formulario");
        assertThat(document.getElementById("form-submit").attr("type")).isEqualTo("submit");
    }

    /**
     * Garante que contratoCampo preenche atributos e que copy de input vira placeholder sem fechamento inválido.
     */
    @Test
    void shouldApplyFieldContractAndInputCopyAsPlaceholder() {
        DesignPresetProvisionalHtmlProcessor processor = new DesignPresetProvisionalHtmlProcessor();

        String html = processor.process(fullWireframe(), fullCopy(), "{}", fullDesign());
        org.jsoup.nodes.Document document = Jsoup.parse(html);

        assertThat(document.getElementById("input-nome").attr("type")).isEqualTo("text");
        assertThat(document.getElementById("input-nome").attr("name")).isEqualTo("nome");
        assertThat(document.getElementById("input-nome").attr("autocomplete")).isEqualTo("name");
        assertThat(document.getElementById("input-nome").hasAttr("required")).isTrue();
        assertThat(document.getElementById("input-nome").attr("placeholder")).isEqualTo("Seu nome");
        assertThat(document.getElementById("input-email").attr("type")).isEqualTo("email");
        assertThat(document.getElementById("input-email").attr("name")).isEqualTo("email");
        assertThat(document.getElementById("input-email").attr("autocomplete")).isEqualTo("email");
        assertThat(document.getElementById("input-email").attr("placeholder")).isEqualTo("Seu melhor email");
        assertThat(html).doesNotContain("</input>");
    }

    /**
     * Garante que copy em container com filhos preserva estrutura e que assets são aplicados por elementId.
     */
    @Test
    void shouldPreserveContainerChildrenAndApplyImageAssetsByElementId() {
        DesignPresetProvisionalHtmlProcessor processor = new DesignPresetProvisionalHtmlProcessor();
        String imagePlanning = """
                {"images":[{"elementId":"hero-img","src":"https://cdn.example.org/final.png","alt":"Imagem planejada","width":1200,"height":900}]}
                """;

        String html = processor.process(fullWireframe(), fullCopy(), imagePlanning, fullDesign());
        org.jsoup.nodes.Document document = Jsoup.parse(html);

        assertThat(document.getElementById("hero-container").children()).hasSize(3);
        assertThat(document.getElementById("hero-title").text()).isEqualTo("Título preservado");
        assertThat(document.getElementById("hero-img").attr("src")).isEqualTo("https://cdn.example.org/final.png");
        assertThat(document.getElementById("hero-img").attr("alt")).isEqualTo("Imagem planejada");
        assertThat(document.getElementById("hero-img").attr("width")).isEqualTo("1200");
        assertThat(document.getElementById("hero-img").attr("height")).isEqualTo("900");
    }

    /**
     * Garante que tokens inexistentes e src temporário example.com são reportados por warning.
     */
    @Test
    void shouldReportUndefinedTokensAndExampleImageSource(CapturedOutput output) {
        DesignPresetProvisionalHtmlProcessor processor = new DesignPresetProvisionalHtmlProcessor();

        processor.process(fullWireframe(), fullCopy(), "{}", fullDesign());

        assertThat(output).contains("Token CSS não definido: marginReset");
        assertThat(output).contains("Imagem usa src temporário example.com: hero-img");
    }

    /** Retorna wireframe completo para validar atributos funcionais sem estilos hardcoded. */
    private String fullWireframe() {
        return """
                {
                  "pagina": {"corpo": {"secoes": [
                    {"id":"sec-hero","tag":"section","elementosSeccao":[
                      {"id":"hero-container","tag":"div","elementosInternos":[
                        {"id":"hero-title","tag":"h1","elementosInternos":[]},
                        {"id":"hero-img","tag":"img","asset":{"src":"https://example.com/placeholder.png","alt":"Imagem do wireframe","width":640,"height":480},"elementosInternos":[]},
                        {"id":"hero-cta-primary","tag":"a","interacao":{"targetSectionId":"#sec-formulario"},"elementosInternos":[]}
                      ]},
                      {"id":"hero-cta-secondary","tag":"a","interacao":{"targetSectionId":"#sec-como-funciona"},"elementosInternos":[]}
                    ]},
                    {"id":"sec-problema","tag":"section","elementosSeccao":[{"id":"problema-anchor-to-prova","tag":"a","interacao":{"targetSectionId":"#sec-prova-preview"},"elementosInternos":[]}]},
                    {"id":"sec-como-funciona","tag":"section","elementosSeccao":[{"id":"como-cta-to-form","tag":"a","interacao":{"targetSectionId":"#sec-formulario"},"elementosInternos":[]}]},
                    {"id":"sec-prova-preview","tag":"section","elementosSeccao":[{"id":"prova-cta-to-form","tag":"a","interacao":{"targetSectionId":"#sec-formulario"},"elementosInternos":[]}]},
                    {"id":"sec-faq","tag":"section","elementosSeccao":[{"id":"faq-cta","tag":"a","interacao":{"targetSectionId":"#sec-formulario"},"elementosInternos":[]}]},
                    {"id":"sec-formulario","tag":"section","elementosSeccao":[
                      {"id":"lead-form","tag":"form","elementosInternos":[
                        {"id":"input-nome","tag":"input","contratoCampo":{"type":"text","name":"nome","autocomplete":"name","required":true,"placeholder":""},"elementosInternos":[]},
                        {"id":"input-email","tag":"input","contratoCampo":{"type":"email","name":"email","autocomplete":"email","required":true,"placeholder":""},"elementosInternos":[]},
                        {"id":"form-submit","tag":"button","componente":"buttonPrimary","interacao":{"intencaoAcao":"Enviar formulário e gerar amostra"},"elementosInternos":[]}
                      ]}
                    ]}
                  ]}}
                }
                """;
    }

    /** Retorna copy por id para validar placeholder, CTAs e preservação estrutural. */
    private String fullCopy() {
        return """
                {"bodySections":[{"items":[
                  {"id":"hero-container","texto":"Copy que não pode destruir filhos"},
                  {"id":"hero-title","texto":"Título preservado"},
                  {"id":"hero-cta-primary","texto":"Gerar minha amostra em PDF (2 min)"},
                  {"id":"hero-cta-secondary","texto":"Como funciona"},
                  {"id":"problema-anchor-to-prova","texto":"Ver prova"},
                  {"id":"como-cta-to-form","texto":"Ir para formulário"},
                  {"id":"prova-cta-to-form","texto":"Quero minha amostra"},
                  {"id":"faq-cta","texto":"Começar agora"},
                  {"id":"input-nome","texto":"Seu nome"},
                  {"id":"input-email","texto":"Seu melhor email"},
                  {"id":"form-submit","texto":"Enviar e gerar minha amostra"}
                ]}]}
                """;
    }

    /** Retorna design tokenizado com marginReset ausente para validar warning de token inexistente. */
    private String fullDesign() {
        return """
                {
                  "definicoes": {
                    "layout": {"desktop": [
                      {"nome":"pageRoot","atributoCss":"display","valor":"block"},
                      {"nome":"buttonPrimary","atributoCss":"display","valor":"inline-block"}
                    ], "mobile": []}
                  },
                  "pagina": {
                    "body": {"desktop": ["pageRoot", "marginReset"], "mobile": []},
                    "corpo": {"secoes": [
                      {"id":"hero-cta-primary","estilos":{"desktop":["buttonPrimary"],"mobile":[]}}
                    ]}
                  }
                }
                """;
    }
}
