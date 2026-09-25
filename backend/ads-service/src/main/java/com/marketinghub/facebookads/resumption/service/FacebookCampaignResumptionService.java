package com.marketinghub.facebookads.resumption.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.funnel.ExperimentFinancialGuardrailPolicy;
import com.marketinghub.experiment.service.ExperimentCampaignMetricService;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.facebookads.*;
import com.marketinghub.facebookads.resumption.FacebookCampaignResumption;
import com.marketinghub.facebookads.resumption.service.request.ResumeCampaignRequest;
import com.marketinghub.facebookads.resumption.service.result.CampaignReplacementResult;
import com.marketinghub.facebookads.resumption.service.result.ResumeCampaignResult;
import com.marketinghub.facebookads.resumption.service.summary.ResumeCampaignSummary;
import com.marketinghub.facebookads.resumption.service.view.ResumeCampaignView;
import com.marketinghub.facebookads.service.CampaignStrategyService;
import com.marketinghub.repository.jpa.experiment.*;
import com.marketinghub.repository.jpa.facebookads.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
  private final FacebookAdsAdSetRepository adSets;
  private final FacebookAdsAdRepository ads;
  private final FacebookAdsAdTrackingUtmRepository trackingUtms;
  private final ExperimentCampaignMetricRepository metrics;
  private final ExperimentCampaignMetricService campaignMetrics;
  private final CampaignStrategyService campaignStrategies;
  private final ExperimentStatusChangeRepository history;
  private final ExperimentReadinessService readiness;
  private final ObjectMapper json;

  /** Recebe as fontes persistidas da autorização, prontidão e histórico comercial. */
  public FacebookCampaignResumptionService(
      FacebookCampaignResumptionRepository requests,
      ExperimentRepository experiments,
      FacebookAdsCampaignRepository campaigns,
      FacebookAdsAdSetRepository adSets,
      FacebookAdsAdRepository ads,
      FacebookAdsAdTrackingUtmRepository trackingUtms,
      ExperimentCampaignMetricRepository metrics,
      ExperimentCampaignMetricService campaignMetrics,
      CampaignStrategyService campaignStrategies,
      ExperimentStatusChangeRepository history,
      ExperimentReadinessService readiness,
      ObjectMapper json) {
    this.requests = requests;
    this.experiments = experiments;
    this.campaigns = campaigns;
    this.adSets = adSets;
    this.ads = ads;
    this.trackingUtms = trackingUtms;
    this.metrics = metrics;
    this.campaignMetrics = campaignMetrics;
    this.campaignStrategies = campaignStrategies;
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
    FacebookAdsCampaign campaign = currentCampaign(linked).orElseThrow();
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

  /** Expõe o estado persistido para o executor confirmar se um callback foi efetivado. */
  @Transactional(readOnly = true)
  public ResumeCampaignView get(Long id) {
    return view(requests.findById(id).orElseThrow(), false);
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
    FacebookAdsCampaign sourceCampaign = campaigns.findById(r.getCampaignId()).orElseThrow();
    if (input.success()) {
      var evidence = input.evidence();
      CampaignReplacementResult replacement = input.replacement();
      String effectiveCampaignId =
          replacement == null ? r.getCampaignId() : replacement.campaignId();
      String effectiveAdSetId = replacement == null ? r.getAdSetId() : replacement.adSetId();
      if (evidence == null
          || !"ACTIVE".equals(evidence.path("campaignStatus").asText())
          || !effectiveAdSetId.equals(evidence.path("adSetId").asText())
          || !effectiveCampaignId.equals(evidence.path("campaignId").asText())
          || !evidence.path("spend").isNumber()
          || !budgetEvidenceMatches(r, sourceCampaign, replacement, evidence)
          || !r.getStartDate().toString().equals(evidence.path("startDate").asText())
          || !r.getEndDate().toString().equals(evidence.path("endDate").asText())
          || evidence.path("spend").decimalValue().compareTo(r.getTotalLimit()) >= 0
          || e.getMediaSpendLimit().compareTo(r.getTotalLimit()) != 0)
        throw conflict("A Meta não confirmou teto, prazo e estado autorizados.");
      FacebookAdsCampaign campaign =
          replacement == null
              ? sourceCampaign
              : materializeReplacement(r, sourceCampaign, replacement, evidence);
      ExperimentStatus previous = e.getStatus();
      e.setStatus(ExperimentStatus.RUNNING);
      sourceCampaign.setStatus(
          replacement == null ? FacebookAdStatus.ACTIVE : FacebookAdStatus.PAUSED);
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
    if (e.getPlatform() != ExperimentPlatform.FACEBOOK || linked.isEmpty())
      return "Retomada exige uma campanha Facebook publicada.";
    if (latest != null && Set.of("PENDING", "RUNNING").contains(latest.getStatus()))
      return "Retomada aguardando confirmação da Meta.";
    if (!Set.of(
            ExperimentStatus.USER_STOPPED,
            ExperimentStatus.PAUSED,
            ExperimentStatus.STANDBY,
            ExperimentStatus.INCONCLUSIVE)
        .contains(e.getStatus()))
      return "Experimento precisa estar pausado para autorizar uma retomada.";
    Optional<FacebookAdsCampaign> current = currentCampaign(linked);
    if (current.isEmpty())
      return "Retomada exige exatamente uma campanha Meta vigente; campanhas substituídas permanecem apenas no histórico.";
    FacebookAdsCampaign c = current.get();
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

  /** Aceita somente orçamento e teto nativos confirmados na camada compatível da Meta. */
  private boolean budgetEvidenceMatches(
      FacebookCampaignResumption r,
      FacebookAdsCampaign sourceCampaign,
      CampaignReplacementResult replacement,
      com.fasterxml.jackson.databind.JsonNode evidence) {
    long totalMinor = r.getTotalLimit().movePointRight(2).longValueExact();
    long historicalMinor =
        sourceCampaign.getPriorSpendMinor() == null ? 0L : sourceCampaign.getPriorSpendMinor();
    String mode = evidence.path("budgetMode").asText();
    if ("REPLACEMENT_ADSET_LIFETIME_BELOW_MINIMUM".equals(mode)) {
      return replacementBudgetEvidenceMatches(r, replacement, evidence, totalMinor);
    }
    if (replacement != null) return false;
    if ("LIFETIME".equals(mode)) {
      return evidence.path("lifetimeBudgetMinor").asLong(-1) == totalMinor - historicalMinor;
    }
    if (r.getDailyBudget() == null
        || evidence.path("dailyBudgetMinor").asLong(-1)
            != r.getDailyBudget().movePointRight(2).longValueExact()) return false;
    long dailyMinor = r.getDailyBudget().movePointRight(2).longValueExact();
    if ("DAILY_WITH_CAMPAIGN_CAP".equals(mode))
      return evidence.path("campaignSpendCapMinor").asLong(-1) == totalMinor - historicalMinor;
    if (!"CAMPAIGN_LIFETIME_BELOW_MINIMUM".equals(mode)) return false;
    long nativeLifetimeMinor = evidence.path("campaignLifetimeBudgetMinor").asLong(-1L);
    long sourceSpentMinor =
        evidence
            .path("sourceCampaignSpend")
            .decimalValue()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();
    long remainingDays = evidence.path("remainingDays").asLong(0L);
    if (remainingDays <= 0L || nativeLifetimeMinor <= sourceSpentMinor) return false;
    long calculatedAverage =
        (nativeLifetimeMinor - sourceSpentMinor + remainingDays - 1L) / remainingDays;
    return nativeLifetimeMinor <= totalMinor - historicalMinor
        && evidence.path("historicalSpendMinor").asLong(-1L) == historicalMinor
        && evidence.path("authorizedDailyBudgetMinor").asLong(-1L) == dailyMinor
        && evidence.path("campaignDailyBudgetMinor").asLong(-1L) == 0L
        && evidence.path("adSetDailyBudgetMinor").asLong(-1L) == 0L
        && evidence.path("lifetimeBudgetMinor").asLong(-1L) == 0L
        && evidence.path("adSetLifetimeSpendCapMinor").asLong(-1L) == 0L
        && evidence.path("campaignSpendCapMinor").asLong(-1L) == 0L
        && evidence.path("effectiveRemainingAverageMinor").asLong(-1L) == calculatedAverage
        && calculatedAverage <= dailyMinor
        && evidence.path("accountMinimumCampaignSpendCapMinor").asLong(-1L) > totalMinor;
  }

  /** Valida que a substituta limita somente o saldo restante e preserva a origem pausada. */
  private boolean replacementBudgetEvidenceMatches(
      FacebookCampaignResumption r,
      CampaignReplacementResult replacement,
      com.fasterxml.jackson.databind.JsonNode evidence,
      long totalMinor) {
    if (replacement == null
        || !r.getCampaignId().equals(replacement.sourceCampaignId())
        || !r.getAdSetId().equals(replacement.sourceAdSetId())
        || !replacement.campaignId().equals(evidence.path("campaignId").asText())
        || !replacement.adSetId().equals(evidence.path("adSetId").asText())
        || replacement.campaignId().equals(replacement.sourceCampaignId())
        || replacement.adSetId().equals(replacement.sourceAdSetId())
        || !externalId(replacement.campaignId())
        || !externalId(replacement.adSetId())
        || replacement.lifetimeBudgetMinor() == null
        || replacement.lifetimeBudgetMinor() <= 0L
        || replacement.confirmedPriorSpend() == null
        || replacement.confirmedPriorSpend().signum() < 0
        || replacement.ads() == null
        || replacement.ads().isEmpty()) return false;
    long priorMinor;
    try {
      priorMinor = replacement.confirmedPriorSpend().movePointRight(2).longValueExact();
    } catch (ArithmeticException ex) {
      LOG.warn(
          "Gasto anterior da substituição não possui precisão monetária: requestId={} campaignId={}",
          r.getId(),
          replacement.campaignId(),
          ex);
      return false;
    }
    long remainingDays = evidence.path("remainingDays").asLong(0L);
    long lifetime = replacement.lifetimeBudgetMinor();
    long dailyMinor = r.getDailyBudget().movePointRight(2).longValueExact();
    long historicalMinor = evidence.path("historicalSpendMinor").asLong(-1L);
    long expectedAverage =
        remainingDays <= 0L ? Long.MAX_VALUE : (lifetime + remainingDays - 1L) / remainingDays;
    return priorMinor < totalMinor
        && historicalMinor >= 0L
        && historicalMinor <= priorMinor
        && priorMinor + lifetime <= totalMinor
        && evidence.path("confirmedPriorSpendMinor").asLong(-1L) == priorMinor
        && evidence.path("lifetimeBudgetMinor").asLong(-1L) == lifetime
        && evidence.path("campaignDailyBudgetMinor").asLong(-1L) == 0L
        && evidence.path("campaignLifetimeBudgetMinor").asLong(-1L) == 0L
        && evidence.path("campaignSpendCapMinor").asLong(-1L) == 0L
        && evidence.path("adSetDailyBudgetMinor").asLong(-1L) == 0L
        && evidence.path("adSetLifetimeSpendCapMinor").asLong(-1L) == 0L
        && evidence.path("effectiveRemainingAverageMinor").asLong(-1L) == expectedAverage
        && expectedAverage <= dailyMinor
        && evidence.path("accountMinimumCampaignSpendCapMinor").asLong(-1L)
            > totalMinor - historicalMinor
        && evidence.path("spend").decimalValue().compareTo(replacement.confirmedPriorSpend()) == 0;
  }

  /** Materializa no backend a hierarquia substituta já confirmada e mantém a anterior auditável. */
  private FacebookAdsCampaign materializeReplacement(
      FacebookCampaignResumption request,
      FacebookAdsCampaign source,
      CampaignReplacementResult replacement,
      com.fasterxml.jackson.databind.JsonNode evidence) {
    if (campaigns.existsById(replacement.campaignId())
        || adSets.existsById(replacement.adSetId())) {
      throw conflict("A hierarquia substituta já pertence a outro callback.");
    }
    List<FacebookAdsAdSet> sourceSets = adSets.findDetailedByCampaignIds(List.of(source.getId()));
    if (sourceSets.size() != 1 || !sourceSets.get(0).getId().equals(request.getAdSetId())) {
      throw conflict("A campanha de origem não possui o conjunto único autorizado.");
    }
    FacebookAdsAdSet sourceSet = sourceSets.get(0);
    Map<String, FacebookAdsAd> sourceAds = new LinkedHashMap<>();
    sourceSet.getAds().forEach(ad -> sourceAds.put(ad.getId(), ad));
    Map<String, String> replacements = new LinkedHashMap<>();
    for (CampaignReplacementResult.ReplacementAdResult item : replacement.ads()) {
      if (item == null
          || !sourceAds.containsKey(item.sourceAdId())
          || !externalId(item.adId())
          || item.adId().equals(item.sourceAdId())
          || replacements.put(item.sourceAdId(), item.adId()) != null) {
        throw conflict("Mapeamento dos anúncios substitutos é inválido.");
      }
    }
    if (!replacements.keySet().equals(sourceAds.keySet())
        || replacements.values().stream().distinct().count() != replacements.size()) {
      throw conflict("Todos os anúncios da origem precisam de um único substituto.");
    }

    FacebookAdsCampaign target = new FacebookAdsCampaign();
    target.setId(replacement.campaignId());
    target.setExternalId(replacement.campaignId());
    target.setPublicationKey(
        "FACEBOOK:EXPERIMENT:" + request.getExperimentId() + ":RESUMPTION:" + request.getId());
    target.setAdAccountId(source.getAdAccountId());
    target.setExperiment(source.getExperiment());
    target.setFacebookAccount(source.getFacebookAccount());
    target.setName(replacementName(source.getName(), request.getId()));
    target.setObjective(source.getObjective());
    target.setStatus(FacebookAdStatus.PAUSED);
    target.setBudgetMode(BudgetMode.ADSET);
    target.setApiVersion(source.getApiVersion());
    target.setReplacesCampaignId(source.getId());
    target.setSpecialAdCategories(new HashSet<>(source.getSpecialAdCategories()));
    target.setSpecialAdCountries(new HashSet<>(source.getSpecialAdCountries()));
    target = campaigns.save(target);

    FacebookAdsAdSet targetSet = new FacebookAdsAdSet();
    targetSet.setId(replacement.adSetId());
    targetSet.setExternalId(replacement.adSetId());
    targetSet.setExperimentAdSet(sourceSet.getExperimentAdSet());
    targetSet.setCampaign(target);
    targetSet.setName(replacementName(sourceSet.getName(), request.getId()));
    targetSet.setStatus(FacebookAdStatus.PAUSED);
    targetSet.setLifetimeBudgetMinor(replacement.lifetimeBudgetMinor());
    targetSet.setStartTime(request.getStartDate().atStartOfDay());
    targetSet.setEndTime(request.getEndDate().atTime(23, 59, 59));
    targetSet.setBillingEvent(sourceSet.getBillingEvent());
    targetSet.setOptimizationGoal(sourceSet.getOptimizationGoal());
    targetSet.setBidStrategy(sourceSet.getBidStrategy());
    targetSet.setBidAmountMinor(sourceSet.getBidAmountMinor());
    targetSet.setPromotedObjectJson(sourceSet.getPromotedObjectJson());
    targetSet.setTargetingJson(sourceSet.getTargetingJson());
    targetSet = adSets.save(targetSet);

    for (Map.Entry<String, FacebookAdsAd> entry : sourceAds.entrySet()) {
      FacebookAdsAd sourceAd = entry.getValue();
      FacebookAdsAd targetAd = new FacebookAdsAd();
      targetAd.setId(replacements.get(entry.getKey()));
      targetAd.setExternalId(targetAd.getId());
      targetAd.setAdSet(targetSet);
      targetAd.setName(replacementAdName(sourceAd.getName(), request.getId(), sourceAd.getId()));
      targetAd.setCreative(sourceAd.getCreative());
      targetAd.setStatus(FacebookAdStatus.ACTIVE);
      targetAd = ads.save(targetAd);
      cloneTracking(sourceAd, targetAd);
    }

    source.setSupersededByCampaignId(target.getId());
    source.setStatus(FacebookAdStatus.PAUSED);
    source.setMetricsFinalSyncedAt(Instant.now());
    campaignMetrics.activateReplacement(target, replacement.confirmedPriorSpend());
    campaignStrategies.ensureDefaultStrategy(target);
    targetSet.setStatus(FacebookAdStatus.ACTIVE);
    target.getAdSets().add(targetSet);
    LOG.info(
        "Campanha Meta substituída com histórico preservado: requestId={} experimentId={} sourceCampaignId={} replacementCampaignId={} lifetimeBudgetMinor={} priorSpend={}",
        request.getId(),
        request.getExperimentId(),
        source.getId(),
        target.getId(),
        replacement.lifetimeBudgetMinor(),
        replacement.confirmedPriorSpend());
    return target;
  }

  /** Copia a atribuição UTM persistida quando o anúncio de origem possui esse contrato. */
  private void cloneTracking(FacebookAdsAd source, FacebookAdsAd target) {
    FacebookAdsAdTrackingUtm current = source.getTrackingUtm();
    if (current == null) return;
    FacebookAdsAdTrackingUtm copy = new FacebookAdsAdTrackingUtm();
    copy.setAdId(target.getId());
    copy.setAd(target);
    copy.setUtmSource(current.getUtmSource());
    copy.setUtmMedium(current.getUtmMedium());
    copy.setUtmCampaign(current.getUtmCampaign());
    copy.setUtmContent(current.getUtmContent());
    copy.setUtmTerm(current.getUtmTerm());
    trackingUtms.save(copy);
  }

  /** Mantém o nome operacional determinístico dentro do limite aceito pela Meta e pelo banco. */
  private String replacementName(String sourceName, Long requestId) {
    String suffix = " · retomada " + requestId;
    String base = sourceName == null ? "Campanha" : sourceName;
    return base.substring(0, Math.min(base.length(), 255 - suffix.length())) + suffix;
  }

  /** Reproduz no backend o nome determinístico usado pelo worker para cada anúncio substituto. */
  private String replacementAdName(String sourceName, Long requestId, String sourceAdId) {
    String suffix =
        " · retomada "
            + requestId
            + " · origem "
            + sourceAdId.substring(Math.max(0, sourceAdId.length() - 8));
    String base = sourceName == null ? "Anúncio" : sourceName;
    return base.substring(0, Math.min(base.length(), 255 - suffix.length())) + suffix;
  }

  /** Aceita somente identificadores numéricos externos que cabem nas chaves persistidas. */
  private boolean externalId(String value) {
    return value != null && value.length() <= 36 && value.matches("[0-9]+");
  }

  /**
   * Localiza a única campanha vigente e ignora somente antecessoras explicitamente substituídas.
   */
  private Optional<FacebookAdsCampaign> currentCampaign(List<FacebookAdsCampaign> linked) {
    if (linked == null) return Optional.empty();
    List<FacebookAdsCampaign> current =
        linked.stream().filter(c -> c.getSupersededByCampaignId() == null).toList();
    return current.size() == 1 ? Optional.of(current.get(0)) : Optional.empty();
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
          campaigns
              .findById(r.getCampaignId())
              .map(FacebookAdsCampaign::getPriorSpendMinor)
              .map(value -> BigDecimal.valueOf(value, 2))
              .orElse(BigDecimal.ZERO),
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
