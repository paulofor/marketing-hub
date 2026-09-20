package com.marketinghub.facebookads.resumption.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.funnel.ExperimentFinancialGuardrailPolicy;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.facebookads.*;
import com.marketinghub.facebookads.resumption.FacebookCampaignResumption;
import com.marketinghub.facebookads.resumption.service.request.ResumeCampaignRequest;
import com.marketinghub.facebookads.resumption.service.result.ResumeCampaignResult;
import com.marketinghub.facebookads.resumption.service.summary.ResumeCampaignSummary;
import com.marketinghub.facebookads.resumption.service.view.ResumeCampaignView;
import com.marketinghub.repository.jpa.experiment.*;
import com.marketinghub.repository.jpa.facebookads.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Orquestra autorização financeira, reserva e confirmação de retomadas sem executar chamadas Meta.
 */
@Service
public class FacebookCampaignResumptionService {
  private static final Logger LOG =
      LoggerFactory.getLogger(FacebookCampaignResumptionService.class);
  private final FacebookCampaignResumptionRepository requests;
  private final ExperimentRepository experiments;
  private final FacebookAdsCampaignRepository campaigns;
  private final ExperimentCampaignMetricRepository metrics;
  private final ExperimentStatusChangeRepository history;
  private final ExperimentReadinessService readiness;
  private final ObjectMapper json;

  /** Recebe as fontes persistidas da autorização, prontidão e histórico comercial. */
  public FacebookCampaignResumptionService(
      FacebookCampaignResumptionRepository requests,
      ExperimentRepository experiments,
      FacebookAdsCampaignRepository campaigns,
      ExperimentCampaignMetricRepository metrics,
      ExperimentStatusChangeRepository history,
      ExperimentReadinessService readiness,
      ObjectMapper json) {
    this.requests = requests;
    this.experiments = experiments;
    this.campaigns = campaigns;
    this.metrics = metrics;
    this.history = history;
    this.readiness = readiness;
    this.json = json;
  }

  /** Expõe a possibilidade de retomada e o histórico mais recente sem alterar dados. */
  @Transactional(readOnly = true)
  public ResumeCampaignSummary summary(Long experimentId) {
    Experiment e = experiments.findById(experimentId).orElseThrow();
    FacebookCampaignResumption latest =
        requests.findFirstByExperimentIdOrderByIdDesc(experimentId).orElse(null);
    List<FacebookAdsCampaign> linked = campaigns.findDetailedByExperimentId(experimentId);
    boolean applicable = e.getPlatform() == ExperimentPlatform.FACEBOOK && !linked.isEmpty();
    String blocker = blocker(e, linked, latest);
    return new ResumeCampaignSummary(
        applicable,
        applicable && blocker == null,
        blocker,
        spend(e),
        e.getMediaSpendLimit(),
        ExperimentFinancialGuardrailPolicy.zeroPrimaryResultMinimumSpend(e),
        view(latest, false));
  }

