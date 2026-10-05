package com.marketinghub.imagegenerator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.openai.service.OpenAiPricingService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o cálculo multimodal contra omissão, duplicação e tarifa indevida. */
class ImageGenerationUsageCostTest {
  private final ObjectMapper json = new ObjectMapper();
  private final OpenAiPricingService pricing = mock(OpenAiPricingService.class);
  private ImageGenerationUsageCost cost;

  /** Carrega a tarifa versionada com modelo principal substituído por cálculo determinístico. */
  @BeforeEach
  void setup() throws Exception {
    cost = new ImageGenerationUsageCost(pricing, json);
    when(pricing.estimateTaskCost(eq("gpt-5.6-sol"), eq("flex"), eq(3476L), eq(0L), eq(715L)))
        .thenReturn(Optional.of(new BigDecimal("0.014102")));
  }

  /** Reproduz os tokens preservados no caso original, sem depender de produto ou execução. */
  @Test
  void combinesActualTextAndImageUsageWithoutBatchDiscountOnTool() throws Exception {
    var estimate = cost.estimate(response(), "gpt-image-2.5-sunburst").orElseThrow();
    assertThat(estimate.estimatedCostUsd()).isEqualByComparingTo("0.079122");
    assertThat(estimate.evidence()).contains("2026-10-05", "AUDITED_TOKEN_RATE_ESTIMATE");
    assertThat(estimate.evidence()).hasSizeLessThanOrEqualTo(64);
    assertThat(estimate.pricingSource())
        .isEqualTo("https://developers.openai.com/api/docs/pricing");
  }

  /** Outra entrada com tokens de imagem possui custo próprio, sem exceção por identidade. */
  @Test
  void pricesAnotherInputWithImageReferences() throws Exception {
    var data = response();
    var input = (ObjectNode) data.path("tool_usage").path("image_gen");
    input.put("input_tokens", 756);
    ((ObjectNode) input.path("input_tokens_details")).put("image_tokens", 100);
    assertThat(cost.estimate(data, "gpt-image-2.5-sunburst").orElseThrow().estimatedCostUsd())
        .isEqualByComparingTo("0.079922");
  }

  /** Dados, modelo, tarifa ou cache não interpretável não podem produzir custo zero conhecido. */
  @Test
  void leavesIncompleteAndContradictoryUsageUnknown() throws Exception {
    assertThat(cost.estimate(null, "gpt-image-2.5-sunburst")).isEmpty();
    assertThat(cost.estimate(response(), "unknown-image-model")).isEmpty();
    var data = response();
    ((ObjectNode) data.path("tool_usage")).remove("image_gen");
    assertThat(cost.estimate(data, "gpt-image-2.5-sunburst")).isEmpty();
    data = response();
    ((ObjectNode) data.path("tool_usage").path("image_gen").path("input_tokens_details"))
        .put("text_tokens", -2);
    assertThat(cost.estimate(data, "gpt-image-2.5-sunburst")).isEmpty();
    data = response();
    ((ObjectNode) data.path("tool_usage").path("image_gen")).put("input_tokens", 900);
    assertThat(cost.estimate(data, "gpt-image-2.5-sunburst")).isEmpty();
    data = response();
    ((ObjectNode) data.path("tool_usage").path("image_gen").path("input_tokens_details"))
        .put("cached_tokens", 100);
    assertThat(cost.estimate(data, "gpt-image-2.5-sunburst")).isEmpty();
  }

  /** Tarifas ausentes do modelo principal também impedem uma estimativa parcial falsa. */
  @Test
  void refusesMissingTextPriceAndDifferentServedTool() throws Exception {
    when(pricing.estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong()))
        .thenReturn(Optional.empty());
    assertThat(cost.estimate(response(), "gpt-image-2.5-sunburst")).isEmpty();
    var data = response();
    ((ObjectNode) data.path("tools").get(0)).put("model", "different-model");
    assertThat(cost.estimate(data, "gpt-image-2.5-sunburst")).isEmpty();
  }

  /** Cria auditoria mínima com todas as modalidades, evitando transferir imagem ou segredo real. */
  private ObjectNode response() throws Exception {
    return (ObjectNode)
        json.readTree(
            """
      {"model":"gpt-5.6-sol","service_tier":"flex",
       "tools":[{"type":"image_generation","model":"gpt-image-2.5-sunburst"}],
       "usage":{"input_tokens":3476,"output_tokens":715,"input_tokens_details":{"cached_tokens":0}},
       "tool_usage":{"image_gen":{"input_tokens":656,"output_tokens":2058,
         "input_tokens_details":{"text_tokens":656,"image_tokens":0},
         "output_tokens_details":{"image_tokens":2058,"text_tokens":0}}}}
      """);
  }
}
