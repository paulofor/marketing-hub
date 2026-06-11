package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o contrato JSON Schema da etapa dois antes da chamada ao modelo. */
class NicheResearchSeedBuilderSchemaTest {
    private final NicheResearchSeedBuilderSchema schemaBuilder = new NicheResearchSeedBuilderSchema();

    /** Deve declarar maxLength nos campos que são gravados em colunas curtas do backend. */
    @Test
    void shouldLimitModelOutputUsingJsonSchemaMaxLength() {
        Map<String, Object> schema = schemaBuilder.buildSchema();

        Map<?, ?> rootProperties = properties(schema);
        Map<?, ?> seedProperties = properties(property(rootProperties, "seed"));
        Map<?, ?> queryProperties = properties(items(property(rootProperties, "queries")));

        assertMaxLength(seedProperties, "cnaeCode", 7);
        assertMaxLength(seedProperties, "cnaeDescription", 255);
        assertMaxLength(seedProperties, "nicheName", 255);
        assertMaxLength(seedProperties, "businessType", 255);
        assertMaxLength(seedProperties, "confidenceLevel", 64);
        assertMaxLength(seedProperties, "createdBy", 32);
        assertMaxLength(queryProperties, "queryText", 500);
        assertMaxLength(queryProperties, "queryGoal", 64);
        assertMaxLength(queryProperties, "sourceGroup", 64);
        assertMaxLength(queryProperties, "status", 32);
        assertMaxLength(queryProperties, "createdBy", 32);
    }

    /** Deve manter campos naturalmente longos sem maxLength artificial. */
    @Test
    void shouldKeepNaturallyLongSeedFieldsWithoutArtificialMaxLength() {
        Map<?, ?> seedProperties = properties(property(properties(schemaBuilder.buildSchema()), "seed"));

        assertThat(property(seedProperties, "operationType").containsKey("maxLength")).isFalse();
        assertThat(property(seedProperties, "customerType").containsKey("maxLength")).isFalse();
        assertThat(property(seedProperties, "commercialObjects").containsKey("maxLength")).isFalse();
        assertThat(property(seedProperties, "initialAssumptions").containsKey("maxLength")).isFalse();
    }

    /** Confirma que uma propriedade textual possui o limite máximo esperado no schema. */
    private void assertMaxLength(Map<?, ?> properties, String fieldName, int expectedMaxLength) {
        assertThat(property(properties, fieldName).get("maxLength")).isEqualTo(expectedMaxLength);
    }

    /** Recupera o mapa de propriedades de um objeto do schema. */
    private Map<?, ?> properties(Map<?, ?> objectSchema) {
        return property(objectSchema, "properties");
    }

    /** Recupera o schema de item de uma propriedade array. */
    private Map<?, ?> items(Map<?, ?> arraySchema) {
        return property(arraySchema, "items");
    }

    /** Recupera uma propriedade do schema como mapa. */
    private Map<?, ?> property(Map<?, ?> properties, String key) {
        Object value = properties.get(key);
        assertThat(value).isInstanceOf(Map.class);
        return (Map<?, ?>) value;
    }
}
