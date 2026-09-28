package com.marketinghub.experiment.service;

import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentStatusChange;
import com.marketinghub.experiment.funnel.ExperimentFunnelStandbyService;
import com.marketinghub.experiment.run.service.ExperimentRunMetricLifecycleService;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentStatusChangeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: reconciliar execuções comerciais com a janela temporal autorizada. */
@Service
public class ExperimentWindowReconciliationService {
  private static final Logger log =
      LoggerFactory.getLogger(ExperimentWindowReconciliationService.class);
  private static final ZoneId COMMERCIAL_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final String ACTION = "AUTHORIZED_WINDOW_RECONCILIATION";

  private final ExperimentRepository experimentRepository;
  private final ExperimentStatusChangeRepository statusChangeRepository;
  private final ExperimentFunnelStandbyService standbyService;
  private final ExperimentRunMetricLifecycleService runMetricLifecycleService;
  private final AgentTaskService agentTaskService;
  private final Clock clock;

  /** Configura as fontes canônicas usadas para encerrar janelas comerciais vencidas. */
  @Autowired
  public ExperimentWindowReconciliationService(
      ExperimentRepository experimentRepository,
      ExperimentStatusChangeRepository statusChangeRepository,
      ExperimentFunnelStandbyService standbyService,
      ExperimentRunMetricLifecycleService runMetricLifecycleService,
      AgentTaskService agentTaskService) {
    this(
        experimentRepository,
        statusChangeRepository,
        standbyService,
        runMetricLifecycleService,
        agentTaskService,
        Clock.systemUTC());
  }

  /** Permite validar a reconciliação com relógio determinístico. */
  ExperimentWindowReconciliationService(
      ExperimentRepository experimentRepository,
      ExperimentStatusChangeRepository statusChangeRepository,
      ExperimentFunnelStandbyService standbyService,
      ExperimentRunMetricLifecycleService runMetricLifecycleService,
      AgentTaskService agentTaskService,
      Clock clock) {
    this.experimentRepository = experimentRepository;
    this.statusChangeRepository = statusChangeRepository;
    this.standbyService = standbyService;
    this.runMetricLifecycleService = runMetricLifecycleService;
    this.agentTaskService = agentTaskService;
    this.clock = clock;
  }

  /** Encerra execuções diretas que estejam sem janela válida, ainda não abertas ou vencidas. */
  @Transactional
  public ReconciliationResult reconcileDirectRunningWindows() {
    List<Experiment> candidates =
        experimentRepository.findForWindowReconciliation(
            ExperimentStatus.RUNNING, ExperimentPlatform.DIRECT_ONE_TO_ONE);
    LocalDate today = LocalDate.now(clock.withZone(COMMERCIAL_ZONE));
    List<Long> closedIds = new ArrayList<>();
    for (Experiment experiment : candidates) {
      String reason = directWindowViolation(experiment, today);
      if (reason != null
          && concludeAsInconclusive(experiment, reason, "GROWTH_OPERATOR_WINDOW_RECONCILIATION")) {
        closedIds.add(experiment.getId());
      }
    }
    return new ReconciliationResult(candidates.size(), closedIds.size(), List.copyOf(closedIds));
  }

  /**
   * Encerra uma campanha Facebook quando o worker ou os horários persistidos comprovam o fim da
   * autorização.
   */
  @Transactional
  public boolean reconcileFacebookWindow(
      FacebookAdsCampaign campaign, Instant observedAt, boolean windowExpiredReported) {
    Instant effectiveObservedAt = observedAt != null ? observedAt : Instant.now(clock);
    if (campaign == null
        || campaign.getExperiment() == null
        || !facebookWindowEnded(campaign, effectiveObservedAt, windowExpiredReported)) {
      return false;
    }
    String reason =
        "A janela autorizada na Meta terminou em %s; a medição final foi solicitada e nenhuma nova entrega é permitida."
            .formatted(resolveFacebookWindowEnd(campaign));
    return concludeAsInconclusive(campaign.getExperiment(), reason, "FACEBOOK_ADS_STATUS_SYNC");
  }

