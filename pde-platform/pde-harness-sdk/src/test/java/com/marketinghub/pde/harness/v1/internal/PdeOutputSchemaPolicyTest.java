package com.marketinghub.pde.harness.v1.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.support.PdeHarnessTestSupport;
import org.junit.jupiter.api.Test;

/** Garante que o SDK bloqueie a recorrência de schemas frouxos ou com raiz array. */
class PdeOutputSchemaPolicyTest {
  private final ObjectMapper mapper = new ObjectMapper();

  /** Aceita um contrato estrito e completamente requerido. */
  @Test
  void acceptsStrictObjectSchema() {
    assertDoesNotThrow(
        () -> PdeOutputSchemaPolicy.validate(PdeHarnessTestSupport.validOutputSchema()));
  }

  /** Bloqueia array na raiz antes de chamar o App Server. */
  @Test
  void rejectsArrayRoot() throws Exception {
    assertThrows(
        PdeHarnessException.class,
        () ->
            PdeOutputSchemaPolicy.validate(
                mapper.readTree("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}")));
  }

  /** Bloqueia objeto interno que aceite campos desconhecidos. */
  @Test
  void rejectsLooseNestedObject() throws Exception {
    assertThrows(
        PdeHarnessException.class,
        () ->
            PdeOutputSchemaPolicy.validate(
                mapper.readTree(
                    """
                    {
                      "type":"object",
                      "additionalProperties":false,
                      "properties":{
                        "detail":{
                          "type":"object",
                          "properties":{"name":{"type":"string"}},
                          "required":["name"]
                        }
                      },
                      "required":["detail"]
                    }
                    """)));
  }

  /** Bloqueia referência externa para que a validação nunca busque schema pela rede. */
  @Test
  void rejectsExternalSchemaReference() throws Exception {
    assertThrows(
        PdeHarnessException.class,
        () ->
            PdeOutputSchemaPolicy.validate(
                mapper.readTree(
                    """
                    {
                      "type":"object",
                      "additionalProperties":false,
                      "properties":{"detail":{"$ref":"https://externo.invalid/schema"}},
                      "required":["detail"]
                    }
                    """)));
  }
}
