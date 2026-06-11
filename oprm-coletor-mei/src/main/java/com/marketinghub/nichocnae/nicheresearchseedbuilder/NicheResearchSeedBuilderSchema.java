package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Fornece o schema JSON estrito usado pela Responses API para a saída da etapa dois. */
@Component
public class NicheResearchSeedBuilderSchema {
    static final int CNAE_CODE_MAX_LENGTH = 7;
    static final int SEED_SHORT_TEXT_MAX_LENGTH = 255;
    static final int CONFIDENCE_LEVEL_MAX_LENGTH = 64;
    static final int CREATED_BY_MAX_LENGTH = 32;
    static final int QUERY_TEXT_MAX_LENGTH = 500;
    static final int QUERY_GOAL_MAX_LENGTH = 64;
    static final int SOURCE_GROUP_MAX_LENGTH = 64;
    static final int STATUS_MAX_LENGTH = 32;

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
                Map.entry("cnaeCode", string(CNAE_CODE_MAX_LENGTH)),
                Map.entry("cnaeDescription", string(SEED_SHORT_TEXT_MAX_LENGTH)),
                Map.entry("nicheName", string(SEED_SHORT_TEXT_MAX_LENGTH)),
                Map.entry("businessType", string(SEED_SHORT_TEXT_MAX_LENGTH)),
                Map.entry("operationType", string()),
                Map.entry("customerType", string()),
                Map.entry("commercialObjects", string()),
                Map.entry("initialAssumptions", string()),
                Map.entry("confidenceLevel", string(CONFIDENCE_LEVEL_MAX_LENGTH)),
                Map.entry("createdBy", string(CREATED_BY_MAX_LENGTH))));
        return schema;
    }

    /** Monta o schema da frase de pesquisa que será executada nas próximas etapas. */
    private Map<String, Object> querySchema() {
        Map<String, Object> schema = object(
                "researchCycleId", "queryText", "queryGoal", "sourceGroup", "priority", "status", "createdBy");
        schema.put("properties", Map.of(
                "researchCycleId", integer(),
                "queryText", string(QUERY_TEXT_MAX_LENGTH),
                "queryGoal", string(QUERY_GOAL_MAX_LENGTH),
                "sourceGroup", string(SOURCE_GROUP_MAX_LENGTH),
                "priority", integer(),
                "status", string(STATUS_MAX_LENGTH),
                "createdBy", string(CREATED_BY_MAX_LENGTH)));
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

    /** Cria um campo textual sem limite físico específico para textos naturalmente longos. */
    private Map<String, Object> string() {
        return Map.of("type", "string");
    }

    /** Cria um campo textual limitado ao tamanho físico do banco. */
    private Map<String, Object> string(int maxLength) {
        return Map.of("type", "string", "maxLength", maxLength);
    }

    /** Cria um campo inteiro simples para o schema da etapa dois. */
    private Map<String, Object> integer() {
        return Map.of("type", "integer");
    }

    /** Cria um array tipado para a coleção de queries geradas pela IA sem restringir volume. */
    private Map<String, Object> array(Map<String, Object> itemSchema) {
        return Map.of("type", "array", "items", itemSchema);
    }
}
