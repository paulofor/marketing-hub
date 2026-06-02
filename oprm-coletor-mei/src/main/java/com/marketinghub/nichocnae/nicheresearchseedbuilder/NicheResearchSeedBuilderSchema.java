package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Fornece o schema JSON estrito usado pela Responses API para a saída da etapa dois. */
@Component
public class NicheResearchSeedBuilderSchema {

    /** Monta o schema aceito pela OpenAI para seed do nicho e queries de pesquisa auditáveis. */
    public Map<String, Object> buildSchema() {
        Map<String, Object> schema = object("researchCycleId", "seed", "queries");
        schema.put("properties", Map.of(
                "researchCycleId", integer(),
                "seed", seedSchema(),
                "queries", array(querySchema())));
        return schema;
    }

    /** Monta o schema do artefato que identifica o nicho operacional. */
    private Map<String, Object> seedSchema() {
        Map<String, Object> schema = object(
                "researchCycleId",
                "cnaeCode",
                "cnaeDescription",
                "nicheName",
                "businessType",
                "operationType",
                "customerType",
                "commercialObjects",
                "initialAssumptions",
                "confidenceLevel",
                "createdBy");
        schema.put("properties", Map.ofEntries(
                Map.entry("researchCycleId", integer()),
                Map.entry("cnaeCode", string()),
                Map.entry("cnaeDescription", string()),
                Map.entry("nicheName", string()),
                Map.entry("businessType", string()),
                Map.entry("operationType", string()),
                Map.entry("customerType", string()),
                Map.entry("commercialObjects", string()),
                Map.entry("initialAssumptions", string()),
                Map.entry("confidenceLevel", enumString("INFERRED_FROM_CNAE", "LOW_CONFIDENCE", "NEEDS_RESEARCH")),
                Map.entry("createdBy", enumString("AI"))));
        return schema;
    }

    /** Monta o schema da frase de pesquisa que será executada nas próximas etapas. */
    private Map<String, Object> querySchema() {
        Map<String, Object> schema = object(
                "researchCycleId", "queryText", "queryGoal", "sourceGroup", "priority", "status", "createdBy");
        schema.put("properties", Map.of(
                "researchCycleId", integer(),
                "queryText", string(),
                "queryGoal", enumString(
                        "ROUTINE_DISCOVERY",
                        "NICHE_OWNER_QUESTION_DISCOVERY",
                        "FINAL_CUSTOMER_QUESTION_DISCOVERY",
                        "SALES_PAIN_DISCOVERY",
                        "PRODUCT_SERVICE_DISCOVERY",
                        "OFFER_PATTERN_DISCOVERY"),
                "sourceGroup", string(),
                "priority", integer(),
                "status", enumString("PENDING"),
                "createdBy", enumString("AI")));
        return schema;
    }

    /** Cria um objeto JSON Schema com propriedades adicionais bloqueadas. */
    private Map<String, Object> object(String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of(required));
        return schema;
    }

    /** Cria um campo textual simples para o schema da etapa dois. */
    private Map<String, Object> string() {
        return Map.of("type", "string");
    }

    /** Cria um campo inteiro simples para o schema da etapa dois. */
    private Map<String, Object> integer() {
        return Map.of("type", "integer");
    }

    /** Cria um campo textual restrito aos valores permitidos pela regra da etapa dois. */
    private Map<String, Object> enumString(String... values) {
        return Map.of("type", "string", "enum", List.of(values));
    }

    /** Cria um array tipado para a coleção de queries geradas pela IA. */
    private Map<String, Object> array(Map<String, Object> itemSchema) {
        return Map.of("type", "array", "minItems", 12, "maxItems", 15, "items", itemSchema);
    }
}
