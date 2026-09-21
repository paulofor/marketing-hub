package com.marketinghub.gerasalespage.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.aiprompt.AiPromptSchemaTemplate;
import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentAiPromptSchemaUsageService;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.gerasalespage.v1.service.retry.GeraSalesPageRetryPolicy;
import com.marketinghub.gerasalespage.v1.service.retry.StageRetryView;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.product.Product;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.aiprompt.AiPromptSchemaTemplateRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: controlar fila, auditoria e avanço das etapas do GeraSalesPage v1. */
@Service
public class GeraSalesPageStageService {
  private static final Logger log = LoggerFactory.getLogger(GeraSalesPageStageService.class);
  private static final String PIPELINE_CODE = "gera-sales-page-v1";
  private static final String RETRY_PREFIX = "Retomada técnica da execução: ";
  private static final String STATUS_STARTED = "INICIADO";
  private static final String STATUS_PROCESSING = "EM_PROCESSAMENTO";
  private static final String STATUS_WAITING_OPENAI = "AGUARDANDO_RETORNO_OPENAI";
  private static final String STATUS_COMPLETED = "CONCLUIDO";
  private static final String STATUS_FAILED = "FALHA";
  private static final String STATUS_REPLACED = "SUBSTITUIDO";
  private static final Pattern TECHNICAL_DELIVERABLE_PREFIX =
      Pattern.compile(
          "^(MDS/MUSA|MDS|FEO|entregaveis|entregáveis)\\s*[-/:]\\s*", Pattern.CASE_INSENSITIVE);

  private final ExperimentRepository experimentRepository;
  private final GeraSalesPageStageExecutionRepository executionRepository;
  private final AiPromptSchemaTemplateRepository templateRepository;
  private final DeliverablePackageRepository deliverablePackageRepository;
  private final GeraSalesPagePublicationAuditService publicationAuditService;
  private final ExperimentAiPromptSchemaUsageService promptSchemaUsageService;
  private final CommercialPlanLandingAssetService landingAssetService;
  private final ObjectMapper objectMapper;

  /** Inicializa o service com repositórios e serializador usados pelos contratos internos. */
  public GeraSalesPageStageService(
      ExperimentRepository experimentRepository,
      GeraSalesPageStageExecutionRepository executionRepository,
      AiPromptSchemaTemplateRepository templateRepository,
      DeliverablePackageRepository deliverablePackageRepository,
      GeraSalesPagePublicationAuditService publicationAuditService,
      ExperimentAiPromptSchemaUsageService promptSchemaUsageService,
      CommercialPlanLandingAssetService landingAssetService,
      ObjectMapper objectMapper) {
    this.experimentRepository = experimentRepository;
    this.executionRepository = executionRepository;
    this.templateRepository = templateRepository;
    this.deliverablePackageRepository = deliverablePackageRepository;
    this.publicationAuditService = publicationAuditService;
    this.promptSchemaUsageService = promptSchemaUsageService;
    this.landingAssetService = landingAssetService;
    this.objectMapper = objectMapper;
  }

  /** Inicia o pipeline na primeira etapa, bloqueando experimento sem destino comercial válido. */
  @Transactional
  public GeraSalesPageStartResponse start(Long experimentId) {
    Experiment experiment =
        experimentRepository
            .findForSalesPageRecovery(experimentId)
            .orElseThrow(
                () -> new EntityNotFoundException("Experiment not found: " + experimentId));
    validateCommercialContract(experiment);
    validateCheckoutUrl(experiment);
    GeraSalesPageStageExecution execution =
        enqueue(experimentId, GeraSalesPageStageCode.OFFER_BRIEF.code());
    return new GeraSalesPageStartResponse(
        experimentId, execution.getStageCode(), idJobText(execution), execution.getStatus());
  }

