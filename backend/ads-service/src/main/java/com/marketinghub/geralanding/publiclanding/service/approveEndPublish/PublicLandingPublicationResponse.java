package com.marketinghub.geralanding.publiclanding.service.approveEndPublish;

/** Resposta da aprovação e publicação da landing pública do GeraLanding. */
public record PublicLandingPublicationResponse(
        Long experimentId,
        Long flowId,
        String iframeUrl,
        String standaloneUrl,
        String message) {
}
