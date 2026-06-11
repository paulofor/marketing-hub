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
                Map.entry("confidenceLevel", string()),
                Map.entry("createdBy", string())));
        return schema;
    }

    /** Monta o schema da frase de pesquisa que será executada nas próximas etapas. */
    private Map<String, Object> querySchema() {
        Map<String, Object> schema = object(
                "researchCycleId", "queryText", "queryGoal", "sourceGroup", "priority", "status", "createdBy");
        schema.put("properties", Map.of(
                "researchCycleId", integer(),
                "queryText", string(),
                "queryGoal", string(),
                "sourceGroup", string(),
                "priority", integer(),
                "status", string(),
                "createdBy", string()));
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

    /** Cria um array tipado para a coleção de queries geradas pela IA sem restringir volume. */
    private Map<String, Object> array(Map<String, Object> itemSchema) {
        return Map.of("type", "array", "items", itemSchema);
    }
}
