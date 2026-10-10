package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.mira.privateprototype.v1.service.contract.MiraPrivateContract;
import com.marketinghub.pde.vega.privateprototype.v1.service.contract.VegaPrivateContract;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import jakarta.validation.Validation;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: impedir que descrições históricas substituam a entrada executável do ciclo. */
class LearningCycleImplementedInputContextTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ProductRepository products = mock(ProductRepository.class);
  private final LearningCycleImplementedInputContext service =
      new LearningCycleImplementedInputContext(products, json);

  /** Confere campos do catálogo contra os records e a validação real dos dois produtos. */
  @Test
  void catalogMatchesActualInputRecords() {
    assertRecord(
        91004L,
        "metodo-musa-7-dias",
        "musa-pde-entry-v14-primeiro-ajuste-aplicavel",
        VegaPrivateContract.Input.class);
    assertRecord(
        92010L, "pde-planejado-36", "mira-private-candidate-v3", MiraPrivateContract.Input.class);
  }

  /** Compara obrigatoriedade e campos com Bean Validation, sem reproduzir o catálogo no teste. */
  private void assertRecord(Long id, String slug, String version, Class<?> inputType) {
    var cycle = cycle(id, version);
    when(products.findById(id))
        .thenReturn(Optional.of(Product.builder().id(id).slug(slug).build()));
    var result = service.resolve(cycle).orElseThrow();
    var required = new java.util.ArrayList<String>();
    var optional = new java.util.ArrayList<String>();
    result.path("requiredFields").forEach(field -> required.add(field.asText()));
    result.path("optionalFields").forEach(field -> optional.add(field.asText()));
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var constraints = factory.getValidator().getConstraintsForClass(inputType);
      var actualRequired =
          constraints.getConstrainedProperties().stream()
              .filter(
                  property ->
                      property.getConstraintDescriptors().stream()
                          .anyMatch(
                              descriptor ->
                                  List.of("NotBlank", "NotEmpty", "NotNull")
                                      .contains(
                                          descriptor
                                              .getAnnotation()
                                              .annotationType()
                                              .getSimpleName())))
              .map(jakarta.validation.metadata.PropertyDescriptor::getPropertyName)
              .toList();
      assertThat(required).containsExactlyInAnyOrderElementsOf(actualRequired);
    }
    var all = new java.util.ArrayList<>(required);
    all.addAll(optional);
    assertThat(all)
        .containsExactlyInAnyOrderElementsOf(
            Arrays.stream(inputType.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList());
    assertThat(result.path("productId").asLong()).isEqualTo(id);
    assertThat(result.path("prototypeVersion").asText()).isEqualTo(version);
    assertThat(result.path("agentApprovalClaimed").asBoolean()).isFalse();
  }

  /** Preserva o caminho antes válido e recusa atribuir a um produto a entrada de outro runtime. */
  @Test
  void neverInheritsInputFromAnotherVersionOrProduct() {
    when(products.findById(93004L))
        .thenReturn(Optional.of(Product.builder().id(93004L).slug("metodo-musa-7-dias").build()));
    assertThat(service.resolve(cycle(93004L, "mira-private-candidate-v3"))).isEmpty();
    assertThat(service.resolve(cycle(93004L, "musa-pde-entry-v999-primeiro-ajuste-aplicavel")))
        .isEmpty();
    assertThat(service.resolve(cycle(93004L, "musa-pde-entry-v12-primeiro-ajuste-aplicavel")))
        .isPresent();
    assertThat(service.resolve(cycle(93004L, "musa-pde-entry-v13-primeiro-ajuste-aplicavel")))
        .isPresent();
  }

  /**
   * Exercita a passagem real pelo contexto que o backend envia aos três agentes do planejamento.
   */
  @Test
  void planningContextCarriesExactInputWithoutChangingHistory() {
    var cycles =
        mock(com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository.class);
    var events =
        mock(com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository.class);
    var cycle = cycle(94004L, "musa-pde-entry-v14-primeiro-ajuste-aplicavel");
    when(products.findById(94004L))
        .thenReturn(Optional.of(Product.builder().id(94004L).slug("metodo-musa-7-dias").build()));
    when(cycles.findByExperimentId(cycle.getExperimentId())).thenReturn(Optional.of(cycle));
    var context = new LearningCycleTaskContext(cycles, events, new LearningCycleJson(json));
    ReflectionTestUtils.setField(context, "implementedInput", service);
    var task =
        context
            .resolve("experiment:" + cycle.getExperimentId(), cycle.getCreatedAt())
            .orElseThrow();
    var input = (com.fasterxml.jackson.databind.JsonNode) task.get("implementedInput");
    assertThat(input.path("cycleId").asLong()).isEqualTo(cycle.getId());
    assertThat(input.path("requiredFields").toString()).contains("occasion", "existingSelection");
    assertThat(task.get("inheritedLearning").toString()).contains("preservada");
    assertThat(
            context.resolve(
                "experiment:" + cycle.getExperimentId(), cycle.getCreatedAt().minusSeconds(1)))
        .isEmpty();
    verify(products, times(1)).findById(94004L);
  }

  /** Prepara identidades sintéticas e memória que nunca representam uma execução comercial. */
  private LearningSalesCycle cycle(Long productId, String version) {
    var cycle = new LearningSalesCycle();
    cycle.setId(productId + 100L);
    cycle.setProductId(productId);
    cycle.setExperimentId(productId + 200L);
    cycle.setProductVersion(version);
    cycle.setCreatedAt(Instant.parse("2026-10-10T00:00:00Z"));
    cycle.setStage("PLANNING");
    cycle.setBriefJson("{}");
    cycle.setInheritedLearningJson("{\"learning\":\"preservada\"}");
    return cycle;
  }
}
