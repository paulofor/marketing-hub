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
