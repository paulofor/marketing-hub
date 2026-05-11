package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopyProvisionalHtmlAssemblerTest {

    private final CopyProvisionalHtmlAssembler assembler = new CopyProvisionalHtmlAssembler(new ObjectMapper());

    @Test
    void assembleAppliesCopyInDomOrderWithoutGroupingByTag() {
        String wireframe = """
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {
                        "uiTags": "<section><h1></h1><p></p><h2></h2><p></p></section>",
                        "uiSizes": ""
                      }
                    ]
                  }
                }
                """;
        String copy = """
                {
                  "landingPageCopy": {
                    "hero": {"headline": "Headline correta"},
                    "bodySections": [
                      {"sectionId": "mechanism", "summary": "Resumo correto", "copy": "Copy correta"},
                      {"sectionId": "proof", "summary": "Prova correta"}
                    ]
                  }
                }
                """;

        String html = assembler.assemble(copy, wireframe, "job-1");

        assertNotNull(html);
        assertTrue(html.contains("<h1>Headline correta</h1>"));
        assertTrue(html.contains("<p>Resumo correto</p>"));
        assertTrue(html.contains("<h2>Copy correta</h2>"));
        assertTrue(html.contains("<p>Prova correta</p>"));
    }
}
