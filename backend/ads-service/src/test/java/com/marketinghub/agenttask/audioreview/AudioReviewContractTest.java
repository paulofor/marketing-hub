package com.marketinghub.agenttask.audioreview;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.audioreview.v1.service.AudioReviewContract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reproduz voz monótona, música invasiva e parecer contraditório sem consumir modelo externo. */
class AudioReviewContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aprovação de contrato não permite ignorar déficit sonoro em nenhuma dimensão. */
  @ParameterizedTest
  @ValueSource(strings = {"voice", "music", "timing"})
  void rejectsContradictions(String key) {
    var result = assessment(json);
    assertThatCode(() -> AudioReviewContract.assessment(result, 30)).doesNotThrowAnyException();
    ((ObjectNode) result.path(key)).put("status", "ADJUST");
    assertThatThrownBy(() -> AudioReviewContract.assessment(result, 30))
        .hasMessageContaining("contraditória");
    result.put("verdict", "ADJUST");
    assertThatCode(() -> AudioReviewContract.assessment(result, 30)).doesNotThrowAnyException();
  }

  /** Não inventa custo quando tokens de áudio não foram informados; preço difere do textual. */
  @Test
  void pricesModalitiesWithoutInventingUsage() throws Exception {
    var raw =
        json.readTree(
            "{\"usage\":{\"prompt_tokens\":1100,\"prompt_tokens_details\":{\"audio_tokens\":1000},\"completion_tokens\":500}}");
    assertThat(AudioReviewContract.cost(raw)).isEqualByComparingTo("0.037250");
    assertThat(
            AudioReviewContract.cost(
                json.readTree("{\"usage\":{\"prompt_tokens\":1100,\"completion_tokens\":500}}")))
        .isNull();
    ((ObjectNode) raw.path("usage")).put("completion_tokens", 2001);
    assertThat(AudioReviewContract.cost(raw)).isNull();
  }

  /** Recusa evidência fora da duração e falta de cobertura no fim da mídia. */
  @Test
  void requiresTemporalCoverage() {
    var result = assessment(json);
    ((ObjectNode) result.path("moments").get(2)).put("endSeconds", 50);
    assertThatThrownBy(() -> AudioReviewContract.assessment(result, 30))
        .hasMessageContaining("fora");
    ((ObjectNode) result.path("moments").get(2)).put("startSeconds", 2).put("endSeconds", 3);
    assertThatThrownBy(() -> AudioReviewContract.assessment(result, 30))
        .hasMessageContaining("encerramento");
  }

  /** Valores textuais e campos extras não substituem o contrato JSON validado. */
  @Test
  void rejectsMalformedTypesAndExtraFields() {
    var result = assessment(json);
    result.put("listened", "true");
    assertThatThrownBy(() -> AudioReviewContract.assessment(result, 30))
        .hasMessageContaining("Escuta");
    result.put("listened", true).put("approval", true);
    assertThatThrownBy(() -> AudioReviewContract.assessment(result, 30))
        .hasMessageContaining("Campos");
    result.remove("approval");
    result.putArray("limitations").add(0);
    assertThatThrownBy(() -> AudioReviewContract.assessment(result, 30))
        .hasMessageContaining("Limitação");
  }

  /** Gera resposta segregada do modelo simulado, sem alegar percepção real. */
  static ObjectNode assessment(ObjectMapper json) {
    var result =
        json.createObjectNode()
            .put("contractVersion", "PSIQUE_AUDIO_REVIEW_V1")
            .put("listened", true)
            .put("verdict", "PASS");
    for (var key : java.util.List.of("voice", "music", "timing"))
      result
          .putObject(key)
          .put("status", "PASS")
          .put("summary", "Fixture local com evidência temporal suficiente.");
    var moments = result.putArray("moments");
    for (double start : new double[] {0, 14, 27})
      moments
          .addObject()
          .put("startSeconds", start)
          .put("endSeconds", start + 3)
          .put("heard", "Fala simulada do benefício neste trecho")
          .put("action", "Manter esta composição.");
    result.putArray("limitations").add("Fixture simulada, sem avaliação perceptual do provedor.");
    return result;
  }
}
