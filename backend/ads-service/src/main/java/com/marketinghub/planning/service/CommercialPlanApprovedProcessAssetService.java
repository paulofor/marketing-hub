package com.marketinghub.planning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskVisualEvidence;
import com.marketinghub.agenttask.AgentTaskVisualEvidenceService;
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
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: promover peças aprovadas no BPM para a biblioteca visual do plano sem perder
 * hashes, pareceres ou decisão humana.
 */
@Service
@Slf4j
public class CommercialPlanApprovedProcessAssetService {
  static final String PROCESS_CODE = "creative-production-approval";
  static final String HUMAN_ACTIVITY_ID = "human";
  private static final String PRODUCER_ACTIVITY_ID = "nonAudiovisual";
  private static final String CUSTOMER_ACTIVITY_ID = "customer";
  private static final String COMMERCIAL_ACTIVITY_ID = "commercial";
  private static final Pattern EXPERIMENT_REFERENCE = Pattern.compile("^experiment:([1-9][0-9]*)$");
  private static final List<String> PURPOSES = List.of("ADS", "LANDING");

  private final CommercialPlanRepository plans;
  private final CommercialPlanVisualAssetRepository assets;
  private final CommercialPlanVisualAssetService visualAssets;
  private final AgentTaskRepository tasks;
  private final AgentTaskVisualEvidenceRepository evidence;
  private final AgentTaskVisualEvidenceService evidenceStorage;
  private final BusinessProcessActivityInstanceRepository activityInstances;
  private final AssetStorageService assetStorage;
  private final ObjectMapper json;

  /** Configura as fontes canônicas de plano, processo, pixels privados e storage público. */
  public CommercialPlanApprovedProcessAssetService(
      CommercialPlanRepository plans,
      CommercialPlanVisualAssetRepository assets,
      CommercialPlanVisualAssetService visualAssets,
      AgentTaskRepository tasks,
      AgentTaskVisualEvidenceRepository evidence,
      AgentTaskVisualEvidenceService evidenceStorage,
      BusinessProcessActivityInstanceRepository activityInstances,
      AssetStorageService assetStorage,
      ObjectMapper json) {
    this.plans = plans;
    this.assets = assets;
    this.visualAssets = visualAssets;
    this.tasks = tasks;
    this.evidence = evidence;
    this.evidenceStorage = evidenceStorage;
    this.activityInstances = activityInstances;
    this.assetStorage = assetStorage;
    this.json = json;
  }

  /** Expõe se o processo possui peça final e os dois pareceres independentes do mesmo arquivo. */
  @Transactional(readOnly = true)
  public ImportReadiness readiness(Product product, String sourceReference) {
    try {
      CommercialPlan plan = resolvePlan(product, sourceReference);
      ApprovedSelection selection = resolveSelection(plan, sourceReference);
      return new ImportReadiness(
          true,
          "A peça final, Psique e Têmis apontam para os mesmos pixels e podem ser vinculados ao plano.",
          plan.getId(),
          selection.renderedAssets().stream().map(RenderedAsset::artifactId).toList());
    } catch (ResponseStatusException ex) {
      return new ImportReadiness(
          false,
          StringUtils.hasText(ex.getReason())
              ? ex.getReason()
              : "A seleção criativa ainda não está pronta para o plano comercial.",
          null,
          List.of());
    }
  }

  /** Importa as peças durante a própria aprovação humana da atividade canônica. */
  @Transactional
  public ImportResult importForHumanDecision(Product product, String sourceReference) {
    CommercialPlan plan = resolvePlan(product, sourceReference);
    return importSelection(plan, sourceReference, resolveSelection(plan, sourceReference));
  }