  /**
   * Valida consentimento, preserva histórico e registra um único pedido pendente por experimento.
   */
  @Transactional
  public ResumeCampaignView request(Long experimentId, ResumeCampaignRequest input) {
    Experiment e = experiments.findForFacebookRelease(experimentId).orElseThrow();
    FacebookCampaignResumption latest =
        requests.findFirstByExperimentIdOrderByIdDesc(experimentId).orElse(null);
    if (latest != null && Set.of("PENDING", "RUNNING").contains(latest.getStatus())) {
      if (input != null
          && Objects.equals(input.endDate(), latest.getEndDate())
          && input.totalLimit() != null
          && input.totalLimit().compareTo(latest.getTotalLimit()) == 0
          && Objects.equals(input.reason(), latest.getReason())
          && input.authorizeSpending()
          && input.useTotalLimitForZeroResults()) return view(latest, false);
      throw conflict("Já existe uma retomada em andamento para este experimento.");
    }
    List<FacebookAdsCampaign> linked = campaigns.findDetailedByExperimentId(experimentId);
    String blocker = blocker(e, linked, latest);
    if (blocker != null) throw conflict(blocker);
    if (input == null || !input.authorizeSpending() || !input.useTotalLimitForZeroResults())
      throw bad("Confirme o teto acumulado e a exceção individual à parada sem resultados.");
    if (input.reason() == null
        || input.reason().trim().length() < 10
        || input.reason().length() > 800) throw bad("Informe motivo entre 10 e 800 caracteres.");
    if (input.totalLimit() == null
        || input.totalLimit().scale() > 2
        || input.totalLimit().signum() <= 0
        || input.totalLimit().compareTo(new BigDecimal("99999999.99")) > 0
        || input.totalLimit().compareTo(spend(e)) <= 0)
      throw bad("Teto acumulado deve superar o gasto e ter no máximo duas casas decimais.");
    LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    if (input.endDate() == null
        || input.endDate().isBefore(today)
        || input.endDate().isAfter(today.plusDays(30)))
      throw bad("Prazo da retomada deve estar entre hoje e 30 dias.");
    if (e.getDailyBudget() == null
        || e.getDailyBudget().signum() <= 0
        || e.getDailyBudget().compareTo(input.totalLimit()) > 0)
      throw bad("Orçamento diário de referência incompatível com o teto.");
    FacebookAdsCampaign campaign = linked.get(0);
    FacebookCampaignResumption r = new FacebookCampaignResumption();
    r.setExperimentId(e.getId());
    r.setCampaignId(campaign.getId());
    r.setAdSetId(campaign.getAdSets().get(0).getId());
    r.setStatus("PENDING");
    r.setTotalLimit(input.totalLimit());
    r.setPreviousLimit(e.getMediaSpendLimit());
    r.setPreviousStartDate(e.getStartDate());
    r.setPreviousEndDate(e.getEndDate());
    r.setEndDate(input.endDate());
    r.setReason(input.reason().trim());
    r.setDestinationUrl(e.getFollowUpActionUrl());
    r.setRequestedAt(Instant.now());
    e.setStartDate(today);
    e.setEndDate(input.endDate());
    e.setMediaSpendLimit(input.totalLimit());
    e.setZeroResultSpendLimit(input.totalLimit());
    if (!readiness.isReadyForCampaign(e))
      throw conflict(
          "Requisitos comerciais da campanha precisam estar aprovados antes da retomada.");
    r = requests.save(r);
    record(
        e,
        "META_RESUME_REQUEST",
        r.getReason()
            + " Teto acumulado: "
            + r.getTotalLimit()
            + "; fim: "
            + r.getEndDate()
            + "; exceção sem resultado: teto autorizado.",
        e.getStatus());
    LOG.info(
        "Retomada autorizada: requestId={} experimentId={} campaignId={} totalLimit={} endDate={}",
        r.getId(),
        e.getId(),
        campaign.getId(),
        r.getTotalLimit(),
        r.getEndDate());
    return view(r, false);
  }

  /** Lista uma fila pequena; o executor precisa reservar cada item antes de alterar a Meta. */
  @Transactional(readOnly = true)
  public List<ResumeCampaignView> pending() {
    return requests.findPending(Instant.now(), PageRequest.of(0, 20)).stream()
        .map(r -> view(r, false))
        .toList();
  }

  /** Reserva exclusivamente o pedido e permite recuperar queda do executor após quinze minutos. */
  @Transactional
  public ResumeCampaignView claim(Long id) {
    FacebookCampaignResumption r = requests.findLocked(id).orElseThrow();
    if (!("PENDING".equals(r.getStatus())
        || ("RUNNING".equals(r.getStatus()) && r.getLeaseUntil().isBefore(Instant.now()))))
      throw conflict("Retomada já reservada ou concluída.");
    r.setStatus("RUNNING");
    r.setLeaseToken(UUID.randomUUID().toString());
    r.setLeaseUntil(Instant.now().plusSeconds(900));
    return view(r, true);
  }

  /**
   * Confirma execução com evidência nativa antes de marcar a campanha e o experimento em execução.
   */
  @Transactional
  public ResumeCampaignView result(Long id, ResumeCampaignResult input) {
    FacebookCampaignResumption r = requests.findLocked(id).orElseThrow();
    if (input == null || r.getLeaseToken() == null || !r.getLeaseToken().equals(input.leaseToken()))
      throw conflict("Reserva inválida para este resultado.");
    if (Set.of("COMPLETED", "FAILED").contains(r.getStatus())) return view(r, false);
    if (!"RUNNING".equals(r.getStatus())) throw conflict("Pedido ainda não reservado.");
    if (input.evidence() != null && input.evidence().toString().length() > 100000)
      throw bad("Evidência excede limite.");
    Experiment e = experiments.findForFacebookRelease(r.getExperimentId()).orElseThrow();
    FacebookAdsCampaign campaign = campaigns.findById(r.getCampaignId()).orElseThrow();
    if (input.success()) {
      var evidence = input.evidence();
      if (evidence == null
          || !"ACTIVE".equals(evidence.path("campaignStatus").asText())
          || !r.getAdSetId().equals(evidence.path("adSetId").asText())
          || !r.getCampaignId().equals(evidence.path("campaignId").asText())
          || evidence.path("lifetimeBudgetMinor").asLong(-1)
              != r.getTotalLimit().movePointRight(2).longValueExact()
          || !r.getEndDate().toString().equals(evidence.path("endDate").asText())
          || !evidence.path("spend").isNumber()
          || evidence.path("spend").decimalValue().compareTo(r.getTotalLimit()) >= 0
          || e.getMediaSpendLimit().compareTo(r.getTotalLimit()) != 0)
        throw conflict("A Meta não confirmou teto, prazo e estado autorizados.");
      ExperimentStatus previous = e.getStatus();
      e.setStatus(ExperimentStatus.RUNNING);
      campaign.setStatus(FacebookAdStatus.ACTIVE);
      campaign.setMetricsFinalSyncedAt(null);
      campaign.setStopReason(null);
      campaign.setStopRequestedAt(null);
      campaign.setStopCompletedAt(null);
      campaign.setStopLastError(null);
      record(e, "META_RESUME_CONFIRMED", r.getReason(), previous);
      r.setStatus("COMPLETED");
      r.setError(null);
    } else {
      r.setStatus("FAILED");
      r.setError(
          input.error() == null
              ? "Falha ao retomar campanha; confira a evidência."
              : input.error().substring(0, Math.min(4000, input.error().length())));
      record(e, "META_RESUME_FAILED", r.getReason(), e.getStatus());
    }
    r.setEvidence(input.evidence() == null ? null : input.evidence().toString());
    r.setCompletedAt(Instant.now());
    LOG.info(
        "Resultado da retomada: requestId={} experimentId={} status={}",
        r.getId(),
        e.getId(),
        r.getStatus());
    return view(r, false);
  }

