package com.marketinghub.planning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanJourneyHomologationDto;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: iniciar a homologação oficial da landing vinculada ao plano comercial. */
@Service
public class CommercialPlanJourneyHomologationService {
  private static final Logger log =
      LoggerFactory.getLogger(CommercialPlanJourneyHomologationService.class);
  private static final String LANDING_PROCESS_CODE = "landing-page-generation";
  private final CommercialPlanService commercialPlanService;
  private final CommercialPlanVersionService versionService;
  private final BusinessProcessDefinitionRepository processRepository;
  private final AgentTaskRepository taskRepository;
  private final CommercialPlanLandingReviewResumeService reviewResumeService;
  private final AgentTaskService agentTaskService;
  private final ObjectMapper objectMapper;

  /** Inicializa o serviço com o plano, sua versão e o processo BPM publicado. */
  public CommercialPlanJourneyHomologationService(
      CommercialPlanService commercialPlanService,
      CommercialPlanVersionService versionService,
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskRepository taskRepository,
      CommercialPlanLandingReviewResumeService reviewResumeService,
      AgentTaskService agentTaskService,
      ObjectMapper objectMapper) {
    this.commercialPlanService = commercialPlanService;
    this.versionService = versionService;
    this.processRepository = processRepository;
    this.taskRepository = taskRepository;
    this.reviewResumeService = reviewResumeService;
    this.agentTaskService = agentTaskService;
    this.objectMapper = objectMapper;
  }

  /** Inicia a jornada ou retoma somente os revisores quando a landing aprovada continua íntegra. */
  @Transactional
  public CommercialPlanJourneyHomologationDto request(Long planId, Long experimentId) {
    commercialPlanService.requireExperiment(planId, experimentId);
    CommercialPlan plan = commercialPlanService.getPlan(planId);
    Instant requestedAt = Instant.now();
    BusinessProcessDefinition process = publishedLandingProcess();
    int planVersion = versionService.current(planId).versionNumber();
    String baseReference =
        "commercial-plan:" + planId + "@v" + planVersion + ":journey:experiment-" + experimentId;
    JourneyExecution currentJourney = journeySourceReference(baseReference, process.getId());
    if (currentJourney.reviewResumeActive()) {
      return new CommercialPlanJourneyHomologationDto(
          planId, experimentId, "REVIEW_RESUMED", requestedAt.toString());
    }
    Optional<String> approvedResume =
        currentJourney.blocked()
            ? reviewResumeService.buildResumeBrief(
                plan.getId(),
                experimentId,
                currentJourney.attemptNumber(),
                currentJourney.previousAttemptBlocks(),
                currentJourney.currentTasks())
            : Optional.empty();
    if (approvedResume.isPresent()) {
      String reviewBrief = approvedResume.get();
      retryReviewTask(
          "customer-agent",
          "customer",
          "Experimento #" + experimentId + " · revalidar percepção da cliente",
          reviewBrief,
          currentJourney.sourceReference(),
          process);
      retryReviewTask(
          "meta-ad-approver",
          "commercial",
          "Experimento #" + experimentId + " · revisar coerência comercial da landing",
          reviewBrief,
          currentJourney.sourceReference(),
          process);
      return new CommercialPlanJourneyHomologationDto(
          planId, experimentId, "REVIEW_RESUMED", requestedAt.toString());
    }
    JourneyExecution journeyExecution = nextFullJourney(baseReference, currentJourney);
    String sourceReference = journeyExecution.sourceReference();
    String auditBrief = buildAuditBrief(plan, experimentId, journeyExecution);
    createTask(
        "communication-director",
        "select",
        "Experimento #" + experimentId + " · selecionar provas reais da landing",
        auditBrief,
        sourceReference,
        process);
    createTask(
        "communication-director",
        "strategy",
        "Experimento #" + experimentId + " · definir arquitetura de conversão",
        "Materializar narrativa, hierarquia e copy a partir dos contratos aprovados, sem redefinir posicionamento, oferta, preço ou produto.",
        sourceReference,
        process);
    createTask(
        "communication-director",
        "compose",
        "Experimento #" + experimentId + " · compor experiência visual da landing",
        "Materializar composição sensorial responsiva com as provas reais selecionadas, sem fabricar demonstração, resultado ou ativo do PDE.",
        sourceReference,
        process);
    createTask(
        "communication-director",
        "html",
        "Experimento #" + experimentId + " · construir e homologar a landing",
        "Entregar HTML responsivo, acessível e instrumentável, preservando o checkout e os contratos canônicos para o Quality Review independente.",
        sourceReference,
        process);
    createTask(
        "customer-agent",
        "customer",
        "Experimento #" + experimentId + " · validar percepção da cliente",
        "Avaliar a landing final aprovada pelo Quality Review e bloquear qualquer divergência entre promessa, entrega e checkout.",
        sourceReference,
        process);
    createTask(
        "meta-ad-approver",
        "commercial",
        "Experimento #" + experimentId + " · revisar coerência comercial da landing",
        "Revisar a mesma landing final depois de Psique e bloquear divergência de oferta, preço, prova, CTA, checkout ou instrumentação.",
        sourceReference,
        process);
    return new CommercialPlanJourneyHomologationDto(
        planId, experimentId, "INICIADO", requestedAt.toString());
  }