  /** Reaplica de forma idempotente uma decisão humana já persistida antes desta integração. */
  @Transactional
  public ImportResult importPreviouslyApproved(Long planId) {
    CommercialPlan plan =
        plans
            .findById(planId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Plano comercial não encontrado."));
    Long experimentId = primaryExperimentId(plan);
    String sourceReference = "experiment:" + experimentId;
    ApprovedSelection selection = resolveSelection(plan, sourceReference);
    requireHumanApproval(sourceReference, selection);
    return importSelection(plan, sourceReference, selection);
  }

  /** Localiza um único plano governante para o experimento e produto informados. */
  private CommercialPlan resolvePlan(Product product, String sourceReference) {
    Long experimentId = experimentId(sourceReference);
    List<CommercialPlan> candidates = plans.findByExperimentReference(experimentId);
    List<CommercialPlan> primary =
        candidates.stream()
            .filter(plan -> Objects.equals(primaryExperimentIdOrNull(plan), experimentId))
            .toList();
    CommercialPlan selected =
        primary.size() == 1
            ? primary.getFirst()
            : candidates.size() == 1 ? candidates.getFirst() : null;
    requireApproval(
        selected != null, "O experimento não possui um único plano comercial governante.");
    Product experimentProduct = experimentProduct(selected, experimentId);
    requireApproval(
        product != null
            && product.getId() != null
            && experimentProduct != null
            && Objects.equals(experimentProduct.getId(), product.getId()),
        "O plano, o experimento e o produto da decisão não correspondem.");
    return selected;
  }

  /** Resolve a última produção concluída e as revisões posteriores que aprovaram o mesmo hash. */
  private ApprovedSelection resolveSelection(CommercialPlan plan, String sourceReference) {
    List<AgentTask> history =
        tasks.findBySourceReferenceAndProcessDefinitionProcessCodeOrderByCreatedAtAscIdAsc(
            sourceReference, PROCESS_CODE);
    AgentTask producer = latestCompleted(history, PRODUCER_ACTIVITY_ID, null);
    requireAgent(producer, "communication-director", "A produção final não pertence a Íris.");
    List<RenderedAsset> renderedAssets = renderedAssets(producer);
    AgentTask customer = latestCompleted(history, CUSTOMER_ACTIVITY_ID, producer.getId());
    requireAgent(customer, "customer-agent", "A revisão de percepção não pertence a Psique.");
    AgentTask commercial = latestCompleted(history, COMMERCIAL_ACTIVITY_ID, producer.getId());
    requireAgent(commercial, "meta-ad-approver", "A revisão de integridade não pertence a Têmis.");
    validateReview(customer, renderedAssets, "Psique");
    validateReview(commercial, renderedAssets, "Têmis");
    requireApproval(
        belongsToPlan(plan, experimentId(sourceReference)),
        "A seleção aprovada pertence a outro plano comercial.");
    return new ApprovedSelection(producer, customer, commercial, renderedAssets);
  }

  /**
   * Persiste os arquivos aprovados em storage público e mantém a operação idempotente por pacote.
   */
  private ImportResult importSelection(
      CommercialPlan plan, String sourceReference, ApprovedSelection selection) {
    String packageId = packageId(sourceReference, selection);
    List<CommercialPlanVisualAsset> existing =
        assets.findByCommercialPlanIdAndCreativePackageIdOrderByCreatedAtAsc(
            plan.getId(), packageId);
    if (!existing.isEmpty()) {
      return result(plan.getId(), sourceReference, packageId);
    }
    List<AssetStorageService.StoredObject> storedObjects = new ArrayList<>();
    try {
      for (RenderedAsset render : selection.renderedAssets()) {
        AgentTaskVisualEvidence storedEvidence =
            evidence
                .findByIdAndTaskId(render.artifactId(), selection.producer().getId())
                .orElseThrow(
                    () -> conflict("A peça aprovada não está persistida na tarefa produtora."));
        requireApproval(
            "CREATIVE_RENDER".equals(storedEvidence.getEvidenceType())
                && "image/png".equalsIgnoreCase(storedEvidence.getContentType())
                && render.sha256().equals(storedEvidence.getSha256()),
            "Metadados e hash da peça aprovada não correspondem ao arquivo persistido.");
        AgentTaskVisualEvidenceService.EvidenceContent content =
            evidenceStorage.read(selection.producer().getId(), render.artifactId());
        requireApproval(
            "image/png".equalsIgnoreCase(content.contentType())
                && render.sha256().equals(sha256(content.bytes())),
            "Os bytes da peça aprovada não correspondem ao hash revisado.");
        AssetStorageService.StoredObject stored =
            assetStorage.storeBytes(
                content.bytes(),
                "approved-process-creative-" + render.artifactId() + ".png",
                "image/png",
                new AssetUploadContext(
                    AssetUploadCategory.COMMERCIAL_PLAN_VISUAL_ASSET,
                    experimentId(sourceReference),
                    null,
                    "plan-" + plan.getId() + "-process-" + selection.producer().getId()));
        storedObjects.add(stored);
        assets.save(
            entity(plan, sourceReference, packageId, selection, render, stored.publicUrl()));
      }
      assets.flush();
      ImportResult imported = result(plan.getId(), sourceReference, packageId);
      removeStoredObjectsIfTransactionRollsBack(plan.getId(), packageId, storedObjects);
      return imported;
    } catch (Exception ex) {
      removeStoredObjects(plan.getId(), packageId, storedObjects);
      log.error(
          "Falha ao vincular peças aprovadas do processo. planId={} sourceReference={} packageId={}",
          plan.getId(),
          sourceReference,
          packageId,
          ex);
      if (ex instanceof RuntimeException runtimeException) throw runtimeException;
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Não foi possível armazenar as peças aprovadas do processo.",
          ex);
    }
  }

