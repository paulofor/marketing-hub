package com.marketinghub.leadportal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configura o endpoint público usado para encaminhar o lead até o checkout.
 */
@Component
@ConfigurationProperties(prefix = "lead-portal.payment-link")
@Getter
@Setter
public class LeadPortalPaymentLinkProperties {

    /**
     * URL base do entrypoint público (ex.: https://pagamentopalf.site/checkout).
     */
    private String entrypointBaseUrl;

    /**
     * Nome do parâmetro de query que receberá o ID do pacote.
     */
    private String packageIdQueryParam = "packageId";

    /**
     * Nome do parâmetro de query que receberá o ID da purchase.
     */
    private String purchaseIdQueryParam = "purchaseId";
}