  /** Localiza a tentativa mais recente e ignora bloqueios substituídos por tarefas posteriores. */
  private JourneyExecution journeySourceReference(String baseReference, Long processDefinitionId) {
    Map<Integer, List<AgentTask>> tasksByAttempt =
        taskRepository
            .findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(baseReference)
            .stream()
            .filter(
                task ->
                    task.getProcessDefinition() != null
                        && processDefinitionId.equals(task.getProcessDefinition().getId()))
            .filter(task -> attemptNumber(baseReference, task.getSourceReference()) != null)
            .collect(
                java.util.stream.Collectors.groupingBy(
                    task -> attemptNumber(baseReference, task.getSourceReference())));
    if (tasksByAttempt.isEmpty()) {
      return new JourneyExecution(baseReference, 1, List.of(), List.of(), false);
    }
    int latestAttempt = tasksByAttempt.keySet().stream().max(Comparator.naturalOrder()).orElse(1);
    List<AgentTask> latestTasks = currentTasks(tasksByAttempt.get(latestAttempt));
    boolean blocked = latestTasks.stream().anyMatch(task -> "BLOCKED".equals(task.getStatus()));
    return new JourneyExecution(
        executionReference(baseReference, latestAttempt),
        latestAttempt,
        blocked ? blockedEvidence(latestTasks) : List.of(),
        latestTasks,
        blocked);
  }

  /** Avança o número somente quando um bloqueio exige nova materialização real pela Íris. */
  private JourneyExecution nextFullJourney(String baseReference, JourneyExecution currentJourney) {
    if (!currentJourney.blocked()) return currentJourney;
    int nextAttempt = currentJourney.attemptNumber() + 1;
    return new JourneyExecution(
        executionReference(baseReference, nextAttempt),
        nextAttempt,
        currentJourney.previousAttemptBlocks(),
        List.of(),
        false);
  }

  /** Mantém somente a tentativa mais nova de cada agente em cada atividade. */
  private List<AgentTask> currentTasks(List<AgentTask> tasks) {
    Map<String, AgentTask> latest = new LinkedHashMap<>();
    tasks.stream()
        .sorted(Comparator.comparing(AgentTask::getId, Comparator.nullsFirst(Long::compareTo)))
        .forEach(
            task ->
                latest.put(
                    task.getAssignedAgent().getAgentKey() + ":" + task.getProcessActivityId(),
                    task));
    return List.copyOf(latest.values());
  }

  /**
   * Preserva os pareceres que bloquearam a tentativa anterior para orientar a correção seguinte.
   */
  private List<Map<String, Object>> blockedEvidence(List<AgentTask> tasks) {
    return tasks.stream()
        .filter(task -> "BLOCKED".equals(task.getStatus()))
        .map(
            task -> {
              Map<String, Object> evidence = new LinkedHashMap<>();
              evidence.put("activityId", task.getProcessActivityId());
              if (task.getExecutionError() != null)
                evidence.put("blockingReason", task.getExecutionError());
              if (task.getResultJson() != null)
                evidence.put("agentResultJson", task.getResultJson());
              return Map.copyOf(evidence);
            })
        .toList();
  }

  /** Interpreta somente a referência base ou o sufixo canônico de uma nova tentativa. */
  private Integer attemptNumber(String baseReference, String sourceReference) {
    if (baseReference.equals(sourceReference)) return 1;
    String prefix = baseReference + ":attempt:";
    if (sourceReference == null || !sourceReference.startsWith(prefix)) return null;
    try {
      int attempt = Integer.parseInt(sourceReference.substring(prefix.length()));
      return attempt > 1 ? attempt : null;
    } catch (NumberFormatException ex) {
      log.warn(
          "Referência de tentativa inválida na homologação da landing. baseReference={} sourceReference={}",
          baseReference,
          sourceReference,
          ex);
      return null;
    }
  }

