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

/** Publica no backend PDE estados financeiros autoritativos dos produtos comerciais suportados. */
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

    /** Encaminha aprovação ou reembolso e falha o webhook quando o entitlement não persiste. */
    public void notifyIfSupported(MercadoPagoPaymentDetails payment) {
        if (payment == null || !supports(payment)) {
            return;
        }
        if (paidVisual(payment) && !livePayment(payment)) {
            log.info("Pagamento visual de teste ou sem ambiente confirmado não libera entrega comercial; paymentId={}", payment.id());
            return;
        }
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Publicação de entitlement PDE está desabilitada");
        }
        String token=paidVisual(payment) ? properties.getMainBackendToken() : properties.getInternalToken();
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("Token interno do entitlement PDE não configurado");
        }
        URI uri = paidVisual(payment) ? URI.create(properties.getMainBackendBaseUrl()+"/api/pde/paid-visual/v1/internal/payments") : endpoint();
        var metadata = new java.util.HashMap<String,Object>(payment.metadata()==null ? java.util.Map.of() : payment.metadata());
        if (paidVisual(payment)) metadata.put("providerLiveMode", true);
        PdePaymentEntitlementNotification notification = new PdePaymentEntitlementNotification(
                payment.id(),
                payment.status(),
                payment.amount(),
                payment.currency(),
                payment.email(),
                payment.externalReference(),
                payment.dateApproved(),
                metadata);
        log.info(
                "Enviando estado financeiro ao PDE; paymentId={}, productSlug={}, status={}, endpoint={}",
                payment.id(),
                payment.externalReference(),
                payment.status(),
                uri);
        try {
            var response = restClient.post()
                    .uri(uri)
                    .header(paidVisual(payment) ? "X-PDE-Internal-Token" : HttpHeaders.AUTHORIZATION,
                            paidVisual(payment) ? token : "Bearer " + token)
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();
            log.info(
                    "Estado financeiro persistido no PDE; paymentId={}, productSlug={}, status={}, endpoint={}, httpStatus={}",
                    payment.id(),
                    payment.externalReference(),
                    payment.status(),
                    uri,
                    response.getStatusCode());
        } catch (Exception ex) {
            log.error(
                    "Falha ao persistir entitlement PDE; paymentId={}, productSlug={}, status={}, endpoint={}",
                    payment.id(),
                    payment.externalReference(),
                    payment.status(),
                    uri,
                    ex);
            throw new IllegalStateException("Não foi possível persistir o entitlement pago do PDE", ex);
        }
    }

    /** Reconhece somente o produto e os estados finais que alteram entitlement. */
    private boolean supports(MercadoPagoPaymentDetails payment) {
        String status = normalize(payment.status());
        if (!SUPPORTED_STATUSES.contains(status) || !StringUtils.hasText(payment.externalReference())) {
            return false;
        }
        String externalReference = normalize(payment.externalReference());
        boolean legacyProduct = StringUtils.hasText(properties.getProductSlug())
                && normalize(properties.getProductSlug()).equals(externalReference);
        boolean configuredProduct = properties.getProductSlugs().stream()
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .anyMatch(externalReference::equals);
        return legacyProduct || configuredProduct || paidVisual(payment);
    }

    /** Confirma o ambiente no recibo consultado no provedor, ignorando metadata escolhida pelo cliente. */
    private boolean livePayment(MercadoPagoPaymentDetails payment) {
        try {
            var value = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payment.rawPayload()).path("live_mode");
            return value.isBoolean() && value.asBoolean();
        } catch (Exception ex) {
            log.error("Ambiente do pagamento visual não pôde ser conferido paymentId={}", payment.id(), ex);
            throw new IllegalStateException("Não foi possível confirmar o ambiente financeiro",ex);
        }
    }

    /** Identifica a capacidade contratada, sem exceção por nome ou identificador de produto. */
    private boolean paidVisual(MercadoPagoPaymentDetails payment) {
        return payment.metadata()!=null && "PDE_PAID_VISUAL_V1".equals(payment.metadata().get("deliveryContract"));
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
