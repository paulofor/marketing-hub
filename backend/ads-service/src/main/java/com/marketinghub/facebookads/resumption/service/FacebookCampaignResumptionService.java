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
  private static final ZoneId COMMERCIAL_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final Set<String> AUTHORIZED_STATUSES = Set.of("PENDING", "RUNNING", "COMPLETED");
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
        e.getDailyBudget(),
        e.getMediaSpendLimit(),
        ExperimentFinancialGuardrailPolicy.zeroPrimaryResultMinimumSpend(e),
        e.getZeroPurchaseSpendLimit(),
        e.getPurchaseStopCount(),
        view(latest, false));
  }

  /**
   * Valida consentimento, preserva histórico e registra um único pedido pendente por experimento.
   */
  @Transactional
  public ResumeCampaignView request(Long experimentId, ResumeCampaignRequest input) {
    Experiment e = experiments.findForFacebookRelease(experimentId).orElseThrow();
    if (input == null || !input.authorizeSpending())
      throw bad("Confirme o orçamento diário, o teto acumulado e as condições de parada.");
    LocalDate today = LocalDate.now(COMMERCIAL_ZONE);
    LocalDate startDate = input.startDate() == null ? today : input.startDate();
    BigDecimal dailyBudget = input.dailyBudget() == null ? e.getDailyBudget() : input.dailyBudget();
    BigDecimal zeroResultSpendLimit =
        input.zeroResultSpendLimit() != null
            ? input.zeroResultSpendLimit()
            : input.useTotalLimitForZeroResults() ? input.totalLimit() : null;
    BigDecimal zeroPurchaseSpendLimit = input.zeroPurchaseSpendLimit();
    FacebookCampaignResumption latest =
        requests.findFirstByExperimentIdOrderByIdDesc(experimentId).orElse(null);
    if (latest != null && Set.of("PENDING", "RUNNING").contains(latest.getStatus())) {
      if (sameAuthorization(
          latest,
          dailyBudget,
          input.totalLimit(),
          startDate,
          input.endDate(),
          zeroResultSpendLimit,
          zeroPurchaseSpendLimit,
          input.purchaseStopCount(),
          input.reason())) return view(latest, false);
      throw conflict("Já existe uma retomada em andamento para este experimento.");
    }
    List<FacebookAdsCampaign> linked = campaigns.findDetailedByExperimentId(experimentId);
    String blocker = blocker(e, linked, latest);
    if (blocker != null) throw conflict(blocker);
    if (input.reason() == null
        || input.reason().trim().length() < 10
        || input.reason().length() > 800) throw bad("Informe motivo entre 10 e 800 caracteres.");
    if (input.totalLimit() == null
        || input.totalLimit().scale() > 2
        || input.totalLimit().signum() <= 0
        || input.totalLimit().compareTo(new BigDecimal("99999999.99")) > 0
        || input.totalLimit().compareTo(spend(e)) <= 0)
      throw bad("Teto acumulado deve superar o gasto e ter no máximo duas casas decimais.");
    if (startDate.isBefore(today)
        || input.endDate() == null
        || input.endDate().isBefore(startDate)
        || input.endDate().isAfter(today.plusDays(30)))
      throw bad("Início e fim da retomada devem estar entre hoje e 30 dias, em ordem válida.");
    if (!validMoney(dailyBudget) || dailyBudget.compareTo(input.totalLimit()) > 0)
      throw bad("Orçamento diário incompatível com o teto acumulado.");
    if (!validMoney(zeroResultSpendLimit)
        || zeroResultSpendLimit.compareTo(spend(e)) <= 0
        || zeroResultSpendLimit.compareTo(input.totalLimit()) > 0)
      throw bad(
          "A parada sem resultado deve superar o gasto atual e não pode ultrapassar o teto acumulado.");
    if (zeroPurchaseSpendLimit != null
        && (!validMoney(zeroPurchaseSpendLimit)
            || zeroPurchaseSpendLimit.compareTo(spend(e)) <= 0
            || zeroPurchaseSpendLimit.compareTo(input.totalLimit()) > 0))
      throw bad(
          "A parada sem compra deve superar o gasto atual e não pode ultrapassar o teto acumulado.");
    if (input.purchaseStopCount() != null
        && (input.purchaseStopCount() <= 0 || input.purchaseStopCount() > 100000))
      throw bad("A meta de compras para parada deve ficar entre 1 e 100000.");
    FacebookAdsCampaign campaign = linked.get(0);
    FacebookCampaignResumption r = new FacebookCampaignResumption();
    r.setExperimentId(e.getId());
    r.setCampaignId(campaign.getId());
    r.setAdSetId(campaign.getAdSets().get(0).getId());
    r.setStatus("PENDING");
    r.setTotalLimit(input.totalLimit());
    r.setDailyBudget(dailyBudget);
    r.setPreviousLimit(e.getMediaSpendLimit());
    r.setPreviousStartDate(e.getStartDate());
    r.setPreviousEndDate(e.getEndDate());
    r.setStartDate(startDate);
    r.setEndDate(input.endDate());
    r.setZeroResultSpendLimit(zeroResultSpendLimit);
    r.setZeroPurchaseSpendLimit(zeroPurchaseSpendLimit);
    r.setPurchaseStopCount(input.purchaseStopCount());
    r.setReason(input.reason().trim());
    r.setDestinationUrl(e.getFollowUpActionUrl());
    r.setRequestedAt(Instant.now());
    e.setDailyBudget(dailyBudget);
    e.setStartDate(startDate);
    e.setEndDate(input.endDate());
    e.setMediaSpendLimit(input.totalLimit());
    e.setZeroResultSpendLimit(zeroResultSpendLimit);
    e.setZeroPurchaseSpendLimit(zeroPurchaseSpendLimit);
    e.setPurchaseStopCount(input.purchaseStopCount());
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
            + "; orçamento diário: "
            + r.getDailyBudget()
            + "; janela: "
            + r.getStartDate()
            + " a "
            + r.getEndDate()
            + "; parada sem resultado: "
            + r.getZeroResultSpendLimit()
            + "; parada sem compra: "
            + (r.getZeroPurchaseSpendLimit() == null
                ? "não definida"
                : r.getZeroPurchaseSpendLimit())
            + "; parada por compras: "
            + (r.getPurchaseStopCount() == null ? "não definida" : r.getPurchaseStopCount())
            + ".",
        e.getStatus());
    LOG.info(
        "Retomada autorizada: requestId={} experimentId={} campaignId={} dailyBudget={} totalLimit={} startDate={} endDate={} zeroResultSpendLimit={} zeroPurchaseSpendLimit={} purchaseStopCount={}",
        r.getId(),
        e.getId(),
        campaign.getId(),
        r.getDailyBudget(),
        r.getTotalLimit(),
        r.getStartDate(),
        r.getEndDate(),
        r.getZeroResultSpendLimit(),
        r.getZeroPurchaseSpendLimit(),
        r.getPurchaseStopCount());
    return view(r, false);
  }

  /** Lista uma fila pequena; o executor precisa reservar cada item antes de alterar a Meta. */
  @Transactional(readOnly = true)
  public List<ResumeCampaignView> pending() {
    return requests
        .findPending(Instant.now(), LocalDate.now(COMMERCIAL_ZONE), PageRequest.of(0, 20))
        .stream()
        .map(r -> view(r, false))
        .toList();
  }

  /** Confirma que a campanha existente possui autorização estruturada igual ao plano vigente. */
  @Transactional(readOnly = true)
  public boolean hasCurrentAuthorization(Long experimentId) {
    Experiment e = experiments.findById(experimentId).orElse(null);
    FacebookCampaignResumption latest =
        requests.findFirstByExperimentIdOrderByIdDesc(experimentId).orElse(null);
    return e != null
        && latest != null
        && AUTHORIZED_STATUSES.contains(latest.getStatus())
        && sameMoney(e.getDailyBudget(), latest.getDailyBudget())
        && sameMoney(e.getMediaSpendLimit(), latest.getTotalLimit())
        && Objects.equals(e.getStartDate(), latest.getStartDate())
        && Objects.equals(e.getEndDate(), latest.getEndDate())
        && latest.getEndDate() != null
        && !latest.getEndDate().isBefore(LocalDate.now(COMMERCIAL_ZONE))
        && sameMoney(e.getZeroResultSpendLimit(), latest.getZeroResultSpendLimit())
        && sameMoney(e.getZeroPurchaseSpendLimit(), latest.getZeroPurchaseSpendLimit())
        && Objects.equals(e.getPurchaseStopCount(), latest.getPurchaseStopCount());
  }

  /** Bloqueia a confirmação BPM quando a retomada não foi autorizada pelo contrato financeiro. */
  @Transactional(readOnly = true)
  public void requireCurrentAuthorization(Long experimentId) {
    if (!hasCurrentAuthorization(experimentId)) {
      throw conflict(
          "Autorize primeiro a retomada da campanha existente com orçamento, janela e condições de parada estruturados.");
    }
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
          || !budgetEvidenceMatches(r, evidence)
          || !r.getStartDate().toString().equals(evidence.path("startDate").asText())
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

  /** Valida dinheiro positivo com precisão compatível com os campos DECIMAL(10,2). */
  private boolean validMoney(BigDecimal value) {
    return value != null
        && value.signum() > 0
        && value.scale() <= 2
        && value.compareTo(new BigDecimal("99999999.99")) <= 0;
  }

  /** Compara valores monetários sem transformar diferença de escala em mudança material. */
  private boolean sameMoney(BigDecimal first, BigDecimal second) {
    return first == null ? second == null : second != null && first.compareTo(second) == 0;
  }

  /** Reconhece repetição idêntica para impedir autorizações concorrentes duplicadas. */
  private boolean sameAuthorization(
      FacebookCampaignResumption latest,
      BigDecimal dailyBudget,
      BigDecimal totalLimit,
      LocalDate startDate,
      LocalDate endDate,
      BigDecimal zeroResultSpendLimit,
      BigDecimal zeroPurchaseSpendLimit,
      Integer purchaseStopCount,
      String reason) {
    return sameMoney(dailyBudget, latest.getDailyBudget())
        && sameMoney(totalLimit, latest.getTotalLimit())
        && Objects.equals(startDate, latest.getStartDate())
        && Objects.equals(endDate, latest.getEndDate())
        && sameMoney(zeroResultSpendLimit, latest.getZeroResultSpendLimit())
        && sameMoney(zeroPurchaseSpendLimit, latest.getZeroPurchaseSpendLimit())
        && Objects.equals(purchaseStopCount, latest.getPurchaseStopCount())
        && reason != null
        && reason.trim().equals(latest.getReason());
  }

  /** Aceita orçamento vitalício legado ou diário protegido pelo teto nativo da campanha. */
  private boolean budgetEvidenceMatches(
      FacebookCampaignResumption r, com.fasterxml.jackson.databind.JsonNode evidence) {
    long totalMinor = r.getTotalLimit().movePointRight(2).longValueExact();
    String mode = evidence.path("budgetMode").asText();
    if ("LIFETIME".equals(mode)) {
      return evidence.path("lifetimeBudgetMinor").asLong(-1) == totalMinor;
    }
    return "DAILY_WITH_CAMPAIGN_CAP".equals(mode)
        && evidence.path("campaignSpendCapMinor").asLong(-1) == totalMinor
        && r.getDailyBudget() != null
        && evidence.path("dailyBudgetMinor").asLong(-1)
            == r.getDailyBudget().movePointRight(2).longValueExact();
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
          r.getDailyBudget(),
          r.getStartDate(),
          r.getEndDate(),
          r.getZeroResultSpendLimit(),
          r.getZeroPurchaseSpendLimit(),
          r.getPurchaseStopCount(),
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
