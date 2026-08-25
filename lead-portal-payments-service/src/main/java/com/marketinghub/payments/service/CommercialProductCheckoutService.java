package com.marketinghub.payments.service;

import com.marketinghub.payments.config.MercadoPagoProperties;
import com.marketinghub.payments.dto.CommercialProductCheckoutRequest;
import com.marketinghub.payments.dto.CommercialProductCheckoutResponse;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoClient;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Cria checkout comercial de produto usando o contrato previamente validado pelo backend. */
@Service
public class CommercialProductCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CommercialProductCheckoutService.class);
    private static final Pattern PRODUCT_KEY = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoProperties mercadoPagoProperties;

    /** Inicializa o serviço com o cliente oficial e as URLs de retorno do Mercado Pago. */
    public CommercialProductCheckoutService(
            MercadoPagoClient mercadoPagoClient, MercadoPagoProperties mercadoPagoProperties) {
        this.mercadoPagoClient = mercadoPagoClient;
        this.mercadoPagoProperties = mercadoPagoProperties;
    }

    /** Cria uma preferência comercial sem aceitar preço ou entrega vindos do navegador público. */
    public CommercialProductCheckoutResponse create(CommercialProductCheckoutRequest request) {
        String productKey = normalizeProductKey(request.productKey());
        String productName = normalizeRequired(request.productName(), "productName obrigatório");
        String deliveryPageUrl = validateHttpsUrl(request.deliveryPageUrl());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productKey", productKey);
        metadata.put("productName", productName);
        metadata.put("productId", request.productId());
        metadata.put("experimentId", request.experimentId());
        metadata.put("delivery_url", deliveryPageUrl);

        MercadoPagoPreferenceRequest preference =
                new MercadoPagoPreferenceRequest(
                        List.of(new MercadoPagoPreferenceRequest.Item(
                                productName, 1, request.amount(), "BRL")),
                        null,
                        new MercadoPagoPreferenceRequest.BackUrls(
                                deliveryPageUrl,
                                mercadoPagoProperties.getFailureUrl(),
                                mercadoPagoProperties.getPendingUrl()),
                        metadata,
                        mercadoPagoProperties.getNotificationUrl(),
                        productKey,
                        mercadoPagoProperties.getStatementDescriptor(),
                        "approved");
        log.info(
                "Solicitando checkout comercial. productKey={}, productId={}, experimentId={}, amount={}, deliveryPageUrl={}",
                productKey,
                request.productId(),
                request.experimentId(),
                request.amount(),
                deliveryPageUrl);
        String idempotencyKey = commercialPreferenceIdempotencyKey(
                request, productKey, productName, deliveryPageUrl);
        MercadoPagoPreferenceResponse response =
                mercadoPagoClient.createPreference(preference, idempotencyKey);
        if (response == null || !StringUtils.hasText(response.initPoint())) {
            throw new IllegalStateException("Mercado Pago não retornou checkout comercial");
        }
        return new CommercialProductCheckoutResponse(
                productKey,
                request.productId(),
                request.experimentId(),
                response.id(),
                response.initPoint(),
                request.amount(),
                "BRL",
                deliveryPageUrl);
    }

    /** Gera chave estável por contrato para reutilizar retries sem preservar preço ou entrega antigos. */
    private String commercialPreferenceIdempotencyKey(
            CommercialProductCheckoutRequest request,
            String productKey,
            String productName,
            String deliveryPageUrl) {
        String normalizedAmount = request.amount().stripTrailingZeros().toPlainString();
        return UUID.nameUUIDFromBytes(
                        ("marketing-hub:commercial-checkout:experiment:" + request.experimentId()
                                + ":product:" + request.productId()
                                + ":key:" + productKey
                                + ":name:" + productName
                                + ":amount:" + normalizedAmount
                                + ":currency:BRL"
                                + ":delivery:" + deliveryPageUrl)
                                .getBytes(StandardCharsets.UTF_8))
                .toString();
    }

    /** Normaliza e valida a chave pública usada na atribuição e na entrega. */
    private String normalizeProductKey(String value) {
        String normalized = normalizeRequired(value, "productKey obrigatório").toLowerCase(Locale.ROOT);
        if (!PRODUCT_KEY.matcher(normalized).matches()) {
            throw new IllegalArgumentException("productKey deve ser um slug válido");
        }
        return normalized;
    }

    /** Exige texto não vazio no contrato interno. */
    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /** Bloqueia retorno ou entrega fora de HTTPS. */
    private String validateHttpsUrl(String value) {
        String normalized = normalizeRequired(value, "deliveryPageUrl obrigatória");
        URI uri = URI.create(normalized);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
            throw new IllegalArgumentException("deliveryPageUrl deve usar HTTPS");
        }
        return normalized;
    }
}
