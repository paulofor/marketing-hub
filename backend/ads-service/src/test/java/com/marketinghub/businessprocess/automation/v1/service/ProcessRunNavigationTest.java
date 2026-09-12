package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger ida e retorno contextual, versões e segregação das chamadas. */
class ProcessRunNavigationTest {
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final ProcessRunRepository runs = mock(ProcessRunRepository.class);
  private final ProcessRunNavigation navigation =
      new ProcessRunNavigation(processes, chains, runs, new ObjectMapper());

  /** Mantém links após conclusão e preserva cadeia e ciclo sem carregar outro contexto. */
  @Test
  void linksBothDirectionsBeforeAndAfterCompletion() {
    var parent =
        definition(
            63L,
            "parent",
            "{\"nodes\":[{\"id\":\"creatives\",\"type\":\"TASK\",\"label\":\"Criativos\",\"subprocessCode\":\"child\"}]}");
    var child = definition(64L, "child", "{\"nodes\":[]}");
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(parent);
    chain.getItems().add(item);
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc("child", "PUBLISHED"))
        .thenReturn(Optional.of(child));
    var parentRun = run(1L, 63L);
    var childRun = run(2L, 64L);
    assertThat(navigation.children(parentRun))
        .singleElement()
        .satisfies(
            link -> {
              assertThat(link.processDefinitionId()).isEqualTo(64L);
              assertThat(link.navigationUrl())
                  .isEqualTo(
                      "/products/4/value-chain-history/processes/64/activities?chainId=14&learningCycleId=2");
            });
    assertThat(navigation.parents(childRun))
        .singleElement()
        .satisfies(
            link ->
                assertThat(link.navigationUrl())
                    .endsWith("/63/activities?chainId=14&learningCycleId=2#activity-creatives"));
    childRun.setParentRunId(1L);
    childRun.setStatus("COMPLETED");
    when(runs.findById(1L)).thenReturn(Optional.of(parentRun));
    when(runs.findAllByParentRunId(1L)).thenReturn(java.util.List.of(childRun));
    var newerChild = definition(65L, "child", "{\"nodes\":[]}");
    newerChild.setVersionNumber(8);
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc("child", "PUBLISHED"))
        .thenReturn(Optional.of(newerChild));
    assertThat(navigation.children(parentRun))
        .singleElement()
        .satisfies(link -> assertThat(link.processDefinitionId()).isEqualTo(64L));
    parent.setStatus("RETIRED");
    assertThat(navigation.parents(childRun))
        .singleElement()
        .satisfies(link -> assertThat(link.processVersion()).isEqualTo(7));
    parentRun.setProductId(10L);
    assertThatThrownBy(() -> navigation.parents(childRun)).hasMessageContaining("outro contexto");
  }

  /** Mostra somente chamadas da cadeia selecionada, inclusive quando mais de um pai é legítimo. */
  @Test
  void listsAllCallSitesAndRejectsUnrelatedParent() {
    var child = definition(64L, "child", "{\"nodes\":[]}");
    var parent =
        definition(
            63L,
            "parent",
            "{\"nodes\":[{\"id\":\"a\",\"type\":\"TASK\",\"subprocessCode\":\"child\"},{\"id\":\"b\",\"type\":\"TASK\",\"subprocessCode\":\"child\"}]}");
    var unrelated = definition(90L, "unrelated", "{\"nodes\":[]}");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc("child", "PUBLISHED"))
        .thenReturn(Optional.of(child));
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(parent);
    chain.getItems().add(item);
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    assertThat(navigation.parents(run(2L, 64L)))
        .extracting(r -> r.activityId())
        .containsExactly("a", "b");
    assertThat(navigation.parents(run(3L, 90L))).isEmpty();
  }

  /** Prepara uma definição versionada mínima do catálogo. */
  private BusinessProcessDefinition definition(Long id, String code, String diagram) {
    var d = new BusinessProcessDefinition();
    d.setId(id);
    d.setProcessCode(code);
    d.setName(code);
    d.setVersionNumber(7);
    d.setStatus("PUBLISHED");
    d.setDiagramJson(diagram);
    when(processes.findById(id)).thenReturn(Optional.of(d));
    return d;
  }

  /** Prepara identidades sintéticas iguais às usadas pelos links, sem escrita externa. */
  private ProcessRun run(Long id, Long process) {
    var r = new ProcessRun();
    r.setId(id);
    r.setProductId(4L);
    r.setProcessDefinitionId(process);
    r.setChainDefinitionId(14L);
    r.setLearningCycleId(2L);
    r.setSourceReference("experiment:92");
    return r;
  }
}
