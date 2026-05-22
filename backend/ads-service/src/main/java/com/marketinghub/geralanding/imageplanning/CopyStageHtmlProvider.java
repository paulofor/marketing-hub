package com.marketinghub.geralanding.imageplanning;

/**
 * Contrato interno da etapa de image planning para obter o HTML provisório gerado na etapa de copy.
 */
public interface CopyStageHtmlProvider {

    /**
     * Monta o HTML provisório da etapa de copy usando os artefatos de copy e wireframe.
     */
    String assemble(String copyModelResponse, String wireframeModelResponse, String jobId);
}
