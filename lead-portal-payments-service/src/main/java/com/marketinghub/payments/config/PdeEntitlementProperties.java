package com.marketinghub.payments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configura a publicação de pagamentos do Kit para a fonte de entitlement do PDE. */
@Component
@ConfigurationProperties(prefix = "pde.entitlement")
public class PdeEntitlementProperties {
    private boolean enabled = true;
    private String backendBaseUrl = "http://pde-platform-backend:8096";
    private String notificationPath = "/api/internal/pde/mercado-pago/entitlements";
    private String internalToken;
    private String productSlug = "kit-whatsapp-pronto";

    /** Informa se a publicação do estado financeiro está ativa. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Ativa ou desativa a publicação do estado financeiro. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Retorna a URL base do backend PDE. */
    public String getBackendBaseUrl() {
        return backendBaseUrl;
    }

    /** Define a URL base do backend PDE. */
    public void setBackendBaseUrl(String backendBaseUrl) {
        this.backendBaseUrl = backendBaseUrl;
    }

    /** Retorna o caminho interno do entitlement. */
    public String getNotificationPath() {
        return notificationPath;
    }

    /** Define o caminho interno do entitlement. */
    public void setNotificationPath(String notificationPath) {
        this.notificationPath = notificationPath;
    }

    /** Retorna o segredo interno sem registrá-lo em logs. */
    public String getInternalToken() {
        return internalToken;
    }

    /** Define o segredo interno compartilhado. */
    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    /** Retorna o produto cujo pagamento deve gerar entitlement. */
    public String getProductSlug() {
        return productSlug;
    }

    /** Define o produto cujo pagamento deve gerar entitlement. */
    public void setProductSlug(String productSlug) {
        this.productSlug = productSlug;
    }
}
