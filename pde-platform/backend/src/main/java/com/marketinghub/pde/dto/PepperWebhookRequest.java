package com.marketinghub.pde.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Contrato do webhook Pepper usado para liberar acesso após compra aprovada. */
public record PepperWebhookRequest(
        String productSlug,
        String buyerEmail,
        String transactionId,
        String status,
        PepperCustomer customer,
        PepperTransaction transaction,
        PepperOffer offer,
        List<PepperItem> items,
        PepperTracking tracking
) {

    /** Resolve o produto PDE usando o payload legado, rastreamento ou o padrão do Clube MUSA. */
    public String resolvedProductSlug(String defaultProductSlug) {
        if (hasText(productSlug)) {
            return productSlug.trim();
        }
        if (tracking != null && hasText(tracking.productSlug())) {
            return tracking.productSlug().trim();
        }
        return defaultProductSlug;
    }

    /** Resolve o e-mail da compradora no payload legado ou no payload v2 da Pepper. */
    public String resolvedBuyerEmail() {
        if (hasText(buyerEmail)) {
            return buyerEmail.trim();
        }
        return customer != null ? customer.email() : null;
    }

    /** Resolve o identificador da transação no payload legado ou no payload v2 da Pepper. */
    public String resolvedTransactionId() {
        if (hasText(transactionId)) {
            return transactionId.trim();
        }
        return transaction != null ? transaction.id() : null;
    }

    /** Resolve o status de pagamento no payload legado, raiz v2 ou subestrutura transaction. */
    public String resolvedStatus() {
        if (hasText(status)) {
            return status.trim();
        }
        return transaction != null ? transaction.status() : null;
    }

    /** Identifica texto preenchido. */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Dados da cliente enviados pela Pepper. */
    public record PepperCustomer(String id, String name, String email, String phone, String document) {}

    /** Dados financeiros da transação enviados pela Pepper. */
    public record PepperTransaction(String id, String status, String method, String amount, String url) {}

    /** Dados da oferta vendida na Pepper. */
    public record PepperOffer(String hash, String title, String price) {}

    /** Dados dos itens vendidos na transação Pepper. */
    public record PepperItem(
            String hash,
            @JsonProperty("product_hash") String productHash,
            @JsonProperty("offer_hash") String offerHash,
            String title,
            String price,
            Integer quantity) {}

    /** Dados de rastreamento comercial usados para correlacionar o produto PDE. */
    public record PepperTracking(
            String src,
            @JsonProperty("utm_source") String utmSource,
            @JsonProperty("utm_campaign") String utmCampaign,
            @JsonProperty("utm_medium") String utmMedium,
            @JsonProperty("utm_term") String utmTerm,
            @JsonProperty("utm_content") String utmContent,
            @JsonProperty("product_slug") String productSlug) {}
}
