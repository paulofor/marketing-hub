package com.marketinghub.product.service.valuechainposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.product.Product;
import com.marketinghub.product.ProductProcessPeriod;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductProcessPeriodRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

/** Responsabilidade: reproduzir no JPA a troca do único período aberto de um produto. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
@Sql(
    statements =
        "ALTER TABLE product_process_period ADD CONSTRAINT uk_product_process_period_open UNIQUE (product_id, open_slot)")
class ProductProcessPeriodTransitionRepositoryTest {
  private static final Instant TRANSITION_AT = Instant.parse("2026-08-26T16:00:00Z");

  @Autowired private ProductRepository productRepository;
  @Autowired private BusinessProcessDefinitionRepository processRepository;
  @Autowired private ProductProcessPeriodRepository periodRepository;

  /** Fecha fisicamente o período vigente antes de inserir a nova posição comercial. */
  @Test
  void replacesOpenPeriodWithoutViolatingUniqueSlot() {
    Product product = new Product();
    product.setName("Vega");
    product.setCommercialStatus("VALIDACAO_COMERCIAL");
    product = productRepository.saveAndFlush(product);

    BusinessProcessDefinition homologation =
        processRepository.saveAndFlush(
            process("pde-commercial-homologation-activation", "Homologação"));
    BusinessProcessDefinition sales =
        processRepository.saveAndFlush(
            process("pde-sales-delivery-learning", "Venda e aprendizado"));
    ProductProcessPeriod previous = openPeriod(product, homologation);
    periodRepository.saveAndFlush(previous);

    BusinessProcessChainDefinition chain = chain(homologation, sales);
    BusinessProcessChainDefinitionRepository chainRepository =
        mock(BusinessProcessChainDefinitionRepository.class);
    when(chainRepository.findAllByChainCodeAndStatusOrderByVersionNumberDesc(
            "pde-value-creation-delivery", "PUBLISHED"))
        .thenReturn(List.of(chain));
    ProductProcessPeriodService service =
        new ProductProcessPeriodService(
            chainRepository,
            periodRepository,
            new PdeProcessCodeResolver(),
            Clock.fixed(TRANSITION_AT, ZoneOffset.UTC));

    product.setCommercialStatus("ATIVO");
    service.recordTransition(product, "VALIDACAO_COMERCIAL");
    periodRepository.flush();

    List<ProductProcessPeriod> periods =
        periodRepository.findByProductIdOrderByEnteredAtAscIdAsc(product.getId());
    assertThat(periods).hasSize(2);
    assertThat(periods.get(0).getExitedAt()).isEqualTo(TRANSITION_AT);
    assertThat(periods.get(0).getOpenSlot()).isNull();
    assertThat(periods.get(0).isObjectiveAchieved()).isTrue();
    assertThat(periods.get(1).getProcessCodeSnapshot())
        .isEqualTo("pde-sales-delivery-learning");
    assertThat(periods.get(1).getEnteredAt()).isEqualTo(TRANSITION_AT);
    assertThat(periods.get(1).getExitedAt()).isNull();
    assertThat(periods.get(1).getOpenSlot()).isEqualTo(1);
  }

  /** Monta uma definição já identificada como se viesse da cadeia publicada. */
  private BusinessProcessDefinition process(String code, String name) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode(code);
    process.setName(name);
    process.setPurpose("Mover o produto na cadeia de valor.");
    process.setOwnerName("Backend");
    process.setTriggerDescription("Estado comercial alterado.");
    process.setOutcomeDescription("Posição comercial persistida.");
    process.setVersionNumber(5);
    process.setStatus("PUBLISHED");
    process.setDiagramJson("{\"nodes\":[],\"flows\":[]}");
    process.setCreatedAt(Instant.parse("2026-08-25T08:00:00Z"));
    return process;
  }

  /** Monta a cadeia mínima com a posição anterior e a posição seguinte. */
  private BusinessProcessChainDefinition chain(
      BusinessProcessDefinition homologation, BusinessProcessDefinition sales) {
    BusinessProcessChainDefinition chain = new BusinessProcessChainDefinition();
    chain.setChainCode("pde-value-creation-delivery");
    chain.setVersionNumber(5);
    chain.setStatus("PUBLISHED");
    chain.setItems(List.of(item(chain, homologation, 5), item(chain, sales, 6)));
    return chain;
  }

  /** Monta um item da cadeia com a ordem comercial informada. */
  private BusinessProcessChainItem item(
      BusinessProcessChainDefinition chain,
      BusinessProcessDefinition process,
      int sequenceNumber) {
    BusinessProcessChainItem item = new BusinessProcessChainItem();
    item.setChainDefinition(chain);
    item.setProcessDefinition(process);
    item.setSequenceNumber(sequenceNumber);
    return item;
  }

  /** Monta o período aberto que representa a homologação vigente. */
  private ProductProcessPeriod openPeriod(
      Product product, BusinessProcessDefinition processDefinition) {
    Instant enteredAt = Instant.parse("2026-08-25T08:08:07Z");
    ProductProcessPeriod period = new ProductProcessPeriod();
    period.setProduct(product);
    period.setProcessDefinition(processDefinition);
    period.setProcessCodeSnapshot(processDefinition.getProcessCode());
    period.setProcessNameSnapshot(processDefinition.getName());
    period.setSequenceNumber(5);
    period.setEnteredAt(enteredAt);
    period.setEntryEvidence("BACKFILLED_PRODUCT_UPDATE");
    period.setObjectiveAchieved(false);
    period.setOpenSlot(1);
    period.setCreatedAt(enteredAt);
    period.setUpdatedAt(enteredAt);
    return period;
  }
}
