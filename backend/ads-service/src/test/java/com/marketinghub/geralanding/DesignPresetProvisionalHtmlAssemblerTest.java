package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.presetdesign.provisorio.DesignPresetProvisionalHtmlAssembler;
import com.marketinghub.geralanding.presetdesign.provisorio.DesignPresetProvisionalHtmlProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
        DesignPresetProvisionalHtmlAssembler assembler = new DesignPresetProvisionalHtmlAssembler(
                processor,
                new ObjectMapper());

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

    /** Garante que o manifesto consolidado de imagens seja aplicado ao planejamento antes do processador montar o HTML. */
    @Test
    void shouldEnrichImagePlanningWithFinalAssetManifest() {
        DesignPresetProvisionalHtmlProcessor processor = mock(DesignPresetProvisionalHtmlProcessor.class);
        when(processor.process(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("<html><body><img id='hero-img' src='https://cdn/final.jpg'></body></html>");
        DesignPresetProvisionalHtmlAssembler assembler = new DesignPresetProvisionalHtmlAssembler(
                processor,
                new ObjectMapper());

        assembler.assemble(
                """
                        {"landingPageWireframe":{"sectionOrder":[]}}
                        """,
                """
                        {"landingPageCopy":{"bodySections":[]}}
                        """,
                """
                        {"landingPageImagePlanning":{"images":[{"sectionId":"sec-hero","elementId":"hero-img"}]}}
                        """,
                """
                        {"images":[{"planningItemKey":"sec-hero","elementId":"hero-img","resolvedUrl":"https://cdn/final.jpg"}]}
                        """,
                """
                        {"landingPageDesignPreset":{"sectionPresets":[]}}
                        """,
                "job-456");

        verify(processor).process(
                anyString(),
                anyString(),
                argThat(payload -> payload.contains("\"imageUrl\":\"https://cdn/final.jpg\"")),
                anyString());
    }

}
