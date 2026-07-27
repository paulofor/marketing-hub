package com.marketinghub.feo.fabricacao.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageExecution;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageStatus;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1CompleteRequest;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1ExecutionSummaryResponse;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1FailureRequest;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1PendingResponse;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1StartResponse;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.repository.jpa.deliverable.DeliverablePackageRepository;
import com.marketinghub.repository.jpa.deliverable.DeliverableRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.feo.fabricacao.v1.FeoFabricacaoV1StageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Responsabilidade: orquestrar a fila backend da FEO v1 para fabricação de entregáveis de
 * experimentos.
 */
@Service
public class FeoFabricacaoV1Service {

  private static final Logger log = LoggerFactory.getLogger(FeoFabricacaoV1Service.class);
  private static final String STAGE_PLANEJAMENTO = "planejamento-entregaveis";
  private static final String STAGE_REDACAO = "redacao-entregaveis";
  private static final String STAGE_ATIVOS_VISUAIS = "geracao-ativos-visuais";
  private static final String STAGE_MONTAGEM = "montagem-pacote";
  private static final Duration RUNNING_RETRY_AFTER = Duration.ofMinutes(15);
  private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
      new TypeReference<>() {};
  private static final TypeReference<List<Map<String, Object>>> ARTIFACT_LIST_TYPE =
      new TypeReference<>() {};

  private final ExperimentRepository experimentRepository;
  private final DeliverableRepository deliverableRepository;
  private final DeliverablePackageRepository deliverablePackageRepository;
  private final FeoFabricacaoV1StageExecutionRepository executionRepository;
  private final ObjectMapper objectMapper;

  /** Inicializa o serviço com repositórios canônicos e serializador JSON. */
  public FeoFabricacaoV1Service(
      ExperimentRepository experimentRepository,
      DeliverableRepository deliverableRepository,
      DeliverablePackageRepository deliverablePackageRepository,
      FeoFabricacaoV1StageExecutionRepository executionRepository,
      ObjectMapper objectMapper) {
    this.experimentRepository = experimentRepository;
    this.deliverableRepository = deliverableRepository;
    this.deliverablePackageRepository = deliverablePackageRepository;
    this.executionRepository = executionRepository;
    this.objectMapper = objectMapper;
  }

  /** Cria a etapa inicial de fabricação de entregáveis para um experimento. */
  @Transactional
  public FeoFabricacaoV1StartResponse startForExperiment(Long experimentId) {
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () -> new EntityNotFoundException("Experiment not found: " + experimentId));
    if (hasActiveInitialExecution(experimentId)) {
      FeoFabricacaoV1StageExecution active =
          executionRepository.findTop20ByExperimentIdOrderByCreatedAtDesc(experimentId).stream()
              .filter(item -> STAGE_PLANEJAMENTO.equals(item.getStageCode()))
              .filter(
                  item ->
                      item.getStatus() == FeoFabricacaoV1StageStatus.PENDING
                          || item.getStatus() == FeoFabricacaoV1StageStatus.RUNNING)
              .findFirst()
              .orElseThrow();
      return toStartResponse(active);
    }

