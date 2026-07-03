package com.marketinghub.payments.service;

import com.marketinghub.payments.config.ProductAiDeliveryProperties;
import com.marketinghub.payments.model.LeadPortalPurchase;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Responsabilidade: notificar o backend principal sobre compras aprovadas de Produto IA. */
@Service
public class ProductAiPaidDeliveryBackendClient {
    private static final Logger log = LoggerFactory.getLogger(ProductAiPaidDeliveryBackendClient.class);

    private final RestClient restClient;
    private final ProductAiDeliveryProperties properties;

    /** Inicializa o cliente com RestClient e propriedades de endpoint. */
    public ProductAiPaidDeliveryBackendClient(RestClient restClient, ProductAiDeliveryProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /** Envia compra aprovada ao backend para enfileirar entrega paga quando aplicável. */
    public void notifyApprovedPurchase(LeadPortalPurchase purchase) {
        if (!properties.isEnabled() || purchase == null || purchase.getId() == null || purchase.getPackageId() == null) {
            return;
        }
        URI uri = URI.create(properties.getBackendBaseUrl() + properties.getApprovedPurchasePath());
        try {
            restClient.post()
                    .uri(uri)
                    .body(Map.of("purchaseId", purchase.getId(), "packageId", purchase.getPackageId()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error(
                    "Falha ao notificar entrega Produto IA purchaseId={} packageId={} endpoint={}",
                    purchase.getId(),
                    purchase.getPackageId(),
                    uri,
                    ex);
        }
    }
}
