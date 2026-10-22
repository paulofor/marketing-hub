package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.communication.v1.PrivateCommunicationCreativeProof;
import com.marketinghub.creative.*;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.history.ExperimentHistoryEventService;
import com.marketinghub.media.Asset;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.AssetStorageService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: integrar a prova privada real à preparação do anúncio sem autoaprovação. */
class OpalaPrivateCreativeProofFlowTest {
  /**
   * Percorre identidade, pareceres e aceite reais em memória, simulando somente os limites
   * externos.
   */
  @Test
  void preparesDraftThroughRealPrivateProof() throws Exception {
    var json = new ObjectMapper();
    var tasks = mock(AgentTaskRepository.class);
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var visuals = mock(AgentTaskVisualEvidenceService.class);
    var storage = mock(AssetStorageService.class);
    var assets = mock(AssetRepository.class);
    var creatives = mock(CreativeRepository.class);
    var creativeService = mock(CreativeService.class);
    var history = mock(ExperimentHistoryEventService.class);
    var product = Product.builder().id(4L).slug("fixture-opala").build();
    var experiment =
        Experiment.builder().id(106L).product(product).status(ExperimentStatus.PLANNED).build();
    var cycle = new LearningSalesCycle();
    cycle.setId(13L);
    cycle.setProductId(4L);
    cycle.setExperimentId(106L);
    cycle.setProductVersion("musa-pde-entry-v14-primeiro-ajuste-aplicavel");
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("creatives");
    var call = new BusinessProcessActivityInstance();
    call.setActivityDefinition(activity);
    call.setStatus("COMPLETED");
    call.setObjectiveAchieved(true);
    call.setObjectiveEvidenceJson(
        "{\"evidenceType\":\"SUBPROCESS_OBJECTIVE_ACHIEVED_V1\",\"sourceReference\":\"experiment:106\",\"childProcessDefinitionId\":121}");
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                anyString(), eq("experiment:106")))
        .thenReturn(List.of(call));
    byte[] bytes = "pixels-sinteticos-do-fluxo-local".getBytes(StandardCharsets.UTF_8);
    String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    var produced =
        json.createObjectNode()
            .put("contractVersion", "IRIS_COMMUNICATION_V1")
            .put("outputType", "NON_AUDIOVISUAL_PACKAGE")
            .put("executionStatus", "COMPLETED")
            .put("sourceReference", "experiment:106");
    var output = produced.putObject("functionalOutput");
    output
        .putObject("copy")
        .put("headline", "Um ajuste com o que você já tem")
        .put("primaryText", "Primeiro ajuste gratuito.");
    output
        .putArray("renderedAssets")
        .addObject()
        .put("artifactId", 1118L)
        .put("sha256", sha)
        .put("prototypeVersion", cycle.getProductVersion())
        .put("privateValidation", true);
    var review = json.createObjectNode().put("decision", "APPROVED");
    review.putArray("requiredChanges");
    review.putArray("renderedAssetAudit").addObject().put("artifactId", 1118L).put("sha256", sha);
    var time = Instant.parse("2026-10-22T00:00:00Z");
    when(tasks.findFunctionalSnapshotsByProcessSince(121L, "experiment:106", null))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    788L,
                    121L,
                    "creative-production-approval",
                    "nonAudiovisual",
                    "communication-director",
                    "COMPLETED",
                    time,
                    time,
                    produced.toString()),
                new AgentTaskFunctionalSnapshot(
                    789L,
                    121L,
                    "creative-production-approval",
                    "customer",
                    "customer-agent",
                    "COMPLETED",
                    time,
                    time,
                    review.toString()),
                new AgentTaskFunctionalSnapshot(
                    790L,
                    121L,
                    "creative-production-approval",
                    "commercial",
                    "meta-ad-approver",
                    "COMPLETED",
                    time,
                    time,
                    review.toString())));
    var humanActivity = new BusinessProcessActivityDefinition();
    humanActivity.setActivityId("human");
    var human = new BusinessProcessActivityInstance();
    human.setId(728L);
    human.setActivityDefinition(humanActivity);
    human.setStatus("COMPLETED");
    human.setObjectiveAchieved(true);
    human.setOccurrenceNumber(1);
    human.setObjectiveEvidenceJson("{\"decision\":\"APPROVE\"}");
    human.setExitedAt(time.plusSeconds(60));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                121L, "experiment:106"))
        .thenReturn(List.of(human));
    var producer = new AgentTask();
    producer.setId(788L);
    producer.setSourceReference("experiment:106");
    producer.setResultJson(produced.toString());
    when(tasks.findById(788L)).thenReturn(Optional.of(producer));
    when(visuals.read(788L, 1118L))
        .thenReturn(new AgentTaskVisualEvidenceService.EvidenceContent("image/png", bytes));
    when(storage.storeBytes(any(), anyString(), anyString(), any()))
        .thenReturn(
            new AssetStorageService.StoredObject(
                "fixture.png",
                "https://assets.sandbox.local/fixture.png",
                bytes.length,
                "image/png",
                true));
    var draft =
        Creative.builder()
            .id(500L)
            .status(CreativeStatus.DRAFT)
            .agentReviewStatus(CreativeAgentReviewStatus.PENDING)
            .build();
    when(creativeService.create(eq(106L), any())).thenReturn(draft);
    var service =
        new OpalaPrivateCreativeMaterialization(
            instances,
            new PrivateCommunicationCreativeProof(tasks, instances, json),
            tasks,
            visuals,
            storage,
            assets,
            creatives,
            creativeService,
            history,
            json);
    service.prepare(
        new OpalaCommercialContext.Scope(cycle, experiment), "https://candidate.sandbox.local");
    var audit = ArgumentCaptor.forClass(Asset.class);
    verify(assets).save(audit.capture());
    assertThat(
            json.readTree(audit.getValue().getPayload())
                .path("privateCreativeProof")
                .path("reviews")
                .size())
        .isEqualTo(2);
    assertThat(draft.getAgentReviewStatus()).isNull();
    verify(creativeService, never()).requestAgentReview(anyLong());
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
  }
}
