package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.media.Asset;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.VideoProject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a projeção da decisão audiovisual comercial para Apolo. */
class ApolloAudiovisualTaskTargetProjectorTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Usa a última rota concluída da mesma versão e descarta o contrato amplo do produto. */
  @Test
  void projectsCurrentCommunicationDecisionIntoMinimalContext() throws Exception {
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var task = task(64L, "creative-production-approval", "audiovisual");
    var obsolete = route(385L, task.getProcessDefinition(), "COMPLETED", true, 500L, false);
    var current = route(388L, task.getProcessDefinition(), "COMPLETED", true, 502L, true);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "creative-production-approval", "experiment:93"))
        .thenReturn(List.of(current, obsolete));
    var projector = projector(instances);
    var original =
        target(
            json.readTree("{\"harness\":{\"audiovisualRequired\":false},\"large\":\"context\"}"));

    var projected = projector.project(task, original);

    assertThat(projected.productId()).isEqualTo(10L);
    assertThat(projected.pdeContext().path("contractVersion").asText())
        .isEqualTo(ApolloAudiovisualTaskTargetProjector.CONTRACT_VERSION);
    assertThat(
            projected
                .pdeContext()
                .path("communicationMaterialization")
                .path("audiovisualRequired")
                .asBoolean())
        .isTrue();
    assertThat(
            projected
                .pdeContext()
                .path("communicationMaterialization")
                .path("communicationTaskId")
                .asLong())
        .isEqualTo(502L);
    assertThat(projected.pdeContext().has("harness")).isFalse();
    assertThat(projected.pdeContext().toString()).doesNotContain("large");
  }

  /** Não reaproveita uma rota incompleta nem converte evidência ambígua em autorização. */
  @Test
  void preservesMissingContractWhenLatestRouteIsNotAchieved() {
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var task = task(64L, "creative-production-approval", "audiovisual");
    var current = route(389L, task.getProcessDefinition(), "BLOCKED", false, 502L, true);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "creative-production-approval", "experiment:93"))
        .thenReturn(List.of(current));
    var projector = projector(instances);
    var original = target(null);

    assertThat(projector.project(task, original)).isSameAs(original);
  }

  /** Mantém inalteradas tarefas da construção, cujo contrato continua no harness do produto. */
  @Test
  void leavesProductConstructionContractUntouched() throws Exception {
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var projector = projector(instances);
    var original = target(json.readTree("{\"harness\":{\"audiovisualRequired\":false}}"));

    assertThat(projector.project(task(40L, "pde-construction-approval", "audiovisual"), original))
        .isSameAs(original);
  }

  /**
   * Projeta a entrega somente quando ciclo, projeto, perfil, custo e ativo pertencem ao destino.
   */
  @Test
  void projectsGovernedMaterializationBackIntoCreativeTask() throws Exception {
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var cycles = mock(VideoProductionCycleRepository.class);
    var projects = mock(VideoProjectRepository.class);
    var jobs = mock(SalesVideoJobRepository.class);
    var task = task(64L, "creative-production-approval", "audiovisual");
    var current = route(388L, task.getProcessDefinition(), "COMPLETED", true, 502L, true);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "creative-production-approval", "experiment:93"))
        .thenReturn(List.of(current));
    var cycle = new VideoProductionCycle();
    cycle.setId(51L);
    cycle.setVideoProjectId(61L);
    cycle.setProductId(10L);
    cycle.setExperimentId(93L);
    cycle.setSalesVideoJobId(71L);
    cycle.setStatus("VIDEO_READY_FOR_REVIEW");
    cycle.setFinancialDecision("APPROVED");
    cycle.setRequestedBy("time@marketinghub.io");
    cycle.setBudgetLimitUsd(new BigDecimal("15.38"));
    cycle.setKnownCostUsd(BigDecimal.ZERO);
    var project =
        VideoProject.builder()
            .id(61L)
            .tenantId("default")
            .productId(10L)
            .experimentId(93L)
            .campaignKey("mira-private-v3")
            .build();
    var product = new Product();
    product.setId(10L);
    var profile = SalesVideoProfile.builder().id(41L).tenantId("default").product(product).build();
    var job =
        SalesVideoJob.builder()
            .id(71L)
            .tenantId("default")
            .profile(profile)
            .providerName("EDITORIAL_MOTION")
            .status(SalesVideoStatus.VIDEO_READY)
            .asset(Asset.builder().id(81L).build())
            .build();
    when(cycles.findTopByProductIdAndExperimentIdOrderByCreatedAtDescIdDesc(10L, 93L))
        .thenReturn(Optional.of(cycle));
    when(projects.findById(61L)).thenReturn(Optional.of(project));
    when(jobs.findById(71L)).thenReturn(Optional.of(job));
    var projector =
        new ApolloAudiovisualTaskTargetProjector(instances, cycles, projects, jobs, json);

    var projected = projector.project(task, target(null));

    var materialization = projected.pdeContext().path("audiovisualMaterialization");
    assertThat(materialization.path("videoProductionCycleId").asLong()).isEqualTo(51L);
    assertThat(materialization.path("actualCostUsd").decimalValue()).isEqualByComparingTo("0");
    assertThat(materialization.path("artifactIds").get(0).asLong()).isEqualTo(81L);
    assertThat(materialization.path("publicationAuthorized").asBoolean()).isFalse();
  }

  /** Cria o projetor com domínios audiovisuais vazios nos cenários que testam apenas a rota. */
  private ApolloAudiovisualTaskTargetProjector projector(
      BusinessProcessActivityInstanceRepository instances) {
    return new ApolloAudiovisualTaskTargetProjector(
        instances,
        mock(VideoProductionCycleRepository.class),
        mock(VideoProjectRepository.class),
        mock(SalesVideoJobRepository.class),
        json);
  }

  /** Cria a tarefa audiovisual com a identidade de processo controlada pelo teste. */
  private AgentTask task(Long processId, String processCode, String activityId) {
    var process = new BusinessProcessDefinition();
    process.setId(processId);
    process.setProcessCode(processCode);
    var task = new AgentTask();
    task.setId(504L);
    task.setProcessDefinition(process);
    task.setProcessActivityId(activityId);
    task.setSourceReference("experiment:93");
    return task;
  }

  /** Cria uma ocorrência de rota com o contrato funcional informado. */
  private BusinessProcessActivityInstance route(
      Long id,
      BusinessProcessDefinition process,
      String status,
      boolean achieved,
      Long communicationTaskId,
      boolean audiovisualRequired) {
    var definition = new BusinessProcessActivityDefinition();
    definition.setId(id + 1000);
    definition.setProcessDefinition(process);
    definition.setActivityId("route");
    var instance = new BusinessProcessActivityInstance();
    instance.setId(id);
    instance.setActivityDefinition(definition);
    instance.setSourceReference("experiment:93");
    instance.setStatus(status);
    instance.setObjectiveAchieved(achieved);
    instance.setObjectiveEvidenceJson(
        """
        {"evidenceType":"COMMUNICATION_FORMATS_V1","communicationTaskId":%d,
         "sourceReference":"experiment:93","nonAudiovisualRequired":true,
         "audiovisualRequired":%s,"publicationAuthorized":false,"spendAuthorized":false}
        """
            .formatted(communicationTaskId, audiovisualRequired));
    return instance;
  }

  /** Cria o alvo preservado pela projeção, inclusive seus campos comerciais. */
  private AgentTaskTargetResponse target(com.fasterxml.jackson.databind.JsonNode context) {
    return new AgentTaskTargetResponse(
        "experiment:93",
        93L,
        10L,
        "pde-planejado-36",
        "Orientação digital individualizada",
        "Mira",
        "mira-private-v3",
        "https://v7.clubemusa.com.br/mira-private",
        null,
        null,
        null,
        new BigDecimal("49.00"),
        context);
  }
}
