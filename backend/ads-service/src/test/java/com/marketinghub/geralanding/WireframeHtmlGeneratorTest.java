package com.marketinghub.geralanding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireframeHtmlGeneratorTest {

    @Test
    void shouldApplyUiSizesWhenProvidedAsJsonObject() {
        String json = """
            {
              "sectionOrder": [
                {
                  "uiTags": "<section id=\\\"hero\\\"><h1></h1></section>",
                  "uiSizes": {
                    "#hero": {
                      "padding": "24px",
                      "max-width": "1024px"
                    },
                    "#hero h1": {
                      "font-size": "32px"
                    }
                  }
                }
              ]
            }
            """;

        WireframeHtmlGenerator generator = new WireframeHtmlGenerator();
        String html = generator.generateFromJson(json);

        assertNotNull(html);
        assertTrue(html.contains("#hero {"));
        assertTrue(html.contains("padding: 24px;"));
        assertTrue(html.contains("max-width: 1024px;"));
        assertTrue(html.contains("#hero h1 {"));
        assertTrue(html.contains("font-size: 32px;"));
    }

    @Test
    void shouldAlternateOnlyTwoSectionColorsAndHighlightFormInLightGreen() {
        String json = """
            {
              "sectionOrder": [
                {
                  "uiTags": "<section id=\\\"s1\\\"><h1></h1><form id=\\\"f1\\\"></form></section>"
                },
                {
                  "uiTags": "<section id=\\\"s2\\\"><h2></h2></section>"
                }
              ]
            }
            """;

        WireframeHtmlGenerator generator = new WireframeHtmlGenerator();
        String html = generator.generateFromJson(json);

        assertNotNull(html);
        assertTrue(html.contains("background:#ffffff;color:#111111"));
        assertTrue(html.contains("background:#111111;color:#ffffff"));
        assertTrue(html.contains("form id=\"f1\" style=\"background:#dcfce7;color:#14532d;border:1px solid #86efac;border-radius:12px;padding:16px;\""));
    }
}
