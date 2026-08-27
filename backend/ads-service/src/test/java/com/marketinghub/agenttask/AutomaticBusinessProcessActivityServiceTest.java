package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: validar a materialização idempotente de atividades automáticas no BPM. */
class AutomaticBusinessProcessActivityServiceTest {
  private AgentTaskRepository taskRepository;
  private BusinessProcessActivityDefinitionRepository activityDefinitionRepository;
  private BusinessProcessActivityInstanceRepository activityInstanceRepository;
  private AutomaticBusinessProcessActivityService service;
  private AgentTask anchorTask;
  private BusinessProcessActivityDefinition technicalActivity;

  /** Prepara uma tarefa e uma atividade pertencentes à mesma versão publicada do processo. */
  @BeforeEach
  void setUp() {
    taskRepository = org.mockito.Mockito.mock(AgentTaskRepository.class);
    activityDefinitionRepository =
        org.mockito.Mockito.mock(BusinessProcessActivityDefinitionRepository.class);
    activityInstanceRepository =
        org.mockito.Mockito.mock(BusinessProcessActivityInstanceRepository.class);
    service =
        new AutomaticBusinessProcessActivityService(
            taskRepository,
            activityDefinitionRepository,
            activityInstanceRepository,
            new ObjectMapper());
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(18L);
    anchorTask = new AgentTask();
    anchorTask.setId(243L);
    anchorTask.setProcessDefinition(process);
    anchorTask.setSourceReference("commercial-plan:4@v3:journey");
    technicalActivity = new BusinessProcessActivityDefinition();
    technicalActivity.setId(123L);
    technicalActivity.setProcessDefinition(process);
    technicalActivity.setActivityId("technical");
    when(taskRepository.findById(243L)).thenReturn(Optional.of(anchorTask));
    when(activityDefinitionRepository.findByProcessDefinitionIdAndActivityId(18L, "technical"))
        .thenReturn(Optional.of(technicalActivity));
  }

  /** Deve registrar datas, custo e evidência real da execução técnica aprovada. */
  @Test
  void completesAutomaticActivityFromTechnicalExecution() throws Exception {
    Instant startedAt = Instant.parse("2026-08-27T03:35:14.887Z");
    Instant completedAt = Instant.parse("2026-08-27T03:36:55.031Z");
    when(activityInstanceRepository
            .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                123L, "commercial-plan:4@v3:journey"))
        .thenReturn(Optional.empty());

    service.completeFromExecution(
        243L,
        "technical",
        "e1efeb2c-781e-46ce-8e6f-b1b8ffba8c88",
        startedAt,
        completedAt,
        new BigDecimal("0.146645"),
        "{\"experimentId\":89,\"qualityReview\":{\"score\":90}}");

    ArgumentCaptor<BusinessProcessActivityInstance> saved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(activityInstanceRepository).save(saved.capture());
    BusinessProcessActivityInstance instance = saved.getValue();
    assertThat(instance.getActivityDefinition()).isSameAs(technicalActivity);
    assertThat(instance.getSourceReference()).isEqualTo("commercial-plan:4@v3:journey");
    assertThat(instance.getOccurrenceNumber()).isEqualTo(1);
    assertThat(instance.getStatus()).isEqualTo("COMPLETED");
    assertThat(instance.getEnteredAt()).isEqualTo(startedAt);
    assertThat(instance.getExitedAt()).isEqualTo(completedAt);
    assertThat(instance.getKnownCostUsd()).isEqualByComparingTo("0.146645");
    assertThat(instance.getCostCoverage()).isEqualTo("COMPLETE");
    assertThat(instance.getEvidenceQuality()).isEqualTo("DIRECT");
    assertThat(instance.isObjectiveAchieved()).isTrue();
    com.fasterxml.jackson.databind.JsonNode evidence =
        new ObjectMapper().readTree(instance.getObjectiveEvidenceJson());
    assertThat(evidence.path("sourceExecutionReference").asText())
        .isEqualTo("e1efeb2c-781e-46ce-8e6f-b1b8ffba8c88");
    assertThat(evidence.path("result").path("qualityReview").path("score").asInt()).isEqualTo(90);
  }

  /** Deve ignorar o replay do mesmo callback técnico já consolidado. */
  @Test
  void keepsRepeatedTechnicalCallbackIdempotent() {
    BusinessProcessActivityInstance completed = new BusinessProcessActivityInstance();
    completed.setStatus("COMPLETED");
    completed.setOccurrenceNumber(1);
    completed.setObjectiveEvidenceJson(
        "{\"sourceExecutionReference\":\"quality-review-89\",\"result\":{}}");
    when(activityInstanceRepository
            .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                123L, "commercial-plan:4@v3:journey"))
        .thenReturn(Optional.of(completed));

    service.completeFromExecution(
        243L,
        "technical",
        "quality-review-89",
        Instant.parse("2026-08-27T03:35:14Z"),
        Instant.parse("2026-08-27T03:36:55Z"),
        BigDecimal.ONE,
        "{\"qualityReview\":{\"score\":90}}");

    verify(activityInstanceRepository, never()).save(any());
  }

  /**
   * Deve abrir nova ocorrência quando uma execução técnica distinta sucede a anterior concluída.
   */
  @Test
  void opensAnotherOccurrenceForDifferentTechnicalExecution() {
    BusinessProcessActivityInstance completed = new BusinessProcessActivityInstance();
    completed.setStatus("COMPLETED");
    completed.setOccurrenceNumber(1);
    completed.setObjectiveEvidenceJson(
        "{\"sourceExecutionReference\":\"quality-review-old\",\"result\":{}}");
    when(activityInstanceRepository
            .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                123L, "commercial-plan:4@v3:journey"))
        .thenReturn(Optional.of(completed));

    service.completeFromExecution(
        243L,
        "technical",
        "quality-review-new",
        Instant.parse("2026-08-27T04:00:00Z"),
        Instant.parse("2026-08-27T04:01:00Z"),
        null,
        "{\"qualityReview\":{\"score\":91}}");

    ArgumentCaptor<BusinessProcessActivityInstance> saved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(activityInstanceRepository).save(saved.capture());
    assertThat(saved.getValue().getOccurrenceNumber()).isEqualTo(2);
    assertThat(saved.getValue().getKnownCostUsd()).isNull();
    assertThat(saved.getValue().getCostCoverage()).isEqualTo("NOT_REPORTED");
  }
}
