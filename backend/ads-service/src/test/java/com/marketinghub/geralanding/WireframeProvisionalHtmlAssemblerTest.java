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
        assertTrue(!html.contains("<!-- jobId ="));
    }

    @Test
    void assemblePreservesPlainCssUiSizesIncludingImageDimensions() {
        String modelResponse = """
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {
                        "sectionId": "s1-hero-proof-split",
                        "uiTags": "<section id='s1-hero-proof-split'><div id='s1-visual'><img id='img-s1-mockup' /><img id='img-s1-kit' /></div></section>",
                        "uiSizes": "#s1-visual{display:grid;grid-template-columns:1fr 1fr;gap:10px;} #img-s1-mockup{width:100%;height:auto;aspect-ratio:4/5;object-fit:cover;border-radius:12px;} #img-s1-kit{width:100%;height:auto;aspect-ratio:1/1;object-fit:cover;border-radius:12px;}"
                      }
                    ]
                  }
                }
                """;

        String html = assembler.assemble(modelResponse);

        assertNotNull(html);
        assertTrue(html.contains("#img-s1-mockup{width:100%;height:auto;aspect-ratio:4/5;object-fit:cover;border-radius:12px;}"));
        assertTrue(html.contains("#img-s1-kit{width:100%;height:auto;aspect-ratio:1/1;object-fit:cover;border-radius:12px;}"));
    }
}
