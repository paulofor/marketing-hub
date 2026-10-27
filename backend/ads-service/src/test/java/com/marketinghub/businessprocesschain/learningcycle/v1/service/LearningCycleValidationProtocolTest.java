package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: preservar o protocolo real sem importar comparação de outra experiência. */
class LearningCycleValidationProtocolTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Reproduz a deriva do ciclo novo e preserva a comparação válida com outras identidades. */
  @ParameterizedTest
  @ValueSource(longs = {4L, 98004L})
  void scopesComparisonToExactImplementedCapability(long productId) throws Exception {
    var products = mock(ProductRepository.class);
    var inputs = new LearningCycleImplementedInputContext(products, json);
    var cycle = new LearningSalesCycle();
    cycle.setId(productId + 100);
    cycle.setProductId(productId);
    cycle.setExperimentId(productId + 200);
    cycle.setProductVersion("musa-pde-entry-v16-modelagem-internacional");
    when(products.findById(productId))
        .thenReturn(
            Optional.of(Product.builder().id(productId).slug("metodo-musa-7-dias").build()));
    assertThat(inputs.resolve(cycle)).isEmpty();
    var bases = inputs.availableImplementations(cycle);
    assertThat(bases)
        .anySatisfy(
            base -> {
              assertThat(base.path("prototypeVersions").toString()).contains("v15-estilo-pessoal");
              assertThat(base.path("requiredFields").toString()).contains("stylePreference");
              assertThat(base.path("targetImplementationClaimed").asBoolean()).isFalse();
            });
    assertThat(bases)
        .allSatisfy(
            base -> assertThat(base.path("productSlug").asText()).isEqualTo("metodo-musa-7-dias"));
    var protocol = LearningCycleValidationProtocol.resolve(json, cycle, null);
    assertThat(protocol.path("inputComparisonApplicable").asBoolean()).isFalse();
    assertThat(protocol.has("documentedInputComparison")).isFalse();
    assertThat(protocol.path("requiredScenarios").size()).isEqualTo(3);
    var imported =
        json.readTree(
            """
      {"marketStrategicContract":{"causalHypothesis":"Primeiro ajuste com entrada reduzida",
       "agentValidationPlan":{"hypothesis":"Comparar REFERENCE e REDUCED em 18 combinações"}}}
      """);
    assertThat(LearningCycleValidationProtocol.hasUnscopedComparison(imported, protocol)).isTrue();

    when(products.findById(productId))
        .thenReturn(Optional.of(Product.builder().id(productId).slug("pde-planejado-36").build()));
    cycle.setProductVersion("mira-private-candidate-v3");
    var exact = inputs.resolve(cycle).orElseThrow();
    var valid = LearningCycleValidationProtocol.resolve(json, cycle, exact);
    assertThat(valid.path("inputComparisonApplicable").asBoolean()).isTrue();
    assertThat(valid.path("documentedInputComparison").path("technicalCombinationCount").asInt())
        .isEqualTo(18);
    assertThat(LearningCycleValidationProtocol.hasUnscopedComparison(imported, valid)).isFalse();
    cycle.setId(cycle.getId() + 1);
    assertThat(
            LearningCycleValidationProtocol.resolve(json, cycle, exact)
                .path("inputComparisonApplicable")
                .asBoolean())
        .isFalse();
  }
}
