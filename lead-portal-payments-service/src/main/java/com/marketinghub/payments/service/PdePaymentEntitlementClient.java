package com.marketinghub.payments.service;

import com.marketinghub.payments.config.PdeEntitlementProperties;
import com.marketinghub.payments.dto.PdePaymentEntitlementNotification;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Publica no backend PDE somente estados financeiros autoritativos do Kit WhatsApp Pronto. */
@Service
public class PdePaymentEntitlementClient {
    private static final Logger log = LoggerFactory.getLogger(PdePaymentEntitlementClient.class);
    private static final Set<String> SUPPORTED_STATUSES = Set.of("approved", "refunded", "charged_back");

    private final PdeEntitlementProperties properties;
    private final RestClient restClient;

    /** Inicializa um cliente isolado para não reutilizar a credencial do Mercado Pago. */
    @Autowired
    public PdePaymentEntitlementClient(PdeEntitlementProperties properties) {
        this(properties, RestClient.builder().build());
    }

    /** Permite injetar um cliente HTTP controlado nos testes de contrato. */
    PdePaymentEntitlementClient(PdeEntitlementProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    /** Encaminha aprovação ou reembolso do Kit e falha o webhook quando o entitlement não persiste. */
    public void notifyIfSupported(MercadoPagoPaymentDetails payment) {
        if (payment == null || !supports(payment)) {
            return;
        }
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Publicação de entitlement do Kit está desabilitada");
        }
        if (!StringUtils.hasText(properties.getInternalToken())) {
            throw new IllegalStateException("Token interno do entitlement PDE não configurado");
        }
        URI uri = endpoint();
        PdePaymentEntitlementNotification notification = new PdePaymentEntitlementNotification(
                payment.id(),
                payment.status(),
                payment.amount(),
                payment.currency(),
                payment.email(),
                payment.externalReference(),
                payment.dateApproved(),
                payment.metadata());
        log.info(
                "Enviando estado financeiro do Kit ao PDE; paymentId={}, status={}, endpoint={}",
                payment.id(),
                payment.status(),
                uri);
        try {
            var response = restClient.post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getInternalToken())
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();
            log.info(
                    "Estado financeiro do Kit persistido no PDE; paymentId={}, status={}, endpoint={}, httpStatus={}",
                    payment.id(),
                    payment.status(),
                    uri,
                    response.getStatusCode());
        } catch (Exception ex) {
            log.error(
                    "Falha ao persistir entitlement do Kit; paymentId={}, status={}, endpoint={}",
                    payment.id(),
                    payment.status(),
                    uri,
                    ex);
            throw new IllegalStateException("Não foi possível persistir o entitlement pago do Kit", ex);
        }
    }

    /** Reconhece somente o produto e os estados finais que alteram entitlement. */
    private boolean supports(MercadoPagoPaymentDetails payment) {
        String status = normalize(payment.status());
        return StringUtils.hasText(properties.getProductSlug())
                && properties.getProductSlug().equalsIgnoreCase(payment.externalReference())
                && SUPPORTED_STATUSES.contains(status);
    }

    /** Monta e valida a URL absoluta usada na integração interna. */
    private URI endpoint() {
        if (!StringUtils.hasText(properties.getBackendBaseUrl())
                || !StringUtils.hasText(properties.getNotificationPath())) {
            throw new IllegalStateException("Endpoint do entitlement PDE não configurado");
        }
        return URI.create(properties.getBackendBaseUrl() + properties.getNotificationPath());
    }

    /** Normaliza o status do provedor sem aceitar valor ausente. */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
