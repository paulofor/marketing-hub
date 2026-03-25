package com.marketinghub.videomanagement.service.provider;

import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;

/**
 * Contrato para integrações com providers de vídeo.
 */
public interface VideoProvider {

    boolean supports(SalesVideoJob job);

    ProviderArtifacts render(SalesVideoJob job,
                             SalesVideoProfile profile,
                             ProgressCallback progressCallback);
}
