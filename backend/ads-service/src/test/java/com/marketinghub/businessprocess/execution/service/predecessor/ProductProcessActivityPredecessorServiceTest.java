package com.marketinghub.businessprocess.execution.service.predecessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: comprovar que comandos manuais e backend respeitam a ordem persistida do BPM.
 */
class ProductProcessActivityPredecessorServiceTest {
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ProductProcessActivityPredecessorService service =
      new ProductProcessActivityPredecessorService(tasks, instances, new ObjectMapper());

  /** Libera pela instância concluída sem reler tarefas ou seus prompts históricos. */
  @Test
  void allowsActivityAfterCompletedPredecessor() {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition review = activity(process, 1L, "review");
    BusinessProcessActivityDefinition approval = activity(process, 2L, "approval");
    BusinessProcessActivityInstance completed = new BusinessProcessActivityInstance();
    completed.setActivityDefinition(review);
    completed.setOccurrenceNumber(1);
    completed.setStatus("COMPLETED");
    completed.setObjectiveAchieved(true);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                56L, "experiment:89"))
        .thenReturn(List.of(completed));
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            56L, "experiment:89"))
        .thenReturn(List.of());

    ProductProcessActivityPredecessorReadiness result =
        service.readiness(process, approval, "experiment:89");

    assertThat(result.ready()).isTrue();
    assertThat(result.reason()).contains("conclusão comprovada");
    verifyNoInteractions(tasks);
  }

  /** Mantém o bloqueio da instância pendente sem reler tarefas ou seus prompts históricos. */
  @Test
  void blocksActivityWhilePredecessorIsPending() {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition review = activity(process, 1L, "review");
    BusinessProcessActivityDefinition approval = activity(process, 2L, "approval");
    BusinessProcessActivityInstance pending = new BusinessProcessActivityInstance();
    pending.setActivityDefinition(review);
    pending.setOccurrenceNumber(1);
    pending.setStatus("PENDING");
    pending.setObjectiveAchieved(false);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                56L, "experiment:89"))
        .thenReturn(List.of(pending));
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            56L, "experiment:89"))
        .thenReturn(List.of());

    ProductProcessActivityPredecessorReadiness result =
        service.readiness(process, approval, "experiment:89");

    assertThat(result.ready()).isFalse();
    assertThat(result.reason()).contains("Revisar evidências");
    verifyNoInteractions(tasks);
  }

  /** Ignora setas visuais de retrabalho para não transformar a correção em ciclo predecessor. */
  @Test
  void ignoresReworkFlowWhenCalculatingOperationalPredecessors() {
    BusinessProcessDefinition process = process();
    process.setDiagramJson(
        """
        {
          "nodes":[
            {"id":"start","type":"START","label":"Início"},
            {"id":"technical","type":"TASK","label":"Homologar"},
            {"id":"correction","type":"TASK","label":"Corrigir"}
          ],
          "flows":[
            {"from":"start","to":"technical"},
            {"from":"technical","to":"correction","kind":"REWORK"},
            {"from":"correction","to":"technical","kind":"REWORK"}
          ]
        }
        """);
    BusinessProcessActivityDefinition technical = activity(process, 1L, "technical");
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                56L, "product:10@agent-validation-v1"))
        .thenReturn(List.of());
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            56L, "product:10@agent-validation-v1"))
        .thenReturn(List.of());

    ProductProcessActivityPredecessorReadiness result =
        service.readiness(process, technical, "product:10@agent-validation-v1");

    assertThat(result.ready()).isTrue();
    assertThat(result.reason()).contains("primeiro trabalho executável");
  }

  /** Monta um fluxo com gateway para validar a travessia entre tarefas. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(56L);
    process.setDiagramJson(
        """
        {
          "nodes":[
            {"id":"start","type":"START","label":"Início"},
            {"id":"review","type":"TASK","label":"Revisar evidências"},
            {"id":"gate","type":"GATEWAY","label":"Evidências aprovadas?"},
            {"id":"approval","type":"TASK","label":"Autorizar operação"}
          ],
          "flows":[
            {"from":"start","to":"review"},
            {"from":"review","to":"gate"},
            {"from":"gate","to":"approval"}
          ]
        }
        """);
    return process;
  }

  /** Monta a definição relacional usada para relacionar instância e diagrama. */
  private BusinessProcessActivityDefinition activity(
      BusinessProcessDefinition process, Long id, String activityId) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(id);
    activity.setProcessDefinition(process);
    activity.setActivityId(activityId);
    return activity;
  }
}
