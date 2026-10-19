package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: impedir que telas de encerramento escondam a prova funcional entregue a Íris.
 */
class CreativeSourceSelectionTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Prioriza conteúdo em duas identidades, mesmo quando a captura antiga aparece primeiro. */
  @Test
  void selectsFunctionalContentForDifferentProductsAndKeepsLegacyFallback() {
    for (long sourceTask : List.of(910395L, 920712L)) {
      var inputs = json.createArrayNode();
      for (String moment : List.of("FULL_PAGE", "BRIEFING", "CONTENT"))
        for (String device : List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7")) {
          var input = inputs.addObject().put("sourceTaskId", sourceTask);
          input
              .putObject("evidence")
              .put("id", inputs.size())
              .put("deviceProfile", device)
              .put("evidenceKey", "ADHERENT-" + device + "-" + moment);
        }
      var selected = IrisCreativeMaterializer.selectSources(inputs);
      assertThat(selected).hasSize(2);
      assertThat(selected)
          .allSatisfy(
              input -> {
                assertThat(input.path("sourceTaskId").asLong()).isEqualTo(sourceTask);
                assertThat(input.path("evidence").path("evidenceKey").asText())
                    .endsWith("-CONTENT");
              });
      assertThat(selected.getFirst().path("evidence").path("deviceProfile").asText())
          .isEqualTo("IPHONE_15_PRO");
      var historical = json.createArrayNode().add(inputs.get(0)).add(inputs.get(1));
      assertThat(IrisCreativeMaterializer.selectSources(historical))
          .containsExactly(historical.get(1), historical.get(0));
    }
  }
}
