package com.marketinghub.nichocnae.meiaudiencesegmenter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Fornece o schema JSON estrito usado pela IA na segmentação comportamental MEI/autônomo. */
@Component
public class MeiAudienceSegmenterSchema {

    /** Monta o schema aceito pela OpenAI para o perfil comportamental sem produto, oferta ou campanha. */
    public Map<String, Object> buildSchema() {
        Map<String, Object> schema = object(
                "audienceName",
                "occupationTerms",
                "workMode",
                "customerAcquisitionBehavior",
                "dailyRoutineSummary",
                "recurringTasksSummary",
                "operationalPainsSummary",
                "emotionalPainsSummary",
                "dreamsSummary",
                "fearsSummary",
                "languagePatterns",
                "channelsUsed",
                "recentSourceSummary",
                "autonomousProfessionalFitScore",
                "behavioralEvidenceScore",
                "sourceFreshnessScore",
                "outdatedSourceRiskScore",
                "structuredBusinessDriftRiskScore",
                "solutionLanguageRiskScore");
        schema.put("properties", Map.ofEntries(
                Map.entry("audienceName", string()),
                Map.entry("occupationTerms", string()),
                Map.entry("workMode", string()),
                Map.entry("customerAcquisitionBehavior", string()),
                Map.entry("dailyRoutineSummary", string()),
                Map.entry("recurringTasksSummary", string()),
                Map.entry("operationalPainsSummary", string()),
                Map.entry("emotionalPainsSummary", string()),
                Map.entry("dreamsSummary", string()),
                Map.entry("fearsSummary", string()),
                Map.entry("languagePatterns", string()),
                Map.entry("channelsUsed", string()),
                Map.entry("recentSourceSummary", string()),
                Map.entry("autonomousProfessionalFitScore", integer()),
                Map.entry("behavioralEvidenceScore", integer()),
                Map.entry("sourceFreshnessScore", integer()),
                Map.entry("outdatedSourceRiskScore", integer()),
                Map.entry("structuredBusinessDriftRiskScore", integer()),
                Map.entry("solutionLanguageRiskScore", integer())));
        return schema;
    }

    /** Cria objeto JSON Schema com propriedades adicionais bloqueadas. */
    private Map<String, Object> object(String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of(required));
        return schema;
    }

    /** Cria campo textual simples. */
    private Map<String, Object> string() {
        return Map.of("type", "string");
    }

    /** Cria campo inteiro percentual simples. */
    private Map<String, Object> integer() {
        return Map.of("type", "integer", "minimum", 0, "maximum", 100);
    }
}