  /** Substitui execuções anteriores e reinicia o GeraSalesPage v1 desde a primeira etapa. */
  @Transactional
  public GeraSalesPageStartResponse rebuild(Long experimentId) {
    Experiment experiment =
        experimentRepository
            .findForSalesPageRecovery(experimentId)
            .orElseThrow(
                () -> new EntityNotFoundException("Experiment not found: " + experimentId));
    validateCommercialContract(experiment);
    validateCheckoutUrl(experiment);
    List<GeraSalesPageStageExecution> executions =
        executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(experimentId);
    executions.forEach(
        execution -> {
          if (!STATUS_REPLACED.equalsIgnoreCase(execution.getStatus())) {
            execution.setStatus(STATUS_REPLACED);
            execution.setErrorMessage(
                "Execução substituída por rebuild manual do GeraSalesPage v1.");
          }
        });
    executionRepository.saveAll(executions);
    GeraSalesPageStageExecution execution =
        createNewExecution(experimentId, GeraSalesPageStageCode.OFFER_BRIEF.code());
    return new GeraSalesPageStartResponse(
        experimentId, execution.getStageCode(), idJobText(execution), execution.getStatus());
  }

  /** Informa a última tentativa e o motivo auditável para permitir ou bloquear sua retomada. */
  @Transactional(readOnly = true)
  public StageRetryView retryView(Long experimentId) {
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () -> new EntityNotFoundException("Experiment not found: " + experimentId));
    return executionRepository
        .findTopByExperimentIdOrderByExecutionRequestedAtDesc(experimentId)
        .map(execution -> retryView(experiment, execution))
        .orElseGet(
            () -> new StageRetryView(null, null, null, false, "Nenhuma geração solicitada."));
  }

  /** Serializa a retomada da tentativa exata, preservando etapas concluídas e a falha original. */
  @Transactional
  public StageRetryView retry(Long experimentId, String failedJobId) {
    Experiment experiment =
        experimentRepository
            .findForSalesPageRecovery(experimentId)
            .orElseThrow(
                () -> new EntityNotFoundException("Experiment not found: " + experimentId));
    var latest =
        executionRepository
            .findTopByExperimentIdOrderByExecutionRequestedAtDesc(experimentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "Nenhuma geração solicitada."));
    if ((RETRY_PREFIX + failedJobId).equals(latest.getErrorDetail())
        || Objects.toString(latest.getErrorDetail(), "")
            .startsWith(RETRY_PREFIX + failedJobId + "\n")) {
      return new StageRetryView(
          latest.getIdJob(),
          latest.getStageCode(),
          latest.getStatus(),
          false,
          "Retomada já registrada. Acompanhe esta mesma tentativa; nenhuma solicitação duplicada foi criada.");
    }
    if (!latest.getIdJob().equals(failedJobId))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A geração mudou. Atualize a tela antes de solicitar a retomada.");
    validateCommercialContract(experiment);
    validateCheckoutUrl(experiment);
    var view = retryView(experiment, latest);
    if (!view.available()) throw new ResponseStatusException(HttpStatus.CONFLICT, view.reason());
    var retry = createNewExecution(experimentId, latest.getStageCode());
    retry.setPrompt(latest.getPrompt());
    retry.setPromptMarkdownContent(latest.getPromptMarkdownContent());
    retry.setSchemaJson(latest.getSchemaJson());
    retry.setOpenAiModel(latest.getOpenAiModel());
    retry.setErrorDetail(RETRY_PREFIX + failedJobId);
    executionRepository.save(retry);
    log.info(
        "GeraSalesPage: retomada técnica solicitada. experimentId={} failedJobId={} retryJobId={} stage={}",
        experimentId,
        failedJobId,
        retry.getIdJob(),
        retry.getStageCode());
    return new StageRetryView(
        retry.getIdJob(),
        retry.getStageCode(),
        retry.getStatus(),
        false,
        "Retomada solicitada somente para a etapa interrompida. Etapas anteriores e histórico preservados.");
  }

  /** Confere a classificação da falha, todos os antecessores e a identidade da entrada enviada. */
  private StageRetryView retryView(Experiment experiment, GeraSalesPageStageExecution execution) {
    String reason =
        switch (Objects.toString(execution.getStatus(), "")) {
          case STATUS_STARTED ->
              "Etapa na fila. Acompanhe esta tentativa; não é necessário refazer a página.";
          case STATUS_PROCESSING, STATUS_WAITING_OPENAI ->
              "Etapa em processamento. Aguarde o resultado desta tentativa.";
          case STATUS_COMPLETED ->
              "Geração concluída. Confira a versão publicada e continue pelo processo corrente.";
          default ->
              "A última etapa não possui falha de transporte recuperável sem resposta. Corrija o bloqueio indicado na auditoria.";
        };
    boolean available = false;
    if (GeraSalesPageRetryPolicy.transportFailureWithoutResult(execution)) {
      var previous = previousStageOutputs(experiment.getId());
      var codes = GeraSalesPageStageCode.orderedCodes();
      int index = codes.indexOf(execution.getStageCode());
      boolean predecessorsComplete =
          index >= 0 && codes.subList(0, index).stream().allMatch(previous::containsKey);
      if (predecessorsComplete
          && GeraSalesPageRetryPolicy.sameInputs(
              execution,
              loadTemplate(execution.getStageCode()),
              experimentPayload(experiment),
              previous,
              objectMapper)) {
        available = true;
        reason =
            "Conexão interrompida sem resposta recuperável. A retomada usa as mesmas entradas e preserva as etapas concluídas. Uma nova chamada de IA pode ter custo; o consumo da tentativa interrompida não foi informado.";
      } else {
        reason =
            "Entradas ou etapas anteriores estão incompletas, alteradas ou sem comprovação de compatibilidade. Confira a origem antes de solicitar nova geração; nenhuma chamada de IA foi feita.";
      }
    }
    return new StageRetryView(
        execution.getIdJob(), execution.getStageCode(), execution.getStatus(), available, reason);
  }

  /**
   * Bloqueia retomada cuja entrada mudou depois da solicitação, antes de entregar trabalho ao
   * executor.
   */
  private boolean retryInputsStillValid(
      GeraSalesPageStageExecution execution, AiPromptSchemaTemplate template) {
    if (execution.getErrorDetail() == null || !execution.getErrorDetail().startsWith(RETRY_PREFIX))
      return true;
    if (GeraSalesPageRetryPolicy.sameInputs(
        execution,
        template,
        experimentPayload(execution.getExperiment()),
        previousStageOutputs(execution.getExperimentId()),
        objectMapper)) return true;
    execution.setStatus(STATUS_FAILED);
    execution.setErrorMessage("Entrada alterada após solicitar retomada; chamada de IA bloqueada.");
    executionRepository.save(execution);
    return false;
  }

  /** Revalida retomadas e entrega pendências válidas com prompt/schema ativo ao worker. */
  @Transactional
  public List<GeraSalesPagePendingResponse> pending(String stageCode) {
    validateStage(stageCode);
    AiPromptSchemaTemplate template = loadTemplate(stageCode);
    return executionRepository
        .findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(stageCode, STATUS_STARTED)
        .stream()
        .filter(execution -> retryInputsStillValid(execution, template))
        .map(execution -> toPendingResponse(execution, template))
        .toList();
  }

  /** Marca uma execução como em processamento para evitar captura concorrente pelo worker. */
  @Transactional
  public void markRunning(String idJob) {
    GeraSalesPageStageExecution execution = findExecution(idJob);
    execution.setStatus(STATUS_PROCESSING);
    execution.setProcessingStartedAt(Instant.now());
    executionRepository.save(execution);
  }

  /** Salva prompt, schema e request bruto enviados pelo worker à OpenAI. */
  @Transactional
  public void receivePrompt(String idJob, GeraSalesPagePromptRequest payload) {
    GeraSalesPageStageExecution execution = findExecution(idJob);
    execution.setStatus(STATUS_WAITING_OPENAI);
    execution.setPrompt(payload.prompt());
    execution.setPromptMarkdownContent(payload.promptMarkdownContent());
    execution.setSchemaJson(payload.schemaJson());
    execution.setOpenAiRequestBody(payload.requestBodyJson());
    execution.setOpenAiModel(payload.openAiModel());
    execution.setOpenAiJobId(payload.openAiJobId());
    executionRepository.save(execution);
  }

  /**
   * Preserva resposta, falha e origem da retomada; enfileira a próxima etapa somente no sucesso.
   */
  @Transactional
  public void receiveResult(String idJob, GeraSalesPageResultRequest payload) {
    GeraSalesPageStageExecution execution = findExecution(idJob);
    if (StringUtils.hasText(payload.errorMessage())) {
      execution.setStatus(STATUS_FAILED);
      execution.setErrorMessage(payload.errorMessage());
      String retryOrigin = Objects.toString(execution.getErrorDetail(), "");
      execution.setErrorDetail(
          retryOrigin.startsWith(RETRY_PREFIX)
              ? retryOrigin.split("\n", 2)[0] + "\n" + Objects.toString(payload.errorDetail(), "")
              : payload.errorDetail());
    } else {
      execution.setStatus(STATUS_COMPLETED);
      execution.setModelResponse(payload.modelResponse());
      execution.setRawResponse(payload.rawResponse());
      execution.setInputTokens(payload.inputTokens());
      execution.setOutputTokens(payload.outputTokens());
      execution.setCostUsd(payload.costUsd());
      execution.setOpenAiJobId(payload.openAiJobId());
      execution.setCompletedAt(Instant.now());
      if (isRejectedQualityReview(execution)) {
        execution.setStatus(STATUS_FAILED);
        execution.setErrorMessage("Quality review reprovou a página; publicação bloqueada.");
        execution.setErrorDetail(extractQualityReviewBlockers(execution.getModelResponse()));
        executionRepository.save(execution);
        return;
      }
      publicationAuditService.snapshotPublication(execution);
      GeraSalesPageStageCode.nextAfter(execution.getStageCode())
          .ifPresent(nextStage -> enqueue(execution.getExperimentId(), nextStage));
    }
    executionRepository.save(execution);
  }

  /** Enfileira uma etapa se ela ainda não estiver concluída ou pendente no experimento. */
  private GeraSalesPageStageExecution enqueue(Long experimentId, String stageCode) {
    return executionRepository
        .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, stageCode)
        .filter(existing -> !STATUS_FAILED.equals(existing.getStatus()))
        .filter(existing -> !STATUS_REPLACED.equals(existing.getStatus()))
        .orElseGet(
            () -> {
              return createNewExecution(experimentId, stageCode);
            });
  }

  /** Cria uma nova execução de etapa com template ativo e idJob único. */
  private GeraSalesPageStageExecution createNewExecution(Long experimentId, String stageCode) {
    AiPromptSchemaTemplate template = loadTemplate(stageCode);
    GeraSalesPageStageExecution execution =
        GeraSalesPageStageExecution.builder()
            .idJob(UUID.randomUUID().toString())
            .experimentId(experimentId)
            .stageCode(stageCode)
            .status(STATUS_STARTED)
            .executionRequestedAt(Instant.now())
            .promptTemplateKey(template.getTemplateKey())
            .build();
    GeraSalesPageStageExecution saved = executionRepository.save(execution);
    promptSchemaUsageService.linkSalesPageTemplate(
        experimentId, template, stageCode, saved.getIdJob());
    return saved;
  }

  /** Converte uma execução persistida no payload de pendência consumido pelo worker. */
  private GeraSalesPagePendingResponse toPendingResponse(
      GeraSalesPageStageExecution execution, AiPromptSchemaTemplate template) {
    Experiment experiment = execution.getExperiment();
    return new GeraSalesPagePendingResponse(
        execution.getExperimentId(),
        execution.getStageCode(),
        idJobText(execution),
        execution.getExecutionRequestedAt(),
        experimentPayload(experiment),
        templatePayload(template),
        previousStageOutputs(execution.getExperimentId()));
  }

  /** Entrega experimento e contrato atual do produto sem inferir condições ausentes da oferta. */
  private Map<String, Object> experimentPayload(Experiment experiment) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", experiment.getId());
    payload.put("name", experiment.getName());
    payload.put("nicheName", experiment.getNiche() != null ? experiment.getNiche().getName() : "");
    payload.put("hypothesis", experiment.getHypothesis());
    payload.put("singlePain", experiment.getSinglePain());
    payload.put("freeReward", experiment.getFreeReward());
    payload.put("funnelPromise", experiment.getFunnelPromise());
    payload.put("primaryCta", experiment.getPrimaryCta());
    payload.put("campaignObjective", experiment.getCampaignObjective());
    payload.put("campaignAngle", parseJsonOrText(experiment.getCampaignAngle()));
    payload.put("adCopy", parseJsonOrText(experiment.getAdCopy()));
    payload.put("adImageBriefing", parseJsonOrText(experiment.getAdImageBriefing()));
    payload.put("checkoutUrl", resolveCheckoutUrl(experiment));
    payload.put("productAiSubtype", experiment.getProductAiSubtype());
    payload.put("salesPageDestination", salesPageDestination(experiment));
    payload.put("unitPrice", experiment.getUnitPrice());
    payload.put("product", productPayload(experiment.getProduct()));
    payload.put(
        "hypothesisFramework", parseJsonOrText(experiment.getHypothesisFrameworkJsonForPending()));
    payload.put("feoDeliverablePackage", latestFeoDeliverablePackage(experiment.getId()));
    payload.put(
        "approvedLandingVisualAssets",
        landingAssetService.payloadForExperiment(experiment.getId()));
    payload.put(
        "minimumApprovedLandingVisualAssets",
        landingAssetService.requiredReferenceCount(experiment.getId()));
    return payload;
  }

  /**
   * Transporta apenas fontes comerciais oficiais, preservando JSON estruturado e dados ausentes.
   */
  private Map<String, Object> productPayload(Product product) {
    if (product == null) return Map.of();
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", product.getId());
    payload.put("name", product.getName());
    payload.put("version", product.getValidationDefinitionVersion());
    payload.put(
        "typeCode",
        product.getProductTypeDefinition() == null
            ? null
            : product.getProductTypeDefinition().getCode());
    payload.put("format", product.getProductFormat());
    payload.put("deliveryMode", product.getDeliveryMode());
    payload.put("revenueModel", product.getRevenueModel());
    payload.put("valueUnit", product.getValueUnit());
    payload.put("priceBrl", product.getCurrentPriceBrl());
    payload.put("promise", product.getPromise());
    payload.put("deliverable", product.getTripwire());
    payload.put("checkoutMonetization", product.getCheckoutMonetization());
    payload.put("riskReversal", product.getRiskReversal());
    payload.put("validationContract", parseJsonOrText(product.getValidationDefinitionJson()));
    payload.put("experienceContract", parseJsonOrText(product.getPdeExperienceJson()));
    return payload;
  }

  /**
   * Monta o pacote FEO mais recente para a página vender entregáveis reais, não itens genéricos.
   */
  private Map<String, Object> latestFeoDeliverablePackage(Long experimentId) {
    return deliverablePackageRepository
        .findByExperimentIdOrderByCreatedAtDesc(experimentId)
        .stream()
        .findFirst()
        .map(this::deliverablePackagePayload)
        .orElseGet(Map::of);
  }

  /** Converte pacote FEO em payload simples e ordenado para os prompts do GeraSalesPage. */
  private Map<String, Object> deliverablePackagePayload(DeliverablePackage deliverablePackage) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", deliverablePackage.getId());
    payload.put("name", deliverablePackage.getName());
    payload.put("description", deliverablePackage.getDescription());
    payload.put("createdAt", deliverablePackage.getCreatedAt());
    payload.put(
        "deliverables",
        deliverablePackage.getDeliverables().stream()
            .sorted(Comparator.comparing(Deliverable::getId))
            .map(this::deliverablePayload)
            .toList());
    return payload;
  }

  /** Converte um entregável FEO para dados comerciais seguros usados na página de venda. */
  private Map<String, Object> deliverablePayload(Deliverable deliverable) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", deliverable.getId());
    payload.put("title", deliverable.getTitle());
    payload.put("publicTitle", publicDeliverableTitle(deliverable.getTitle()));
    payload.put("description", deliverable.getDescription());
    payload.put("publicDescription", publicDeliverableDescription(deliverable.getDescription()));
    return payload;
  }

  /** Converte nomes técnicos de entregáveis em títulos seguros para copy pública. */
  private String publicDeliverableTitle(String title) {
    if (!StringUtils.hasText(title)) {
      return "";
    }
    String publicTitle = title.trim();
    publicTitle = TECHNICAL_DELIVERABLE_PREFIX.matcher(publicTitle).replaceFirst("");
    int slashIndex = publicTitle.lastIndexOf('/');
    if (slashIndex >= 0 && slashIndex < publicTitle.length() - 1) {
      publicTitle = publicTitle.substring(slashIndex + 1);
    }
    publicTitle = publicTitle.replaceFirst("\\.[A-Za-z0-9]+$", "");
    publicTitle = publicTitle.replaceFirst("^kit-\\d+-", "");
    publicTitle = publicTitle.replace('-', ' ');
    return publicTitle.trim().replaceAll("\\s+", " ");
  }

  /** Remove prefixos internos da descrição usada como benefício público do entregável. */
  private String publicDeliverableDescription(String description) {
    if (!StringUtils.hasText(description)) {
      return "";
    }
    return TECHNICAL_DELIVERABLE_PREFIX
        .matcher(description.trim())
        .replaceFirst("")
        .replaceAll("\\s+", " ");
  }

  /**
   * Classifica o destino comercial para o worker nao confundir funil Produto IA com checkout
   * direto.
   */
  private String salesPageDestination(Experiment experiment) {
    if (experiment.getProductAiSubtype() == ProductAiSubtype.AI_PERSONALIZED_SAMPLE) {
      return "LEAD_PORTAL_PERSONALIZED_SAMPLE_FUNNEL";
    }
    return "DIRECT_CHECKOUT";
  }

  /** Monta o bloco de template ativo entregue ao worker. */
  private Map<String, Object> templatePayload(AiPromptSchemaTemplate template) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("templateKey", template.getTemplateKey());
    payload.put("version", template.getVersion());
    payload.put("model", template.getOpenAiModel());
    payload.put("schemaName", template.getSchemaName());
    payload.put("promptMarkdownContent", template.getPromptMarkdownContent());
    payload.put("schemaJson", template.getSchemaJson());
    return payload;
  }

  /**
   * Lê as saídas anteriores concluídas para encadear contexto sem acoplamento entre etapas no
   * worker.
   */
  private Map<String, Object> previousStageOutputs(Long experimentId) {
    Map<String, Object> outputs = new LinkedHashMap<>();
    for (String stage : GeraSalesPageStageCode.orderedCodes()) {
      executionRepository
          .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, stage)
          .filter(execution -> STATUS_COMPLETED.equals(execution.getStatus()))
          .ifPresent(
              execution -> outputs.put(stage, parseJsonOrText(execution.getModelResponse())));
    }
    return outputs;
  }

  /** Carrega o template ativo da etapa e falha cedo se ele não existir. */
  private AiPromptSchemaTemplate loadTemplate(String stageCode) {
    return templateRepository
        .findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(
            PIPELINE_CODE, stageCode)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Template ativo de prompt/schema não encontrado para " + stageCode));
  }

  /** Busca uma execução pelo idJob textual usado nos endpoints. */
  private GeraSalesPageStageExecution findExecution(String idJob) {
    return executionRepository
        .findTopByIdJobOrderByExecutionRequestedAtDesc(idJob)
        .orElseThrow(() -> new EntityNotFoundException("GeraSalesPage job not found: " + idJob));
  }

  /** Valida se a etapa pertence ao GeraSalesPage v1. */
  private void validateStage(String stageCode) {
    if (!GeraSalesPageStageCode.contains(stageCode)) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Etapa GeraSalesPage v1 desconhecida: " + stageCode);
    }
  }

  /** Verifica URL de checkout real e bloqueia âncora local ou URL vazia. */
  private boolean isRealCheckoutUrl(String url) {
    return StringUtils.hasText(url)
        && (url.startsWith("http://") || url.startsWith("https://"))
        && !url.contains("#checkout");
  }

  /** Bloqueia início ou rebuild sem checkout real ou funil de amostra personalizada aprovado. */
  private void validateCheckoutUrl(Experiment experiment) {
    if (usesPersonalizedSampleFunnelAsDestination(experiment)) {
      return;
    }
    if (!isRealCheckoutUrl(resolveCheckoutUrl(experiment))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "GeraSalesPage v1 exige URL real de checkout antes de iniciar.");
    }
  }

  /**
   * Confirma que Produto IA personalizado usa o funil aprovado do Lead Portal como primeiro
   * destino.
   */
  private boolean usesPersonalizedSampleFunnelAsDestination(Experiment experiment) {
    if (experiment == null
        || experiment.getProductAiSubtype() != ProductAiSubtype.AI_PERSONALIZED_SAMPLE) {
      return false;
    }
    if (experiment.getLeadPortalFlow() == null
        || !experiment.getLeadPortalFlow().isApproved()
        || !StringUtils.hasText(experiment.getLeadPortalFlow().getSlug())) {
      return false;
    }
    String destinationPath = destinationPath(experiment.getFollowUpActionUrl());
    String expectedPath =
        "/flows/" + experiment.getLeadPortalFlow().getSlug().trim().toLowerCase(Locale.ROOT);
    return expectedPath.equals(destinationPath);
  }

  /** Extrai o caminho da URL pública para comparar com o slug do funil. */
  private String destinationPath(String url) {
    if (!StringUtils.hasText(url)) {
      return "";
    }
    try {
      return normalizeUrl(URI.create(url.trim()).getPath());
    } catch (IllegalArgumentException ex) {
      return "";
    }
  }

  /** Normaliza caminho ou URL removendo espaços, caixa e barra final. */
  private String normalizeUrl(String url) {
    if (!StringUtils.hasText(url)) {
      return "";
    }
    String normalized = url.trim().toLowerCase(Locale.ROOT);
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  /** Resolve o checkout explícito sem confundir o destino de campanha com pagamento. */
  private String resolveCheckoutUrl(Experiment experiment) {
    if (StringUtils.hasText(experiment.getCommercialCheckoutUrl())) {
      return experiment.getCommercialCheckoutUrl();
    }
    String auditedCheckoutUrl = publicationAuditService.latestCheckoutUrl(experiment.getId());
    if (StringUtils.hasText(auditedCheckoutUrl)) {
      return auditedCheckoutUrl;
    }
    return experiment.getFollowUpActionUrl();
  }

  /** Bloqueia página de venda sem contrato comercial mínimo da etapa Oferta. */
  private void validateCommercialContract(Experiment experiment) {
    if (!StringUtils.hasText(experiment.getSinglePain())
        || !StringUtils.hasText(experiment.getFreeReward())
        || !StringUtils.hasText(experiment.getFunnelPromise())
        || !StringUtils.hasText(experiment.getPrimaryCta())
        || experiment.getUnitPrice() == null
        || experiment.getUnitPrice().signum() <= 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "GeraSalesPage v1 exige contrato comercial completo: dor, prova/preview, promessa, CTA e preço.");
    }
  }

  /** Verifica se a etapa de revisão comercial reprovou a página. */
  private boolean isRejectedQualityReview(GeraSalesPageStageExecution execution) {
    if (!GeraSalesPageStageCode.CHECKOUT_QUALITY_REVIEW.code().equals(execution.getStageCode())) {
      return false;
    }
    try {
      Object approved =
          objectMapper.readValue(execution.getModelResponse(), Map.class).get("approved");
      return Boolean.FALSE.equals(approved);
    } catch (Exception ex) {
      log.warn(
          "Falha ao interpretar quality review do GeraSalesPage job {}", execution.getIdJob(), ex);
      return true;
    }
  }

  /** Extrai os bloqueios de revisão para deixar a causa da falha auditável. */
  private String extractQualityReviewBlockers(String modelResponse) {
    try {
      Object blockers = objectMapper.readValue(modelResponse, Map.class).get("blockers");
      return objectMapper.writeValueAsString(blockers);
    } catch (Exception ex) {
      log.warn("Falha ao extrair bloqueios do quality review do GeraSalesPage.", ex);
      return modelResponse;
    }
  }

  /** Converte JSON textual quando possível, preservando texto simples quando não for JSON. */
  private Object parseJsonOrText(String value) {
    if (!StringUtils.hasText(value)) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(value, Object.class);
    } catch (JsonProcessingException ex) {
      log.debug("Campo textual do GeraSalesPage não é JSON válido; preservando texto.", ex);
      return value;
    }
  }

  /** Converte o idJob binário no texto original salvo pela aplicação. */
  private String idJobText(GeraSalesPageStageExecution execution) {
    return execution.getIdJob();
  }
}
