package com.marketinghub.product.service.valuechainposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a resolução canônica do produto na cadeia de valor PDE. */
class ProductValueChainPositionServiceTest {
  /** Resolve estados comerciais atuais e códigos canônicos nas posições corretas da cadeia. */
  @Test
  void resolvesKnownStatusesInPublishedChain() {
    ProductRepository productRepository = mock(ProductRepository.class);
    BusinessProcessChainDefinitionRepository chainRepository =
        mock(BusinessProcessChainDefinitionRepository.class);
    BusinessProcessChainDefinition chain = chain();
    when(productRepository.findAll())
        .thenReturn(
            List.of(
                product(6L, "IDEIA_PRIORIZADA_PARA_TESTE"),
                product(9L, "COMUNICACAO_E_JORNADA"),
                product(4L, "VALIDACAO_COMERCIAL"),
                product(10L, "pde-sales-delivery-learning")));
    when(chainRepository.findAllByChainCodeAndStatusOrderByVersionNumberDesc(
            "pde-value-creation-delivery", "PUBLISHED"))
        .thenReturn(List.of(chain));
    var service =
        new ProductValueChainPositionService(
            productRepository, chainRepository, mock(ProductSubprocessPositionResolver.class));

    var positions = service.listPositions();

    assertThat(positions).hasSize(4);
    assertThat(positions)
        .extracting(
            ProductValueChainPositionResponse::productId,
            ProductValueChainPositionResponse::sequenceNumber,
            ProductValueChainPositionResponse::processName)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(6L, 2, "Plano Comercial e desenho da oferta PDE"),
            org.assertj.core.groups.Tuple.tuple(9L, 4, "Comunicação e jornada de venda do PDE"),
            org.assertj.core.groups.Tuple.tuple(4L, 5, "Homologação e ativação comercial do PDE"),
            org.assertj.core.groups.Tuple.tuple(10L, 6, "Venda, entrega e aprendizado do PDE"));
    assertThat(positions).allMatch(position -> "IDENTIFIED".equals(position.resolutionStatus()));
    assertThat(positions).allMatch(position -> position.processCount() == 6);
  }

  /** Mantém explícito quando um status não possui vínculo, sem inventar um processo. */
  @Test
  void reportsUnmappedStatus() {
    ProductRepository productRepository = mock(ProductRepository.class);
    BusinessProcessChainDefinitionRepository chainRepository =
        mock(BusinessProcessChainDefinitionRepository.class);
    when(productRepository.findAll()).thenReturn(List.of(product(11L, "STATUS_NOVO")));
    when(chainRepository.findAllByChainCodeAndStatusOrderByVersionNumberDesc(
            "pde-value-creation-delivery", "PUBLISHED"))
        .thenReturn(List.of(chain()));
    var service =
        new ProductValueChainPositionService(
            productRepository, chainRepository, mock(ProductSubprocessPositionResolver.class));

    var position = service.listPositions().getFirst();

    assertThat(position.resolutionStatus()).isEqualTo("NOT_IDENTIFIED");
    assertThat(position.processDefinitionId()).isNull();
    assertThat(position.resolutionMessage()).contains("sem vínculo");
  }

  /** Informa a indisponibilidade quando nenhuma cadeia PDE está publicada. */
  @Test
  void reportsUnavailableChain() {
    ProductRepository productRepository = mock(ProductRepository.class);
    BusinessProcessChainDefinitionRepository chainRepository =
        mock(BusinessProcessChainDefinitionRepository.class);
    when(productRepository.findAll()).thenReturn(List.of(product(9L, "COMUNICACAO_E_JORNADA")));
    when(chainRepository.findAllByChainCodeAndStatusOrderByVersionNumberDesc(
            "pde-value-creation-delivery", "PUBLISHED"))
        .thenReturn(List.of());
    var service =
        new ProductValueChainPositionService(
            productRepository, chainRepository, mock(ProductSubprocessPositionResolver.class));

    var position = service.listPositions().getFirst();

    assertThat(position.resolutionStatus()).isEqualTo("CHAIN_UNAVAILABLE");
    assertThat(position.chainDefinitionId()).isNull();
    assertThat(position.resolutionMessage()).contains("não encontrada");
  }

  /** Monta um produto enxuto para representar um estado comercial. */
  private Product product(Long id, String status) {
    return Product.builder().id(id).commercialStatus(status).build();
  }

  /** Monta a cadeia PDE publicada com os seis processos de valor vigentes. */
  private BusinessProcessChainDefinition chain() {
    BusinessProcessChainDefinition chain = new BusinessProcessChainDefinition();
    chain.setId(5L);
    chain.setChainCode("pde-value-creation-delivery");
    chain.setName("Criação e entrega de valor de Produtos Digitais Experienciais");
    chain.setVersionNumber(5);
    chain.setStatus("PUBLISHED");
    chain.setItems(
        List.of(
            item(chain, 37L, "pde-opportunity-discovery", "Descoberta e priorização", 1),
            item(
                chain,
                38L,
                "pde-commercial-plan-offer",
                "Plano Comercial e desenho da oferta PDE",
                2),
            item(chain, 39L, "pde-construction-approval", "Construção e aprovação do PDE", 3),
            item(
                chain,
                43L,
                "pde-communication-sales-journey",
                "Comunicação e jornada de venda do PDE",
                4),
            item(
                chain,
                45L,
                "pde-commercial-homologation-activation",
                "Homologação e ativação comercial do PDE",
                5),
            item(
                chain,
                46L,
                "pde-sales-delivery-learning",
                "Venda, entrega e aprendizado do PDE",
                6)));
    return chain;
  }

  /** Vincula uma definição enxuta à posição informada da cadeia de teste. */
  private BusinessProcessChainItem item(
      BusinessProcessChainDefinition chain,
      Long id,
      String processCode,
      String name,
      int sequenceNumber) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode(processCode);
    process.setName(name);
    process.setVersionNumber(4);
    process.setStatus("PUBLISHED");
    BusinessProcessChainItem item = new BusinessProcessChainItem();
    item.setChainDefinition(chain);
    item.setProcessDefinition(process);
    item.setSequenceNumber(sequenceNumber);
    item.setValueContribution("Contribuição " + sequenceNumber);
    item.setCreatedAt(Instant.parse("2026-08-23T02:00:00Z"));
    return item;
  }
}