    FeoFabricacaoV1StageExecution execution =
        FeoFabricacaoV1StageExecution.builder()
            .experiment(experiment)
            .jobId("feo-exp-" + experiment.getId() + "-" + UUID.randomUUID())
            .stageCode(STAGE_PLANEJAMENTO)
            .status(FeoFabricacaoV1StageStatus.PENDING)
            .inputPayload(toJson(buildFabricationContext(experiment)))
            .build();
    return toStartResponse(executionRepository.save(execution));
  }

  /** Lista execuções recentes da FEO para o experimento. */
  @Transactional(readOnly = true)
  public List<FeoFabricacaoV1ExecutionSummaryResponse> listByExperiment(Long experimentId) {
    return executionRepository.findTop20ByExperimentIdOrderByCreatedAtDesc(experimentId).stream()
        .map(this::toSummary)
        .toList();
  }

  /** Lista pendências de uma etapa para consumo pelo worker FEO. */
  @Transactional
  public List<FeoFabricacaoV1PendingResponse> listPending(String stageCode, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 50));
    return executionRepository
        .findPendingOrStaleRunning(
            stageCode, Instant.now().minus(RUNNING_RETRY_AFTER), PageRequest.of(0, safeLimit))
        .stream()
        .map(this::markRunningAndMap)
        .toList();
  }

  /** Registra conclusão, persiste saída e enfileira próxima etapa quando o contrato permitir. */
  @Transactional
  public void complete(String stageCode, Long executionId, FeoFabricacaoV1CompleteRequest request) {
    FeoFabricacaoV1StageExecution execution = findExecution(stageCode, executionId);
    execution.setWorkerId(request.workerId());
    execution.setOutputPayload(toJson(request.output()));
    execution.setArtifactsPayload(toJson(request.artifacts()));
    execution.setMetricsPayload(toJson(request.metrics()));
    execution.setBlockReason(trimToNull(request.blockReason()));
    execution.setNextStageCode(trimToNull(request.nextStageCode()));
    execution.setFinishedAt(Instant.now());
    execution.setStatus(resolveCompletedStatus(request));
    executionRepository.save(execution);

    if (execution.getStatus() == FeoFabricacaoV1StageStatus.COMPLETED) {
      enqueueNextStageWhenAllowed(execution, request);
      materializePackageWhenFinal(stageCode, execution, request);
    }
  }

  /** Registra falha técnica reportada pelo worker FEO. */
  @Transactional
  public void fail(String stageCode, Long executionId, FeoFabricacaoV1FailureRequest request) {
    FeoFabricacaoV1StageExecution execution = findExecution(stageCode, executionId);
    execution.setWorkerId(request.workerId());
    execution.setErrorMessage(trimToNull(request.error()));
    execution.setFinishedAt(Instant.now());
    execution.setStatus(FeoFabricacaoV1StageStatus.FAILED);
    executionRepository.save(execution);
  }

  /** Marca a execução como assumida e converte para o contrato pending do worker. */
  private FeoFabricacaoV1PendingResponse markRunningAndMap(
      FeoFabricacaoV1StageExecution execution) {
    execution.setStatus(FeoFabricacaoV1StageStatus.RUNNING);
    execution.setStartedAt(Instant.now());
    FeoFabricacaoV1StageExecution saved = executionRepository.save(execution);
    return new FeoFabricacaoV1PendingResponse(
        saved.getJobId(),
        String.valueOf(saved.getId()),
        fromJson(saved.getInputPayload(), MAP_TYPE),
        Map.of("experimentId", saved.getExperiment().getId()));
  }

  /**
   * Monta contexto mínimo de fabricação usando experimento, hipótese e pacote de entregáveis
   * existente.
   */
  private Map<String, Object> buildFabricationContext(Experiment experiment) {
    Hypothesis hypothesis = experiment.getHypothesisRef();
    DeliverablePackage latestPackage = latestPackage(experiment.getId());
    boolean musaContext = isMusaContext(experiment, latestPackage, hypothesis);
    List<String> deliverables =
        latestPackage != null
            ? latestPackage.getDeliverables().stream().map(Deliverable::getTitle).toList()
            : fallbackDeliverables(hypothesis);

    Map<String, Object> context = new LinkedHashMap<>();
    context.put("requestId", "experiment-" + experiment.getId());
    context.put("experimentId", String.valueOf(experiment.getId()));
    context.put("offerName", publicOfferName(experiment, latestPackage));
    context.put("niche", experiment.getNiche() != null ? experiment.getNiche().getName() : null);
    context.put("centralPromise", publicPromise(experiment, latestPackage, hypothesis));
    context.put("promisedResult", publicResult(experiment, latestPackage, hypothesis));
    context.put("coreMechanism", publicMechanism(experiment, latestPackage, hypothesis));
    context.put("proofSummary", publicProofSummary(experiment, hypothesis, musaContext));
    context.put("deliverables", publicDeliverables(deliverables, musaContext));
    context.put(
        "validationSignals",
        List.of(
            "Produto pronto para revisão editorial de entrega.",
            "Pacote gerado para orientar aplicação prática da cliente."));
    return context;
  }

  /** Define nome público do produto sem rótulos internos de pacote ou FEO. */
  private String publicOfferName(Experiment experiment, DeliverablePackage latestPackage) {
    String source = latestPackage != null ? latestPackage.getName() : experiment.getName();
    if (containsMusa(source) || containsMusa(experiment.getHypothesis())) {
      return "Método MUSA - Presença Elegante em 7 Dias";
    }
    return cleanPublicText(source);
  }

  /** Define promessa pública que pode aparecer para a compradora. */
  private String publicPromise(
      Experiment experiment, DeliverablePackage latestPackage, Hypothesis hypothesis) {
    String source =
        firstText(
            experiment.getFunnelPromise(),
            experiment.getHypothesis(),
            hypothesis != null ? hypothesis.getPromise() : null);
    if (containsMusa(source)
        || containsMusa(latestPackage != null ? latestPackage.getName() : null)) {
      return "Monte em 7 dias uma presença mais elegante, marcante e coerente sem depender de luxo caro, compras impulsivas ou transformação radical.";
    }
    return cleanPublicText(source);
  }

  /** Define resultado público esperado para orientar o conteúdo final. */
  private String publicResult(
      Experiment experiment, DeliverablePackage latestPackage, Hypothesis hypothesis) {
    String source =
        firstText(
            experiment.getFreeReward(),
            experiment.getSinglePain(),
            hypothesis != null ? hypothesis.getEntrega() : null);
    if (containsMusa(source)
        || containsMusa(latestPackage != null ? latestPackage.getName() : null)) {
      return "Sair com diagnóstico, plano de 7 dias, checklists e templates para aplicar microajustes de presença elegante no dia a dia.";
    }
    return cleanPublicText(source);
  }

  /** Define mecanismo em linguagem de cliente, sem expor validação interna. */
  private String publicMechanism(
      Experiment experiment, DeliverablePackage latestPackage, Hypothesis hypothesis) {
    String source =
        firstText(
            hypothesis != null ? hypothesis.getUniqueMechanism() : null,
            hypothesis != null ? hypothesis.getMechanism() : null);
    if (containsMusa(source)
        || containsMusa(latestPackage != null ? latestPackage.getName() : null)) {
      return "Arquitetura de Presença Elegante Acessível: diagnóstico de ruído visual, microajustes coordenados e escolhas conscientes com o que a cliente já tem.";
    }
    return cleanPublicText(source);
  }

  /** Identifica contexto comercial MUSA mesmo quando o texto veio de nome técnico. */
  private boolean containsMusa(String value) {
    return value != null && value.toLowerCase(java.util.Locale.ROOT).contains("musa");
  }

  /** Identifica se o contexto deve usar o contrato publico curado do produto MUSA. */
  private boolean isMusaContext(
      Experiment experiment, DeliverablePackage latestPackage, Hypothesis hypothesis) {
    return containsMusa(latestPackage != null ? latestPackage.getName() : null)
        || containsMusa(experiment.getName())
        || containsMusa(experiment.getHypothesis())
        || containsMusa(hypothesis != null ? hypothesis.getPromise() : null)
        || containsMusa(hypothesis != null ? hypothesis.getEntrega() : null);
  }

  /** Define prova em linguagem publica sem metricas ou termos de campanha. */
  private String publicProofSummary(
      Experiment experiment, Hypothesis hypothesis, boolean musaContext) {
    if (musaContext) {
      return "Produto estruturado para transformar decisões abstratas de aparência em diagnóstico, microações de 7 dias, checklists e evidências simples de evolução.";
    }
    return cleanPublicText(
        firstText(
            experiment.getLandingPageQualityReview(),
            hypothesis != null ? hypothesis.getSuccessRule() : null));
  }

  /** Define entregaveis publicos sem reaproveitar títulos internos de campanhas antigas. */
  private List<String> publicDeliverables(List<String> deliverables, boolean musaContext) {
    if (musaContext) {
      return List.of(
          "Diagnóstico de presença elegante acessível",
          "Plano guiado de 7 dias",
          "Checklist de cabelo, pele, roupa, perfume e ocasião",
          "Templates de decisão de compra consciente",
          "Painel simples de progresso e próxima ação");
    }
    return deliverables.stream().map(this::cleanPublicText).filter(StringUtils::hasText).toList();
  }

  /** Remove marcadores internos de fabricação de textos enviados ao worker FEO. */
  private String cleanPublicText(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value
        .trim()
        .replaceAll("(?i)^pacote\\s+final\\s*-\\s*", "")
        .replaceAll("(?i)\\s*-\\s*FEO\\s*#?\\d+\\s*$", "")
        .replaceAll("(?i)\\bFEO\\b\\s*#?\\d*", "")
        .replaceAll("(?i)\\bpromessa\\s+validada\\b", "promessa do produto")
        .replaceAll("(?i)\\bmecanismo\\s+validado\\b", "método do produto")
        .replaceAll("(?i)\\bCTR\\b", "interesse inicial")
        .replaceAll("(?i)\\bCPL\\b", "custo de aquisição")
        .replaceAll("(?i)\\bpré-venda\\b", "apresentação do produto")
        .replaceAll("(?i)\\bpre-venda\\b", "apresentação do produto")
        .replaceAll("(?i)\\bcheckout\\b", "página de compra")
        .replaceAll("(?i)\\btráfego\\b", "divulgação")
        .replaceAll("(?i)\\btrafego\\b", "divulgação")
        .trim();
  }

  /** Busca o pacote mais recente vinculado ao experimento. */
  private DeliverablePackage latestPackage(Long experimentId) {
    return deliverablePackageRepository
        .findByExperimentIdOrderByCreatedAtDesc(experimentId)
        .stream()
        .findFirst()
        .orElse(null);
  }

  /** Usa a entrega textual da hipótese como fallback quando ainda não há pacote vinculado. */
  private List<String> fallbackDeliverables(Hypothesis hypothesis) {
    if (hypothesis == null || !StringUtils.hasText(hypothesis.getEntrega())) {
      return List.of();
    }
    return List.of(hypothesis.getEntrega().trim());
  }

  /** Enfileira a próxima etapa permitida após o worker concluir a etapa atual. */
  private void enqueueNextStageWhenAllowed(
      FeoFabricacaoV1StageExecution execution, FeoFabricacaoV1CompleteRequest request) {
    if (STAGE_PLANEJAMENTO.equals(execution.getStageCode())
        && STAGE_REDACAO.equals(request.nextStageCode())) {
      enqueueContentWriting(execution, request);
      return;
    }
    if (STAGE_REDACAO.equals(execution.getStageCode())
        && STAGE_ATIVOS_VISUAIS.equals(request.nextStageCode())) {
      enqueueVisualAssetGeneration(execution, request);
      return;
    }
    if (STAGE_ATIVOS_VISUAIS.equals(execution.getStageCode())
        && STAGE_MONTAGEM.equals(request.nextStageCode())) {
      enqueuePackageAssembly(execution, request);
    }
  }

  /** Enfileira redação dos entregáveis após o planejamento gerar o plano. */
  private void enqueueContentWriting(
      FeoFabricacaoV1StageExecution execution, FeoFabricacaoV1CompleteRequest request) {
    Map<String, Object> assemblyInput = new LinkedHashMap<>();
    assemblyInput.put("context", fromJson(execution.getInputPayload(), MAP_TYPE));
    assemblyInput.put("plan", request.output());
    FeoFabricacaoV1StageExecution next =
        FeoFabricacaoV1StageExecution.builder()
            .experiment(execution.getExperiment())
            .jobId(execution.getJobId())
            .stageCode(STAGE_REDACAO)
            .status(FeoFabricacaoV1StageStatus.PENDING)
            .inputPayload(toJson(assemblyInput))
            .build();
    executionRepository.save(next);
  }

  /** Enfileira geração visual após existir conteúdo redigido e aprovado pelo gate. */
  private void enqueueVisualAssetGeneration(
      FeoFabricacaoV1StageExecution execution, FeoFabricacaoV1CompleteRequest request) {
    Map<String, Object> assemblyInput = objectMapper.convertValue(request.output(), MAP_TYPE);
    FeoFabricacaoV1StageExecution next =
        FeoFabricacaoV1StageExecution.builder()
            .experiment(execution.getExperiment())
            .jobId(execution.getJobId())
            .stageCode(STAGE_ATIVOS_VISUAIS)
            .status(FeoFabricacaoV1StageStatus.PENDING)
            .inputPayload(toJson(assemblyInput))
            .build();
    executionRepository.save(next);
  }

  /** Enfileira montagem final somente após existir conteúdo e imagens editoriais aprovadas. */
  private void enqueuePackageAssembly(
      FeoFabricacaoV1StageExecution execution, FeoFabricacaoV1CompleteRequest request) {
    Map<String, Object> assemblyInput = objectMapper.convertValue(request.output(), MAP_TYPE);
    FeoFabricacaoV1StageExecution next =
        FeoFabricacaoV1StageExecution.builder()
            .experiment(execution.getExperiment())
            .jobId(execution.getJobId())
            .stageCode(STAGE_MONTAGEM)
            .status(FeoFabricacaoV1StageStatus.PENDING)
            .inputPayload(toJson(assemblyInput))
            .build();
    executionRepository.save(next);
  }

  /** Materializa o pacote final da FEO em entregáveis consumíveis pelo experimento. */
  private void materializePackageWhenFinal(
      String stageCode,
      FeoFabricacaoV1StageExecution execution,
      FeoFabricacaoV1CompleteRequest request) {
    if (!STAGE_MONTAGEM.equals(stageCode)) {
      return;
    }
    Map<String, Object> output = objectMapper.convertValue(request.output(), MAP_TYPE);
    Map<String, Object> manifest = objectMapper.convertValue(output.get("manifest"), MAP_TYPE);
    String packageTitle =
        packageTitleForPersistence(
            stringValue(
                manifest.get("packageTitle"),
                "Pacote Cliente - Experimento " + execution.getExperiment().getId()),
            execution);
    List<Map<String, Object>> items =
        objectMapper.convertValue(manifest.get("items"), ARTIFACT_LIST_TYPE);
    LinkedHashSet<Deliverable> deliverables = new LinkedHashSet<>();
    for (Map<String, Object> item : items) {
      Deliverable deliverable =
          deliverableRepository.save(
              Deliverable.builder()
                  .niche(execution.getExperiment().getNiche())
                  .title(stringValue(item.get("fileName"), "Entregável FEO"))
                  .description(
                      stringValue(item.get("role"), "Entregável final do produto digital."))
                  .content(toJson(item))
                  .model("feo.fabricacao.v1")
                  .prompt("Materializado a partir do contexto comercial aprovado do experimento.")
                  .build());
      deliverables.add(deliverable);
    }
    deliverablePackageRepository.save(
        DeliverablePackage.builder()
            .experiment(execution.getExperiment())
            .name(packageTitle)
            .description(toJson(output.get("report")))
            .model("feo.fabricacao.v1")
            .prompt("Pacote final materializado para entrega ao comprador.")
            .deliverables(deliverables)
            .build());
  }

  /** Gera nome persistido unico para permitir refabricacoes do mesmo pacote final. */
  private String packageTitleForPersistence(
      String packageTitle, FeoFabricacaoV1StageExecution execution) {
    return packageTitle + " - pacote " + execution.getId();
  }

  /** Localiza execução pelo id e etapa informada no callback. */
  private FeoFabricacaoV1StageExecution findExecution(String stageCode, Long executionId) {
    return executionRepository
        .findByIdAndStageCode(executionId, stageCode)
        .orElseThrow(() -> new EntityNotFoundException("FEO execution not found: " + executionId));
  }

  /** Indica se já existe planejamento pendente ou em execução para o experimento. */
  private boolean hasActiveInitialExecution(Long experimentId) {
    return executionRepository.existsByExperimentIdAndStageCodeAndStatusIn(
        experimentId,
        STAGE_PLANEJAMENTO,
        List.of(FeoFabricacaoV1StageStatus.PENDING, FeoFabricacaoV1StageStatus.RUNNING));
  }

  /** Resolve status final a partir do contrato do worker. */
  private FeoFabricacaoV1StageStatus resolveCompletedStatus(
      FeoFabricacaoV1CompleteRequest request) {
    if ("BLOCKED".equalsIgnoreCase(request.status())
        || StringUtils.hasText(request.blockReason())) {
      return FeoFabricacaoV1StageStatus.BLOCKED;
    }
    return FeoFabricacaoV1StageStatus.COMPLETED;
  }

  /** Converte entidade para resposta de início. */
  private FeoFabricacaoV1StartResponse toStartResponse(FeoFabricacaoV1StageExecution execution) {
    return new FeoFabricacaoV1StartResponse(
        execution.getId(),
        execution.getJobId(),
        execution.getStageCode(),
        execution.getStatus().name());
  }

  /** Converte entidade para resumo operacional. */
  private FeoFabricacaoV1ExecutionSummaryResponse toSummary(
      FeoFabricacaoV1StageExecution execution) {
    return new FeoFabricacaoV1ExecutionSummaryResponse(
        execution.getId(),
        execution.getJobId(),
        execution.getStageCode(),
        execution.getStatus().name(),
        execution.getBlockReason(),
        execution.getErrorMessage(),
        execution.getCreatedAt(),
        execution.getFinishedAt());
  }

  /** Serializa objeto para JSON preservando falha com stack no chamador transacional. */
  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao serializar payload FEO valueType={}",
          value != null ? value.getClass().getName() : null,
          ex);
      throw new IllegalStateException("Falha ao serializar payload FEO", ex);
    }
  }

  /** Desserializa JSON para o tipo solicitado. */
  private <T> T fromJson(String json, TypeReference<T> type) {
    try {
      return objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), type);
    } catch (Exception ex) {
      log.error("Falha ao ler payload FEO jsonLength={}", json != null ? json.length() : 0, ex);
      throw new IllegalStateException("Falha ao ler payload FEO", ex);
    }
  }

  /** Retorna o primeiro texto preenchido. */
  private String firstText(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  /** Normaliza texto vazio como nulo. */
  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Converte valor arbitrário em string com fallback. */
  private String stringValue(Object value, String fallback) {
    return value == null || !StringUtils.hasText(String.valueOf(value))
        ? fallback
        : String.valueOf(value);
  }
}
