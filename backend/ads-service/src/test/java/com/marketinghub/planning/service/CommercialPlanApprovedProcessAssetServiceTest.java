package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskVisualEvidence;
import com.marketinghub.agenttask.AgentTaskVisualEvidenceService;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.dto.CommercialPlanVisualAssetDto;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskVisualEvidenceRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import com.marketinghub.storage.AssetStorageService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: proteger o transporte auditável do BPM para a biblioteca visual do plano. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanApprovedProcessAssetServiceTest {
  @Mock private CommercialPlanRepository plans;
  @Mock private CommercialPlanVisualAssetRepository assets;
  @Mock private CommercialPlanVisualAssetService visualAssets;
  @Mock private AgentTaskRepository tasks;
  @Mock private AgentTaskVisualEvidenceRepository evidence;
  @Mock private AgentTaskVisualEvidenceService evidenceStorage;
  @Mock private BusinessProcessActivityInstanceRepository activityInstances;
  @Mock private AssetStorageService assetStorage;

  private CommercialPlanApprovedProcessAssetService service;
  private CommercialPlan plan;
  private Product product;
  private AgentTask producer;
  private AgentTask customer;
  private AgentTask commercial;
  private String contentHash;

  /** Monta uma seleção completa e coerente antes de cada cenário. */
  @BeforeEach
  void setUp() throws Exception {
    service =
        new CommercialPlanApprovedProcessAssetService(
            plans,
            assets,
            visualAssets,
            tasks,
            evidence,
            evidenceStorage,
            activityInstances,
            assetStorage,
            new ObjectMapper());
    product = new Product();
    product.setId(10L);
    Experiment experiment = new Experiment();
    experiment.setId(93L);
    experiment.setProduct(product);
    plan = new CommercialPlan();
    plan.setId(8L);
    plan.setExperiment(experiment);
    plan.setExperiments(new LinkedHashSet<>());
    byte[] content = "png-aprovado".getBytes(StandardCharsets.UTF_8);
    contentHash = sha256(content);
    producer =
        task(
            526L,
            "nonAudiovisual",
            "communication-director",
            """
            {"guardrails":{"noPublication":true,"noExternalSpend":true},
             "functionalOutput":{"renderedAssets":[{"artifactId":407,"sha256":"%s","templateVersion":"PROOF_CARD_V1"}]}}
            """
                .formatted(contentHash));
    customer = task(527L, "customer", "customer-agent", approvedReview(contentHash));
    commercial = task(528L, "commercial", "meta-ad-approver", approvedReview(contentHash));
    when(tasks.findBySourceReferenceAndProcessDefinitionProcessCodeOrderByCreatedAtAscIdAsc(
            "experiment:93", "creative-production-approval"))
        .thenReturn(List.of(producer, customer, commercial));
  }

  /** Importa o PNG exato depois da decisão humana e preserva os dois pareceres no ativo. */
  @Test
  void importsPreviouslyApprovedProcessWithoutRepeatingAgents() throws Exception {
    byte[] content = "png-aprovado".getBytes(StandardCharsets.UTF_8);
    BusinessProcessActivityInstance human = approvedHumanInstance();
    when(plans.findById(8L)).thenReturn(java.util.Optional.of(plan));
    when(activityInstances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "creative-production-approval", "experiment:93"))
        .thenReturn(List.of(human));
    when(assets.findByCommercialPlanIdAndCreativePackageIdOrderByCreatedAtAsc(eq(8L), any()))
        .thenReturn(List.of());
    AgentTaskVisualEvidence storedEvidence = new AgentTaskVisualEvidence();
    storedEvidence.setId(407L);
    storedEvidence.setEvidenceType("CREATIVE_RENDER");
    storedEvidence.setContentType("image/png");
    storedEvidence.setSha256(contentHash);
    when(evidence.findByIdAndTaskId(407L, 526L)).thenReturn(java.util.Optional.of(storedEvidence));
    when(evidenceStorage.read(526L, 407L))
        .thenReturn(new AgentTaskVisualEvidenceService.EvidenceContent("image/png", content));
    when(assetStorage.storeBytes(any(), any(), eq("image/png"), any()))
        .thenReturn(
            new AssetStorageService.StoredObject(
                "stored/mira.png",
                "https://cdn.example/mira.png",
                content.length,
                "image/png",
                true));
    AtomicReference<CommercialPlanVisualAsset> saved = new AtomicReference<>();
    when(assets.save(any(CommercialPlanVisualAsset.class)))
        .thenAnswer(
            invocation -> {
              CommercialPlanVisualAsset asset = invocation.getArgument(0);
              asset.setId(701L);
              saved.set(asset);
              return asset;
            });
    when(visualAssets.list(8L)).thenAnswer(invocation -> List.of(dto(saved.get())));

    var result = service.importPreviouslyApproved(8L);

    assertThat(result.commercialPlanId()).isEqualTo(8L);
    assertThat(result.assets())
        .singleElement()
        .satisfies(
            asset -> {
              assertThat(asset.status()).isEqualTo(CommercialPlanVisualAssetStatus.APPROVED);
              assertThat(asset.purposes()).containsExactly("ADS", "LANDING");
              assertThat(asset.agentReviewStatus())
                  .isEqualTo(CommercialPlanVisualAssetReviewStatus.APPROVED);
              assertThat(asset.customerReviewStatus())
                  .isEqualTo(CommercialPlanVisualAssetReviewStatus.APPROVED);
              assertThat(asset.contentSha256()).isEqualTo(contentHash);
            });
    verify(assetStorage).storeBytes(any(), any(), eq("image/png"), any());
  }

  /** Recusa uma decisão humana histórica que aprovou outra versão da seleção criativa. */
  @Test
  void rejectsHistoricalDecisionWithoutMatchingTasksAndArtifact() throws Exception {
    BusinessProcessActivityInstance human = approvedHumanInstance();
    human.setObjectiveEvidenceJson(
        "{\"decision\":\"APPROVE\",\"evidenceReference\":\"artifact:406; agent-task:525\"}");
    when(plans.findById(8L)).thenReturn(java.util.Optional.of(plan));
    when(activityInstances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "creative-production-approval", "experiment:93"))
        .thenReturn(List.of(human));

    assertThatThrownBy(() -> service.importPreviouslyApproved(8L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("mesma peça e estes mesmos pareceres");

    verify(assetStorage, never()).storeBytes(any(), any(), any(), any());
  }

  /** Remove do storage o arquivo criado quando a persistência do pacote não conclui. */
  @Test
  void removesStoredObjectWhenDatabaseFlushFails() throws Exception {
    byte[] content = "png-aprovado".getBytes(StandardCharsets.UTF_8);
    when(plans.findByExperimentReference(93L)).thenReturn(List.of(plan));
    when(assets.findByCommercialPlanIdAndCreativePackageIdOrderByCreatedAtAsc(eq(8L), any()))
        .thenReturn(List.of());
    AgentTaskVisualEvidence storedEvidence = new AgentTaskVisualEvidence();
    storedEvidence.setId(407L);
    storedEvidence.setEvidenceType("CREATIVE_RENDER");
    storedEvidence.setContentType("image/png");
    storedEvidence.setSha256(contentHash);
    when(evidence.findByIdAndTaskId(407L, 526L)).thenReturn(java.util.Optional.of(storedEvidence));
    when(evidenceStorage.read(526L, 407L))
        .thenReturn(new AgentTaskVisualEvidenceService.EvidenceContent("image/png", content));
    when(assetStorage.storeBytes(any(), any(), eq("image/png"), any()))
        .thenReturn(
            new AssetStorageService.StoredObject(
                "stored/mira.png",
                "https://cdn.example/mira.png",
                content.length,
                "image/png",
                true));
    doThrow(new IllegalStateException("falha simulada no banco")).when(assets).flush();

    assertThatThrownBy(() -> service.importForHumanDecision(product, "experiment:93"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("falha simulada no banco");

    verify(assetStorage).deleteStoredObject("stored/mira.png", true);
  }

  /** Remove o arquivo se uma gravação posterior fizer a transação externa inteira retroceder. */
  @Test
  void removesStoredObjectWhenOuterTransactionRollsBack() throws Exception {
    byte[] content = "png-aprovado".getBytes(StandardCharsets.UTF_8);
    when(plans.findByExperimentReference(93L)).thenReturn(List.of(plan));
    when(assets.findByCommercialPlanIdAndCreativePackageIdOrderByCreatedAtAsc(eq(8L), any()))
        .thenReturn(List.of());
    AgentTaskVisualEvidence storedEvidence = new AgentTaskVisualEvidence();
    storedEvidence.setId(407L);
    storedEvidence.setEvidenceType("CREATIVE_RENDER");
    storedEvidence.setContentType("image/png");
    storedEvidence.setSha256(contentHash);
    when(evidence.findByIdAndTaskId(407L, 526L)).thenReturn(java.util.Optional.of(storedEvidence));
    when(evidenceStorage.read(526L, 407L))
        .thenReturn(new AgentTaskVisualEvidenceService.EvidenceContent("image/png", content));
    when(assetStorage.storeBytes(any(), any(), eq("image/png"), any()))
        .thenReturn(
            new AssetStorageService.StoredObject(
                "stored/mira.png",
                "https://cdn.example/mira.png",
                content.length,
                "image/png",
                true));
    AtomicReference<CommercialPlanVisualAsset> saved = new AtomicReference<>();
    when(assets.save(any(CommercialPlanVisualAsset.class)))
        .thenAnswer(
            invocation -> {
              CommercialPlanVisualAsset asset = invocation.getArgument(0);
              asset.setId(701L);
              saved.set(asset);
              return asset;
            });
    when(visualAssets.list(8L)).thenAnswer(invocation -> List.of(dto(saved.get())));

    TransactionSynchronizationManager.initSynchronization();
    try {
      service.importForHumanDecision(product, "experiment:93");
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(
              synchronization ->
                  synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    verify(assetStorage).deleteStoredObject("stored/mira.png", true);
  }

  /** Recusa uma revisão que aprovou outro hash em vez de relaxar o gate da landing. */
  @Test
  void rejectsReviewOfDifferentPixels() throws Exception {
    when(plans.findByExperimentReference(93L)).thenReturn(List.of(plan));
    customer.setResultJson(approvedReview("0".repeat(64)));

    assertThatThrownBy(() -> service.importForHumanDecision(product, "experiment:93"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Psique não auditou os mesmos pixels");

    verify(assetStorage, never()).storeBytes(any(), any(), any(), any());
  }

  /** Mantém a importação idempotente quando o pacote exato já está na biblioteca. */
  @Test
  void reusesExistingPackageWithoutCopyingPixelsAgain() throws Exception {
    when(plans.findByExperimentReference(93L)).thenReturn(List.of(plan));
    CommercialPlanVisualAsset existing = new CommercialPlanVisualAsset();
    existing.setId(701L);
    existing.setAssetUrl("https://cdn.example/mira.png");
    existing.setMediaType("IMAGE");
    existing.setLabel("Mira aprovada");
    existing.setPurpose("ADS");
    existing.setOrigin("Processo aprovado");
    existing.setRightsStatement("Uso autorizado");
    existing.setContentSha256(contentHash);
    existing.setVersionNumber(1);
    existing.setStatus(CommercialPlanVisualAssetStatus.APPROVED);
    existing.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.APPROVED);
    existing.setCustomerReviewStatus(CommercialPlanVisualAssetReviewStatus.APPROVED);
    AtomicReference<String> packageId = new AtomicReference<>();
    when(assets.findByCommercialPlanIdAndCreativePackageIdOrderByCreatedAtAsc(eq(8L), any()))
        .thenAnswer(
            invocation -> {
              packageId.set(invocation.getArgument(1));
              existing.setCreativePackageId(packageId.get());
              return List.of(existing);
            });
    when(visualAssets.list(8L)).thenAnswer(invocation -> List.of(dto(existing)));

    var result = service.importForHumanDecision(product, "experiment:93");

    assertThat(result.creativePackageId()).isEqualTo(packageId.get());
    assertThat(result.assets()).hasSize(1);
    verify(assetStorage, never()).storeBytes(any(), any(), any(), any());
  }

  /** Cria uma tarefa concluída com agente e atividade explícitos. */
  private AgentTask task(long id, String activityId, String agentKey, String resultJson) {
    Agent agent = new Agent();
    agent.setAgentKey(agentKey);
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setAssignedAgent(agent);
    task.setProcessActivityId(activityId);
    task.setStatus("COMPLETED");
    task.setResultJson(resultJson);
    return task;
  }

  /** Monta um parecer aprovado que referencia o artefato e hash informados. */
  private String approvedReview(String hash) {
    return """
        {"decision":"APPROVED","requiredChanges":[],
         "renderedAssetAudit":[{"artifactId":407,"sha256":"%s"}]}
        """
        .formatted(hash);
  }

  /** Monta a ocorrência final que autoriza a reaplicação histórica pela tela. */
  private BusinessProcessActivityInstance approvedHumanInstance() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("creative-production-approval");
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("human");
    activity.setProcessDefinition(process);
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(activity);
    instance.setStatus("COMPLETED");
    instance.setObjectiveAchieved(true);
    instance.setObjectiveEvidenceJson(
        "{\"decision\":\"APPROVE\",\"evidenceReference\":\"artifact:407; agent-task:527 APPROVED; agent-task:528 APPROVED\"}");
    return instance;
  }

  /** Projeta a entidade salva no mesmo contrato devolvido pelo serviço de biblioteca. */
  private CommercialPlanVisualAssetDto dto(CommercialPlanVisualAsset asset) {
    return new CommercialPlanVisualAssetDto(
        asset.getId(),
        asset.getAssetUrl(),
        asset.getMediaType(),
        asset.getLabel(),
        asset.getPurpose(),
        List.of("ADS", "LANDING"),
        asset.getOrigin(),
        asset.getRightsStatement(),
        asset.getContentSha256(),
        asset.getCreativePackageId(),
        asset.getVersionNumber(),
        asset.getStatus(),
        null,
        asset.getAgentReviewStatus(),
        "Aprovado por Têmis",
        asset.getCustomerReviewStatus(),
        "Aprovado por Psique",
        Instant.parse("2026-09-26T15:30:00Z"),
        Instant.parse("2026-09-26T15:30:00Z"));
  }

  /** Calcula o hash usado pelos fixtures de produção e revisão. */
  private String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
