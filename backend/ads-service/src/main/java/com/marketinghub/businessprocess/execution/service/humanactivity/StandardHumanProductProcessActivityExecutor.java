package com.marketinghub.businessprocess.execution.service.humanactivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: padronizar confirmação, efeito especializado e auditoria das decisões humanas.
 */
@Service
@Slf4j
public class StandardHumanProductProcessActivityExecutor
    implements HumanProductProcessActivityExecutor {
  private static final String HUMAN_OWNER = "operador humano";
  private static final String APPROVE = "APPROVE";
  private static final String REJECT = "REJECT";
  private static final String MARKETING_HUB_OPERATOR = "Operador humano pelo Marketing Hub";

  private final BusinessProcessActivityInstanceRepository activityInstanceRepository;
  private final ProductProcessActivityPredecessorService predecessorService;
  private final ObjectMapper objectMapper;
  private final List<HumanProductProcessActivityHandler> handlers;
  private final Clock clock;

  /** Configura persistência, ordem do processo e efeitos especializados das decisões. */
  @Autowired
  public StandardHumanProductProcessActivityExecutor(
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      ProductProcessActivityPredecessorService predecessorService,
      ObjectMapper objectMapper,
      List<HumanProductProcessActivityHandler> handlers) {
    this(activityInstanceRepository, predecessorService, objectMapper, handlers, Clock.systemUTC());
  }

  /** Permite validar horários e decisões com um relógio determinístico. */
  StandardHumanProductProcessActivityExecutor(
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      ProductProcessActivityPredecessorService predecessorService,
      ObjectMapper objectMapper,
      List<HumanProductProcessActivityHandler> handlers,
      Clock clock) {
    this.activityInstanceRepository = activityInstanceRepository;
    this.predecessorService = predecessorService;
    this.objectMapper = objectMapper;
    this.handlers = List.copyOf(handlers);
    this.clock = clock;
  }

  /** Reconhece somente atividades explicitamente atribuídas ao Operador humano. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && activityDefinition.getOwnerName() != null
        && HUMAN_OWNER.equals(activityDefinition.getOwnerName().trim().toLowerCase(Locale.ROOT));
  }

  /** Combina a ordem BPM com os requisitos de domínio sem executar nenhum efeito. */
  @Override
  @Transactional(readOnly = true)
  public HumanProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    if (!supports(process, activityDefinition)) {
      return unavailable(
          "A atividade não pertence ao Operador humano.", process, activityDefinition);
    }
    ProductProcessActivityPredecessorReadiness predecessor =
        predecessorService.readiness(process, activityDefinition, sourceReference);
    HumanProductProcessActivityReadiness domain =
        handler(process, activityDefinition)
            .map(value -> value.readiness(process, activityDefinition, product, sourceReference))
            .orElseGet(() -> standardReadiness(process, activityDefinition));
    if (predecessor.ready()) {
      return domain;
    }
    List<HumanProductProcessActivityRequirement> requirements =
        new java.util.ArrayList<>(domain.requirements());
    requirements.addFirst(
        new HumanProductProcessActivityRequirement(
            "PREDECESSORS_COMPLETED",
            "Etapas anteriores concluídas",
            false,
            predecessor.reason(),
            predecessor.reason()));
    return new HumanProductProcessActivityReadiness(
        false,
        predecessor.reason(),
        domain.actionLabel(),
        domain.description(),
        domain.confirmationTitle(),
        domain.confirmationMessage(),
        domain.confirmationToken(),
        domain.workspaceCode(),
        domain.workspaceReferenceId(),
        requirements);
  }

  /** Valida a confirmação, aplica o efeito aprovado e registra a decisão na instância BPM. */
  @Override
  @Transactional
  public HumanProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request) {
    HumanProductProcessActivityReadiness readiness =
        readiness(process, activityDefinition, product, sourceReference);
    if (!readiness.ready()) {
      throw new IllegalStateException(readiness.reason());
    }
    ProductProcessActivityExecutionRequest resolvedRequest =
        validateAndResolveRequest(request, readiness);
    Instant decidedAt = Instant.now(clock);
    BusinessProcessActivityInstance instance =
        decisionInstance(activityDefinition, sourceReference, decidedAt);
    instance.setStatus("IN_PROGRESS");
    instance.setObjectiveAchieved(false);
    instance.setKnownCostUsd(BigDecimal.ZERO.setScale(8));
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    instance.setUpdatedAt(decidedAt);
    reserveDecision(instance, activityDefinition, sourceReference);
    Optional<HumanProductProcessActivityHandler> handler = handler(process, activityDefinition);
    if (APPROVE.equals(resolvedRequest.decision()) && handler.isPresent()) {
      handler.get().approve(process, activityDefinition, product, sourceReference, resolvedRequest);
    }
    boolean approved = APPROVE.equals(resolvedRequest.decision());
    ObjectNode evidence = objectMapper.createObjectNode();
    evidence.put("decision", resolvedRequest.decision());
    evidence.put("operatorName", resolvedRequest.operatorName());
    evidence.put("justification", resolvedRequest.justification());
    evidence.put("evidenceReference", resolvedRequest.evidenceReference());
    evidence.put("confirmationToken", resolvedRequest.confirmationToken());
    evidence.put("decisionMode", readiness.decisionMode());
    evidence.put("decidedAt", decidedAt.toString());
    instance.setStatus(approved ? "COMPLETED" : "BLOCKED");
    instance.setExitedAt(decidedAt);
    instance.setObjectiveAchieved(approved);
    instance.setObjectiveEvidenceJson(evidence.toString());
    instance.setBlockedReason(approved ? null : resolvedRequest.justification());
    instance.setKnownCostUsd(BigDecimal.ZERO.setScale(8));
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    instance.setUpdatedAt(decidedAt);
    activityInstanceRepository.save(instance);
    return new HumanProductProcessActivityExecutionResult(
        sourceReference,
        approved ? "COMPLETED" : "BLOCKED",
        approved,
        approved
            ? "Decisão humana aprovada e registrada com evidência auditável."
            : "Decisão humana reprovada; a causa foi registrada para correção.");
  }

  /** Reserva a ocorrência antes do efeito para impedir duas autorizações simultâneas. */
  private void reserveDecision(
      BusinessProcessActivityInstance instance,
      BusinessProcessActivityDefinition activityDefinition,
      String sourceReference) {
    try {
      activityInstanceRepository.saveAndFlush(instance);
    } catch (DataIntegrityViolationException ex) {
      log.error(
          "Conflito ao reservar decisão humana. activityDefinitionId={} sourceReference={} occurrenceNumber={}",
          activityDefinition.getId(),
          sourceReference,
          instance.getOccurrenceNumber(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Outra decisão para esta atividade já está sendo registrada.", ex);
    }
  }

  /** Resolve no máximo um efeito especializado para impedir duas autoridades no mesmo gate. */
  private Optional<HumanProductProcessActivityHandler> handler(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    List<HumanProductProcessActivityHandler> compatible =
        handlers.stream().filter(value -> value.supports(process, activityDefinition)).toList();
    if (compatible.size() > 1) {
      throw new IllegalStateException(
          "Mais de um handler humano atende à atividade "
              + activityDefinition.getActivityId()
              + " do processo "
              + process.getProcessCode()
              + ".");
    }
    return compatible.stream().findFirst();
  }

  /** Define a confirmação segura usada por gates humanos sem efeito especializado. */
  private HumanProductProcessActivityReadiness standardReadiness(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return new HumanProductProcessActivityReadiness(
        true,
        "A decisão está liberada e será preservada na instância desta atividade.",
        "Registrar decisão",
        "Aprove ou reprove explicitamente, informando motivo e evidência verificável.",
        "Confirmar decisão humana",
        "Confirmo que revisei o objetivo, as evidências e o impacto desta decisão.",
        confirmationToken(process, activityDefinition),
        null,
        null,
        List.of());
  }

  /** Monta a resposta segura para uma atividade que não pode usar este executor. */
  private HumanProductProcessActivityReadiness unavailable(
      String reason,
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition) {
    return new HumanProductProcessActivityReadiness(
        false,
        reason,
        "Registrar decisão",
        "A atividade precisa de uma decisão humana explícita.",
        "Confirmar decisão humana",
        "Confirme somente após revisar objetivo, evidências e impacto.",
        confirmationToken(process, activityDefinition),
        null,
        null,
        List.of());
  }

  /** Cria um token específico da versão e atividade para impedir confirmação genérica acidental. */
  private String confirmationToken(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    String processCode = process == null ? "unknown-process" : process.getProcessCode();
    String activityId =
        activityDefinition == null ? "unknown-activity" : activityDefinition.getActivityId();
    return "CONFIRM:" + processCode + ":" + activityId;
  }

  /** Valida a decisão e completa no backend a auditoria da confirmação simplificada. */
  private ProductProcessActivityExecutionRequest validateAndResolveRequest(
      ProductProcessActivityExecutionRequest request,
      HumanProductProcessActivityReadiness readiness) {
    if (request == null) {
      throw invalidRequest("A decisão humana é obrigatória.");
    }
    if (!APPROVE.equals(request.decision()) && !REJECT.equals(request.decision())) {
      throw invalidRequest("A decisão deve ser APPROVE ou REJECT.");
    }
    if (!readiness.confirmationToken().equals(request.confirmationToken())) {
      throw invalidRequest("A confirmação não corresponde a esta atividade.");
    }
    if (readiness.reviewAndAccept()) {
      return resolveReviewAndAcceptRequest(request, readiness);
    }
    requireUsefulText(request.operatorName(), 3, 191, "Informe o responsável pela decisão.");
    requireUsefulText(
        request.justification(), 10, 2000, "Informe uma justificativa objetiva para a decisão.");
    requireUsefulText(
        request.evidenceReference(), 3, 1000, "Informe uma referência de evidência auditável.");
    return new ProductProcessActivityExecutionRequest(
        request.decision(),
        request.operatorName().trim(),
        request.justification().trim(),
        request.evidenceReference().trim(),
        request.confirmationToken().trim());
  }

  /** Usa o contexto persistido para que aprovar exija somente revisar e aceitar. */
  private ProductProcessActivityExecutionRequest resolveReviewAndAcceptRequest(
      ProductProcessActivityExecutionRequest request,
      HumanProductProcessActivityReadiness readiness) {
    String justification =
        APPROVE.equals(request.decision())
            ? "Autorização registrada após revisão do resumo: " + readiness.confirmationMessage()
            : request.justification();
    requireUsefulText(
        justification,
        10,
        2000,
        APPROVE.equals(request.decision())
            ? "O resumo auditável da autorização está incompleto."
            : "Informe o motivo para não autorizar.");
    requireUsefulText(
        readiness.auditEvidenceReference(),
        3,
        1000,
        "A autorização ainda não possui referência de evidência auditável.");
    return new ProductProcessActivityExecutionRequest(
        request.decision(),
        MARKETING_HUB_OPERATOR,
        justification.trim(),
        readiness.auditEvidenceReference().trim(),
        request.confirmationToken().trim());
  }

  /** Rejeita texto ausente ou fora do limite antes de qualquer alteração persistida. */
  private void requireUsefulText(
      String value, int minimumLength, int maximumLength, String reason) {
    if (!usefulText(value, minimumLength, maximumLength)) {
      throw invalidRequest(reason);
    }
  }

  /** Converte entrada humana inválida em resposta HTTP 400 sem executar efeito de domínio. */
  private ResponseStatusException invalidRequest(String reason) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
  }

  /** Valida conteúdo e limite antes de persistir a auditoria humana. */
  private boolean usefulText(String value, int minimumLength, int maximumLength) {
    return value != null
        && value.trim().length() >= minimumLength
        && value.trim().length() <= maximumLength;
  }

  /** Preserva tentativas anteriores bloqueadas ao abrir uma nova ocorrência de decisão. */
  private BusinessProcessActivityInstance decisionInstance(
      BusinessProcessActivityDefinition activityDefinition,
      String sourceReference,
      Instant decidedAt) {
    Optional<BusinessProcessActivityInstance> latest =
        activityInstanceRepository
            .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                activityDefinition.getId(), sourceReference);
    if (latest.isPresent()
        && !"BLOCKED".equals(latest.get().getStatus())
        && !"CANCELLED".equals(latest.get().getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A atividade já possui decisão ativa ou concluída neste ciclo.");
    }
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(activityDefinition);
    instance.setSourceReference(sourceReference);
    instance.setOccurrenceNumber(latest.map(value -> value.getOccurrenceNumber() + 1).orElse(1));
    instance.setEnteredAt(decidedAt);
    instance.setCreatedAt(decidedAt);
    return instance;
  }
}
