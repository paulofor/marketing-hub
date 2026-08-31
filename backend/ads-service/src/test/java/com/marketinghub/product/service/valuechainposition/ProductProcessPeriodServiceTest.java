package com.marketinghub.product.service.valuechainposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.product.Product;
import com.marketinghub.product.ProductProcessPeriod;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductProcessPeriodRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** Responsabilidade: comprovar o registro auditável das transições de macroprocesso. */
class ProductProcessPeriodServiceTest {
  private final BusinessProcessChainDefinitionRepository chainRepository =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final ProductProcessPeriodRepository periodRepository =
      mock(ProductProcessPeriodRepository.class);
  private final Instant now = Instant.parse("2026-08-25T12:00:00Z");
  private final ProductProcessPeriodService service =
      new ProductProcessPeriodService(
          chainRepository,
          periodRepository,
          new PdeProcessCodeResolver(),
          Clock.fixed(now, ZoneOffset.UTC));

  /** Efetiva o fechamento antes da abertura para preservar o slot único do produto. */
  @Test
  void closesPreviousBeforeOpeningCurrentProcess() {
    Product product = Product.builder().id(9L).commercialStatus("VALIDACAO_COMERCIAL").build();
    BusinessProcessChainDefinition chain = chain();
    ProductProcessPeriod previous = new ProductProcessPeriod();
    previous.setId(31L);
    previous.setProduct(product);
    previous.setEnteredAt(Instant.parse("2026-08-20T12:00:00Z"));
    previous.setUpdatedAt(previous.getEnteredAt());
    when(chainRepository.findAllByChainCodeAndStatusOrderByVersionNumberDesc(
            "pde-value-creation-delivery", "PUBLISHED"))
        .thenReturn(List.of(chain));
    when(periodRepository.findTopByProductIdAndExitedAtIsNullOrderByEnteredAtDescIdDesc(9L))
        .thenReturn(Optional.of(previous));

    service.recordTransition(product, "COMUNICACAO_E_JORNADA");

    assertThat(previous.getExitedAt()).isEqualTo(now);
    assertThat(previous.getExitEvidence()).isEqualTo("COMMERCIAL_STATUS_TRANSITION");
    assertThat(previous.isObjectiveAchieved()).isTrue();
    assertThat(previous.getOpenSlot()).isNull();
    ArgumentCaptor<ProductProcessPeriod> openedPeriod =
        ArgumentCaptor.forClass(ProductProcessPeriod.class);
    InOrder persistenceOrder = inOrder(periodRepository);
    persistenceOrder.verify(periodRepository).saveAndFlush(previous);
    persistenceOrder.verify(periodRepository).save(openedPeriod.capture());
    ProductProcessPeriod current = openedPeriod.getValue();
    assertThat(current.getProcessCodeSnapshot())
        .isEqualTo("pde-commercial-homologation-activation");
    assertThat(current.getEnteredAt()).isEqualTo(now);
    assertThat(current.isObjectiveAchieved()).isFalse();
    assertThat(current.getOpenSlot()).isEqualTo(1);
  }

  /** Não cria outro período quando uma edição mantém o mesmo macroprocesso. */
  @Test
  void ignoresEditsWithoutCommercialProcessTransition() {
    Product product = Product.builder().id(9L).commercialStatus("COMUNICACAO_E_JORNADA").build();
    when(chainRepository.findAllByChainCodeAndStatusOrderByVersionNumberDesc(
            "pde-value-creation-delivery", "PUBLISHED"))
        .thenReturn(List.of(chain()));

    service.recordTransition(product, "COMUNICACAO_E_JORNADA_DE_VENDA");

    verify(periodRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
  }

  /** Registra explicitamente quando um preflight posterior reconciliou o avanço comercial. */
  @Test
  void preservesAuditedPreflightAsTransitionEvidence() {
    Product product = Product.builder().id(9L).commercialStatus("ATIVO").build();
    ProductProcessPeriod previous = new ProductProcessPeriod();
    previous.setId(32L);
    previous.setProduct(product);
    previous.setEnteredAt(Instant.parse("2026-08-20T12:00:00Z"));
    previous.setUpdatedAt(previous.getEnteredAt());
    when(chainRepository.findAllByChainCodeAndStatusOrderByVersionNumberDesc(
            "pde-value-creation-delivery", "PUBLISHED"))
        .thenReturn(List.of(chain()));
    when(periodRepository.findTopByProductIdAndExitedAtIsNullOrderByEnteredAtDescIdDesc(9L))
        .thenReturn(Optional.of(previous));

    service.recordAuditedPreflightTransition(product, "COMUNICACAO_E_JORNADA");

    assertThat(previous.getExitEvidence()).isEqualTo("AUDITED_PRODUCTION_PREFLIGHT");
    assertThat(previous.isObjectiveAchieved()).isTrue();
    ArgumentCaptor<ProductProcessPeriod> openedPeriod =
        ArgumentCaptor.forClass(ProductProcessPeriod.class);
    verify(periodRepository).save(openedPeriod.capture());
    assertThat(openedPeriod.getValue().getProcessCodeSnapshot())
        .isEqualTo("pde-sales-delivery-learning");
    assertThat(openedPeriod.getValue().getEntryEvidence())
        .isEqualTo("AUDITED_PRODUCTION_PREFLIGHT");
  }

  /** Monta a cadeia mínima com os processos de comunicação e homologação. */
  private BusinessProcessChainDefinition chain() {
    BusinessProcessChainDefinition chain = new BusinessProcessChainDefinition();
    chain.setId(5L);
    chain.setChainCode("pde-value-creation-delivery");
    chain.setVersionNumber(5);
    chain.setStatus("PUBLISHED");
    chain.setItems(
        List.of(
            item(chain, 43L, "pde-communication-sales-journey", "Comunicação", 4),
            item(chain, 44L, "pde-commercial-homologation-activation", "Homologação", 5),
            item(chain, 45L, "pde-sales-delivery-learning", "Venda e entrega", 6)));
    return chain;
  }

  /** Monta um item canônico da cadeia para o cenário de transição. */
  private BusinessProcessChainItem item(
      BusinessProcessChainDefinition chain,
      Long processId,
      String processCode,
      String processName,
      int sequence) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(processId);
    process.setProcessCode(processCode);
    process.setName(processName);
    BusinessProcessChainItem item = new BusinessProcessChainItem();
    item.setChainDefinition(chain);
    item.setProcessDefinition(process);
    item.setSequenceNumber(sequence);
    return item;
  }
}
