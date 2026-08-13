package com.marketinghub.experiment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Comprova o contrato estruturado do diagnóstico textual da Meta. */
class ExperimentMapperMetaCopyTest {

  /** Extrai todas as contagens persistidas pelo worker sem inferência no frontend. */
  @Test
  void mapsEveryMetaCopyViolation() {
    ExperimentMapper mapper =
        new ExperimentMapper() {
          @Override
          public com.marketinghub.experiment.dto.ExperimentDto toDto(
              com.marketinghub.experiment.Experiment experiment) {
            throw new UnsupportedOperationException();
          }

          @Override
          public com.marketinghub.experiment.dto.FacebookPageDto toDto(
              com.marketinghub.ads.FacebookPage page) {
            throw new UnsupportedOperationException();
          }

          @Override
          public com.marketinghub.experiment.dto.InstagramAccountDto toDto(
              com.marketinghub.ads.InstagramAccount account) {
            throw new UnsupportedOperationException();
          }
        };

    var violations =
        mapper.metaCopyViolations(
            "Copy Meta inválida: primaryText excede 125 caracteres (atual: 148); "
                + "headline excede 40 caracteres (atual: 52); reescrita obrigatória");

    assertThat(violations)
        .extracting("field", "actualLength", "maxLength")
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("primaryText", 148, 125),
            org.assertj.core.groups.Tuple.tuple("headline", 52, 40));
  }
}
