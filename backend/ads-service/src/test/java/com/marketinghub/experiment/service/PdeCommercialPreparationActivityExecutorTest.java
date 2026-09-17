package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar o roteamento comercial determinístico pelo tipo cadastrado. */
class PdeCommercialPreparationActivityExecutorTest {
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final PdeCommercialPreparationActivityExecutor executor =
      new PdeCommercialPreparationActivityExecutor(processes, cycles, new ObjectMapper());

  /** Seleciona a versão exata do subprocesso Opala e preserva o contexto na navegação. */
  @Test
  void routesRegisteredTypeToExactPublishedSubprocess() {
    Product product = product("PDE");
    BusinessProcessDefinition target = process(77L, "opala-commercial-preparation-v1", 1);
    LearningSalesCycle cycle = cycle();
    when(processes.findByProcessCodeAndVersionNumber("opala-commercial-preparation-v1", 1))
        .thenReturn(Optional.of(target));
    when(cycles.findByExperimentId(92L)).thenReturn(Optional.of(cycle));

    var result =
        executor.readiness(
            process(56L, "pde-commercial-homologation-activation", 7),
            activity(),
            product,
            "experiment:92");

    assertThat(result.ready()).isTrue();
    assertThat(result.targetProcessDefinitionId()).isEqualTo(77L);
    assertThat(result.navigationUrl())
        .isEqualTo(
            "/products/4/value-chain-history/processes/77/activities?chainId=16&learningCycleId=2");
  }

  /** Bloqueia um tipo sem percurso em vez de enviá-lo silenciosamente ao subprocesso Opala. */
  @Test
  void blocksUnconfiguredProductType() {
    var result =
        executor.readiness(
            process(56L, "pde-commercial-homologation-activation", 7),
            activity(),
            product("QUARTZ"),
            "experiment:92");

    assertThat(result.ready()).isFalse();
    assertThat(result.reason()).contains("QUARTZ", "não possui percurso");
  }

  /** Cria o contrato versionado mínimo usado pelas duas variações de produto. */
  private BusinessProcessActivityDefinition activity() {
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("commercialPreparation");
    activity.setDefinitionJson(
        "{\"commercialPreparationRouterVersion\":\"COMMERCIAL_PREPARATION_BY_PRODUCT_TYPE_V1\",\"subprocessRoutes\":[{\"productTypeCode\":\"PDE\",\"productTypeInternalName\":\"Opala\",\"subprocessCode\":\"opala-commercial-preparation-v1\",\"subprocessVersion\":1}]}");
    return activity;
  }

  /** Monta um produto com a classificação oficial, sem inferir pelo nome ou formato. */
  private Product product(String typeCode) {
    return Product.builder()
        .id(4L)
        .productTypeDefinition(ProductTypeDefinition.builder().code(typeCode).build())
        .build();
  }

  /** Monta a versão publicada exata exigida pela rota. */
  private BusinessProcessDefinition process(Long id, String code, int version) {
    var process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode(code);
    process.setName(code);
    process.setVersionNumber(version);
    process.setStatus("PUBLISHED");
    return process;
  }

  /** Monta o ciclo que fixa produto, cadeia e experimento usados no link. */
  private LearningSalesCycle cycle() {
    var cycle = new LearningSalesCycle();
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setChainDefinitionId(16L);
    cycle.setExperimentId(92L);
    return cycle;
  }
}
