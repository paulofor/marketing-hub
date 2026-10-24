package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: preservar o contexto original ao consultar uma nova versão da mesma cadeia. */
class LearningCyclePublishedChainContextTest {
  /**
   * Reproduz a navegação de Capella e de outro produto, mantendo isolamento e consultas históricas.
   */
  @ParameterizedTest
  @ValueSource(longs = {7, 92007})
  void publishedFamilyKeepsOriginalCycleAndHistoricalQueriesStayExact(long productId) {
    var cycles = mock(LearningSalesCycleRepository.class);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var products = mock(ProductRepository.class);
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var json = new LearningCycleJson(new ObjectMapper());
    var service =
        new LearningCycleService(
            cycles, null, products, null, chains, processes, null, json, null, null, null, null,
            null);
    var work = mock(LearningCycleWorkResolver.class);
    ReflectionTestUtils.setField(service, "workResolver", work);
    ReflectionTestUtils.setField(
        service,
        "executionContext",
        new LearningCycleExecutionContext(cycles, chains, processes, json));
    var product = Product.builder().id(productId).build();
    var process = new BusinessProcessDefinition();
    process.setId(119L);
    process.setProcessCode("pde-sales-delivery-learning");
    var original = new BusinessProcessChainDefinition();
    original.setId(26L);
    original.setChainCode("PDE");
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(process);
    original.getItems().add(item);
    var published = new BusinessProcessChainDefinition();
    published.setId(27L);
    published.setChainCode("PDE");
    published.setStatus("PUBLISHED");
    var cycle = new LearningSalesCycle();
    cycle.setId(12L);
    cycle.setProductId(productId);
    cycle.setExperimentId(productId + 98);
    cycle.setChainDefinitionId(26L);
    cycle.setChainCode("PDE");
    cycle.setStatus("OPEN");
    cycle.setStage("PUBLICATION");
    cycle.setProductVersion("v3");
    cycle.setBriefJson("{}");
    cycle.setInheritedLearningJson("{}");
    when(products.findById(productId)).thenReturn(Optional.of(product));
    when(processes.findById(119L)).thenReturn(Optional.of(process));
    when(chains.findById(26L)).thenReturn(Optional.of(original));
    when(chains.findById(27L)).thenReturn(Optional.of(published));
    when(cycles.findByProductIdAndOpenSlot(productId, 1)).thenReturn(List.of(cycle));
    when(cycles.findById(12L)).thenReturn(Optional.of(cycle));
    var context = service.processContext(productId, 119L, null, 27L);
    assertThat(context.cycleId()).isEqualTo(12L);
    assertThat(context.chainDefinitionId()).isEqualTo(26L);
    assertThat(context.experimentId()).isEqualTo(productId + 98);
    published.setStatus("ARCHIVED");
    assertThat(service.processContext(productId, 119L, null, 27L)).isNull();
    published.setStatus("PUBLISHED");
    published.setChainCode("OTHER");
    assertThat(service.processContext(productId, 119L, null, 27L)).isNull();
    published.setChainCode("PDE");
    cycle.setProductId(productId + 1);
    assertThat(service.processContext(productId, 119L, null, 27L)).isNull();
    verify(cycles, never()).save(any());
    verify(cycles, never()).saveAndFlush(any());
  }
}
