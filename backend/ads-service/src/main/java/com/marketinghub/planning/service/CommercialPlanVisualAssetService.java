package com.marketinghub.planning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskExecutionAuditRequest;
import com.marketinghub.agenttask.AgentTaskModelUsageRequest;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.ImportedCompletedAgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.dto.CommercialPlanVisualAssetDto;
import com.marketinghub.planning.dto.CreateCommercialPlanVisualAssetRequest;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: governar o cadastro, aprovação e consumo da biblioteca audiovisual comercial.
 */
@Service
public class CommercialPlanVisualAssetService {
  private static final Logger log = LoggerFactory.getLogger(CommercialPlanVisualAssetService.class);
  private static final Set<String> PURPOSES =
      Set.of("ADS", "LANDING", "SOCIAL", "DELIVERY", "PRODUCT_PROOF");
  private final CommercialPlanService planService;
  private final CommercialPlanVisualAssetRepository repository;
  private final ObjectMapper objectMapper;
  private final AssetStorageService storageService;
  private final CommercialPlanVersionService versionService;
  private final BusinessProcessDefinitionRepository processRepository;
  private final AgentTaskService agentTaskService;

  /** Inicializa a governança com as fontes canônicas de plano e kit visual. */
  @Autowired
  public CommercialPlanVisualAssetService(
      CommercialPlanService planService,
      CommercialPlanVisualAssetRepository repository,
      ObjectMapper objectMapper,
      AssetStorageService storageService,
      CommercialPlanVersionService versionService,
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskService agentTaskService) {
    this.planService = planService;
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.storageService = storageService;
    this.versionService = versionService;
    this.processRepository = processRepository;
    this.agentTaskService = agentTaskService;
  }

  /** Mantém a construção direta de testes legados com serialização JSON padrão. */
  CommercialPlanVisualAssetService(
      CommercialPlanService planService, CommercialPlanVisualAssetRepository repository) {
    this(planService, repository, new ObjectMapper(), null, null, null, null);
  }

  /**
   * Importa pela decisão humana da tela um pacote local íntegro, seus ativos e os pareceres reais.
   */
  @Transactional
  public List<CommercialPlanVisualAssetDto> importApprovedPackage(
      Long planId, byte[] archiveBytes) {
    if (storageService == null
        || versionService == null
        || processRepository == null
        || agentTaskService == null) {
      throw new IllegalStateException("Importação de pacote indisponível nesta configuração.");
    }
    CommercialPlan plan = planService.getPlan(planId);
    ApprovedCreativePackageArchive.ValidatedPackage approvedPackage =
        new ApprovedCreativePackageArchive(objectMapper).validate(archiveBytes, planId);
    validateExperiment(plan, approvedPackage.contract());
    List<CommercialPlanVisualAsset> existing =
        repository.findByCommercialPlanIdAndCreativePackageIdOrderByCreatedAtAsc(
            planId, approvedPackage.packageId());
    if (!existing.isEmpty()) return existing.stream().map(this::dto).toList();

    List<AssetStorageService.StoredObject> storedObjects = new ArrayList<>();
    try {
      List<CommercialPlanVisualAsset> imported = new ArrayList<>();
      for (ApprovedCreativePackageArchive.PackageAsset asset : approvedPackage.assets()) {
        StoredAsset storedAsset = storeApprovedAsset(plan, approvedPackage, asset);
        storedObjects.add(storedAsset.storedObject());
        imported.add(repository.save(storedAsset.entity()));
      }
      repository.flush();
      recordAgentReviews(planId, approvedPackage, imported);
      return imported.stream().map(this::dto).toList();
    } catch (Exception ex) {
      removeStoredObjects(planId, approvedPackage.packageId(), storedObjects);
      log.error(
          "Falha ao importar pacote criativo aprovado. planId={} packageId={}",
          planId,
          approvedPackage.packageId(),
          ex);
      if (ex instanceof RuntimeException runtimeException) throw runtimeException;
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível armazenar o pacote criativo.", ex);
    }
  }

  /** Lista todo o kit visual, incluindo rascunhos e itens retirados. */
  @Transactional(readOnly = true)
  public List<CommercialPlanVisualAssetDto> list(Long planId) {
    planService.getPlan(planId);
    return repository.findByCommercialPlanIdOrderByCreatedAtAsc(planId).stream()
        .map(this::dto)
        .toList();
  }

