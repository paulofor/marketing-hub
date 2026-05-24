package com.marketinghub.geralanding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.designpreset.DesignPresetProvisionalHtmlAssembler;
import com.marketinghub.geralanding.designpreset.DesignPresetProvisionalHtmlProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignPresetProvisionalHtmlAssemblerErrorDetailTest {

    @Test
    void shouldIncludeElementDetailWhenDesignPresetAssemblyFails() {
        DesignPresetProvisionalHtmlProcessor processor = mock(DesignPresetProvisionalHtmlProcessor.class);
        when(processor.process(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Falha ao processar propriedades do elemento: id=hero-title, tag=h1"));

        DesignPresetProvisionalHtmlAssembler assembler = new DesignPresetProvisionalHtmlAssembler(
                processor,
                new ObjectMapper());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> assembler.assemble(
                        "{\"landingPageWireframe\":{\"pagina\":{\"corpo\":{\"secoes\":[]}}}}",
                        "{\"landingPageCopy\":{\"bodySections\":[]}}",
                        "{\"landingPageImagePlanning\":{\"images\":[]}}",
                        "{\"landingPageDesignPreset\":{\"definicoes\":{},\"pagina\":{}}}",
                        "job-design-erro-1"));

        assertTrue(ex.getMessage().contains("jobId=job-design-erro-1"));
        assertTrue(ex.getMessage().contains("errorDetails="));
        assertTrue(ex.getMessage().contains("id=hero-title"));
    }
}
