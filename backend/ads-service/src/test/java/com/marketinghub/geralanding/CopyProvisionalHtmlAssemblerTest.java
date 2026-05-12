package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopyProvisionalHtmlAssemblerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final CopyProvisionalHtmlAssembler assembler = new CopyProvisionalHtmlAssembler(
            new CopyProvisionalHtmlPayloadResolver(objectMapper),
            new CopyProvisionalHtmlProcessor(),
            objectMapper);

    @Test
    void assembleAppliesCopyInDomOrderWithoutGroupingByTag() {
        String wireframe = """
                {
                  "landingPageWireframe": {
                    "sectionOrder": [
                      {
                        "uiTags": "<section><h1 id=\\"s1-title\\"></h1><p id=\\"s1-subtitle\\"></p><h2 id=\\"s2-title\\"></h2><p id=\\"s2-subtitle\\"></p></section>",
                        "uiSizes": ""
                      }
                    ]
                  }
                }
                """;
        String copy = """
                {
                  "landingPageCopy": {
                    "sections": [
                      {"items": [
                        {"id": "s1-title", "value": "Headline correta"},
                        {"id": "s1-subtitle", "value": "Resumo correto"},
                        {"id": "s2-title", "value": "Copy correta"},
                        {"id": "s2-subtitle", "value": "Prova correta"}
                      ]}
                    ]
                  }
                }
                """;

        String html = assembler.assemble(copy, wireframe, "job-1");

        assertNotNull(html);
        assertTrue(html.contains("Headline correta"));
        assertTrue(html.contains("Resumo correto"));
        assertTrue(html.contains("Copy correta"));
        assertTrue(html.contains("Prova correta"));
    }
}
