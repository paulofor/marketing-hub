package com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

/** Calcula custo auditável das interações OpenAI recebidas pelas etapas OPRM NichoCNAE v2. */
public final class OpenAiInteractionCostCalculator {
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);
    private static final Map<String, ModelPrice> OFFICIAL_PRICES = Map.of(
            "gpt-5.2", new ModelPrice("1.75", "14.00"),
            "gpt-5", new ModelPrice("1.25", "10.00"),
            "gpt-5-mini", new ModelPrice("0.25", "2.00"),
            "gpt-5-nano", new ModelPrice("0.05", "0.40"),
            "gpt-4o", new ModelPrice("2.50", "10.00"),
            "gpt-4o-mini", new ModelPrice("0.15", "0.60"));

    private OpenAiInteractionCostCalculator() {}

    /** Resolve o custo a partir do modelo e dos tokens; usa custo legado só quando o modelo não tem preço conhecido. */
    public static BigDecimal resolveCostUsd(OpenAiInteractionAuditRequest request) {
        if (request == null) {
            return null;
        }
        ModelPrice price = OFFICIAL_PRICES.get(normalizeModel(request.model()));
        if (price == null) {
            return request.costUsd();
        }
        return tokenCost(price.inputPrice(), request.inputTokens())
                .add(tokenCost(price.outputPrice(), request.outputTokens()))
                .setScale(4, RoundingMode.HALF_UP);
    }

    /** Normaliza variantes datadas para o código base usado no catálogo oficial de preço. */
    private static String normalizeModel(String model) {
        if (model == null) {
            return null;
        }
        String normalized = model.trim().toLowerCase(Locale.ROOT);
        for (String code : OFFICIAL_PRICES.keySet()) {
            if (normalized.equals(code) || normalized.startsWith(code + "-20")) {
                return code;
            }
        }
        return normalized;
    }

    /** Calcula o custo proporcional ao preço por um milhão de tokens. */
    private static BigDecimal tokenCost(BigDecimal pricePerMillion, Integer tokens) {
        if (pricePerMillion == null || tokens == null || tokens <= 0) {
            return BigDecimal.ZERO;
        }
        return pricePerMillion.multiply(BigDecimal.valueOf(tokens)).divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
    }

    /** Representa preço oficial de entrada e saída em USD por um milhão de tokens. */
    private record ModelPrice(BigDecimal inputPrice, BigDecimal outputPrice) {
        /** Constrói preço a partir de strings para preservar escala decimal exata. */
        private ModelPrice(String inputPrice, String outputPrice) {
            this(new BigDecimal(inputPrice), new BigDecimal(outputPrice));
        }
    }
}
