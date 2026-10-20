package com.marketinghub.salesvideo.service.reconcileNarrationCosts;

import static com.marketinghub.salesvideo.service.reconcileNarrationCosts.NarrationUsageEvidence.require;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

/** Calcula envelope conservador de voz medida, sem simular tokens reportados ou fatura. */
final class NarrationCostEstimate {
  static final String POLICY = "NARRATION_MEASURED_DURATION_ESTIMATE_V1";
  static final BigDecimal USD_PER_MINUTE = new BigDecimal("0.072");

  /** Impede instâncias do cálculo sem estado. */
  private NarrationCostEstimate() {}

  /** Usa somente recibos já validados, modelo conhecido e duração física de cada síntese. */
  static BigDecimal cost(JsonNode audit) {
    BigDecimal total = BigDecimal.ZERO;
    for (var item : audit.path("tts_interactions")) {
      var pricing = item.path("pricing");
      var duration = item.path("output_duration_seconds");
      require(
          "gpt-4o-mini-tts-2025-12-15".equals(item.path("model").asText())
              && pricing
                      .path("input_usd_per_million_text_tokens")
                      .decimalValue()
                      .compareTo(new BigDecimal("0.6"))
                  == 0
              && pricing
                      .path("output_usd_per_million_audio_tokens")
                      .decimalValue()
                      .compareTo(new BigDecimal("12"))
                  == 0,
          "O modelo ou a tarifa não têm política de estimativa; preserve o custo desconhecido.");
      require(
          duration.isNumber()
              && duration.decimalValue().signum() > 0
              && duration.decimalValue().compareTo(new BigDecimal("3600")) <= 0,
          "A estimativa exige duração física positiva e válida em cada recibo de voz.");
      BigDecimal audio =
          duration
              .decimalValue()
              .multiply(USD_PER_MINUTE)
              .divide(new BigDecimal("60"), 12, RoundingMode.CEILING);
      // Bytes são apenas base de envelope de entrada; não são tokens medidos pelo provedor.
      BigDecimal input =
          BigDecimal.valueOf(
                  item.path("raw_request").toString().getBytes(StandardCharsets.UTF_8).length)
              .multiply(new BigDecimal("1.2"))
              .divide(new BigDecimal("1000000"), 12, RoundingMode.CEILING);
      total = total.add(audio).add(input);
    }
    return total.setScale(6, RoundingMode.CEILING);
  }
}