  /** Identifica a causa temporal que invalida uma execução direta ativa. */
  private String directWindowViolation(Experiment experiment, LocalDate today) {
    if (experiment.getStartDate() == null || experiment.getEndDate() == null) {
      return "Execução direta encerrada porque não possui início e término comerciais autorizados.";
    }
    if (experiment.getStartDate().isAfter(experiment.getEndDate())) {
      return "Execução direta encerrada porque a janela comercial está invertida.";
    }
    if (today.isBefore(experiment.getStartDate())) {
      return "Execução direta encerrada porque foi marcada como RUNNING antes do início autorizado.";
    }
    if (today.isAfter(experiment.getEndDate())) {
      return "Execução direta encerrada porque a janela comercial terminou em %s."
          .formatted(experiment.getEndDate());
    }
    return null;
  }

  /** Confirma o vencimento pelo término agregado da campanha ou de todos os conjuntos. */
  private boolean facebookWindowEnded(
      FacebookAdsCampaign campaign, Instant observedAt, boolean windowExpiredReported) {
    if (windowExpiredReported) {
      return true;
    }
    if (campaign.getMetaStopTime() != null && !campaign.getMetaStopTime().isAfter(observedAt)) {
      return true;
    }
    List<FacebookAdsAdSet> adSets = campaign.getAdSets();
    if (adSets == null || adSets.isEmpty()) {
      return false;
    }
    LocalDateTime observedLocal = LocalDateTime.ofInstant(observedAt, COMMERCIAL_ZONE);
    return adSets.stream()
        .allMatch(
            adSet -> adSet.getEndTime() != null && !adSet.getEndTime().isAfter(observedLocal));
  }

  /** Resolve o término mais preciso disponível para compor a auditoria do encerramento. */
  private Object resolveFacebookWindowEnd(FacebookAdsCampaign campaign) {
    if (campaign.getMetaStopTime() != null) {
      return campaign.getMetaStopTime();
    }
    return campaign.getAdSets().stream()
        .map(FacebookAdsAdSet::getEndTime)
        .filter(java.util.Objects::nonNull)
        .max(LocalDateTime::compareTo)
        .orElse(null);
  }

  /** Persiste o estado terminal, encerra o run e cancela trabalhos que não podem mais avançar. */
  private boolean concludeAsInconclusive(Experiment experiment, String reason, String changedBy) {
    if (experiment == null || experiment.getStatus() != ExperimentStatus.RUNNING) {
      return false;
    }
    Instant changedAt = Instant.now(clock);
    ExperimentStatus previousStatus = experiment.getStatus();
    experiment.setStatus(ExperimentStatus.INCONCLUSIVE);
    experiment.setLastStatusChangeAction(ACTION);
    experiment.setLastStatusChangeReason(reason);
    experiment.setLastStatusChangedAt(changedAt);
    experimentRepository.save(experiment);
    statusChangeRepository.save(
        ExperimentStatusChange.builder()
            .experiment(experiment)
            .previousStatus(previousStatus)
            .newStatus(ExperimentStatus.INCONCLUSIVE)
            .action(ACTION)
            .reason(reason)
            .changedBy(changedBy)
            .changedAt(changedAt)
            .build());
    standbyService.requestFacebookCampaignStops(
        experiment.getId(), FacebookCampaignStopReason.CAMPAIGN_AUTHORIZED_WINDOW_ENDED, reason);
    runMetricLifecycleService.completeCommercialStop(
        experiment, FacebookCampaignStopReason.CAMPAIGN_AUTHORIZED_WINDOW_ENDED, reason);
    agentTaskService.cancelActiveTasksBySourceReference("experiment:" + experiment.getId(), reason);
    log.info(
        "experiment_window_reconciled experimentId={} previousStatus={} newStatus={} changedBy={} reason={}",
        experiment.getId(),
        previousStatus,
        experiment.getStatus(),
        changedBy,
        reason);
    return true;
  }

  /** Resume a varredura determinística executada pelo Operador de Crescimento. */
  public record ReconciliationResult(int scanned, int closed, List<Long> closedExperimentIds) {}
}
