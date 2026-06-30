package com.marketinghub.worker.pipeline.gerasalespagev1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import org.junit.jupiter.api.Test;

/** Valida o contrato mínimo de resposta JSON do GeraSalesPage v1. */
class GeraSalesPageResponseValidatorTest {
    private final GeraSalesPageResponseValidator validator = new GeraSalesPageResponseValidator(new ObjectMapper());

    /** Aceita respostas JSON objetuais e preserva o payload estruturado. */
    @Test
    void shouldParseStructuredJsonResponse() {
        GeraSalesPageOutput output = validator.validateAndParse("{\"headline\":\"Venda direta\"}");

        assertEquals("Venda direta", output.payload().get("headline"));
    }

    /** Bloqueia respostas que não sejam JSON válido. */
    @Test
    void shouldRejectInvalidJsonResponse() {
        assertThrows(StageWorkerException.class, () -> validator.validateAndParse("texto solto"));
    }
}
