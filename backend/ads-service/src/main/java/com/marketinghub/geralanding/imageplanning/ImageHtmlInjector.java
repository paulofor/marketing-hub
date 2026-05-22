package com.marketinghub.geralanding.imageplanning;

/**
 * Contrato interno da etapa de image planning para injetar URLs finais de imagens no HTML provisório.
 */
public interface ImageHtmlInjector {

    /**
     * Injeta as URLs de imagem da landing no HTML provisório da etapa.
     */
    String inject(Long experimentId, String provisionalHtml);
}
