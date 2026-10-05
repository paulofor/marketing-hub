package com.marketinghub.imagegenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.openai.service.OpenAiPricingService;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: estimar texto e imagem da mesma resposta sem ocultar uso ou tarifa ausentes.
 */
@Component
public class ImageGenerationUsageCost {
  private final OpenAiPricingService pricing;
  private final JsonNode imageRates;

  /** Carrega tarifas multimodais versionadas e reutiliza o catálogo do modelo de texto. */
  public ImageGenerationUsageCost(OpenAiPricingService pricing, ObjectMapper json)
      throws IOException {
    this.pricing = pricing;
    try (var input =
        new ClassPathResource("pricing/image-generation-token-rates.v1.json").getInputStream()) {
      imageRates = json.readTree(input);
    }
  }

  /** Soma uma vez o modelo principal e a ferramenta; entrada incompleta permanece desconhecida. */
  public Optional<Estimate> estimate(JsonNode response, String imageModel) {
    if (response == null) return Optional.empty();
    if (!response.path("tools").isArray()
        || response.path("tool_usage").path("web_search").path("num_requests").asLong() > 0)
      return Optional.empty();
    boolean declared = false;
    for (var tool : response.path("tools")) {
      if ("image_generation".equals(tool.path("type").asText())) {
        if (!imageModel.equals(tool.path("model").asText())) return Optional.empty();
        declared = true;
      }
    }
    if (!declared) return Optional.empty();
    var usage = response.path("usage");
    var imageUsage = response.path("tool_usage").path("image_gen");
    var rate = imageRates.path("models").path(imageModel);
    if (rate.isMissingNode()
        || !count(usage.path("input_tokens"))
        || !count(usage.path("output_tokens"))
        || !count(imageUsage.path("input_tokens"))
        || !count(imageUsage.path("output_tokens"))) return Optional.empty();
    var cached = usage.path("input_tokens_details").path("cached_tokens");
    if (!count(cached)
        || usage.path("input_tokens_details").path("cache_write_tokens").asLong() > 0)
      return Optional.empty();
    var input = imageUsage.path("input_tokens_details");
    var output = imageUsage.path("output_tokens_details");
    if (!count(input.path("text_tokens"))
        || !count(input.path("image_tokens"))
        || !count(output.path("image_tokens"))
        || !count(output.path("text_tokens"))
        || input.path("text_tokens").asLong() + input.path("image_tokens").asLong()
            != imageUsage.path("input_tokens").asLong()
        || output.path("image_tokens").asLong() != imageUsage.path("output_tokens").asLong()
        || output.path("text_tokens").asLong() != 0
        || input.path("cached_tokens").asLong() != 0
        || imageUsage.path("cached_tokens").asLong() != 0) return Optional.empty();
    var textCost =
        pricing.estimateTaskCost(
            response.path("model").asText(),
            response.path("service_tier").asText(),
            usage.path("input_tokens").asLong(),
            cached.asLong(),
            usage.path("output_tokens").asLong());
    if (textCost.isEmpty()) return Optional.empty();
    // Flex do modelo principal não concede desconto Batch à ferramenta de imagem.
    var imageCost =
        tokens(rate.path("textInputUsdPerMillion"), input.path("text_tokens").asLong())
            .add(tokens(rate.path("imageInputUsdPerMillion"), input.path("image_tokens").asLong()))
            .add(
                tokens(
                    rate.path("imageOutputUsdPerMillion"), output.path("image_tokens").asLong()));
    return Optional.of(
        new Estimate(
            textCost.get().add(imageCost).setScale(8, RoundingMode.HALF_UP),
            "AUDITED_TOKEN_RATE_ESTIMATE_V1:" + imageRates.path("checkedOn").asText(),
            imageRates.path("source").asText(),
            imageRates.path("checkedOn").asText()));
  }

  /** Recusa contagem fracionária, negativa ou ausente em vez de transformá-la em zero. */
  private boolean count(JsonNode value) {
    return value.isIntegralNumber() && value.canConvertToLong() && value.asLong() >= 0;
  }

  /** Converte uma modalidade pelos preços oficiais por milhão de tokens. */
  private BigDecimal tokens(JsonNode rate, long count) {
    return rate.decimalValue().multiply(BigDecimal.valueOf(count)).movePointLeft(6);
  }

  /** Responsabilidade: distinguir estimativa por tokens de cobrança conciliada do provedor. */
  public record Estimate(
      BigDecimal estimatedCostUsd,
      String evidence,
      String pricingSource,
      String pricingCheckedOn) {}
}