  /** Cadastra uma nova versão como rascunho para revisão independente. */
  @Transactional
  public CommercialPlanVisualAssetDto create(
      Long planId, CreateCommercialPlanVisualAssetRequest request) {
    require(request.assetUrl(), "assetUrl");
    require(request.mediaType(), "mediaType");
    require(request.label(), "label");
    require(request.purpose(), "purpose");
    require(request.origin(), "origin");
    require(request.rightsStatement(), "rightsStatement");
    CommercialPlanVisualAsset asset = new CommercialPlanVisualAsset();
    asset.setCommercialPlan(planService.getPlan(planId));
    asset.setAssetUrl(request.assetUrl().trim());
    asset.setMediaType(normalizeMediaType(request.mediaType()));
    asset.setLabel(request.label().trim());
    asset.setPurpose(normalizePurpose(request.purpose()));
    asset.setPurposesJson(writePurposes(List.of(asset.getPurpose())));
    asset.setOrigin(request.origin().trim());
    asset.setRightsStatement(request.rightsStatement().trim());
    asset.setVersionNumber(
        (int) repository.countByCommercialPlanIdAndAssetUrl(planId, asset.getAssetUrl()) + 1);
    asset.setStatus(CommercialPlanVisualAssetStatus.DRAFT);
    return dto(repository.save(asset));
  }

