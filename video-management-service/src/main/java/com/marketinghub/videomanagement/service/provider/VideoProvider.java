package com.marketinghub.videomanagement.service.provider;

import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;

/**
 * Contrato para integrações com providers de vídeo.
 */
public interface VideoProvider {

    /** Identifica os jobs atendidos por esta integração. */
    boolean supports(SalesVideoJob job);

    /** Valida entradas determinísticas antes de qualquer planejador ou provedor pago. */
    default void validateInput(SalesVideoJob job, SalesVideoProfile profile) { }

    /** Executa a produção contratada e devolve arquivo e auditoria para o backend. */
    ProviderArtifacts render(SalesVideoJob job,
                             SalesVideoProfile profile,
                             ProgressCallback progressCallback);
}
