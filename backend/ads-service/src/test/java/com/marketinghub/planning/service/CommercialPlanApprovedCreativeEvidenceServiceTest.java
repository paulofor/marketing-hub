package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a continuidade auditável entre os criativos aprovados e a landing. */
class CommercialPlanApprovedCreativeEvidenceServiceTest {
  private static final String PACKAGE_ID =
      "2bf0a2a4ef3725da5258f41a04db9fd9816c2f300f1d68d3f2ab538498fc9cf2";

  /** Consolida o pacote somente quando todos os gates aprovam a mesma evidência segregada. */
  @Test
  void resolvesApprovedPackageFromTheSamePlanVersion() {
    Fixture fixture = fixture();
    when(fixture.tasks().findBySourceReferenceOrderByCreatedAtAscIdAsc("commercial-plan:4@v3"))
        .thenReturn(
            List.of(
                task(
                    fixture.process(), "nonAudiovisual", "{\"decision\":\"SELECTED\"}", PACKAGE_ID),
                task(
                    fixture.process(),
                    "audiovisual",
                    "{\"deliverables\":[\"carousel\",\"vertical-demo\"]}",
                    PACKAGE_ID),
                task(fixture.process(), "customer", "{\"decision\":\"APPROVED\"}", PACKAGE_ID),
                task(fixture.process(), "commercial", "{\"decision\":\"APPROVED\"}", PACKAGE_ID)));

    var evidence = fixture.service().resolve(89L);

    assertThat(evidence)
        .containsEntry("status", "APPROVED")
        .containsEntry("sourceReference", "commercial-plan:4@v3")
        .containsEntry("creativePackageId", PACKAGE_ID)
        .containsEntry("published", false)
        .containsEntry("externalMediaSpendUsd", 0);
    assertThat(evidence.get("packageEvidence").toString()).contains("carousel-01.png");
  }

  /** Bloqueia a propagação quando qualquer atividade referencia outro pacote criativo. */
  @Test
  void blocksMixedCreativePackages() {
    Fixture fixture = fixture();
    String differentPackage = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    when(fixture.tasks().findBySourceReferenceOrderByCreatedAtAscIdAsc("commercial-plan:4@v3"))
        .thenReturn(
            List.of(
                task(
                    fixture.process(), "nonAudiovisual", "{\"decision\":\"SELECTED\"}", PACKAGE_ID),
                task(
                    fixture.process(),
                    "audiovisual",
                    "{\"deliverables\":[\"carousel\"]}",
                    PACKAGE_ID),
                task(fixture.process(), "customer", "{\"decision\":\"APPROVED\"}", PACKAGE_ID),
                task(
                    fixture.process(),
                    "commercial",
                    "{\"decision\":\"APPROVED\"}",
                    differentPackage)));

    var evidence = fixture.service().resolve(89L);

    assertThat(evidence).containsEntry("status", "UNAVAILABLE");
    assertThat(evidence.get("blockReason").toString()).contains("segregação local");
  }

  /** Bloqueia pacote sem arquivo concreto mesmo quando os demais campos parecem aprovados. */
  @Test
  void blocksPackageWithoutConcreteAsset() {
    Fixture fixture = fixture();
    when(fixture.tasks().findBySourceReferenceOrderByCreatedAtAscIdAsc("commercial-plan:4@v3"))
        .thenReturn(
            List.of(
                task(
                    fixture.process(), "nonAudiovisual", "{\"decision\":\"SELECTED\"}", PACKAGE_ID),
                task(
                    fixture.process(),
                    "audiovisual",
                    "{\"deliverables\":[\"carousel\"]}",
                    PACKAGE_ID),
                task(fixture.process(), "customer", "{\"decision\":\"APPROVED\"}", PACKAGE_ID),
                task(
                    fixture.process(),
                    "commercial",
                    "{\"decision\":\"APPROVED\"}",
                    PACKAGE_ID,
                    "{}",
                    false,
                    0)));

    var evidence = fixture.service().resolve(89L);

    assertThat(evidence).containsEntry("status", "UNAVAILABLE");
    assertThat(evidence.get("blockReason").toString()).contains("evidência íntegra");
  }

  /** Bloqueia pacote já publicado ou que tenha consumido mídia externa. */
  @Test
  void blocksPublishedOrSpentPackage() {
    Fixture fixture = fixture();
    when(fixture.tasks().findBySourceReferenceOrderByCreatedAtAscIdAsc("commercial-plan:4@v3"))
        .thenReturn(
            List.of(
                task(
                    fixture.process(), "nonAudiovisual", "{\"decision\":\"SELECTED\"}", PACKAGE_ID),
                task(
                    fixture.process(),
                    "audiovisual",
                    "{\"deliverables\":[\"carousel\"]}",
                    PACKAGE_ID),
                task(fixture.process(), "customer", "{\"decision\":\"APPROVED\"}", PACKAGE_ID),
                task(
                    fixture.process(),
                    "commercial",
                    "{\"decision\":\"APPROVED\"}",
                    PACKAGE_ID,
                    "{\"url\":\"https://cdn.example/carousel-01.png\"}",
                    true,
                    1)));

    var evidence = fixture.service().resolve(89L);

    assertThat(evidence).containsEntry("status", "UNAVAILABLE");
    assertThat(evidence.get("blockReason").toString()).contains("evidência íntegra");
  }

  /** Prepara plano, versão e processo canônicos usados por cada cenário. */
  private Fixture fixture() {
    CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
    CommercialPlanVersionService versions = mock(CommercialPlanVersionService.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    CommercialPlan plan = CommercialPlan.builder().id(4L).name("Rigel").build();
    when(plans.findByExperimentReference(89L)).thenReturn(List.of(plan));
    when(versions.current(4L))
        .thenReturn(
            new CommercialPlanVersionDto(
                12L, 4L, 3, "{}", "teste", "versão canônica", Instant.now()));
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(17L);
    process.setProcessCode("creative-production-approval");
    return new Fixture(
        new CommercialPlanApprovedCreativeEvidenceService(
            plans, versions, tasks, new ObjectMapper()),
        tasks,
        process);
  }

  /** Monta uma tarefa concluída com resultado funcional e pacote humano não publicado. */
  private AgentTask task(
      BusinessProcessDefinition process, String activity, String result, String packageId) {
    return task(
        process,
        activity,
        result,
        packageId,
        "{\"url\":\"https://cdn.example/carousel-01.png\"}",
        false,
        0);
  }

  /** Monta uma tarefa permitindo variar as proteções de arquivo, publicação e gasto. */
  private AgentTask task(
      BusinessProcessDefinition process,
      String activity,
      String result,
      String packageId,
      String asset,
      boolean published,
      int externalMediaSpendUsd) {
    AgentTask task = new AgentTask();
    task.setProcessDefinition(process);
    task.setProcessActivityId(activity);
    task.setStatus("COMPLETED");
    task.setResultJson(result);
    task.setEvidenceJson(
        "{\"creativePackageId\":\""
            + packageId
            + "\",\"assets\":["
            + asset
            + "],\"importedByHuman\":true,\"published\":"
            + published
            + ",\"externalMediaSpendUsd\":"
            + externalMediaSpendUsd
            + "}");
    return task;
  }

  /** Transporta as dependências isoladas e o processo da mesma versão de criativos. */
  private record Fixture(
      CommercialPlanApprovedCreativeEvidenceService service,
      AgentTaskRepository tasks,
      BusinessProcessDefinition process) {}
}
