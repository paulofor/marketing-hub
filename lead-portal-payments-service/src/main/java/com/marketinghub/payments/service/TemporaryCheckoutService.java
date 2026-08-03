package com.marketinghub.payments.service;

import com.marketinghub.payments.config.MercadoPagoProperties;
import com.marketinghub.payments.dto.TemporaryCheckoutRequest;
import com.marketinghub.payments.dto.TemporaryCheckoutResponse;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoClient;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceResponse;
import com.marketinghub.payments.model.TemporaryCheckout;
import com.marketinghub.payments.repository.TemporaryCheckoutRepository;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Cria, resolve e restaura checkouts de teste sem alterar a oferta comercial. */
@Service
public class TemporaryCheckoutService {
    private static final Logger log = LoggerFactory.getLogger(TemporaryCheckoutService.class);
    private static final int MAX_DURATION_MINUTES = 1440;

    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoProperties mercadoPagoProperties;
    private final TemporaryCheckoutRepository repository;
    private final String publicBaseUrl;

    /** Configura as integrações necessárias para administrar os checkouts temporários. */
    public TemporaryCheckoutService(
            MercadoPagoClient mercadoPagoClient,
            MercadoPagoProperties mercadoPagoProperties,
            TemporaryCheckoutRepository repository,
            @Value("${payments.public-base-url:https://pagamentopalf.site}") String publicBaseUrl) {
        this.mercadoPagoClient = mercadoPagoClient;
        this.mercadoPagoProperties = mercadoPagoProperties;
        this.repository = repository;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    /** Ativa uma preferência de teste com expiração e destino comercial preservado. */
    @Transactional
    public TemporaryCheckoutResponse activate(TemporaryCheckoutRequest request) {
        String productKey = normalizeKey(request.productKey());
        validateUrl(request.commercialCheckoutUrl());
        int duration = normalizeDuration(request.durationMinutes());
        MercadoPagoPreferenceRequest preference = new MercadoPagoPreferenceRequest(
                List.of(new MercadoPagoPreferenceRequest.Item(
                        request.productName() + " - Compra teste", 1, request.testAmount(), "BRL")),
                null,
                new MercadoPagoPreferenceRequest.BackUrls(
                        publicBaseUrl + "/agenda-cheia/obrigado.html",
                        mercadoPagoProperties.getFailureUrl(),
                        mercadoPagoProperties.getPendingUrl()),
                Map.of("productKey", productKey, "checkoutMode", "temporary_test"),
                mercadoPagoProperties.getNotificationUrl(),
                productKey,
                mercadoPagoProperties.getStatementDescriptor(),
                "approved");
        MercadoPagoPreferenceResponse created = mercadoPagoClient.createPreference(preference);
        if (created == null || !StringUtils.hasText(created.initPoint())) {
            throw new IllegalStateException("Mercado Pago não retornou o checkout temporário");
        }

        Instant now = Instant.now();
        TemporaryCheckout checkout = repository.findByProductKey(productKey).orElseGet(TemporaryCheckout::new);
        checkout.setProductKey(productKey);
        checkout.setProductName(request.productName().trim());
        checkout.setCommercialCheckoutUrl(request.commercialCheckoutUrl().trim());
        checkout.setTemporaryCheckoutUrl(created.initPoint());
        checkout.setMercadoPagoPreferenceId(created.id());
        checkout.setTestAmount(request.testAmount());
        checkout.setActivatedAt(now);
        checkout.setExpiresAt(now.plus(duration, ChronoUnit.MINUTES));
        checkout.setRestoredAt(null);
        TemporaryCheckout saved = repository.save(checkout);
        log.info("Checkout temporário ativado. productKey={}, preferenceId={}, amount={}, expiresAt={}",
                productKey, created.id(), request.testAmount(), saved.getExpiresAt());
        return toResponse(saved, now);
    }

    /** Retorna o estado atual e marca como restaurado quando a validade terminou. */
    @Transactional
    public TemporaryCheckoutResponse status(String productKey) {
        TemporaryCheckout checkout = load(productKey);
        Instant now = Instant.now();
        restoreIfExpired(checkout, now);
        return toResponse(checkout, now);
    }

    /** Restaura imediatamente o checkout comercial configurado. */
    @Transactional
    public TemporaryCheckoutResponse restore(String productKey) {
        TemporaryCheckout checkout = load(productKey);
        if (checkout.getRestoredAt() == null) {
            checkout.setRestoredAt(Instant.now());
            repository.save(checkout);
            log.info("Checkout comercial restaurado manualmente. productKey={}", checkout.getProductKey());
        }
        return toResponse(checkout, Instant.now());
    }

    /** Resolve o destino vigente sem expor a preferência na página de venda. */
    @Transactional
    public URI resolveDestination(String productKey) {
        TemporaryCheckout checkout = load(productKey);
        Instant now = Instant.now();
        restoreIfExpired(checkout, now);
        String destination = isActive(checkout, now)
                ? checkout.getTemporaryCheckoutUrl()
                : checkout.getCommercialCheckoutUrl();
        return URI.create(destination);
    }

    private TemporaryCheckout load(String productKey) {
        return repository.findByProductKey(normalizeKey(productKey))
                .orElseThrow(() -> new IllegalStateException("Produto não possui checkout temporário configurado"));
    }

    private void restoreIfExpired(TemporaryCheckout checkout, Instant now) {
        if (checkout.getRestoredAt() == null && !checkout.getExpiresAt().isAfter(now)) {
            checkout.setRestoredAt(now);
            repository.save(checkout);
            log.info("Checkout comercial restaurado automaticamente. productKey={}", checkout.getProductKey());
        }
    }

    private boolean isActive(TemporaryCheckout checkout, Instant now) {
        return checkout.getRestoredAt() == null && checkout.getExpiresAt().isAfter(now);
    }

    private TemporaryCheckoutResponse toResponse(TemporaryCheckout checkout, Instant now) {
        return new TemporaryCheckoutResponse(
                checkout.getProductKey(), checkout.getProductName(),
                publicBaseUrl + "/api/v1/payments/temporary/" + checkout.getProductKey() + "/redirect",
                checkout.getTemporaryCheckoutUrl(), checkout.getCommercialCheckoutUrl(), checkout.getTestAmount(),
                isActive(checkout, now) ? "ACTIVE" : "RESTORED", checkout.getActivatedAt(), checkout.getExpiresAt());
    }

    private String normalizeKey(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (!normalized.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("productKey deve ser um slug válido");
        }
        return normalized;
    }

    private int normalizeDuration(Integer value) {
        if (value == null || value < 5 || value > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("A duração deve ficar entre 5 e 1440 minutos");
        }
        return value;
    }

    private void validateUrl(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException("O checkout comercial deve usar HTTPS");
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Checkout comercial inválido recebido na ativação temporária. url={}", value, ex);
            throw new IllegalArgumentException("Checkout comercial inválido", ex);
        }
    }
}