  /** Mantém a primeira execução legível e explicita apenas as tentativas posteriores. */
  private String executionReference(String baseReference, int attempt) {
    return attempt <= 1 ? baseReference : baseReference + ":attempt:" + attempt;
  }

  /** Localiza somente a versão publicada do subprocesso oficial de landing. */
  private BusinessProcessDefinition publishedLandingProcess() {
    return processRepository
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(LANDING_PROCESS_CODE, "PUBLISHED")
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O subprocesso oficial de landing ainda não está publicado."));
  }

  /** Cria cada gate uma única vez para a versão corrente do plano. */
  private void createTask(
      String agentKey,
      String activityId,
      String title,
      String description,
      String sourceReference,
      BusinessProcessDefinition process) {
    agentTaskService.createByHumanIfAbsent(
        new CreateAgentTaskRequest(
            agentKey,
            "Operador do Marketing Hub",
            title,
            description,
            "HIGH",
            sourceReference,
            process.getId(),
            activityId,
            false,
            null));
  }

  /** Abre uma tentativa nova para bloqueio e atualiza o snapshot de uma revisão ainda pendente. */
  private void retryReviewTask(
      String agentKey,
      String activityId,
      String title,
      String description,
      String sourceReference,
      BusinessProcessDefinition process) {
    agentTaskService.retryBlockedByHumanOrRefreshPending(
        new CreateAgentTaskRequest(
            agentKey,
            "Operador do Marketing Hub",
            title,
            description,
            "HIGH",
            sourceReference,
            process.getId(),
            activityId,
            false,
            null));
  }

  /** Monta o contexto estruturado que restringe a execução à homologação sem tráfego real. */
  private String buildAuditBrief(
      CommercialPlan plan, Long experimentId, JourneyExecution journeyExecution) {
    Map<String, Object> brief = new LinkedHashMap<>();
    brief.put("approvalRecommendation", "REGENERATE_BEFORE_PUBLICATION");
    brief.put("score", 0);
    brief.put("source", "COMMERCIAL_PLAN_JOURNEY_HOMOLOGATION");
    brief.put("commercialPlanId", plan.getId());
    brief.put("experimentId", experimentId);
    brief.put("journeyAttempt", journeyExecution.attemptNumber());
    brief.put("previousAttemptBlocks", journeyExecution.previousAttemptBlocks());
    brief.put("successCriteria", plan.getSuccessCriteria());
    brief.put("stopCriteria", plan.getStopCriteria());
    brief.put("currentBlocker", plan.getCurrentBlocker());
    brief.put("rootCause", plan.getRootCause());
    brief.put("nextAction", plan.getNextAction());
    brief.put("testIsolation", "mh_test=1");
    brief.put("recoveryPolicy", "BPM_TASK_RETRY_WITH_PERSISTED_CAUSE");
    brief.put("publicationAuthorized", false);
    brief.put("mediaSpendAuthorized", false);
    brief.put(
        "requiredEvidence",
        List.of(
            "landing responsiva e sem bloqueios no Quality Review independente",
            "checkout canônico preservado pelo contrato comercial do backend",
            "mesmo pacote criativo aprovado no subprocesso anterior",
            "decisão APPROVED de Psique e Têmis sobre a mesma landing",
            "nenhuma publicação, pagamento, contato ou gasto externo"));
    brief.put(
        "objective",
        "Concluir a geração e a aprovação da landing com dados segregados; preflight do checkout, pagamento, acesso e eventos pertencem ao subprocesso seguinte.");
    try {
      return objectMapper.writeValueAsString(brief);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao montar contexto da homologação da landing. planId={} experimentId={} journeyAttempt={}",
          plan.getId(),
          experimentId,
          journeyExecution.attemptNumber(),
          ex);
      throw new IllegalStateException("Não foi possível montar o contexto da homologação", ex);
    }
  }

  /** Identifica uma execução idempotente e o aprendizado funcional herdado do bloqueio anterior. */
  private record JourneyExecution(
      String sourceReference,
      int attemptNumber,
      List<Map<String, Object>> previousAttemptBlocks,
      List<AgentTask> currentTasks,
      boolean blocked) {
    /** Reconhece a retomada já aberta e mantém o comando idempotente durante os pareceres. */
    private boolean reviewResumeActive() {
      return currentTasks.stream()
          .map(AgentTask::getDescription)
          .filter(java.util.Objects::nonNull)
          .anyMatch(
              description ->
                  description.contains("REUSE_APPROVED_LANDING_WITH_FRESH_CANONICAL_EVIDENCE"));
    }
  }
}
