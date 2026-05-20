package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignPresetProvisionalHtmlAssemblerTest {

    @Test
    void shouldInjectBehaviorTrackingIntoDesignPresetProvisionalHtml() {
        DesignPresetProvisionalHtmlProcessor processor = mock(DesignPresetProvisionalHtmlProcessor.class);
        when(processor.process(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("<html><head><title>x</title></head><body><section data-section-id='hero'></section></body></html>");
        DesignPresetProvisionalHtmlAssembler assembler = new DesignPresetProvisionalHtmlAssembler(processor, new ObjectMapper());

        String html = assembler.assemble(
                "{\"landingPageWireframe\":{\"sectionOrder\":[]}}",
                "{\"landingPageCopy\":{\"bodySections\":[]}}",
                "{\"landingPageImagePlanning\":{\"images\":[]}}",
                "{\"landingPageDesignPreset\":{\"sectionPresets\":[]}}",
                "job-123");

        assertTrue(html.contains("data-mh-funnel-tracking=\"true\""));
        assertTrue(html.contains("page_view"));
        assertTrue(html.contains("section_view_time"));
        assertTrue(html.contains("data-track-section=\"hero\""));
    }
}
