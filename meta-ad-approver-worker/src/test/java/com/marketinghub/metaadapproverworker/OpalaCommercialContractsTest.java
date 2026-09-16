package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: manter o contrato comercial de revisão no subprocesso Opala. */
class OpalaCommercialContractsTest {
  /** A revisão especializada consome a fila correta e usa o schema comercial. */
  @Test
  void resolvesCommercialReview() {
    String code = "opala-commercial-preparation-v1";
    assertThat(CommercialBpmTaskConsumer.supportsContract(code, "commercialIntegrityReview"))
        .isTrue();
    assertThat(CommercialBpmTaskConsumer.schemaResourceFor(code)).contains("commercial");
    assertThat(new ClassPathResource(CommercialBpmTaskConsumer.promptResourceFor(code)).exists())
        .isTrue();
  }
}
