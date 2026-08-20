package com.marketinghub.businessprocessresource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocessresource.service.BusinessProcessExecutionResourceService;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a leitura oficial dos recursos ativos de atividades. */
class BusinessProcessExecutionResourceServiceTest {

  /** Expõe somente os recursos ativos retornados pelo repository canônico. */
  @Test
  void listsActiveResourcesWithExecutorInstructions() {
    var repository = mock(BusinessProcessExecutionResourceRepository.class);
    var studio = studio();
    when(repository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(studio));

    var result = new BusinessProcessExecutionResourceService(repository).listResources();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().resourceCode()).isEqualTo("themis-image-studio");
    assertThat(result.getFirst().executorReference()).isEqualTo("themis-image-studio");
    assertThat(result.getFirst().usageInstructions()).contains("pending");
  }

  /** Monta o recurso visual canônico usado no contrato de leitura. */
  private BusinessProcessExecutionResource studio() {
    BusinessProcessExecutionResource resource = new BusinessProcessExecutionResource();
    resource.setId(1L);
    resource.setResourceCode("themis-image-studio");
    resource.setName("Estúdio de Imagens de Têmis");
    resource.setDescription("Cria e edita imagens.");
    resource.setResourceType("CONTAINER");
    resource.setResponsibleAgentKey("meta-ad-approver");
    resource.setExecutorReference("themis-image-studio");
    resource.setUsageInstructions("Consumir o endpoint pending do backend.");
    resource.setActive(true);
    return resource;
  }
}
