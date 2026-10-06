package com.marketinghub.product.service.valuechainposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir que a posição comercial oculte a navegação da passagem pendente. */
class ProductLearningCycleNavigationResolverTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ProductLearningCycleNavigationResolver resolver =
      new ProductLearningCycleNavigationResolver(cycles);

  /** Reproduz a decisão histórica do Capella, o sucessor atual e uma passagem independente. */
  @ParameterizedTest
  @CsvSource({"7,26,4,88,DECISION", "7,26,5,98,ADJUSTMENT", "92001,92026,92005,92098,PLANNING"})
  void prioritizesPendingPassageWithoutWriting(
      Long productId, Long chainId, Long cycleId, Long experimentId, String stage) {
    var cycle = cycle(productId, chainId, cycleId, experimentId, stage, "OPEN");
    when(cycles.findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(productId, chainId))
        .thenReturn(Optional.of(cycle));

    var navigation = resolver.resolve(productId, chain(chainId));

    assertThat(navigation.productId()).isEqualTo(productId);
    assertThat(navigation.cycleId()).isEqualTo(cycleId);
    assertThat(navigation.experimentId()).isEqualTo(experimentId);
    assertThat(navigation.stage()).isEqualTo(stage);
    assertThat(navigation.url())
        .isEqualTo(
            "/business-process-chains/learning-cycles?productId="
                + productId
                + "&chainId="
                + chainId
                + "&cycleId="
                + cycleId);
    verify(cycles).findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(productId, chainId);
    verifyNoMoreInteractions(cycles);
  }

  /** Mantém a navegação durante preparação, operação e decisão sem interpretar autorização. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "LEARNING",
        "PLANNING",
        "ADJUSTMENT",
        "VALIDATION",
        "AUTHORIZATION",
        "PUBLICATION",
        "MEASUREMENT",
        "DECISION",
        "SCALE_AUTHORIZATION"
      })
  void supportsOpenStages(String stage) {
    when(cycles.findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(10L, 26L))
        .thenReturn(Optional.of(cycle(10L, 26L, 3L, 93L, stage, "OPEN")));
    assertThat(resolver.resolve(10L, chain(26L)).status()).isEqualTo("OPEN");
  }

  /** Preserva o ajuste do Mira e oferece a continuidade sem renovar o experimento encerrado. */
  @Test
  void exposesAdjustedPassageAwaitingSuccessor() {
    var cycle = cycle(10L, 26L, 3L, 93L, "DECISION", "ADJUSTED");
    cycle.setClosedAt(Instant.parse("2026-10-04T21:04:32Z"));
    when(cycles.findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(10L, 26L))
        .thenReturn(Optional.of(cycle));
    when(cycles.findByPreviousCycleId(3L)).thenReturn(Optional.empty());

    assertThat(resolver.resolve(10L, chain(26L)).url())
        .isEqualTo("/business-process-chains/learning-cycles?productId=10&chainId=26&cycleId=3");
    verify(cycles).findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(10L, 26L);
    verify(cycles).findByPreviousCycleId(3L);
    verifyNoMoreInteractions(cycles);
  }

  /** Impede reapresentar uma decisão histórica depois da criação de seu sucessor. */
  @Test
  void doesNotOfferAdjustedPredecessorWithSuccessor() {
    var cycle = cycle(7L, 26L, 4L, 88L, "DECISION", "ADJUSTED");
    cycle.setClosedAt(Instant.parse("2026-10-06T01:32:56Z"));
    when(cycles.findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(7L, 26L))
        .thenReturn(Optional.of(cycle));
    when(cycles.findByPreviousCycleId(4L))
        .thenReturn(Optional.of(cycle(7L, 26L, 5L, 98L, "ADJUSTMENT", "OPEN")));
    assertThat(resolver.resolve(7L, chain(26L))).isNull();
  }

  /** Conserva a navegação anterior quando a última passagem já terminou sem ajuste pendente. */
  @ParameterizedTest
  @ValueSource(strings = {"CLOSED", "INCONCLUSIVE"})
  void ignoresTerminalPassages(String status) {
    var cycle = cycle(7L, 26L, 4L, 88L, "DECISION", status);
    cycle.setClosedAt(Instant.parse("2026-10-06T01:32:56Z"));
    when(cycles.findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(7L, 26L))
        .thenReturn(Optional.of(cycle));
    assertThat(resolver.resolve(7L, chain(26L))).isNull();
    verify(cycles).findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(7L, 26L);
    verifyNoMoreInteractions(cycles);
  }

  /** Não inventa passagem nem busca outra versão da cadeia quando não existe ciclo no contexto. */
  @Test
  void preservesAbsenceOfCycle() {
    assertThat(resolver.resolve(11L, chain(26L))).isNull();
    verify(cycles).findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(11L, 26L);
    verifyNoMoreInteractions(cycles);
  }

  /** Recusa identidade divergente, inclusive uma passagem de outra versão da cadeia. */
  @ParameterizedTest
  @CsvSource({"9,26,pde-value-creation-delivery", "7,14,pde-value-creation-delivery", "7,26,other"})
  void refusesCrossedContext(Long productId, Long chainId, String chainCode) {
    var cycle = cycle(productId, chainId, 5L, 98L, "ADJUSTMENT", "OPEN");
    cycle.setChainCode(chainCode);
    when(cycles.findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(7L, 26L))
        .thenReturn(Optional.of(cycle));
    assertThat(resolver.resolve(7L, chain(26L))).isNull();
  }

  /** Produz uma cadeia com identidade própria para testar a segregação de navegação. */
  private BusinessProcessChainDefinition chain(Long id) {
    var chain = new BusinessProcessChainDefinition();
    chain.setId(id);
    chain.setChainCode("pde-value-creation-delivery");
    return chain;
  }

  /** Produz uma passagem sintética sem autorização comercial nem métricas de mercado. */
  private LearningSalesCycle cycle(
      Long productId, Long chainId, Long id, Long experimentId, String stage, String status) {
    var cycle = new LearningSalesCycle();
    cycle.setId(id);
    cycle.setProductId(productId);
    cycle.setChainDefinitionId(chainId);
    cycle.setChainCode("pde-value-creation-delivery");
    cycle.setExperimentId(experimentId);
    cycle.setStage(stage);
    cycle.setStatus(status);
    return cycle;
  }
}
