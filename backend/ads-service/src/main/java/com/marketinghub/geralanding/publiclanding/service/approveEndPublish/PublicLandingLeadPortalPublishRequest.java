package com.marketinghub.geralanding.publiclanding.service.approveEndPublish;

/** Payload enviado ao Lead Portal para publicar o HTML final da landing pública. */
public record PublicLandingLeadPortalPublishRequest(
        String slug,
        String name,
        String description,
        String customFormHtml) {
}
