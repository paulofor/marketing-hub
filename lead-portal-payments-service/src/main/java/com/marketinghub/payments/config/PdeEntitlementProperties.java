package com.marketinghub.payments.config;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configura a publicação de pagamentos dos produtos comerciais para o entitlement do PDE. */
@Component
@ConfigurationProperties(prefix = "pde.entitlement")
public class PdeEntitlementProperties {
    private boolean enabled = true;
    private String mainBackendToken;
    /** Retorna a credencial da integração com o backend principal. */
    public String getMainBackendToken() { return mainBackendToken; }
    /** Define a credencial financeira sem reutilizar uma conexão pública. */
    public void setMainBackendToken(String value) { mainBackendToken=value; }
    private String mainBackendBaseUrl="http://191.252.181.168";
    /** Retorna a origem principal para direitos de entregas visuais pagas. */
    public String getMainBackendBaseUrl() { return mainBackendBaseUrl; }
    /** Configura a origem principal sem alterar integrações legadas. */
    public void setMainBackendBaseUrl(String value) { mainBackendBaseUrl=value; }
    private String backendBaseUrl = "http://pde-platform-backend:8096";
    private String notificationPath = "/api/internal/pde/mercado-pago/entitlements";
    private String internalToken;
    private String productSlug = "kit-whatsapp-pronto";
    private Set<String> productSlugs =
            new LinkedHashSet<>(Set.of("kit-whatsapp-pronto", "pde-planejado-36"));

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

    /** Retorna todos os produtos comerciais aceitos pelo publicador de entitlement. */
    public Set<String> getProductSlugs() {
        return productSlugs;
    }

    /** Define a lista explícita de produtos comerciais aceitos. */
    public void setProductSlugs(Set<String> productSlugs) {
        this.productSlugs = productSlugs == null ? new LinkedHashSet<>() : new LinkedHashSet<>(productSlugs);
    }
}
