package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Valida a montagem do HTML provisório da etapa de design preset sem inserir metadados técnicos no resultado.
 */
class DesignPresetProvisionalHtmlAssemblerTest {

    /**
     * Garante que a saída do assembler preserve o HTML sem scripts de tracking e sem comentário técnico de jobId.
     */
    @Test
    void shouldAssembleHtmlWithoutInjectingBehaviorTrackingScript() {
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

        assertFalse(html.contains("data-mh-funnel-tracking=\"true\""));
        assertFalse(html.contains("page_view"));
        assertFalse(html.contains("section_view_time"));
        assertFalse(html.contains("data-track-section=\"hero\""));
        assertFalse(html.contains("<!-- jobId = job-123 -->"));
    }
}
