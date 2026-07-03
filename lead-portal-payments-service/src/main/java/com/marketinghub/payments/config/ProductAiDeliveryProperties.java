package com.marketinghub.payments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Responsabilidade: configurar a notificação de entrega paga de Produto IA ao backend. */
@Component
@ConfigurationProperties(prefix = "product-ai.delivery")
public class ProductAiDeliveryProperties {
    private boolean enabled = true;
    private String backendBaseUrl = "http://191.252.181.168";
    private String approvedPurchasePath =
            "/api/internal/product-ai/personalizedsample/v1/paid-delivery/stage-executions/approved-purchase";

    /** Indica se a notificação ao backend está ativa. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Define se a notificação ao backend está ativa. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Retorna a URL base do backend principal. */
    public String getBackendBaseUrl() {
        return backendBaseUrl;
    }

    /** Define a URL base do backend principal. */
    public void setBackendBaseUrl(String backendBaseUrl) {
        this.backendBaseUrl = backendBaseUrl;
    }

    /** Retorna o caminho do endpoint de compra aprovada. */
    public String getApprovedPurchasePath() {
        return approvedPurchasePath;
    }

    /** Define o caminho do endpoint de compra aprovada. */
    public void setApprovedPurchasePath(String approvedPurchasePath) {
        this.approvedPurchasePath = approvedPurchasePath;
    }
}
