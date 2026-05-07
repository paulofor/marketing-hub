package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireframeProvisionalHtmlAssemblerTest {

    private final WireframeProvisionalHtmlAssembler assembler = new WireframeProvisionalHtmlAssembler(new ObjectMapper());

    @Test
    void assembleAddsLoremBasedOnUiSizeTexts() {
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
        assertTrue(html.contains("data-wireframe-lorem-slot=\"s1-title\""));
        assertTrue(html.contains("data-wireframe-lorem-slot=\"s1-sub\""));
        assertTrue(html.contains("Lorem ipsum"));
    }
}
