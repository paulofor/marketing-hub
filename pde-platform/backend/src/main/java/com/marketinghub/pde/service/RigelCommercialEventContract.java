package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.model.AccessGrant;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Responsabilidade: compor e validar a correlação comercial exata dos eventos produtivos do Rigel
 * sem persistir o bearer reutilizável.
 */
final class RigelCommercialEventContract {
    static final long EXPERIMENT_ID = 89L;
    static final BigDecimal AMOUNT_BRL = new BigDecimal("349.00");
    static final String CURRENCY = "BRL";
    private static final String PRODUCT_SLUG = RigelPaidEntitlementService.PRODUCT_SLUG;
    private static final String EXPERIENCE_VERSION = RigelPaidEntitlementService.EXPERIENCE_VERSION;
    private static final Set<String> CONTRACT_EVENTS = Set.of(
            "PURCHASE_COMPLETED",
            "ACCESS_RELEASED",
            "MISSION_COMPLETED",
            "DELIVERY_COMPLETED",
            "FIRST_USE",
            "JOURNEY_COMPLETED",
            "REFUND_CONFIRMED");

    /** Impede instanciação de um contrato puramente determinístico. */
    private RigelCommercialEventContract() {}

    /** Acrescenta identidade, experimento e referência irreversível aos marcos do acesso. */
    static Map<String, Object> enrichAccessMetadata(AccessGrant grant, Map<String, Object> source) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (source != null) {
            metadata.putAll(source);
        }
        if (!PRODUCT_SLUG.equals(grant.getProductSlug())) {
            return metadata;
        }
        metadata.remove("accessToken");
        metadata.put("productSlug", PRODUCT_SLUG);
        metadata.put("experimentId", EXPERIMENT_ID);
        metadata.put("experienceVersion", EXPERIENCE_VERSION);
        metadata.put("accessReferenceHash", accessReferenceHash(grant.getToken()));
        return metadata;
    }

    /** Valida antes da persistência todos os campos exigidos pelo contrato versionado do Rigel. */
    static void requireComplete(FunnelEventRequest request, String eventType) {
        if (!PRODUCT_SLUG.equals(request.productSlug()) || !CONTRACT_EVENTS.contains(eventType)) {
            return;
        }
        Map<String, Object> metadata = request.metadata();
        if (metadata == null || metadata.containsKey("accessToken")) {
            throw new IllegalArgumentException(
                    "Evento comercial do Rigel sem metadados seguros de correlação");
        }
        requireValue(metadata, "productSlug");
        requireValue(metadata, "experimentId");
        if (!PRODUCT_SLUG.equals(String.valueOf(metadata.get("productSlug")))
                || number(metadata.get("experimentId")) != EXPERIMENT_ID) {
            throw new IllegalArgumentException("Evento comercial do Rigel diverge do produto ou experimento");
        }
        switch (eventType) {
            case "PURCHASE_COMPLETED" -> requireFields(
                    metadata, "paymentId", "amountBrl", "currency", "approvedAt");
            case "ACCESS_RELEASED" -> requireFields(
                    metadata, "paymentId", "accessReferenceHash", "releasedAt");
            case "MISSION_COMPLETED" -> requireFields(
                    metadata,
                    "accessReferenceHash",
                    "experienceVersion",
                    "missionId",
                    "completionRole",
                    "completedAt");
            case "DELIVERY_COMPLETED" -> requireFields(
                    metadata,
                    "accessReferenceHash",
                    "experienceVersion",
                    "missionId",
                    "deliveryVersion",
                    "completedAt");
            case "FIRST_USE" -> requireFields(
                    metadata,
                    "accessReferenceHash",
                    "experienceVersion",
                    "missionId",
                    "applicationStatus",
                    "occurredAt");
            case "JOURNEY_COMPLETED" -> requireFields(
                    metadata,
                    "accessReferenceHash",
                    "experienceVersion",
                    "missionId",
                    "completedMissions",
                    "completedAt");
            case "REFUND_CONFIRMED" -> requireFields(
                    metadata,
                    "paymentId",
                    "amountBrl",
                    "providerStatus",
                    "confirmedAt",
                    "reason");
            default -> throw new IllegalStateException("Evento comercial do Rigel sem contrato conhecido");
        }
        requireCanonicalValue(metadata, eventType);
        if (request.accessToken() != null && !request.accessToken().isBlank()) {
            String informedReference = String.valueOf(metadata.get("accessReferenceHash"));
            if (!"PURCHASE_COMPLETED".equals(eventType)
                    && !"REFUND_CONFIRMED".equals(eventType)
                    && !accessReferenceHash(request.accessToken()).equals(informedReference)) {
                throw new IllegalArgumentException("Evento comercial do Rigel não corresponde ao acesso informado");
            }
        }
    }

    /** Converte o bearer em referência estável não reutilizável para armazenamento e conciliação. */
    static String persistedAccessReference(String accessToken) {
        return accessToken == null || accessToken.isBlank() ? null : accessReferenceHash(accessToken);
    }

    /** Calcula a mesma referência irreversível usada pelo entitlement financeiro. */
    static String accessReferenceHash(String accessToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(accessToken.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Não foi possível proteger a correlação do acesso", ex);
        }
    }

    /** Exige um conjunto de campos sem aceitar aliases silenciosos. */
    private static void requireFields(Map<String, Object> metadata, String... fields) {
        for (String field : fields) {
            requireValue(metadata, field);
        }
    }

    /** Exige valor não vazio para o nome canônico informado. */
    private static void requireValue(Map<String, Object> metadata, String field) {
        Object value = metadata.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Evento comercial do Rigel sem campo obrigatório: " + field);
        }
    }

    /** Confere os valores comerciais congelados sem depender apenas da presença dos campos. */
    private static void requireCanonicalValue(Map<String, Object> metadata, String eventType) {
        if (metadata.containsKey("experienceVersion")
                && !EXPERIENCE_VERSION.equals(String.valueOf(metadata.get("experienceVersion")))) {
            throw new IllegalArgumentException("Evento comercial do Rigel diverge da versão vigente");
        }
        if (Set.of("PURCHASE_COMPLETED", "REFUND_CONFIRMED").contains(eventType)) {
            BigDecimal amount = new BigDecimal(String.valueOf(metadata.get("amountBrl")));
            if (amount.compareTo(AMOUNT_BRL) != 0
                    || !CURRENCY.equals(String.valueOf(metadata.getOrDefault("currency", CURRENCY)))) {
                throw new IllegalArgumentException("Evento comercial do Rigel diverge do valor ou moeda");
            }
        }
    }

    /** Converte número JSON em inteiro sem aceitar texto inválido. */
    private static long number(Object value) {
        try {
            return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Evento comercial do Rigel possui correlação numérica inválida", ex);
        }
    }
}
