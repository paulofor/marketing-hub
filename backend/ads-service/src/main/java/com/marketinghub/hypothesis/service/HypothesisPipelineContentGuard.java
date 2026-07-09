package com.marketinghub.hypothesis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: validar e materializar textos comerciais seguros do pipeline de hipótese. */
@Component
public class HypothesisPipelineContentGuard {
    private static final Logger log = LoggerFactory.getLogger(HypothesisPipelineContentGuard.class);
    private static final Pattern REPLACEMENT_OR_CONTROL_CHAR = Pattern.compile("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\uFFFD]");
    private static final Pattern UNEXPECTED_CJK_CHAR = Pattern.compile("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}]");
    private static final int MAX_JSON_UNWRAP_DEPTH = 3;
    private static final Map<String, List<String>> STAGE_TEXT_PRIORITIES = Map.of(
            "hypothesis-pain", List.of("summary", "root", "surface"),
            "hypothesis-result", List.of("summary", "desiredOutcome", "desiredResult", "businessOutcome"),
            "hypothesis-mechanism", List.of("summary", "coreMechanism", "core", "mechanismName"),
            "hypothesis-proof", List.of("summary", "proofMessage", "message", "proofAsset"),
            "hypothesis-offer", List.of("summary", "coreOffer", "offerName", "promise", "deliverables"));

    private final ObjectMapper objectMapper;

    /** Inicializa o validador com o parser JSON usado para desempacotar respostas estruturadas. */
    public HypothesisPipelineContentGuard(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida uma resposta bruta de etapa antes de ela ser marcada como concluída. */
    public void validateStageResponse(String stageCode, String response) {
        if (!StringUtils.hasText(response)) {
            throw new IllegalStateException("Resposta vazia da IA para a etapa " + stageCode + ".");
        }
        String materialized = materializeStageText(stageCode, response);
        validateCommercialText(stageCode, materialized);
    }

    /** Converte a resposta da etapa em texto comercial final, sem JSON serializado dentro de campo legado. */
    public String materializeStageText(String stageCode, String response) {
        String candidate = unwrapJsonText(stageCode, response, 0);
        validateCommercialText(stageCode, candidate);
        return candidate.trim();
    }

    /** Garante que o texto final não carrega sinais de corrupção, truncamento ou payload técnico. */
    public void validateCommercialText(String stageCode, String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("Texto comercial vazio na etapa " + stageCode + ".");
        }
        String normalized = text.trim();
        if (REPLACEMENT_OR_CONTROL_CHAR.matcher(normalized).find() || UNEXPECTED_CJK_CHAR.matcher(normalized).find()) {
            throw new IllegalStateException(
                    "Resposta da etapa " + stageCode + " contém caractere corrompido ou inesperado e não pode virar hipótese.");
        }
        if (looksLikeJson(normalized)) {
            throw new IllegalStateException(
                    "Resposta da etapa " + stageCode + " ainda está em JSON e precisa ser materializada em texto comercial.");
        }
    }

    /** Desembrulha JSON direto ou JSON serializado em string até encontrar o melhor texto comercial da etapa. */
    private String unwrapJsonText(String stageCode, String response, int depth) {
        String normalized = response == null ? null : response.trim();
        if (!StringUtils.hasText(normalized) || depth >= MAX_JSON_UNWRAP_DEPTH || !looksLikeJson(normalized)) {
            return normalized;
        }
        try {
            JsonNode node = objectMapper.readTree(normalized);
            if (node.isTextual()) {
                return unwrapJsonText(stageCode, node.asText(), depth + 1);
            }
            if (node.isObject()) {
                String stageText = firstTextByPriority(stageCode, node);
                if (StringUtils.hasText(stageText)) {
                    return unwrapJsonText(stageCode, stageText, depth + 1);
                }
            }
            return normalized;
        } catch (JsonProcessingException | RuntimeException ex) {
            log.warn(
                    "Falha ao desempacotar resposta JSON do pipeline de hipótese (stageCode={}, depth={}, responseLength={})",
                    stageCode,
                    depth,
                    normalized.length(),
                    ex);
            return normalized;
        }
    }

    /** Seleciona o primeiro campo textual canônico da etapa, priorizando resumo e campos funcionais. */
    private String firstTextByPriority(String stageCode, JsonNode node) {
        for (String field : STAGE_TEXT_PRIORITIES.getOrDefault(stageCode, List.of("summary"))) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        for (JsonNode value : node) {
            if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return null;
    }

    /** Identifica payloads JSON que não devem ser persistidos como texto comercial final. */
    private boolean looksLikeJson(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
}
