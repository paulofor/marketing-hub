package com.marketinghub.product.executionprofile.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.product.Product;
import com.marketinghub.product.executionprofile.v1.*;
import com.marketinghub.product.executionprofile.v1.service.saveprofile.ProfileContract.Capability;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.productexecution.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: provar bloqueios financeiros antes da chamada e preservação de custos após
 * falhas.
 */
class ExecutionProfileBudgetTest {
  private final ProductRepository products = mock(ProductRepository.class);
  private final ExecutionProfileRepository profiles = mock(ExecutionProfileRepository.class);
  private final ExecutionProfileBindingRepository bindings =
      mock(ExecutionProfileBindingRepository.class);
  private final ExecutionProfileConsumptionRepository consumption =
      mock(ExecutionProfileConsumptionRepository.class);
  private final ExecutionProfileContext context = mock(ExecutionProfileContext.class);
  private final ExecutionProfileBudget budget =
      new ExecutionProfileBudget(products, profiles, bindings, consumption, context);
  private final List<ExecutionProfileConsumption> ledger = new ArrayList<>();

  /** Prepara fontes segregadas e mantém o ledger simulado entre as chamadas da unidade. */
  @BeforeEach
  void setup() {
    var profile = new ExecutionProfile();
    profile.setId(1L);
    profile.setProductId(7L);
    var binding = new ExecutionProfileBinding();
    binding.setId(2L);
    binding.setProfileId(1L);
    binding.setProductId(7L);
    when(products.findLockedById(7L)).thenReturn(Optional.of(new Product()));
    when(bindings.findByProductIdAndSourceReference(7L, "experiment:99"))
        .thenReturn(Optional.of(binding));
    when(bindings.findById(2L)).thenReturn(Optional.of(binding));
    when(profiles.findByIdAndProductId(1L, 7L)).thenReturn(Optional.of(profile));
    when(context.contract(profile))
        .thenReturn(ExecutionProfileRulesTest.contract(Capability.PERSONALIZED_IMAGES));
    when(consumption.findByBindingIdAndTestData(2L, true)).thenAnswer(i -> List.copyOf(ledger));
    when(consumption.findByBindingIdAndOperationKey(eq(2L), anyString()))
        .thenAnswer(
            i ->
                ledger.stream()
                    .filter(e -> e.getOperationKey().equals(i.getArgument(1)))
                    .findFirst());
    when(consumption.findById(anyLong()))
        .thenAnswer(
            i -> ledger.stream().filter(e -> e.getId().equals(i.getArgument(0))).findFirst());
    when(consumption.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              var entry = (ExecutionProfileConsumption) i.getArgument(0);
              if (entry.getId() == null) {
                entry.setId((long) ledger.size() + 1);
                ledger.add(entry);
              }
              return entry;
            });
  }

  /** Usa a reserva da tentativa inteira e impede extrapolar as quotas mesmo com custo menor. */
  @Test
  void enforcesPackageCeilingAndAttemptLimit() {
    Long first = reserve("one", 2);
    budget.settle(7L, first, BigDecimal.ONE, "provider:one", false);
    reserve("two", 2);
    assertThatThrownBy(() -> reserve("three", 1)).hasMessageContaining("limite");
    assertThat(ledger.getFirst().getReservedBrl()).isEqualByComparingTo("4");
  }

  /** Repetir a mesma chave nunca autoriza outra chamada externa. */
  @Test
  void rejectsReplayAndChangedInput() {
    reserve("same", 1);
    assertThatThrownBy(() -> reserve("same", 1)).hasMessageContaining("já foi reservada");
    assertThatThrownBy(() -> reserve("same", 2)).hasMessageContaining("outra entrada");
    assertThat(ledger).hasSize(1);
  }

  /** Custo ausente mantém o teto ocupado e bloqueia nova geração até conciliação. */
  @Test
  void unknownCostDoesNotBecomeZero() {
    Long id = reserve("unknown", 1);
    budget.settle(7L, id, null, "provider:timeout", true);
    assertThat(ledger.getFirst().getActualBrl()).isNull();
    assertThat(ledger.getFirst().getStatus()).isEqualTo("COST_PENDING");
    assertThatThrownBy(() -> reserve("next", 1)).hasMessageContaining("custo desconhecido");
    budget.settle(7L, id, BigDecimal.ONE, "provider:invoice", true);
    assertThat(ledger.getFirst().getStatus()).isEqualTo("FAILED_CHARGED");
    reserve("after-reconciliation", 1);
  }

  /** Sobrecusto real é preservado e impede novas chamadas sem apagar a perda. */
  @Test
  void overrunPreservesActualCostAndBlocks() {
    Long id = reserve("over", 1);
    budget.settle(7L, id, BigDecimal.TEN, "provider:invoice", false);
    assertThat(ledger.getFirst().getActualBrl()).isEqualByComparingTo("10");
    assertThatThrownBy(() -> reserve("next", 1)).hasMessageContaining("acima da reserva");
  }

  /** Produtos adotantes não conseguem voltar ao caminho sem ficha omitindo o contexto. */
  @Test
  void missingBindingCannotBypassAdoptedPolicy() {
    when(bindings.existsByProductId(7L)).thenReturn(true);
    assertThatThrownBy(() -> budget.reserve(7L, null, "package", "x", "input", "model", 1, true))
        .hasMessageContaining("referência vinculada");
    verify(consumption, never()).saveAndFlush(any());
  }

  /** Modelo diferente ou checkpoint negado bloqueiam antes de consumir a reserva. */
  @Test
  void rejectsModelDriftAndFinancialGate() {
    assertThatThrownBy(
            () ->
                budget.reserve(
                    7L, "experiment:99", "package", "x", "input", "changed-model", 1, true))
        .hasMessageContaining("modelo mudou");
    when(context.financialBlocker(any(), eq("DELIVERY_DESIGN"))).thenReturn("Plutus pendente");
    assertThatThrownBy(() -> reserve("y", 1)).hasMessageContaining("Plutus pendente");
  }

  /** Centraliza somente a chamada com os dados sintéticos deste teste. */
  private Long reserve(String key, int units) {
    return budget.reserve(
        7L, "experiment:99", "package", key, "input", "gpt-image-2.5-sunburst", units, true);
  }
}
