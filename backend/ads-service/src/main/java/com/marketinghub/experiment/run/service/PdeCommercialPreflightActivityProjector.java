package com.marketinghub.experiment.run.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: projetar o run técnico na atividade preflight do processo comercial do produto.
 */
@Service
@Slf4j
public class PdeCommercialPreflightActivityProjector {
  static final String PROCESS_CODE = "pde-commercial-homologation-activation";
  static final String ACTIVITY_ID = "preflight";
  private static final Set<ExperimentRunStatus> COMPLETED_STATUSES =
      Set.of(
          ExperimentRunStatus.READY_TO_PUBLISH,
          ExperimentRunStatus.PUBLICATION_PENDING,
          ExperimentRunStatus.PUBLISHING,
          ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE,
          ExperimentRunStatus.RUNNING,
          ExperimentRunStatus.PAUSE_REQUESTED,
          ExperimentRunStatus.PAUSED,
          ExperimentRunStatus.STOP_REQUESTED,
          ExperimentRunStatus.COMPLETED);

  private final BusinessProcessDefinitionRepository processRepository;
  private final BusinessProcessActivityDefinitionRepository activityDefinitionRepository;
  private final BusinessProcessActivityInstanceRepository activityInstanceRepository;
  private final ProductProcessActivityPredecessorService predecessorService;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Configura catálogo, instâncias, ordem BPM e evidências do preflight. */
  @Autowired
  public PdeCommercialPreflightActivityProjector(
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      ProductProcessActivityPredecessorService predecessorService,
      ObjectMapper objectMapper) {
    this(
        processRepository,
        activityDefinitionRepository,
        activityInstanceRepository,
        predecessorService,
        objectMapper,
        Clock.systemUTC());
  }

