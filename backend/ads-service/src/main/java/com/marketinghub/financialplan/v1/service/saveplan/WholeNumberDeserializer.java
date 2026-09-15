package com.marketinghub.financialplan.v1.service.saveplan;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * Responsabilidade: rejeitar frações em quantidades financeiras em vez de truncá-las
 * silenciosamente.
 */
public final class WholeNumberDeserializer extends JsonDeserializer<Integer> {
  /** Exige token inteiro para período, clientes, unidades e tentativas. */
  @Override
  public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT))
      return (Integer) context.handleUnexpectedToken(Integer.class, parser);
    return parser.getIntValue();
  }
}
