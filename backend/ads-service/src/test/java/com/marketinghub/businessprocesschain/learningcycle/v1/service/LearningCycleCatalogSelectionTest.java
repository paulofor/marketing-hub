package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: impedir que a entrada do ciclo permaneça presa a um BPM aposentado. */
class LearningCycleCatalogSelectionTest {
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final LearningCycleService service =
      new LearningCycleService(
          null,
          null,
          null,
          null,
          chains,
          processes,
          null,
          new LearningCycleJson(new ObjectMapper()),
          null,
          null,
          null,
          mock(LearningCycleOrganization.class),
          null);

  /** O catálogo acompanha a publicação seguinte sem migrar ou gravar ciclos históricos. */
  @ParameterizedTest
  @ValueSource(ints = {4, 5})
  void selectsPublishedVersionWithoutFixedNumber(int version) {
    var chain = new BusinessProcessChainDefinition();
    chain.setId(14L);
    chain.setChainCode("pde");
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    var process = new BusinessProcessDefinition();
    process.setId(100L + version);
    process.setVersionNumber(version);
    process.setStatus("PUBLISHED");
    process.setDiagramJson("{}");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            LearningCycleRules.PROCESS_CODE, "PUBLISHED"))
        .thenReturn(Optional.of(process));
    var catalog = service.catalog(14L, null);
    assertThat(catalog.version()).isEqualTo(version);
    assertThat(catalog.processDefinitionId()).isEqualTo(100L + version);
    verify(processes, never()).findByProcessCodeAndVersionNumber(anyString(), anyInt());
    verify(processes, never()).save(any());
  }

  /** Ausência de versão publicada não pode reutilizar silenciosamente um BPM aposentado. */
  @Test
  void refusesCatalogWithoutPublishedVersion() {
    when(chains.findById(14L)).thenReturn(Optional.of(new BusinessProcessChainDefinition()));
    assertThatThrownBy(() -> service.catalog(14L, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("versão publicada");
  }
}
