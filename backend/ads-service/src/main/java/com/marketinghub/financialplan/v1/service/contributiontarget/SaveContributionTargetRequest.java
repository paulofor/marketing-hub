package com.marketinghub.financialplan.v1.service.contributiontarget;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.math.BigDecimal;

/** Responsabilidade: receber a meta humana sobre receita líquida vinculada à revisão lida. */
public record SaveContributionTargetRequest(
    @NotNull @Min(1) @JsonDeserialize(using = RevisionIdDeserializer.class) Long sourceRevisionId,
    @NotNull @DecimalMin("0.01") @DecimalMax("99.99") BigDecimal minimumMarginPercent) {
  /** Responsabilidade: recusar identificadores fracionários sem truncar a referência de origem. */
  public static final class RevisionIdDeserializer extends JsonDeserializer<Long> {
    /** Aceita somente token inteiro dentro do intervalo de identificadores do banco. */
    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
      if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT))
        return (Long) context.handleUnexpectedToken(Long.class, parser);
      return parser.getLongValue();
    }
  }
}
