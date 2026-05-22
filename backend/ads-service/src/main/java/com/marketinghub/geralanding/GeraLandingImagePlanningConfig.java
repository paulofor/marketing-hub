package com.marketinghub.geralanding;

import com.marketinghub.experiment.pipeline.service.LandingPageImageInjector;
import com.marketinghub.geralanding.copy.CopyProvisionalHtmlAssembler;
import com.marketinghub.geralanding.imageplanning.CopyStageHtmlProvider;
import com.marketinghub.geralanding.imageplanning.ImageHtmlInjector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura os adaptadores de integração usados pela etapa de image planning sem quebrar isolamento de pacote.
 */
@Configuration
public class GeraLandingImagePlanningConfig {

    /**
     * Adapta o assembler da etapa de copy para o contrato interno da etapa de image planning.
     */
    @Bean
    public CopyStageHtmlProvider copyStageHtmlProvider(CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler) {
        return copyProvisionalHtmlAssembler::assemble;
    }

    /**
     * Adapta o injetor de imagens para o contrato interno da etapa de image planning.
     */
    @Bean
    public ImageHtmlInjector imageHtmlInjector(LandingPageImageInjector landingPageImageInjector) {
        return landingPageImageInjector::injectImages;
    }
}
