package com.marketinghub.product.service.salesjourneyintegration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.valuechainposition.ProductProcessPeriodService;
import com.marketinghub.product.service.valuechainposition.ProductStageMeasurementResolver;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Responsabilidade: integrar a jornada comercial PDE e avançar o produto somente com contratos
 * preparados e auditáveis.
 */
@Service
@Slf4j
public class PdeSalesJourneyIntegrationActivityExecutor
    implements BackendProductProcessActivityExecutor {
  static final String PROCESS_CODE = "pde-communication-sales-journey";
  static final String ACTIVITY_ID = "integration";
  static final String NEXT_COMMERCIAL_STATUS = "VALIDACAO_COMERCIAL";
  private static final Set<String> ELIGIBLE_COMMERCIAL_STATUSES =
      Set.of("COMUNICACAO_E_JORNADA", "COMUNICACAO_E_JORNADA_DE_VENDA", NEXT_COMMERCIAL_STATUS);

  private final CommercialPlanRepository commercialPlanRepository;
  private final ExperimentRepository experimentRepository;
  private final BusinessProcessDefinitionRepository processRepository;
  private final BusinessProcessActivityDefinitionRepository activityDefinitionRepository;
  private final AgentTaskRepository taskRepository;
  private final BusinessProcessActivityInstanceRepository activityInstanceRepository;
  private final ProductStageMeasurementResolver stageMeasurementResolver;
  private final PdeProductionSlotRepository slotRepository;
  private final PdeProductionSlotService slotService;
  private final ProductRepository productRepository;
  private final ProductProcessPeriodService processPeriodService;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Configura as fontes canônicas usadas para validar e registrar a integração comercial. */
  @Autowired
  public PdeSalesJourneyIntegrationActivityExecutor(
      CommercialPlanRepository commercialPlanRepository,
      ExperimentRepository experimentRepository,
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      AgentTaskRepository taskRepository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      ProductStageMeasurementResolver stageMeasurementResolver,
      PdeProductionSlotRepository slotRepository,
      PdeProductionSlotService slotService,
      ProductRepository productRepository,
      ProductProcessPeriodService processPeriodService,
      ObjectMapper objectMapper) {
    this(
        commercialPlanRepository,
        experimentRepository,
        processRepository,
        activityDefinitionRepository,
        taskRepository,
        activityInstanceRepository,
        stageMeasurementResolver,
        slotRepository,
        slotService,
        productRepository,
        processPeriodService,
        objectMapper,
        Clock.systemUTC());
  }

  /** Permite validar a persistência e os horários com relógio determinístico. */
  PdeSalesJourneyIntegrationActivityExecutor(
      CommercialPlanRepository commercialPlanRepository,
      ExperimentRepository experimentRepository,
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      AgentTaskRepository taskRepository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      ProductStageMeasurementResolver stageMeasurementResolver,
      PdeProductionSlotRepository slotRepository,
      PdeProductionSlotService slotService,
      ProductRepository productRepository,
      ProductProcessPeriodService processPeriodService,
      ObjectMapper objectMapper,
      Clock clock) {
    this.commercialPlanRepository = commercialPlanRepository;
    this.experimentRepository = experimentRepository;
    this.processRepository = processRepository;
    this.activityDefinitionRepository = activityDefinitionRepository;
    this.taskRepository = taskRepository;
    this.activityInstanceRepository = activityInstanceRepository;
    this.stageMeasurementResolver = stageMeasurementResolver;
    this.slotRepository = slotRepository;
    this.slotService = slotService;
    this.productRepository = productRepository;
    this.processPeriodService = processPeriodService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Reconhece exclusivamente a integração determinística da jornada comercial PDE. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITY_ID.equals(activityDefinition.getActivityId());
  }

  /** Bloqueia antecipação quando estratégia de comunicação ou subprocessos ainda não concluíram. */
  @Override
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    if (!supports(process, activityDefinition)) {
      return new BackendProductProcessActivityReadiness(
          false, "Não existe executor backend para esta atividade.");
    }
    if (!ELIGIBLE_COMMERCIAL_STATUSES.contains(product.getCommercialStatus())) {
      return new BackendProductProcessActivityReadiness(
          false,
          "O produto não está no processo de comunicação e jornada; a execução por uma rota histórica foi bloqueada.");
    }
    try {
      Optional<String> predecessorIssue = predecessorIssue(process, product);
      if (predecessorIssue.isPresent()) {
        return new BackendProductProcessActivityReadiness(false, predecessorIssue.get());
      }
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao verificar pré-requisitos da integração PDE. processDefinitionId={} productId={} activityId={}",
          process.getId(),
          product.getId(),
          activityDefinition.getActivityId(),
          ex);
      return new BackendProductProcessActivityReadiness(
          false, "Não foi possível confirmar os pré-requisitos persistidos desta integração.");
    }
    return new BackendProductProcessActivityReadiness(
        true,
        "Comunicação, criativos e destino estão aprovados; a jornada pode validar URL, checkout, acesso e eventos.");
  }

  /**
   * Valida contratos reais, registra sucesso ou bloqueio e só então move o produto ao processo 5.
   */
  @Override
  @Transactional
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    BackendProductProcessActivityReadiness readiness =
        readiness(process, activityDefinition, product, sourceReference);
    if (!readiness.ready()) {
      throw new IllegalStateException(readiness.reason());
    }
    Experiment experiment = latestExperiment(product.getId());
    CommercialPlan plan = latestPlan(product.getId(), experiment.getId());
    String resolvedReference =
        StringUtils.hasText(sourceReference)
            ? sourceReference.trim()
            : "experiment:" + experiment.getId();
    Instant startedAt = Instant.now(clock);
    synchronizeCompletedSubprocesses(process, product, resolvedReference, startedAt);
    Optional<BusinessProcessActivityInstance> latest =
        activityInstanceRepository
            .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                activityDefinition.getId(), resolvedReference);
    if (latest.isPresent() && "COMPLETED".equals(latest.get().getStatus())) {
      advanceProductIfNeeded(product);
      return new BackendProductProcessActivityExecutionResult(
          resolvedReference,
          "COMPLETED",
          true,
          "A integração já estava concluída e o próximo processo permanece liberado.");
    }
    BusinessProcessActivityInstance instance =
        newInstance(
            activityDefinition,
            resolvedReference,
            latest.map(value -> value.getOccurrenceNumber() + 1).orElse(1),
            startedAt);
    activityInstanceRepository.saveAndFlush(instance);

    Optional<PdeProductionSlot> persistedSlot =
        slotRepository.findFirstBySourceExperimentIdOrderByUpdatedAtDesc(experiment.getId());
    List<String> blockers = staticBlockers(product, experiment, persistedSlot.orElse(null));
    PostDeployPdeProductionSlotDto validatedSlot = null;
    if (blockers.isEmpty()) {
      PdeProductionSlot slot = persistedSlot.orElseThrow();
      validatedSlot = slotService.validateProductionSlot(slot.getProductSlug(), slot.getSlotCode());
      blockers.addAll(validationBlockers(product, experiment, validatedSlot));
    }

    Instant completedAt = Instant.now(clock);
    ObjectNode evidence =
        evidence(product, plan, experiment, persistedSlot.orElse(null), validatedSlot, blockers);
    if (!blockers.isEmpty()) {
      String reason = String.join(" ", blockers);
      completeBlocked(instance, reason, evidence, completedAt);
      return new BackendProductProcessActivityExecutionResult(
          resolvedReference,
          "BLOCKED",
          false,
          "A integração foi registrada como bloqueada: " + reason);
    }

    PdeProductionSlot slot = persistedSlot.orElseThrow();
    experiment.setFollowUpActionUrl(slot.getPublicUrl());
    experimentRepository.save(experiment);
    completeApproved(instance, evidence, completedAt);
    advanceProductIfNeeded(product);
    return new BackendProductProcessActivityExecutionResult(
        resolvedReference,
        "COMPLETED",
        true,
        "Canal, checkout, acesso e eventos foram preparados. O Rigel avançou para Homologação e ativação comercial.");
  }

  /** Localiza o experimento mais recente sem misturar outro produto no gate de integração. */
  private Experiment latestExperiment(Long productId) {
    return experimentRepository.findByProductIdOrderByUpdatedAtDescIdDesc(productId).stream()
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException("O produto ainda não possui experimento para integrar."));
  }

  /**
   * Prefere o plano que governa o experimento e usa o plano mais recente do produto como fallback.
   */
  private CommercialPlan latestPlan(Long productId, Long experimentId) {
    return commercialPlanRepository.findByExperimentReference(experimentId).stream()
        .findFirst()
        .or(() -> commercialPlanRepository.findByProductId(productId).stream().findFirst())
        .orElseThrow(
            () -> new IllegalStateException("O produto ainda não possui plano comercial vigente."));
  }

  /** Identifica a primeira atividade anterior ainda sem conclusão comprovada. */
  private Optional<String> predecessorIssue(BusinessProcessDefinition process, Product product) {
    List<AgentTask> tasks = productTasks(product.getId());
    for (JsonNode node : orderedTaskNodes(process)) {
      String activityId = node.path("id").asText();
      if (ACTIVITY_ID.equals(activityId)) {
        return Optional.empty();
      }
      String subprocessCode = node.path("subprocessCode").asText(null);
      if (StringUtils.hasText(subprocessCode)) {
        BusinessProcessDefinition subprocess = publishedProcess(subprocessCode);
        if (!stageMeasurementResolver.objectiveAchieved(product, subprocess)) {
          return Optional.of(
              "Conclua primeiro o subprocesso " + node.path("label").asText(subprocessCode) + ".");
        }
        continue;
      }
      if (node.path("responsibleAgentKeys").isArray()
          && !node.path("responsibleAgentKeys").isEmpty()
          && !hasCompletedTask(tasks, process.getProcessCode(), activityId)) {
        return Optional.of(
            "Conclua primeiro a atividade " + node.path("label").asText(activityId) + ".");
      }
    }
    return Optional.of("A atividade de integração não foi encontrada na ordem do processo.");
  }

  /** Busca todas as tarefas do produto por plano e experimento sem duplicar a mesma execução. */
  private List<AgentTask> productTasks(Long productId) {
    Map<Long, AgentTask> tasks = new LinkedHashMap<>();
    commercialPlanRepository
        .findByProductId(productId)
        .forEach(
            plan ->
                taskRepository
                    .findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
                        "commercial-plan:" + plan.getId() + "@")
                    .forEach(task -> tasks.put(task.getId(), task)));
    experimentRepository
        .findByProductIdOrderByUpdatedAtDescIdDesc(productId)
        .forEach(
            experiment ->
                taskRepository
                    .findBySourceReferenceOrderByCreatedAtAscIdAsc(
                        "experiment:" + experiment.getId())
                    .forEach(task -> tasks.put(task.getId(), task)));
    return List.copyOf(tasks.values());
  }

  /**
   * Confirma conclusão funcional da atividade pelo estado consolidado da instância ou da tarefa.
   */
  private boolean hasCompletedTask(List<AgentTask> tasks, String processCode, String activityId) {
    return tasks.stream()
        .filter(task -> task.getProcessDefinition() != null)
        .filter(task -> processCode.equals(task.getProcessDefinition().getProcessCode()))
        .filter(task -> activityId.equals(task.getProcessActivityId()))
        .anyMatch(
            task ->
                task.getActivityInstance() != null
                    ? "COMPLETED".equals(task.getActivityInstance().getStatus())
                        && task.getActivityInstance().isObjectiveAchieved()
                    : "COMPLETED".equals(task.getStatus()));
  }

  /** Lê as atividades de trabalho na ordem declarada pelo processo publicado. */
  private List<JsonNode> orderedTaskNodes(BusinessProcessDefinition process) {
    try {
      JsonNode nodes = objectMapper.readTree(process.getDiagramJson()).path("nodes");
      return StreamSupport.stream(nodes.spliterator(), false)
          .filter(node -> "TASK".equals(node.path("type").asText()))
          .toList();
    } catch (Exception ex) {
      log.error(
          "Falha ao interpretar atividades da integração PDE. processDefinitionId={} processCode={}",
          process.getId(),
          process.getProcessCode(),
          ex);
      throw new IllegalStateException("Não foi possível interpretar o processo comercial.", ex);
    }
  }

  /** Localiza a versão publicada de um subprocesso exigido pela composição atual. */
  private BusinessProcessDefinition publishedProcess(String processCode) {
    return processRepository
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(processCode, "PUBLISHED")
        .orElseThrow(
            () ->
                new IllegalStateException("Subprocesso publicado não encontrado: " + processCode));
  }

  /** Projeta no processo pai os subprocessos já comprovadamente concluídos. */
  private void synchronizeCompletedSubprocesses(
      BusinessProcessDefinition process,
      Product product,
      String sourceReference,
      Instant synchronizedAt) {
    for (JsonNode node : orderedTaskNodes(process)) {
      String subprocessCode = node.path("subprocessCode").asText(null);
      if (!StringUtils.hasText(subprocessCode)) {
        continue;
      }
      BusinessProcessDefinition subprocess = publishedProcess(subprocessCode);
      if (!stageMeasurementResolver.objectiveAchieved(product, subprocess)) {
        continue;
      }
      BusinessProcessActivityDefinition parentActivity =
          activityDefinitionRepository
              .findByProcessDefinitionIdAndActivityId(process.getId(), node.path("id").asText())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Atividade pai do subprocesso não foi persistida."));
      Optional<BusinessProcessActivityInstance> latest =
          activityInstanceRepository
              .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                  parentActivity.getId(), sourceReference);
      if (latest.isPresent() && "COMPLETED".equals(latest.get().getStatus())) {
        continue;
      }
      BusinessProcessActivityInstance instance =
          newInstance(
              parentActivity,
              sourceReference,
              latest.map(value -> value.getOccurrenceNumber() + 1).orElse(1),
              synchronizedAt);
      ObjectNode evidence = objectMapper.createObjectNode();
      evidence.put("source", "SUBPROCESS_OBJECTIVE_ACHIEVED");
      evidence.put("subprocessCode", subprocessCode);
      evidence.put("productId", product.getId());
      instance.setStatus("COMPLETED");
      instance.setExitedAt(synchronizedAt);
      instance.setObjectiveAchieved(true);
      instance.setObjectiveEvidenceJson(evidence.toString());
      instance.setBlockedReason(null);
      instance.setKnownCostUsd(BigDecimal.ZERO.setScale(8));
      instance.setCostCoverage("COMPLETE");
      instance.setEvidenceQuality("DIRECT");
      instance.setUpdatedAt(synchronizedAt);
      activityInstanceRepository.save(instance);
    }
  }

  /** Cria a ocorrência de integração antes de executar qualquer verificação externa. */
  private BusinessProcessActivityInstance newInstance(
      BusinessProcessActivityDefinition activity,
      String sourceReference,
      int occurrenceNumber,
      Instant enteredAt) {
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(activity);
    instance.setSourceReference(sourceReference);
    instance.setOccurrenceNumber(occurrenceNumber);
    instance.setStatus("IN_PROGRESS");
    instance.setEnteredAt(enteredAt);
    instance.setExitedAt(null);
    instance.setObjectiveAchieved(false);
    instance.setKnownCostUsd(BigDecimal.ZERO.setScale(8));
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    instance.setCreatedAt(enteredAt);
    instance.setUpdatedAt(enteredAt);
    return instance;
  }

  /** Reúne lacunas persistidas antes da validação HTTP da superfície pública. */
  private List<String> staticBlockers(
      Product product, Experiment experiment, PdeProductionSlot slot) {
    List<String> blockers = new ArrayList<>();
    if (slot == null) {
      blockers.add("Nenhum slot PDE está vinculado ao experimento #" + experiment.getId() + ".");
      return blockers;
    }
    if (slot.getStatus() != PdeProductionSlotStatus.READY
        && slot.getStatus() != PdeProductionSlotStatus.ACTIVE) {
      blockers.add("O slot PDE precisa estar READY ou ACTIVE.");
    }
    if (!Objects.equals(slot.getSourceExperimentId(), experiment.getId())) {
      blockers.add("O slot PDE pertence a outro experimento.");
    }
    if (!Objects.equals(slot.getProductSlug(), product.getSlug())) {
      blockers.add("O slot PDE pertence a outro produto.");
    }
    if (!StringUtils.hasText(slot.getPublishedExperienceJson()) || slot.getPublishedAt() == null) {
      blockers.add("O contrato da experiência PDE ainda não foi publicado no slot.");
    }
    if (!isHttps(slot.getPublicUrl())) {
      blockers.add("A URL pública do PDE precisa usar HTTPS.");
    }
    if (!StringUtils.hasText(product.getPublicUrl())
        || !normalizeUrl(product.getPublicUrl()).equals(normalizeUrl(slot.getPublicUrl()))) {
      blockers.add("A URL pública do produto diverge do slot PDE aprovado.");
    }
    if (!StringUtils.hasText(slot.getBackendUrl())) {
      blockers.add("O slot PDE não declara o backend responsável por acesso e eventos.");
    }
    return blockers;
  }

  /** Interpreta o resultado do contrato público sem confundir validação antiga com estado atual. */
  private List<String> validationBlockers(
      Product product, Experiment experiment, PostDeployPdeProductionSlotDto slot) {
    List<String> blockers = new ArrayList<>();
    if (!"OK".equals(slot.validationStatus())) {
      blockers.add(
          StringUtils.hasText(slot.validationSummary())
              ? slot.validationSummary() + "."
              : "A URL pública do PDE não passou na validação atual.");
    }
    if (!Objects.equals(slot.sourceExperimentId(), experiment.getId())) {
      blockers.add("A oferta pública não preservou a atribuição do experimento.");
    }
    if (!Objects.equals(slot.validationContractSlug(), product.getSlug())) {
      blockers.add("O contrato público retornou um slug diferente do Rigel.");
    }
    if (!isHttps(slot.validationResolvedUrl())) {
      blockers.add("A entrada pública validada não usa HTTPS.");
    }
    return blockers;
  }

  /** Monta evidência funcional sem incorporar payload bruto dentro de outro documento JSON. */
  private ObjectNode evidence(
      Product product,
      CommercialPlan plan,
      Experiment experiment,
      PdeProductionSlot persistedSlot,
      PostDeployPdeProductionSlotDto validatedSlot,
      List<String> blockers) {
    ObjectNode evidence = objectMapper.createObjectNode();
    evidence.put("evidenceType", "PDE_SALES_JOURNEY_INTEGRATION_V1");
    evidence.put("productId", product.getId());
    evidence.put("productSlug", product.getSlug());
    evidence.put("commercialPlanId", plan.getId());
    evidence.put("experimentId", experiment.getId());
    evidence.put("publicationAuthorized", false);
    evidence.put("mediaSpendAuthorized", false);
    evidence.put("testPaymentExecuted", false);
    evidence.put("nextProcessCode", "pde-commercial-homologation-activation");
    if (persistedSlot != null) {
      evidence.put("slotId", persistedSlot.getId());
      evidence.put("slotCode", persistedSlot.getSlotCode());
      evidence.put("experienceVersion", persistedSlot.getExperienceVersion());
      evidence.put("publicUrl", persistedSlot.getPublicUrl());
      evidence.put("backendUrl", persistedSlot.getBackendUrl());
    }
    if (validatedSlot != null) {
      evidence.put("validationStatus", validatedSlot.validationStatus());
      evidence.put("validationSummary", validatedSlot.validationSummary());
      evidence.put("validationDetail", validatedSlot.validationDetail());
      evidence.put("validationCheckedAt", String.valueOf(validatedSlot.validationCheckedAt()));
      evidence.put("validationResolvedUrl", validatedSlot.validationResolvedUrl());
    }
    ArrayNode blockerArray = evidence.putArray("blockers");
    blockers.forEach(blockerArray::add);
    return evidence;
  }

  /** Finaliza uma tentativa sem mascarar a causa que impede o avanço comercial. */
  private void completeBlocked(
      BusinessProcessActivityInstance instance,
      String reason,
      ObjectNode evidence,
      Instant completedAt) {
    instance.setStatus("BLOCKED");
    instance.setExitedAt(completedAt);
    instance.setObjectiveAchieved(false);
    instance.setObjectiveEvidenceJson(evidence.toString());
    instance.setBlockedReason(reason);
    instance.setUpdatedAt(completedAt);
    activityInstanceRepository.save(instance);
  }

  /** Finaliza a integração com custo zero e evidência direta dos contratos preparados. */
  private void completeApproved(
      BusinessProcessActivityInstance instance, ObjectNode evidence, Instant completedAt) {
    instance.setStatus("COMPLETED");
    instance.setExitedAt(completedAt);
    instance.setObjectiveAchieved(true);
    instance.setObjectiveEvidenceJson(evidence.toString());
    instance.setBlockedReason(null);
    instance.setUpdatedAt(completedAt);
    activityInstanceRepository.save(instance);
  }

  /** Persiste a transição macro somente quando o produto ainda está no processo de comunicação. */
  private void advanceProductIfNeeded(Product product) {
    if (NEXT_COMMERCIAL_STATUS.equals(product.getCommercialStatus())) {
      return;
    }
    if (!ELIGIBLE_COMMERCIAL_STATUSES.contains(product.getCommercialStatus())) {
      throw new IllegalStateException(
          "O produto não pode avançar por uma atividade pertencente a um processo histórico.");
    }
    String previousStatus = product.getCommercialStatus();
    product.setCommercialStatus(NEXT_COMMERCIAL_STATUS);
    productRepository.save(product);
    processPeriodService.recordTransition(product, previousStatus);
  }

  /** Reconhece URLs HTTPS completas usadas pela superfície pública. */
  private boolean isHttps(String value) {
    return StringUtils.hasText(value) && value.trim().startsWith("https://");
  }

  /** Remove barras finais para comparar URLs equivalentes sem alterar a versão persistida. */
  private String normalizeUrl(String value) {
    return value == null ? "" : value.trim().replaceAll("/+$", "");
  }
}