  /** Registra aprovação ou retirada sem apagar o histórico visual. */
  @Transactional
  public CommercialPlanVisualAssetDto updateStatus(
      Long planId, Long assetId, CommercialPlanVisualAssetStatus status) {
    if (status == null || status == CommercialPlanVisualAssetStatus.DRAFT) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "status must be APPROVED or RETIRED");
    }
    CommercialPlanVisualAsset asset = repository.findById(assetId).orElseThrow();
    if (!planId.equals(asset.getCommercialPlan().getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "visual asset not found for plan");
    }
    if (status == CommercialPlanVisualAssetStatus.APPROVED
        && asset.getAgentReviewStatus() != null
        && asset.getAgentReviewStatus()
            != com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus
                .APPROVED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A imagem materializada por Íris exige revisão independente antes da aprovação.");
    }
    asset.setStatus(status);
    return dto(asset);
  }

  /** Produz o manifesto de referências aprovadas entregue aos produtores e revisores. */
  @Transactional(readOnly = true)
  public List<CommercialPlanVisualAssetDto> approved(Long planId) {
    return repository
        .findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            planId, CommercialPlanVisualAssetStatus.APPROVED)
        .stream()
        .map(this::dto)
        .toList();
  }

  /** Converte a entidade persistida para o contrato público. */
  private CommercialPlanVisualAssetDto dto(CommercialPlanVisualAsset asset) {
    return new CommercialPlanVisualAssetDto(
        asset.getId(),
        asset.getAssetUrl(),
        asset.getMediaType(),
        asset.getLabel(),
        asset.getPurpose(),
        readPurposes(asset),
        asset.getOrigin(),
        asset.getRightsStatement(),
        asset.getContentSha256(),
        asset.getCreativePackageId(),
        asset.getVersionNumber(),
        asset.getStatus(),
        asset.getSourceVisualAsset() != null ? asset.getSourceVisualAsset().getId() : null,
        asset.getAgentReviewStatus(),
        reviewSummary(asset),
        asset.getCustomerReviewStatus(),
        customerReviewSummary(asset),
        asset.getCreatedAt(),
        asset.getUpdatedAt());
  }

  /** Confere se o experimento congelado no pacote pertence ao plano selecionado. */
  private void validateExperiment(CommercialPlan plan, JsonNode contract) {
    long experimentId = contract.path("product").path("experimentId").asLong(-1);
    boolean primary = plan.getExperiment() != null && plan.getExperiment().getId() == experimentId;
    boolean portfolio =
        plan.getExperiments() != null
            && plan.getExperiments().stream().anyMatch(item -> item.getId() == experimentId);
    if (!primary && !portfolio) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O experimento do pacote não pertence ao plano comercial.");
    }
  }

  /** Armazena um arquivo validado e monta a entidade com os dois pareceres independentes. */
  private StoredAsset storeApprovedAsset(
      CommercialPlan plan,
      ApprovedCreativePackageArchive.ValidatedPackage approvedPackage,
      ApprovedCreativePackageArchive.PackageAsset asset)
      throws IOException {
    byte[] content = approvedPackage.entries().get(asset.archivePath());
    String contentType = "VIDEO".equals(asset.mediaType()) ? "video/mp4" : "image/png";
    AssetStorageService.StoredObject stored =
        storageService.storeBytes(
            content,
            asset.fileName(),
            contentType,
            new AssetUploadContext(
                AssetUploadCategory.COMMERCIAL_PLAN_VISUAL_ASSET,
                approvedPackage.contract().path("product").path("experimentId").asLong(),
                null,
                "plan-" + plan.getId() + "-package-" + approvedPackage.packageId()));
    CommercialPlanVisualAsset entity = new CommercialPlanVisualAsset();
    entity.setCommercialPlan(plan);
    entity.setAssetUrl(stored.publicUrl());
    entity.setMediaType(asset.mediaType());
    entity.setLabel(asset.fileName());
    entity.setPurpose(asset.purposes().getFirst());
    entity.setPurposesJson(writePurposes(asset.purposes()));
    entity.setOrigin(asset.origin());
    entity.setRightsStatement(asset.rightsStatement());
    entity.setContentSha256(asset.sha256());
    entity.setCreativePackageId(approvedPackage.packageId());
    entity.setVersionNumber(1);
    entity.setStatus(CommercialPlanVisualAssetStatus.APPROVED);
    entity.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.APPROVED);
    entity.setReviewerExecutionId(approvedPackage.temisExecution().executionId());
    entity.setAgentReviewJson(approvedPackage.temisReview().toString());
    entity.setAgentReviewRequestJson(reviewRequest(approvedPackage));
    entity.setAgentReviewResponseJson(approvedPackage.temisReview().toString());
    entity.setCustomerReviewStatus(CommercialPlanVisualAssetReviewStatus.APPROVED);
    entity.setCustomerReviewerExecutionId(approvedPackage.psiqueExecution().executionId());
    entity.setCustomerReviewJson(approvedPackage.psiqueReview().toString());
    return new StoredAsset(entity, stored);
  }

  /** Remove do storage arquivos de uma tentativa cujo registro transacional não foi concluído. */
  private void removeStoredObjects(
      Long planId, String packageId, List<AssetStorageService.StoredObject> storedObjects) {
    for (AssetStorageService.StoredObject storedObject : storedObjects) {
      try {
        storageService.deleteStoredObject(
            storedObject.storedFileName(), storedObject.storedInBucket());
      } catch (RuntimeException cleanupError) {
        log.error(
            "Falha ao remover ativo órfão de pacote criativo. planId={} packageId={} objectKey={}",
            planId,
            packageId,
            storedObject.storedFileName(),
            cleanupError);
      }
    }
  }

  /**
   * Consolida os insumos auditáveis entregues aos revisores sem serializar JSON como texto interno.
   */
  private String reviewRequest(ApprovedCreativePackageArchive.ValidatedPackage approvedPackage) {
    ObjectNode request = objectMapper.createObjectNode();
    request.set("contract", approvedPackage.contract());
    request.set("manifest", approvedPackage.manifest());
    request.set("direction", approvedPackage.direction());
    request.set("apolloStoryboard", approvedPackage.apollo());
    return request.toString();
  }

  /** Registra no BPM os pareceres realmente executados e a evidência do mesmo manifesto. */
  private void recordAgentReviews(
      Long planId,
      ApprovedCreativePackageArchive.ValidatedPackage approvedPackage,
      List<CommercialPlanVisualAsset> imported) {
    BusinessProcessDefinition process =
        processRepository
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
                "creative-production-approval", "PUBLISHED")
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "O processo de criação e aprovação não está publicado."));
    int version = versionService.current(planId).versionNumber();
    String sourceReference = "commercial-plan:" + planId + "@v" + version;
    String evidence = reviewEvidence(approvedPackage, imported);
    agentTaskService.recordImportedCompletedTask(
        importedTask(
            "communication-director",
            "Íris",
            "nonAudiovisual",
            approvedPackage.direction(),
            approvedPackage.directionExecution(),
            sourceReference,
            process,
            evidence));
    agentTaskService.recordImportedCompletedTask(
        importedTask(
            "videomaker",
            "Apolo",
            "audiovisual",
            approvedPackage.apollo(),
            approvedPackage.apolloExecution(),
            sourceReference,
            process,
            evidence));
    agentTaskService.recordImportedCompletedTask(
        importedTask(
            "customer-agent",
            "Psique",
            "customer",
            approvedPackage.psiqueReview(),
            approvedPackage.psiqueExecution(),
            sourceReference,
            process,
            evidence));
    agentTaskService.recordImportedCompletedTask(
        importedTask(
            "meta-ad-approver",
            "Têmis",
            "commercial",
            approvedPackage.temisReview(),
            approvedPackage.temisExecution(),
            sourceReference,
            process,
            evidence));
  }

  /** Monta o contrato interno que preserva tokens, modelo e execução importada. */
  private ImportedCompletedAgentTask importedTask(
      String agentKey,
      String agentName,
      String activityId,
      JsonNode result,
      ApprovedCreativePackageArchive.AgentExecution execution,
      String sourceReference,
      BusinessProcessDefinition process,
      String evidence) {
    return new ImportedCompletedAgentTask(
        agentKey,
        "Operador humano pela biblioteca visual",
        "Execução de " + agentName + " importada com pacote criativo aprovado",
        "Preserva o resultado local real do pacote selecionado pela pessoa operadora.",
        sourceReference,
        process.getId(),
        activityId,
        result.toString(),
        evidence,
        List.of(
            new AgentTaskModelUsageRequest(
                execution.model(),
                "STANDARD",
                execution.inputTokens(),
                execution.cachedInputTokens(),
                execution.outputTokens())),
        new AgentTaskExecutionAuditRequest(
            execution.model(), execution.reasoningEffort(), execution.prompt()));
  }

  /** Expõe hashes e URLs persistidos, deixando explícita a ausência de publicação ou gasto. */
  private String reviewEvidence(
      ApprovedCreativePackageArchive.ValidatedPackage approvedPackage,
      List<CommercialPlanVisualAsset> imported) {
    ObjectNode evidence = objectMapper.createObjectNode();
    evidence.put("creativePackageId", approvedPackage.packageId());
    evidence.set("manifest", approvedPackage.manifest());
    evidence.set(
        "assets",
        objectMapper.valueToTree(
            imported.stream()
                .map(
                    asset ->
                        java.util.Map.of(
                            "url", asset.getAssetUrl(),
                            "sha256", asset.getContentSha256(),
                            "purpose", asset.getPurpose()))
                .toList()));
    evidence.put("importedByHuman", true);
    evidence.put("published", false);
    evidence.put("externalMediaSpendUsd", 0);
    return evidence.toString();
  }

  /** Serializa finalidades reutilizáveis sem acoplar a entidade a uma coluna JSON nativa. */
  public String writePurposes(List<String> purposes) {
    try {
      return objectMapper.writeValueAsString(purposes);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Finalidades da mídia inválidas", ex);
    }
  }

  /** Recupera finalidades novas e mantém compatibilidade com itens de finalidade única. */
  public List<String> readPurposes(CommercialPlanVisualAsset asset) {
    if (!StringUtils.hasText(asset.getPurposesJson())) {
      return StringUtils.hasText(asset.getPurpose()) ? List.of(asset.getPurpose()) : List.of();
    }
    try {
      return objectMapper.readValue(
          asset.getPurposesJson(),
          objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Finalidades persistidas da mídia são inválidas", ex);
    }
  }

  /** Extrai o resumo funcional sem expor request e response brutos na tela. */
  private String reviewSummary(CommercialPlanVisualAsset asset) {
    if (!StringUtils.hasText(asset.getAgentReviewJson())) {
      return null;
    }
    try {
      JsonNode review = objectMapper.readTree(asset.getAgentReviewJson());
      return firstText(review, "summary", "commercialRationale");
    } catch (JsonProcessingException ex) {
      return "Parecer persistido indisponível para leitura";
    }
  }

  /** Extrai a perspectiva funcional de Psique sem expor o parecer bruto. */
  private String customerReviewSummary(CommercialPlanVisualAsset asset) {
    if (!StringUtils.hasText(asset.getCustomerReviewJson())) return null;
    try {
      JsonNode review = objectMapper.readTree(asset.getCustomerReviewJson());
      return firstText(review, "summary", "customerPerspective");
    } catch (JsonProcessingException ex) {
      return "Parecer persistido indisponível para leitura";
    }
  }

  /** Retorna o primeiro campo textual preenchido de um parecer versionado. */
  private String firstText(JsonNode node, String... fields) {
    for (String field : fields) {
      String value = node.path(field).asText(null);
      if (StringUtils.hasText(value)) return value;
    }
    return null;
  }

  /** Exige metadado textual preenchido antes de persistir a referência. */
  private void require(String value, String field) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
    }
  }

  /** Normaliza e restringe o tipo às mídias que os executores conseguem consumir. */
  private String normalizeMediaType(String mediaType) {
    String normalized = mediaType.trim().toUpperCase();
    if (!normalized.equals("IMAGE") && !normalized.equals("VIDEO")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mediaType must be IMAGE or VIDEO");
    }
    return normalized;
  }

  /** Normaliza e restringe a finalidade aos papéis canônicos da biblioteca audiovisual. */
  private String normalizePurpose(String purpose) {
    String normalized = purpose.trim().toUpperCase();
    if (!PURPOSES.contains(normalized)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "purpose must be ADS, LANDING, SOCIAL, DELIVERY or PRODUCT_PROOF");
    }
    return normalized;
  }

  /** Mantém juntos a entidade ainda não persistida e o objeto criado no storage. */
  private record StoredAsset(
      CommercialPlanVisualAsset entity, AssetStorageService.StoredObject storedObject) {}
}
