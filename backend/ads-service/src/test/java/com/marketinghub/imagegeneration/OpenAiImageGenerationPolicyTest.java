package com.marketinghub.imagegeneration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a política canônica aplicada a novas gerações visuais OpenAI. */
class OpenAiImageGenerationPolicyTest {

  /** Reconhece o identificador canônico sem aceitar modelos de gerações anteriores. */
  @Test
  void recognizesOnlyCanonicalImageModel() {
    assertThat(OpenAiImageGenerationPolicy.isCanonicalModel(" gpt-image-2.5-sunburst ")).isTrue();
    assertThat(OpenAiImageGenerationPolicy.isCanonicalModel("gpt-image-2")).isFalse();
    assertThat(OpenAiImageGenerationPolicy.isCanonicalModel(null)).isFalse();
  }

  /** Promove configuração antiga, vazia ou divergente ao modelo visual canônico. */
  @Test
  void normalizesEveryConfigurationToCanonicalImageModel() {
    assertThat(OpenAiImageGenerationPolicy.normalizeToCanonicalModel("gpt-image-1.5"))
        .isEqualTo("gpt-image-2.5-sunburst");
    assertThat(OpenAiImageGenerationPolicy.normalizeToCanonicalModel(""))
        .isEqualTo("gpt-image-2.5-sunburst");
    assertThat(OpenAiImageGenerationPolicy.normalizeToCanonicalModel(null))
        .isEqualTo("gpt-image-2.5-sunburst");
  }
}
