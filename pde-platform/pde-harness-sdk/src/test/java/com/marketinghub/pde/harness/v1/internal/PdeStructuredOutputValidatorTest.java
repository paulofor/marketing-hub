package com.marketinghub.pde.harness.v1.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import com.marketinghub.pde.harness.v1.support.PdeHarnessTestSupport;
import org.junit.jupiter.api.Test;

/** Homologa parsing e aderência real da resposta ao JSON Schema versionado. */
class PdeStructuredOutputValidatorTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final PdeStructuredOutputValidator validator = new PdeStructuredOutputValidator(mapper);

  /** Devolve uma cópia estruturada quando a saída atende integralmente ao contrato. */
  @Test
  void acceptsOutputThatMatchesSchema() {
    JsonNode output =
        validator.validate(
            "{\"message\":\"orientação útil\"}", PdeHarnessTestSupport.validOutputSchema());

    assertEquals("orientação útil", output.path("message").asText());
  }

  /** Bloqueia conteúdo adicional depois do JSON final. */
  @Test
  void rejectsTrailingContent() {
    PdeHarnessException error =
        assertThrows(
            PdeHarnessException.class,
            () ->
                validator.validate(
                    "{\"message\":\"ok\"} outro", PdeHarnessTestSupport.validOutputSchema()));

    assertEquals(PdeHarnessFailureCategory.EXECUTION_FAILED, error.category());
  }

  /** Bloqueia campos não previstos mesmo quando o conteúdo é JSON válido. */
  @Test
  void rejectsSchemaMismatch() {
    PdeHarnessException error =
        assertThrows(
            PdeHarnessException.class,
            () ->
                validator.validate(
                    "{\"message\":\"ok\",\"extra\":true}",
                    PdeHarnessTestSupport.validOutputSchema()));

    assertEquals(PdeHarnessFailureCategory.EXECUTION_FAILED, error.category());
  }
}
