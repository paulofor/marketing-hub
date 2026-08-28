package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.agent.v1.LandingCheckoutEvidenceResolver;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: proteger a reutilização auditável de uma landing tecnicamente aprovada. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanLandingReviewResumeServiceTest {
  @Mock private ExperimentRepository experimentRepository;
  @Mock private GeraLandingStageExecutionRepository landingExecutionRepository;
  @Mock private LandingCheckoutEvidenceResolver checkoutEvidenceResolver;
  @Mock private CommercialPlanApprovedCreativeEvidenceService approvedCreativeEvidenceService;

  /** Reutiliza o HTML idêntico e entrega contratos canônicos atualizados aos dois revisores. */
  @Test
  void buildsReviewSnapshotWhenOnlyEvidenceTransportWasBlocked() throws Exception {
    AgentTask landing = task(243L, "landing-generator", "html", "COMPLETED");
    landing.setEvidenceJson("{\"checkoutUrl\":null}");
    AgentTask customer = task(244L, "customer-agent", "customer", "BLOCKED");
    customer.setResultJson(
        "{\"decision\":\"BLOCKED\",\"requiredChanges\":["
            + "\"Persistir checkoutUrl no campo auditável\","
            + "\"Fornecer evidência auditável do checkout\"]}");
    Experiment experiment =
        Experiment.builder().id(89L).htmlGeraLanding("<html>Rigel</html>").build();
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));
    GeraLandingStageExecution qualityReview = qualityReview("<html>Rigel</html>");
    when(landingExecutionRepository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                89L, "landing-page-quality-review", "agent-task:243"))
        .thenReturn(List.of(qualityReview));
    when(checkoutEvidenceResolver.resolve(experiment))
        .thenReturn(
            Map.of(
                "validationStatus",
                "VALIDATED_FROM_PERSISTED_CANONICAL_BINDING",
                "canonicalUrl",
                "https://checkout.example/rigel"));
    when(approvedCreativeEvidenceService.resolve(89L))
        .thenReturn(Map.of("status", "APPROVED", "creativePackageId", "package-89"));
    var service = service();

    Optional<String> brief =
        service.buildResumeBrief(
            4L,
            89L,
            1,
            List.of(Map.of("activityId", "customer", "blockingReason", "checkout")),
            List.of(landing, customer));

    assertThat(brief).isPresent();
    assertThat(brief.orElseThrow())
        .contains(
            "REUSE_APPROVED_LANDING_WITH_FRESH_CANONICAL_EVIDENCE",
            "VALIDATED_FROM_PERSISTED_CANONICAL_BINDING",
            "quality-review-89",
            "\"landingRegenerationAuthorized\":false",
            "\"score\":90");
  }

  /** Exige nova execução do Dédalo quando a Psique apontou defeito real de conteúdo. */
  @Test
  void rejectsReuseForLandingContentBlock() {
    AgentTask landing = task(243L, "landing-generator", "html", "COMPLETED");
    landing.setEvidenceJson("{\"checkoutUrl\":null}");
    AgentTask customer = task(244L, "customer-agent", "customer", "BLOCKED");
    customer.setResultJson(
        "{\"decision\":\"BLOCKED\",\"remediationTarget\":\"LANDING_CONTENT\","
            + "\"requiredChanges\":[\"Mudar a headline\"]}");
    var service = service();

    Optional<String> brief =
        service.buildResumeBrief(4L, 89L, 1, List.of(), List.of(landing, customer));

    assertThat(brief).isEmpty();
    verify(experimentRepository, never()).findById(89L);
  }

  /** Exige nova homologação quando o HTML atual diverge daquele aprovado tecnicamente. */
  @Test
  void rejectsReuseWhenApprovedHtmlChanged() throws Exception {
    AgentTask landing = task(243L, "landing-generator", "html", "COMPLETED");
    landing.setEvidenceJson("{\"checkoutUrl\":null}");
    AgentTask customer = task(244L, "customer-agent", "customer", "BLOCKED");
    customer.setResultJson(
        "{\"decision\":\"BLOCKED\",\"remediationTarget\":\"EVIDENCE_TRANSPORT\","
            + "\"requiredChanges\":[\"Persistir checkoutUrl\"]}");
    Experiment experiment =
        Experiment.builder().id(89L).htmlGeraLanding("<html>Alterado</html>").build();
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));
    when(landingExecutionRepository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                89L, "landing-page-quality-review", "agent-task:243"))
        .thenReturn(List.of(qualityReview("<html>Rigel</html>")));
    var service = service();

    Optional<String> brief =
        service.buildResumeBrief(4L, 89L, 1, List.of(), List.of(landing, customer));

    assertThat(brief).isEmpty();
    verify(checkoutEvidenceResolver, never()).resolve(experiment);
  }

  /** Bloqueia sem chamar modelo quando o checkout atual ainda não satisfaz o binding persistido. */
  @Test
  void blocksResumeWhenCanonicalCheckoutIsInvalid() throws Exception {
    AgentTask landing = task(243L, "landing-generator", "html", "COMPLETED");
    landing.setEvidenceJson("{\"checkoutUrl\":null}");
    AgentTask customer = task(244L, "customer-agent", "customer", "BLOCKED");
    customer.setResultJson(
        "{\"decision\":\"BLOCKED\",\"remediationTarget\":\"EVIDENCE_TRANSPORT\","
            + "\"requiredChanges\":[\"Persistir checkoutUrl\"]}");
    Experiment experiment =
        Experiment.builder().id(89L).htmlGeraLanding("<html>Rigel</html>").build();
    when(experimentRepository.findById(89L)).thenReturn(Optional.of(experiment));
    when(landingExecutionRepository
            .findTop20ByExperimentIdAndStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
                89L, "landing-page-quality-review", "agent-task:243"))
        .thenReturn(List.of(qualityReview("<html>Rigel</html>")));
    when(checkoutEvidenceResolver.resolve(experiment))
        .thenReturn(Map.of("validationStatus", "BLOCKED"));
    var service = service();

    assertThatThrownBy(
            () -> service.buildResumeBrief(4L, 89L, 1, List.of(), List.of(landing, customer)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("checkout canônico");
    verify(approvedCreativeEvidenceService, never()).resolve(89L);
  }

  /** Cria o serviço com as dependências canônicas simuladas localmente. */
  private CommercialPlanLandingReviewResumeService service() {
    return new CommercialPlanLandingReviewResumeService(
        experimentRepository,
        landingExecutionRepository,
        checkoutEvidenceResolver,
        approvedCreativeEvidenceService,
        new ObjectMapper());
  }

  /** Monta uma tarefa suficiente para representar a tentativa publicada do Rigel. */
  private AgentTask task(Long id, String agentKey, String activityId, String status) {
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setAssignedAgent(Agent.builder().id(id).agentKey(agentKey).nickname(agentKey).build());
    task.setProcessActivityId(activityId);
    task.setStatus(status);
    return task;
  }

  /** Monta a aprovação técnica vinculada ao hash do HTML informado. */
  private GeraLandingStageExecution qualityReview(String html) throws Exception {
    return GeraLandingStageExecution.builder()
        .idJob("quality-review-89".getBytes(StandardCharsets.UTF_8))
        .experimentId(89L)
        .stageCode("landing-page-quality-review")
        .autonomousCycleId("agent-task:243")
        .status("CONCLUIDO")
        .modelResponse("{\"approvalRecommendation\":\"APPROVE_FOR_PUBLICATION\",\"score\":90}")
        .qualityReviewAudit("{\"landingHtmlSha256\":\"" + sha256(html) + "\"}")
        .build();
  }

  /** Calcula o hash esperado pelo contrato auditável do Quality Review. */
  private String sha256(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