  /** Permite validar estados e horários com um relógio determinístico. */
  PdeCommercialPreflightActivityProjector(
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      ProductProcessActivityPredecessorService predecessorService,
      ObjectMapper objectMapper,
      Clock clock) {
    this.processRepository = processRepository;
    this.activityDefinitionRepository = activityDefinitionRepository;
    this.activityInstanceRepository = activityInstanceRepository;
    this.predecessorService = predecessorService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Sincroniza status, gates e evidência do run sem avançar antes das atividades predecessoras. */
  @Transactional
  public void synchronize(ExperimentRun run, List<ExperimentRunGateResult> gates) {
    if (run == null
        || run.getId() == null
        || run.getExperiment() == null
        || run.getExperiment().getId() == null
        || run.getExperiment().getProduct() == null
        || run.getStatus() == ExperimentRunStatus.DRAFT) {
      return;
    }
    Optional<BusinessProcessDefinition> process =
        processRepository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            PROCESS_CODE, "PUBLISHED");
    if (process.isEmpty()) {
      return;
    }
    BusinessProcessActivityDefinition activity =
        activityDefinitionRepository
            .findByProcessDefinitionIdAndActivityId(process.get().getId(), ACTIVITY_ID)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Atividade preflight não encontrada no processo comercial publicado."));
    String sourceReference = "experiment:" + run.getExperiment().getId();
    ProductProcessActivityPredecessorReadiness predecessor =
        predecessorService.readiness(process.get(), activity, sourceReference);
    if (!predecessor.ready()) {
      return;
    }
    Instant now = Instant.now(clock);
    Optional<BusinessProcessActivityInstance> latest =
        activityInstanceRepository
            .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                activity.getId(), sourceReference);
    if (latest.isPresent()
        && "COMPLETED".equals(latest.get().getStatus())
        && latest.get().isObjectiveAchieved()) {
      return;
    }
    BusinessProcessActivityInstance instance =
        latest
            .filter(value -> representsRun(value, run.getId()))
            .orElseGet(
                () ->
                    newInstance(
                        activity,
                        sourceReference,
                        latest.map(value -> value.getOccurrenceNumber() + 1).orElse(1),
                        run.getPreflightStartedAt() == null
                            ? run.getRequestedAt()
                            : run.getPreflightStartedAt(),
                        now));
    boolean completed = COMPLETED_STATUSES.contains(run.getStatus());
    boolean blocked =
        run.getStatus() == ExperimentRunStatus.PREFLIGHT_FAILED
            || run.getStatus() == ExperimentRunStatus.FAILED;
    boolean cancelled = run.getStatus() == ExperimentRunStatus.CANCELLED;
    String status =
        completed ? "COMPLETED" : blocked ? "BLOCKED" : cancelled ? "CANCELLED" : "PENDING";
    instance.setStatus(status);
    instance.setExitedAt(completed || blocked || cancelled ? now : null);
    instance.setObjectiveAchieved(completed);
    instance.setObjectiveEvidenceJson(evidence(run, gates, now).toString());
    instance.setBlockedReason(blocked ? blockerReason(gates) : null);
    instance.setKnownCostUsd(BigDecimal.ZERO.setScale(8));
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    instance.setUpdatedAt(now);
    activityInstanceRepository.save(instance);
  }

  /** Cria uma ocorrência vinculada ao run antes de projetar seu estado atual. */
  private BusinessProcessActivityInstance newInstance(
      BusinessProcessActivityDefinition activity,
      String sourceReference,
      int occurrenceNumber,
      Instant enteredAt,
      Instant now) {
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(activity);
    instance.setSourceReference(sourceReference);
    instance.setOccurrenceNumber(occurrenceNumber);
    instance.setEnteredAt(enteredAt == null ? now : enteredAt);
    instance.setCreatedAt(now);
    return instance;
  }

  /** Monta evidência funcional sem incorporar JSON serializado dentro de outro JSON. */
  private ObjectNode evidence(
      ExperimentRun run, List<ExperimentRunGateResult> gates, Instant synchronizedAt) {
    List<ExperimentRunGateResult> safeGates = gates == null ? List.of() : gates;
    long blockers =
        safeGates.stream()
            .filter(
                gate ->
                    gate.getStatus() == ExperimentRunGateStatus.FAIL
                        || gate.getStatus() == ExperimentRunGateStatus.PENDING)
            .count();
    ObjectNode evidence = objectMapper.createObjectNode();
    evidence.put("source", "EXPERIMENT_RUN");
    evidence.put("experimentId", run.getExperiment().getId());
    evidence.put("runId", run.getId());
    evidence.put("runNumber", run.getRunNumber());
    evidence.put("runStatus", run.getStatus().name());
    evidence.put("gateCount", safeGates.size());
    evidence.put("blockerCount", blockers);
    evidence.put("synchronizedAt", synchronizedAt.toString());
    if (run.getPreflightCompletedAt() != null) {
      evidence.put("preflightCompletedAt", run.getPreflightCompletedAt().toString());
    }
    return evidence;
  }

  /** Resume a primeira causa persistida que impede a conclusão do preflight. */
  private String blockerReason(List<ExperimentRunGateResult> gates) {
    return (gates == null ? List.<ExperimentRunGateResult>of() : gates)
        .stream()
            .filter(
                gate ->
                    gate.getStatus() == ExperimentRunGateStatus.FAIL
                        || gate.getStatus() == ExperimentRunGateStatus.PENDING)
            .map(ExperimentRunGateResult::getSummary)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse("O preflight possui requisito técnico pendente.");
  }

  /** Confirma se a ocorrência atual representa o mesmo run antes de reutilizá-la. */
  private boolean representsRun(BusinessProcessActivityInstance instance, Long runId) {
    try {
      return instance.getObjectiveEvidenceJson() != null
          && objectMapper.readTree(instance.getObjectiveEvidenceJson()).path("runId").asLong(-1L)
              == runId;
    } catch (Exception ex) {
      log.error(
          "Falha ao ler a evidência da instância BPM durante reconciliação do preflight. activityInstanceId={} runId={}",
          instance.getId(),
          runId,
          ex);
      return false;
    }
  }
}
