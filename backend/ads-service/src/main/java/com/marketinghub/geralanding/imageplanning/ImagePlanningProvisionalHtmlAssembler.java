package com.marketinghub.geralanding.imageplanning;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Conjunto exclusivo da etapa LANDING_PAGE_IMAGE_PLANNING: monta o HTML provisório base
 * a partir da etapa de copy e enriquece com URLs de imagens geradas para esta etapa.
 */
@Component
public class ImagePlanningProvisionalHtmlAssembler {

    private final CopyStageHtmlProvider copyStageHtmlProvider;
    private final ImageHtmlInjector imageHtmlInjector;

    public ImagePlanningProvisionalHtmlAssembler(CopyStageHtmlProvider copyStageHtmlProvider,
                                                 ImageHtmlInjector imageHtmlInjector) {
        this.copyStageHtmlProvider = copyStageHtmlProvider;
        this.imageHtmlInjector = imageHtmlInjector;
    }

    /**
     * Gera o HTML provisório da etapa de image planning usando copy+wireframe e aplica as URLs finais de imagem.
     */
    public String assemble(Long experimentId,
                           String copyModelResponse,
                           String wireframeModelResponse,
                           String jobId) {
        String copyStageHtml = copyStageHtmlProvider.assemble(copyModelResponse, wireframeModelResponse, jobId);
        if (!StringUtils.hasText(copyStageHtml)) {
            return null;
        }
        return imageHtmlInjector.inject(experimentId, copyStageHtml);
    }
}