  /** Exige campanha pausada, conjunto único e ausência de pausa ainda pendente. */
  private String blocker(
      Experiment e, List<FacebookAdsCampaign> linked, FacebookCampaignResumption latest) {
    if (e.getPlatform() != ExperimentPlatform.FACEBOOK || linked.size() != 1)
      return "Retomada exige uma única campanha Facebook publicada.";
    if (latest != null && Set.of("PENDING", "RUNNING").contains(latest.getStatus()))
      return "Retomada aguardando confirmação da Meta.";
    if (!Set.of(
            ExperimentStatus.USER_STOPPED,
            ExperimentStatus.PAUSED,
            ExperimentStatus.STANDBY,
            ExperimentStatus.INCONCLUSIVE)
        .contains(e.getStatus()))
      return "Experimento precisa estar pausado para autorizar uma retomada.";
    FacebookAdsCampaign c = linked.get(0);
    if (c.getStatus() != FacebookAdStatus.PAUSED || c.getAdSets().size() != 1)
      return "Campanha deve estar pausada e possuir um único conjunto.";
    if (c.getStopRequestedAt() != null && c.getStopCompletedAt() == null)
      return "Aguarde a conclusão da pausa anterior.";
    if (e.getFollowUpActionUrl() == null || e.getFollowUpActionUrl().isBlank())
      return "Destino público não configurado.";
    return null;
  }

  /** Consulta o gasto acumulado sem reiniciar ou recomputar a coorte. */
  private BigDecimal spend(Experiment e) {
    return metrics
        .findByExperiment(e)
        .map(ExperimentCampaignMetric::getSpend)
        .orElse(BigDecimal.ZERO);
  }

  /** Registra autorização e confirmação no histórico já exibido pelo experimento. */
  private void record(Experiment e, String action, String reason, ExperimentStatus previous) {
    e.setLastStatusChangeAction(action);
    e.setLastStatusChangeReason(reason);
    e.setLastStatusChangedAt(Instant.now());
    history.save(
        ExperimentStatusChange.builder()
            .experiment(e)
            .previousStatus(previous)
            .newStatus(e.getStatus())
            .action(action)
            .reason(reason)
            .changedBy("ADMIN_UI")
            .changedAt(e.getLastStatusChangedAt())
            .build());
  }

  /** Converte a execução em contrato público e revela lease somente ao executor que reservou. */
  private ResumeCampaignView view(FacebookCampaignResumption r, boolean includeLease) {
    if (r == null) return null;
    try {
      return new ResumeCampaignView(
          r.getId(),
          r.getExperimentId(),
          r.getCampaignId(),
          r.getAdSetId(),
          r.getStatus(),
          r.getTotalLimit(),
          r.getEndDate(),
          r.getReason(),
          r.getDestinationUrl(),
          r.getRequestedAt(),
          r.getCompletedAt(),
          includeLease ? r.getLeaseToken() : null,
          r.getError(),
          r.getEvidence() == null ? null : json.readTree(r.getEvidence()));
    } catch (Exception ex) {
      LOG.error(
          "Erro lendo evidência da retomada: requestId={} experimentId={}",
          r.getId(),
          r.getExperimentId(),
          ex);
      throw new IllegalStateException("Evidência de retomada inválida", ex);
    }
  }

  /** Produz erro de validação sem alterar o estado comercial. */
  private ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  /** Produz conflito operacional preservando o pedido e a campanha existentes. */
  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
