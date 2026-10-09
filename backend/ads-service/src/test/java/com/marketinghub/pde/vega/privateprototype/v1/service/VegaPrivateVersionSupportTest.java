package com.marketinghub.pde.vega.privateprototype.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.vega.privateprototype.v1.service.contract.VegaPrivateContract.InternalSession;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.vega.VegaAdjustmentExecutionRepository;
import com.marketinghub.repository.jpa.vega.VegaPrivateSessionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: permitir a variante corrente sem misturar a versão ou a identidade do ciclo.
 */
class VegaPrivateVersionSupportTest {
  private final VegaPrivateSessionRepository sessions = mock(VegaPrivateSessionRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final VegaPrivateService service =
      new VegaPrivateService(
          sessions,
          mock(VegaAdjustmentExecutionRepository.class),
          cycles,
          products,
          new ObjectMapper());

  /**
   * Confirma que versões históricas e a sucessora preservam a mesma identidade e limites privados.
   */
  @ParameterizedTest
  @ValueSource(ints = {12, 13, 14})
  void supportsOnlyVersionDeclaredInCycle(int number) {
    String version = version(number);
    prepare(version);
    var result = service.create(new InternalSession(91002L, version, "QA_INTERNAL", null));
    assertThat(result.path("prototypeVersion").asText()).isEqualTo(version);
    assertThat(result.path("productId").asLong()).isEqualTo(91004L);
    assertThat(result.path("experimentId").asLong()).isEqualTo(91092L);
    assertThat(result.path("paymentEnabled").asBoolean()).isFalse();
    assertThat(result.path("published").asBoolean()).isFalse();
    assertThat(result.path("mediaSpendBrl").asInt()).isZero();
    assertThat(service.contract().path("supportedPrototypeVersions").toString()).contains(version);
  }

  /** Recusa usar a v14 no ciclo ainda congelado em v13, antes de persistir uma sessão. */
  @Test
  void doesNotReusePredecessorForChangedExperience() {
    prepare(version(13));
    assertThatThrownBy(
            () -> service.create(new InternalSession(91002L, version(14), "QA_INTERNAL", null)))
        .isInstanceOf(ResponseStatusException.class);
    verify(sessions, never()).saveAndFlush(any());
  }

  /** Não anuncia suporte nem abre acesso para uma variante ainda sem implementação. */
  @Test
  void rejectsUnknownVersionEvenWhenCycleContainsIt() {
    prepare(version(999));
    assertThatThrownBy(
            () -> service.create(new InternalSession(91002L, version(999), "QA_INTERNAL", null)))
        .isInstanceOf(ResponseStatusException.class);
    verify(sessions, never()).saveAndFlush(any());
  }

  /** Prepara ciclo e produto sintéticos, com o contrato de acesso privado aplicável. */
  private void prepare(String version) {
    ReflectionTestUtils.setField(service, "version", version(12));
    var cycle = new LearningSalesCycle();
    cycle.setId(91002L);
    cycle.setProductId(91004L);
    cycle.setExperimentId(91092L);
    cycle.setPreviousCycleId(91001L);
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    cycle.setProductVersion(version);
    when(cycles.findLockedById(91002L)).thenReturn(Optional.of(cycle));
    when(products.findById(91004L))
        .thenReturn(Optional.of(Product.builder().id(91004L).slug("metodo-musa-7-dias").build()));
  }

  /** Gera somente identificadores de versão do contrato testado. */
  private String version(int number) {
    return "musa-pde-entry-v" + number + "-primeiro-ajuste-aplicavel";
  }
}
