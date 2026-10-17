package com.marketinghub.financialplan.v1.service.saveplan;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * Responsabilidade: impedir conversão silenciosa de texto ou frações em identidades de autorização.
 */
public final class WholeIdentifierDeserializer extends JsonDeserializer<Long> {
  /** Exige identificador inteiro representável no contrato Long. */
  @Override
  public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT))
      return (Long) context.handleUnexpectedToken(Long.class, parser);
    return parser.getLongValue();
  }
}
