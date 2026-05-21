package com.marketinghub.geralanding.imageplanning;

import com.marketinghub.experiment.pipeline.service.LandingPageImageInjector;
import com.marketinghub.geralanding.copy.CopyProvisionalHtmlAssembler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Conjunto exclusivo da etapa LANDING_PAGE_IMAGE_PLANNING: monta o HTML provisório base
 * a partir da etapa de copy e enriquece com URLs de imagens geradas para esta etapa.
 */
@Component
public class ImagePlanningProvisionalHtmlAssembler {

    private final CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler;
    private final LandingPageImageInjector landingPageImageInjector;

    public ImagePlanningProvisionalHtmlAssembler(CopyProvisionalHtmlAssembler copyProvisionalHtmlAssembler,
                                                 LandingPageImageInjector landingPageImageInjector) {
        this.copyProvisionalHtmlAssembler = copyProvisionalHtmlAssembler;
        this.landingPageImageInjector = landingPageImageInjector;
    }

    /**
     * Gera o HTML provisório da etapa de image planning usando copy+wireframe e aplica as URLs finais de imagem.
     */
    public String assemble(Long experimentId,
                           String copyModelResponse,
                           String wireframeModelResponse,
                           String jobId) {
        String copyStageHtml = copyProvisionalHtmlAssembler.assemble(copyModelResponse, wireframeModelResponse, jobId);
        if (!StringUtils.hasText(copyStageHtml)) {
            return null;
        }
        return landingPageImageInjector.injectImages(experimentId, copyStageHtml);
    }
}
