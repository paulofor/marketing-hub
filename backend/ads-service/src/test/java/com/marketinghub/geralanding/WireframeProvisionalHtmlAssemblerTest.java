package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireframeProvisionalHtmlAssemblerTest {

    private final WireframeProvisionalHtmlAssembler assembler = new WireframeProvisionalHtmlAssembler(new ObjectMapper());

    @Test
    void assembleDelegatesToGeneratorAndBuildsHtml() {
        String modelResponse = """
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {
                        "sectionId": "s1-hero",
                        "uiTags": "<section id='s1-hero'><h1 id='s1-title'></h1><p id='s1-sub'></p></section>",
                        "uiSizeTexts": "s1-title(min:20,max:30); s1-sub(min:60,max:90)",
                        "uiSizes": "#s1-hero{padding:20px}"
                      }
                    ]
                  }
                }
                """;

        String html = assembler.assemble(modelResponse);

        assertNotNull(html);
        assertTrue(html.contains("<!doctype html>"));
        assertTrue(html.contains("id=\"s1-hero\"") || html.contains("id='s1-hero'"));
        assertTrue(html.contains("Lorem ipsum"));
    }

    @Test
    void assembleDelegatesRawJsonWithoutAssemblerNormalization() throws Exception {
        Map<String, Object> section = Map.of(
                "sectionId", "s1-hero-form",
                "uiTags", "<!doctype html><html><body><section id='s1-hero-form'><h1 id='s1-h1'></h1></section></body></html>",
                "uiSizes", "{\"#s1-hero-form\":{\"padding\":\"20px 16px\"},\"#s1-wrap\":{\"gridTemplateColumns\":\"1fr\"}}"
        );
        Map<String, Object> model = Map.of(
                "landingPageWireframe", Map.of("sectionOrder", List.of(section))
        );
        String modelResponse = new ObjectMapper().writeValueAsString(model);

        String html = assembler.assemble(modelResponse);

        assertNotNull(html);
        assertTrue(html.contains("#s1-hero-form {"));
        assertTrue(html.contains("padding: 20px 16px;"));
        assertTrue(html.contains("grid-template-columns: 1fr;"));
        assertTrue(html.toLowerCase().contains("<html><body><section"));
    }
}
