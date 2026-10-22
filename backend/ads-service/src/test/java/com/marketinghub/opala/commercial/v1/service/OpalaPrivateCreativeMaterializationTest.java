package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskVisualEvidenceService;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.communication.v1.PrivateCommunicationCreativeProof;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.history.ExperimentHistoryEventContracts.CreateRequest;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

/**
 * Responsabilidade: preservar pixels, autoria e aceite privado sem fabricar aprovação comercial.
 */
class OpalaPrivateCreativeMaterializationTest {
  private final ObjectMapper json = new ObjectMapper();
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final PrivateCommunicationCreativeProof proofs =
      mock(PrivateCommunicationCreativeProof.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final AgentTaskVisualEvidenceService visuals = mock(AgentTaskVisualEvidenceService.class);
  private final AssetStorageService storage = mock(AssetStorageService.class);
  private final AssetRepository assets = mock(AssetRepository.class);
  private final CreativeRepository creatives = mock(CreativeRepository.class);
  private final CreativeService creativeService = mock(CreativeService.class);
  private final ExperimentHistoryEventService history = mock(ExperimentHistoryEventService.class);
  private final OpalaPrivateCreativeMaterialization service =
      new OpalaPrivateCreativeMaterialization(
          instances,
          proofs,
          tasks,
          visuals,
          storage,
          assets,
          creatives,
          creativeService,
          history,
          json);

  /**
   * Prepara a mesma fonte uma vez em duas identidades e mantém ambos os gates comerciais pendentes.
   */
  @ParameterizedTest
  @CsvSource({"4,106,13", "407,922,71"})
  void preparesExactPixelsAsUnreviewedDraft(long productId, long experimentId, long cycleId)
      throws Exception {
    var scope = scope(productId, experimentId, cycleId);
    var source = configureSource(scope);
    var draft =
        Creative.builder()
            .id(500L)
            .format("IMAGE")
            .status(CreativeStatus.DRAFT)
            .agentReviewStatus(CreativeAgentReviewStatus.PENDING)
            .experiment(scope.experiment())
            .build();
    when(creativeService.create(eq(experimentId), any())).thenReturn(draft);
    when(creatives.findByExperimentId(experimentId)).thenReturn(List.of(), List.of(draft));
    service.prepare(scope, "https://candidate.sandbox.local");
    service.prepare(scope, "https://candidate.sandbox.local");
    var request = ArgumentCaptor.forClass(CreateCreativeRequest.class);
    verify(creativeService).create(eq(experimentId), request.capture());
    assertThat(request.getValue().getHeadline()).isEqualTo("Um ajuste com o que você já tem");
    assertThat(request.getValue().getFormat()).isEqualTo("IMAGE");
    assertThat(request.getValue().getStatus()).isEqualTo(CreativeStatus.DRAFT);
    assertThat(request.getValue().getCostUsd()).isZero();
    assertThat(draft.getAgentReviewStatus()).isNull();
    verify(creativeService, never()).requestAgentReview(any());
    verify(storage).storeBytes(eq(source.bytes()), anyString(), eq("image/png"), any());
    var stored = ArgumentCaptor.forClass(Asset.class);
    verify(assets).save(stored.capture());
    var receipt = json.readTree(stored.getValue().getPayload());
    assertThat(receipt.path("contentSha256").asText()).isEqualTo(source.sha());
    assertThat(receipt.path("experimentId").asLong()).isEqualTo(experimentId);
    assertThat(receipt.path("privateCreativeProof").path("humanDecisionInstanceId").asLong())
        .isEqualTo(productId == 4L ? 728L : 1777L);
    assertThat(receipt.path("creativeAgentReviewGranted").asBoolean()).isFalse();
    assertThat(receipt.path("creativeHumanApprovalGranted").asBoolean()).isFalse();
    var event = ArgumentCaptor.forClass(CreateRequest.class);
    verify(history).create(eq(experimentId), event.capture());
    assertThat(json.readTree(event.getValue().evidenceJson()).path("creativeId").asLong())
        .isEqualTo(500L);
    assertThat(scope.experiment().getStatus()).isEqualTo(ExperimentStatus.PLANNED);
  }

  /** Rejeita alteração dos pixels e ausência de aceite sem publicar nem chamar outro modelo. */
  @Test
  void rejectsChangedPixelsAndMissingApproval() throws Exception {
    var scope = scope(4L, 106L, 13L);
    configureSource(scope);
    when(visuals.read(788L, 1118L))
        .thenReturn(
            new AgentTaskVisualEvidenceService.EvidenceContent("image/png", new byte[] {0}));
    assertThatThrownBy(() -> service.prepare(scope, "https://candidate.sandbox.local"))
        .hasRootCauseMessage("Os pixels não correspondem à peça privada revisada.");
    when(proofs.resolve(any(), anyString(), anyString()))
        .thenThrow(new IllegalStateException("Confirme o uso da peça atual"));
    assertThatThrownBy(() -> service.prepare(scope, "https://candidate.sandbox.local"))
        .hasRootCauseMessage("Confirme o uso da peça atual");
    verifyNoInteractions(storage, assets, creativeService, history);
  }

  /** Rejeita ambiguidade e origem de outra ocorrência antes de ler ou copiar os bytes privados. */
  @Test
  void rejectsAmbiguousOrCrossContextSources() throws Exception {
    var scope = scope(4L, 106L, 13L);
    var source = configureSource(scope);
    source
        .proof()
        .withArray("renderedAssets")
        .add(source.proof().path("renderedAssets").get(0).deepCopy());
    assertThatThrownBy(() -> service.prepare(scope, "https://candidate.sandbox.local"))
        .hasRootCauseMessage("Selecione uma única peça privada para preparar o anúncio.");
    source.proof().withArray("renderedAssets").remove(1);
    source.producer().setSourceReference("experiment:922");
    assertThatThrownBy(() -> service.prepare(scope, "https://candidate.sandbox.local"))
        .hasRootCauseMessage("A peça pertence a outra ocorrência.");
    verifyNoInteractions(visuals, storage, assets, creativeService, history);
  }

  /** Solicita revisão própria depois do checkout sem reabrir parecer aprovado ou em execução. */
  @Test
  void queuesOnlyUnreviewedDraftAfterCheckout() {
    var scope = scope(4L, 106L, 13L);
    assertThatThrownBy(() -> service.requestReview(scope)).hasMessageContaining("checkout");
    var draft =
        Creative.builder()
            .id(500L)
            .format("IMAGE")
            .status(CreativeStatus.DRAFT)
            .imageUrl("https://assets.sandbox.local/owned.png")
            .build();
    var unrelated =
        Creative.builder()
            .id(502L)
            .format("IMAGE")
            .status(CreativeStatus.DRAFT)
            .imageUrl("https://assets.sandbox.local/another.png")
            .build();
    var approved =
        Creative.builder()
            .id(501L)
            .format("IMAGE")
            .status(CreativeStatus.READY)
            .agentReviewStatus(CreativeAgentReviewStatus.APPROVED)
            .build();
    when(creatives.findByExperimentId(106L)).thenReturn(List.of(draft, approved, unrelated));
    var receipt =
        json.createObjectNode()
            .put("contractVersion", "OPALA_PRIVATE_CREATIVE_DRAFT_V1")
            .put("experimentId", 106L)
            .put("productId", 4L)
            .put("cycleId", 13L)
            .put("productVersion", scope.cycle().getProductVersion());
    when(assets.findByUrlIn(any()))
        .thenReturn(
            List.of(
                Asset.builder()
                    .url(draft.getImageUrl())
                    .model("IRIS_EXISTING_RENDER_NO_NEW_GENERATION")
                    .payload(receipt.toString())
                    .build(),
                Asset.builder()
                    .url(unrelated.getImageUrl())
                    .model("IRIS_EXISTING_RENDER_NO_NEW_GENERATION")
                    .payload(receipt.deepCopy().put("cycleId", 999).toString())
                    .build()));
    scope.experiment().setCommercialCheckoutUrl("https://checkout.sandbox.local/oferta");
    service.requestReview(scope);
    verify(creativeService).requestAgentReview(500L);
    verify(creativeService, never()).requestAgentReview(501L);
    verify(creativeService, never()).requestAgentReview(502L);
  }

  /** Conserva uma campanha já liberada e impede criação automática posterior. */
  @Test
  void refusesReleasedExperiment() {
    var scope = scope(4L, 106L, 13L);
    scope.experiment().setStatus(ExperimentStatus.RUNNING);
    assertThatThrownBy(() -> service.prepare(scope, "https://candidate.sandbox.local"))
        .hasRootCauseMessage("O anúncio só pode ser preparado antes da liberação comercial.");
    verifyNoInteractions(
        instances, proofs, tasks, visuals, storage, assets, creativeService, history);
  }

  /** Prepara fontes sintéticas nos limites externos sem simular uma aprovação comercial real. */
  private Source configureSource(OpalaCommercialContext.Scope scope) throws Exception {
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("creatives");
    var call = new BusinessProcessActivityInstance();
    call.setActivityDefinition(activity);
    call.setStatus("COMPLETED");
    call.setObjectiveAchieved(true);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                anyString(), anyString()))
        .thenReturn(List.of(call));
    byte[] bytes = "pixels-sinteticos-locais".getBytes(StandardCharsets.UTF_8);
    String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    long producerId = scope.cycle().getProductId() == 4L ? 788L : 991L;
    long artifactId = scope.cycle().getProductId() == 4L ? 1118L : 992L;
    var proof =
        json.createObjectNode()
            .put("producerTaskId", producerId)
            .put("humanDecisionInstanceId", scope.cycle().getProductId() == 4L ? 728L : 1777L);
    proof.putArray("renderedAssets").addObject().put("artifactId", artifactId).put("sha256", sha);
    when(proofs.resolve(
            eq(call), eq("experiment:" + scope.experiment().getId()), eq("fixture-v14")))
        .thenReturn(proof);
    var producer = new AgentTask();
    producer.setId(producerId);
    producer.setSourceReference("experiment:" + scope.experiment().getId());
    producer.setResultJson(
        "{\"functionalOutput\":{\"copy\":{\"headline\":\"Um ajuste com o que você já tem\",\"primaryText\":\"Veja seu primeiro ajuste gratuito.\",\"description\":\"Primeiro passo gratuito\"}}}");
    when(tasks.findById(producerId)).thenReturn(Optional.of(producer));
    when(visuals.read(producerId, artifactId))
        .thenReturn(new AgentTaskVisualEvidenceService.EvidenceContent("image/png", bytes));
    when(storage.storeBytes(any(), anyString(), anyString(), any()))
        .thenReturn(
            new AssetStorageService.StoredObject(
                "fixture.png",
                "https://assets.sandbox.local/fixture.png",
                bytes.length,
                "image/png",
                true));
    return new Source(bytes, sha, proof, producer);
  }

  /** Monta uma ocorrência com limites e identidades explícitos, sem operação externa. */
  private OpalaCommercialContext.Scope scope(long productId, long experimentId, long cycleId) {
    var product = Product.builder().id(productId).slug("fixture-opala").build();
    var experiment =
        Experiment.builder()
            .id(experimentId)
            .product(product)
            .status(ExperimentStatus.PLANNED)
            .build();
    var cycle = new LearningSalesCycle();
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setProductVersion("fixture-v14");
    return new OpalaCommercialContext.Scope(cycle, experiment);
  }

  /**
   * Responsabilidade: transportar os pixels e a identidade da origem sintética desta prova local.
   */
  private record Source(byte[] bytes, String sha, ObjectNode proof, AgentTask producer) {}
}
