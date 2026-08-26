package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: proteger o enfileiramento auditável da homologação do plano comercial. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanJourneyHomologationServiceTest {
  @Mock private CommercialPlanService commercialPlanService;
  @Mock private CommercialPlanVersionService versionService;
  @Mock private BusinessProcessDefinitionRepository processRepository;
  @Mock private AgentTaskService agentTaskService;

  /** Confirma que o experimento escolhido recebe uma homologação segregada e sem gasto. */
  @Test
  void requestsIsolatedJourneyHomologationForLinkedExperiment() {
    when(commercialPlanService.getPlan(2L))
        .thenReturn(
            CommercialPlan.builder()
                .id(2L)
                .successCriteria("Landing com quatro exemplos finais e três criativos aprovados")
                .stopCriteria("Parar antes de gasto ou publicação externa")
                .currentBlocker("Prova visual incompleta")
                .rootCause("Contrato sem critério observável")
                .nextAction("Dédalo itera na sandbox e Têmis revisa")
                .build());
    when(versionService.current(2L))
        .thenReturn(
            new CommercialPlanVersionDto(
                9L, 2L, 3, "{}", "teste", "versão de teste", Instant.now()));
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(18L);
    process.setProcessCode("landing-page-generation");
    when(processRepository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "landing-page-generation", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService,
            versionService,
            processRepository,
            agentTaskService,
            new ObjectMapper());

    var result = service.request(2L, 88L);

    assertThat(result.planId()).isEqualTo(2L);
    assertThat(result.experimentId()).isEqualTo(88L);
    assertThat(result.status()).isEqualTo("INICIADO");
    verify(commercialPlanService).requireExperiment(2L, 88L);
    ArgumentCaptor<CreateAgentTaskRequest> tasks =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTaskService, times(3)).createByHumanIfAbsent(tasks.capture());
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::assignedAgentKey)
        .containsExactly("landing-generator", "customer-agent", "meta-ad-approver");
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::processActivityId)
        .containsExactly("html", "customer", "commercial");
    assertThat(tasks.getAllValues())
        .allSatisfy(
            task -> {
              assertThat(task.sourceReference()).isEqualTo("commercial-plan:2@v3:journey");
              assertThat(task.processDefinitionId()).isEqualTo(18L);
              assertThat(task.title()).contains("Experimento #88");
            });
    assertThat(tasks.getAllValues().getFirst().description())
        .contains("\"mediaSpendAuthorized\":false")
        .contains("BPM_TASK_RETRY_WITH_PERSISTED_CAUSE")
        .contains("quatro exemplos finais")
        .contains("Dédalo itera na sandbox");
  }
}
