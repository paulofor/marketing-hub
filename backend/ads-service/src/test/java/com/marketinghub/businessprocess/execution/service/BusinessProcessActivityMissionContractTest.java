package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskActivityCoverageRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

/** Comprova a transmissão da missão BPM sem alterar testes atestados de produtos anteriores. */
class BusinessProcessActivityMissionContractTest {

  /** Preserva objetivo completo, responsáveis e identidade em execuções sintéticas distintas. */
  @ParameterizedTest
  @CsvSource({"901,902,903", "951,952,953"})
  void preservesCompleteVersionedMission(long processId, long productId, long experimentId)
      throws Exception {
    var mapper = new ObjectMapper();
    var contract =
        mapper
            .readTree(
                Path.of("../../infra/testing/pde-satisfaction-continuity/requirements.json")
                    .toFile())
            .path("pde-commercial-homologation-activation");
    String mission = contract.path("objectives").path("humanExperienceReview").asText();
    assertThat(mission).contains("SATISFACTION_CONTINUITY_V1", "Entregar:", "Aceite:", "Medir:");
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var activities = mock(BusinessProcessActivityDefinitionRepository.class);
    var products = mock(ProductRepository.class);
    var experiments = mock(ExperimentRepository.class);
    var agentTasks = mock(AgentTaskService.class);
    var service =
        new BusinessProcessActivityExecutionService(
            processes,
            activities,
            mock(AgentTaskRepository.class),
            mock(AgentTaskActivityCoverageRepository.class),
            mock(BusinessProcessActivityInstanceRepository.class),
            mock(CommercialPlanRepository.class),
            null,
            products,
            experiments,
            agentTasks,
            mapper);
    var process = new BusinessProcessDefinition();
    process.setId(processId);
    process.setProcessCode("pde-commercial-homologation-activation");
    process.setName("Processo sintético");
    process.setVersionNumber(contract.path("targetVersion").asInt());
    process.setStatus("PUBLISHED");
    var node =
        Map.of(
            "id", "pdeGate",
            "type", "TASK",
            "label", "Conferir missão",
            "description", mission,
            "responsibleAgentKeys", List.of("customer-agent", "meta-ad-approver"));
    process.setDiagramJson(mapper.writeValueAsString(Map.of("nodes", List.of(node))));
    var product = new Product();
    product.setId(productId);
    product.setName("Produto sintético");
    product.setInternalName("Produto sintético");
    product.setAutomaticExecutionEnabled(true);
    var experiment = new Experiment();
    experiment.setId(experimentId);
    experiment.setProduct(product);
    var activity = new BusinessProcessActivityDefinition();
    activity.setId(processId + 1000);
    activity.setProcessDefinition(process);
    activity.setActivityId("pdeGate");
    activity.setName("Conferir missão");
    activity.setObjective(mission);
    activity.setOwnerName("Revisores");
    activity.setDefinitionJson(mapper.writeValueAsString(node));
    when(processes.findById(processId)).thenReturn(Optional.of(process));
    when(products.findById(productId)).thenReturn(Optional.of(product));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(productId))
        .thenReturn(List.of(experiment));
    when(activities.findByProcessDefinitionIdAndActivityId(processId, "pdeGate"))
        .thenReturn(Optional.of(activity));
    when(agentTasks.retryBlockedByHumanOrRefreshPending(any(CreateAgentTaskRequest.class)))
        .thenReturn(mock(AgentTaskResponse.class));

    var result = service.requestProductActivityExecution(processId, productId, "pdeGate");

    assertThat(result.sourceReference()).isEqualTo("experiment:" + experimentId);
    assertThat(result.tasks()).hasSize(2);
    var requests = ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTasks, times(2)).retryBlockedByHumanOrRefreshPending(requests.capture());
    assertThat(requests.getAllValues())
        .extracting(CreateAgentTaskRequest::assignedAgentKey)
        .containsExactly("customer-agent", "meta-ad-approver");
    assertThat(requests.getAllValues())
        .allSatisfy(
            request -> {
              assertThat(request.description()).isEqualTo(mission);
              assertThat(request.sourceReference()).isEqualTo("experiment:" + experimentId);
              assertThat(request.processDefinitionId()).isEqualTo(processId);
              assertThat(request.processActivityId()).isEqualTo("pdeGate");
            });
  }
}
