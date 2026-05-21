package com.marketinghub.geralanding;

import com.marketinghub.geralanding.wireframe.WireframeHtmlGenerator;
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

}