  /** Monta o ativo governado com os dois pareceres e a linhagem da tarefa produtora. */
  private CommercialPlanVisualAsset entity(
      CommercialPlan plan,
      String sourceReference,
      String packageId,
      ApprovedSelection selection,
      RenderedAsset render,
      String publicUrl) {
    CommercialPlanVisualAsset asset = new CommercialPlanVisualAsset();
    asset.setCommercialPlan(plan);
    asset.setAssetUrl(publicUrl);
    asset.setMediaType("IMAGE");
    asset.setLabel("Peça aprovada do processo · artefato #" + render.artifactId());
    asset.setPurpose(PURPOSES.getFirst());
    asset.setPurposesJson(visualAssets.writePurposes(PURPOSES));
    asset.setOrigin("Processo BPM " + PROCESS_CODE + " · " + sourceReference);
    asset.setRightsStatement(
        "Pixels persistidos pelo Marketing Hub; linhagem e revisões independentes registradas no mesmo processo.");
    asset.setContentSha256(render.sha256());
    asset.setCreativePackageId(packageId);
    asset.setVersionNumber(1);
    asset.setStatus(CommercialPlanVisualAssetStatus.APPROVED);
    asset.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.APPROVED);
    asset.setReviewerExecutionId("agent-task:" + selection.commercial().getId());
    asset.setAgentReviewJson(selection.commercial().getResultJson());
    asset.setAgentReviewRequestJson(reviewRequest(sourceReference, selection, render));
    asset.setAgentReviewResponseJson(selection.commercial().getResultJson());
    asset.setCustomerReviewStatus(CommercialPlanVisualAssetReviewStatus.APPROVED);
    asset.setCustomerReviewerExecutionId("agent-task:" + selection.customer().getId());
    asset.setCustomerReviewJson(selection.customer().getResultJson());
    return asset;
  }

  /** Expõe somente os ativos do pacote importado e seus identificadores auditáveis. */
  private ImportResult result(Long planId, String sourceReference, String packageId) {
    List<CommercialPlanVisualAssetDto> imported =
        visualAssets.list(planId).stream()
            .filter(asset -> packageId.equals(asset.creativePackageId()))
            .toList();
    requireApproval(!imported.isEmpty(), "A importação não materializou nenhum ativo visual.");
    return new ImportResult(planId, sourceReference, packageId, imported);
  }

  /** Exige a aprovação humana histórica antes de executar o comando de correção pela tela. */
  private void requireHumanApproval(String sourceReference, ApprovedSelection selection) {
    boolean approved =
        activityInstances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                PROCESS_CODE, sourceReference)
            .stream()
            .filter(
                instance ->
                    HUMAN_ACTIVITY_ID.equals(instance.getActivityDefinition().getActivityId())
                        && "COMPLETED".equals(instance.getStatus())
                        && instance.isObjectiveAchieved())
            .anyMatch(
                instance ->
                    approvedDecision(
                        instance.getObjectiveEvidenceJson(), sourceReference, selection));
    requireApproval(
        approved,
        "Registre primeiro a decisão humana sobre esta mesma peça e estes mesmos pareceres.");
  }

  /** Confirma que a decisão persistida aprovou explicitamente esta seleção e não outra versão. */
  private boolean approvedDecision(
      String evidenceJson, String sourceReference, ApprovedSelection selection) {
    if (!StringUtils.hasText(evidenceJson)) return false;
    try {
      JsonNode decision = json.readTree(evidenceJson);
      if (!"APPROVE".equals(decision.path("decision").asText())) return false;
      String expectedPackage = packageId(sourceReference, selection);
      if (expectedPackage.equals(
          decision.path("structuredEvidence").path("creativePackageId").asText())) {
        return true;
      }
      String reference = decision.path("evidenceReference").asText("");
      return referenceContains(reference, "agent-task:" + selection.customer().getId())
          && referenceContains(reference, "agent-task:" + selection.commercial().getId())
          && selection.renderedAssets().stream()
              .allMatch(render -> referenceContains(reference, "artifact:" + render.artifactId()));
    } catch (JsonProcessingException ex) {
      log.warn("Decisão humana de criativo possui evidência inválida.", ex);
      return false;
    }
  }

  /** Procura uma referência auditável inteira e impede colisão entre IDs como 407 e 4070. */
  private boolean referenceContains(String references, String expected) {
    if (!StringUtils.hasText(references)) return false;
    return Pattern.compile("(^|[\\s;,])" + Pattern.quote(expected) + "(?=$|[\\s;,])")
        .matcher(references)
        .find();
  }

  /** Lê e valida a lista de imagens efetivamente renderizadas pela última produção. */
  private List<RenderedAsset> renderedAssets(AgentTask producer) {
    try {
      JsonNode result = json.readTree(producer.getResultJson());
      requireApproval(
          result.path("guardrails").path("noPublication").asBoolean(false)
              && result.path("guardrails").path("noExternalSpend").asBoolean(false),
          "A produção não preservou os limites de publicação e gasto.");
      JsonNode rendered = result.path("functionalOutput").path("renderedAssets");
      requireApproval(
          rendered.isArray() && !rendered.isEmpty(),
          "A produção aprovada não contém imagem final renderizada.");
      List<RenderedAsset> output = new ArrayList<>();
      for (JsonNode item : rendered) {
        long artifactId = item.path("artifactId").asLong(0);
        String hash = item.path("sha256").asText("");
        requireApproval(
            artifactId > 0
                && hash.matches("[0-9a-f]{64}")
                && "PROOF_CARD_V1".equals(item.path("templateVersion").asText()),
            "A identidade de uma peça renderizada está incompleta.");
        output.add(new RenderedAsset(artifactId, hash));
      }
      return List.copyOf(output);
    } catch (JsonProcessingException ex) {
      throw conflict("O resultado da produção criativa não é um JSON válido.", ex);
    }
  }

  /** Confere decisão, ausência de ajustes e auditoria exata de cada artefato revisado. */
  private void validateReview(
      AgentTask review, List<RenderedAsset> renderedAssets, String reviewerName) {
    try {
      JsonNode result = json.readTree(review.getResultJson());
      requireApproval(
          "APPROVED".equals(result.path("decision").asText()),
          reviewerName + " não aprovou a peça final.");
      requireApproval(
          result.path("requiredChanges").isArray() && result.path("requiredChanges").isEmpty(),
          reviewerName + " deixou mudanças obrigatórias.");
      Map<Long, String> audited = new LinkedHashMap<>();
      for (JsonNode item : result.path("renderedAssetAudit")) {
        audited.put(item.path("artifactId").asLong(), item.path("sha256").asText());
      }
      requireApproval(
          audited.size() == renderedAssets.size()
              && renderedAssets.stream()
                  .allMatch(render -> render.sha256().equals(audited.get(render.artifactId()))),
          reviewerName + " não auditou os mesmos pixels da produção final.");
    } catch (JsonProcessingException ex) {
      throw conflict("O parecer de " + reviewerName + " não é um JSON válido.", ex);
    }
  }

  /** Localiza a conclusão mais recente da atividade, depois da produção quando exigido. */
  private AgentTask latestCompleted(
      List<AgentTask> history, String activityId, Long minimumExclusiveTaskId) {
    return history.stream()
        .filter(task -> activityId.equals(task.getProcessActivityId()))
        .filter(task -> "COMPLETED".equals(task.getStatus()))
        .filter(task -> minimumExclusiveTaskId == null || task.getId() > minimumExclusiveTaskId)
        .max(Comparator.comparing(AgentTask::getId))
        .orElseThrow(
            () ->
                conflict(
                    "A atividade " + activityId + " ainda não possui conclusão aprovada vigente."));
  }

  /** Confere a identidade do agente que produziu ou revisou a seleção. */
  private void requireAgent(AgentTask task, String agentKey, String reason) {
    requireApproval(
        task.getAssignedAgent() != null && agentKey.equals(task.getAssignedAgent().getAgentKey()),
        reason);
  }

  /** Monta o contexto mínimo preservado junto do parecer comercial da peça importada. */
  private String reviewRequest(
      String sourceReference, ApprovedSelection selection, RenderedAsset render) {
    ObjectNode request = json.createObjectNode();
    request.put("contractVersion", "APPROVED_PROCESS_VISUAL_ASSET_V1");
    request.put("sourceReference", sourceReference);
    request.put("producerTaskId", selection.producer().getId());
    request.put("customerTaskId", selection.customer().getId());
    request.put("commercialTaskId", selection.commercial().getId());
    request.put("artifactId", render.artifactId());
    request.put("sha256", render.sha256());
    request.put("published", false);
    request.put("externalMediaSpendAuthorized", false);
    return request.toString();
  }

  /** Calcula a identidade idempotente do mesmo produtor e dos mesmos dois pareceres. */
  private String packageId(String sourceReference, ApprovedSelection selection) {
    return sha256(
        ("APPROVED_PROCESS_VISUAL_ASSET_V1|"
                + sourceReference
                + "|"
                + selection.producer().getId()
                + "|"
                + selection.customer().getId()
                + "|"
                + selection.commercial().getId())
            .getBytes(StandardCharsets.UTF_8));
  }

  /** Calcula SHA-256 canônico para comparar bytes e formar a chave do pacote. */
  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }

  /** Remove objetos criados por uma tentativa cujo banco não conseguiu concluir. */
  private void removeStoredObjects(
      Long planId, String packageId, List<AssetStorageService.StoredObject> storedObjects) {
    for (AssetStorageService.StoredObject stored : storedObjects) {
      try {
        assetStorage.deleteStoredObject(stored.storedFileName(), stored.storedInBucket());
      } catch (RuntimeException cleanupError) {
        log.error(
            "Falha ao remover peça órfã do processo. planId={} packageId={} objectKey={}",
            planId,
            packageId,
            stored.storedFileName(),
            cleanupError);
      }
    }
  }

  /** Garante limpeza também quando outra gravação da transação falha depois desta importação. */
  private void removeStoredObjectsIfTransactionRollsBack(
      Long planId, String packageId, List<AssetStorageService.StoredObject> storedObjects) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
    List<AssetStorageService.StoredObject> snapshot = List.copyOf(storedObjects);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
              removeStoredObjects(planId, packageId, snapshot);
            }
          }
        });
  }

  /** Extrai e valida o identificador do experimento na referência operacional. */
  private Long experimentId(String sourceReference) {
    Matcher matcher =
        EXPERIMENT_REFERENCE.matcher(sourceReference == null ? "" : sourceReference.trim());
    requireApproval(matcher.matches(), "A importação exige uma referência experiment:<id>.");
    return Long.parseLong(matcher.group(1));
  }

  /** Retorna o experimento primário obrigatório do plano usado pelo comando histórico. */
  private Long primaryExperimentId(CommercialPlan plan) {
    Long id = primaryExperimentIdOrNull(plan);
    requireApproval(id != null, "O plano não possui experimento primário para importar peças.");
    return id;
  }

  /** Consulta o identificador primário sem lançar erro durante a seleção de candidatos. */
  private Long primaryExperimentIdOrNull(CommercialPlan plan) {
    return plan != null && plan.getExperiment() != null ? plan.getExperiment().getId() : null;
  }

  /** Localiza o produto do experimento primário ou de portfólio dentro do mesmo plano. */
  private Product experimentProduct(CommercialPlan plan, Long experimentId) {
    if (plan.getExperiment() != null
        && Objects.equals(plan.getExperiment().getId(), experimentId)) {
      return plan.getExperiment().getProduct();
    }
    if (plan.getExperiments() == null) return null;
    return plan.getExperiments().stream()
        .filter(experiment -> Objects.equals(experiment.getId(), experimentId))
        .map(com.marketinghub.experiment.Experiment::getProduct)
        .findFirst()
        .orElse(null);
  }

  /** Confirma que o plano contém o experimento indicado pela referência. */
  private boolean belongsToPlan(CommercialPlan plan, Long experimentId) {
    return experimentProduct(plan, experimentId) != null;
  }

  /** Converte inconsistência funcional em conflito sem relaxar o gate visual. */
  private void requireApproval(boolean condition, String reason) {
    if (!condition) throw conflict(reason);
  }

  /** Cria um conflito funcional com mensagem objetiva para a tela. */
  private ResponseStatusException conflict(String reason) {
    return new ResponseStatusException(HttpStatus.CONFLICT, reason);
  }

  /** Cria um conflito funcional preservando a causa técnica para logs e diagnóstico. */
  private ResponseStatusException conflict(String reason, Exception cause) {
    return new ResponseStatusException(HttpStatus.CONFLICT, reason, cause);
  }

  /** Expõe a prontidão sem transportar prompts ou pareceres brutos para a tela. */
  public record ImportReadiness(
      boolean ready, String reason, Long commercialPlanId, List<Long> artifactIds) {
    /** Preserva a lista de artefatos como snapshot imutável. */
    public ImportReadiness {
      artifactIds = artifactIds == null ? List.of() : List.copyOf(artifactIds);
    }
  }

  /** Expõe o resultado idempotente usado pela decisão humana e pelo comando de correção. */
  public record ImportResult(
      Long commercialPlanId,
      String sourceReference,
      String creativePackageId,
      List<CommercialPlanVisualAssetDto> assets) {
    /** Preserva os ativos importados como snapshot imutável. */
    public ImportResult {
      assets = assets == null ? List.of() : List.copyOf(assets);
    }
  }

  /** Preserva as tarefas e imagens exatas que formam uma seleção aprovada. */
  private record ApprovedSelection(
      AgentTask producer,
      AgentTask customer,
      AgentTask commercial,
      List<RenderedAsset> renderedAssets) {}

  /** Preserva a identidade mínima dos pixels revisados. */
  private record RenderedAsset(long artifactId, String sha256) {}
}
