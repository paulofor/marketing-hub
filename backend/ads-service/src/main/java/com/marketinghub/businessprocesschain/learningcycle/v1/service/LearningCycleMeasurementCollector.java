package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.monitoring.ExperimentAcquisitionMetricsReader;
import com.marketinghub.experiment.monitoring.ExperimentAcquisitionMetricsSnapshot;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.experiment.monitoring.pde.PdeExperimentAnalyticsReader;
import com.marketinghub.experiment.service.ExperimentCostReconciliationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: produzir a fotografia comercial automática de um ciclo a partir das fontes
 * oficiais atribuídas ao experimento.
 */
@Component
@Slf4j
public class LearningCycleMeasurementCollector {
  static final String CONTRACT = "LEARNING_CYCLE_AUTOMATIC_MEASUREMENT_V1";
  private final PdeExperimentAnalyticsReader analytics;
  private final ExperimentAcquisitionMetricsReader acquisition;
  private final ExperimentCostReconciliationService costs;
  private final ObjectMapper json;

  /** Configura as fontes canônicas de funil, desfechos comerciais e custos. */
  public LearningCycleMeasurementCollector(
      PdeExperimentAnalyticsReader analytics,
      ExperimentAcquisitionMetricsReader acquisition,
      ExperimentCostReconciliationService costs,
      ObjectMapper json) {
    this.analytics = analytics;
    this.acquisition = acquisition;
    this.costs = costs;
    this.json = json;
  }

  /**
   * Concilia o experimento sem aceitar números informados pela tela e devolve bloqueio auditável
   * quando uma fonte não permite conclusão segura.
   */
  public Result collect(LearningSalesCycle cycle, Experiment experiment, Instant now) {
    try {
      return collectVerified(cycle, experiment, now);
    } catch (MeasurementSourceException ex) {
      log.warn(
          "Ciclos: conciliação automática bloqueada cycleId={} experimentId={} motivo={}",
          cycle.getId(),
          experiment.getId(),
          ex.getMessage(),
          ex);
      return blocked(cycle, experiment, now, ex.getMessage());
    } catch (RuntimeException ex) {
      log.error(
          "Ciclos: falha ao conciliar fontes automáticas cycleId={} experimentId={}",
          cycle.getId(),
          experiment.getId(),
          ex);
      return blocked(
          cycle,
          experiment,
          now,
          "Uma fonte oficial da medição está indisponível. Corrija a integração e tente novamente; ausência de leitura não será convertida em zero.");
    }
  }

  /**
   * Monta a evidência somente depois de conferir identidade, atualidade e correlação financeira.
   */
  private Result collectVerified(LearningSalesCycle cycle, Experiment experiment, Instant now) {
    requireSource(
        experiment.getProduct() != null
            && Objects.equals(experiment.getProduct().getId(), cycle.getProductId()),
        "O experimento não pertence ao produto deste ciclo.");
    Instant periodEnd = now.isBefore(cycle.getWindowEnd()) ? now : cycle.getWindowEnd();
    requireSource(
        periodEnd.isAfter(cycle.getWindowStart()),
        "A janela de medição ainda não começou; nenhum zero será registrado antes dela.");

    PdeAnalyticsSummary funnel = analytics.read(experiment, cycle.getWindowStart(), periodEnd);
    requireSource(
        cycle.getProductVersion().equals(funnel.currentExperienceVersion()),
        "A versão da experiência encontrada nas métricas diverge da versão deste ciclo.");
    var outcomes =
        analytics.commercialOutcomes(experiment, funnel, cycle.getWindowStart(), periodEnd);
    long purchaseEvents = eventCount(funnel, "PURCHASE_COMPLETED");
    long refundEvents = eventCount(funnel, "REFUND_CONFIRMED");
    requireSource(
        purchaseEvents == outcomes.purchases() && refundEvents == outcomes.refunds(),
        "Os eventos financeiros possuem duplicidade ou referência inconsistente.");
    requireSource(
        funnel.subscriptionApproved() <= purchaseEvents,
        "Existe aprovação de assinatura sem o evento canônico de compra; concilie pagamento e atribuição antes de decidir.");
    requireSource(
        outcomes.financialReferencesComplete() && outcomes.financialAmountsComplete(),
        "Compra ou reembolso sem identificador, valor ou moeda BRL impede calcular receita com segurança.");
    requireSource(
        outcomes.refundReferencesMatchPurchases()
            && outcomes.refunds() <= outcomes.purchases()
            && outcomes.refundedRevenueBrl().compareTo(outcomes.grossRevenueBrl()) <= 0,
        "Existe reembolso sem compra correspondente ou com valor superior; corrija a correlação financeira.");
    requireSource(
        outcomes.valueReferencesComplete(),
        "Evento de acesso, entrega, uso ou satisfação sem correlação segura impede avaliar o valor entregue.");

    MediaEvidence media = media(experiment, now);
    ExperimentDto costDto = costs.enrich(experiment, new ExperimentDto());
    requireSource(
        costDto != null
            && costDto.getAuditableTotalCost() != null
            && costDto.getLegacyTotalCost() != null
            && costDto.getUnreconciledLegacyCost() != null,
        "O ledger de custos não devolveu uma conciliação completa para o experimento.");
    BigDecimal auditableCost = money(costDto.getAuditableTotalCost());
    requireSource(
        auditableCost.signum() >= 0
            && costDto.getLegacyTotalCost().signum() >= 0
            && costDto.getUnreconciledLegacyCost().signum() >= 0,
        "O ledger de custos devolveu valor negativo e precisa ser corrigido.");
    BigDecimal revenue = money(outcomes.netRevenueBrl());
    BigDecimal contribution = revenue.subtract(auditableCost).setScale(2, RoundingMode.HALF_UP);
    long netSales = Math.max(0, outcomes.purchases() - outcomes.refunds());
    long starts = eventCount(funnel, "TASTING_STARTED");
    long firstResults = eventCount(funnel, "VALUE_MOMENT");
    long deliveries = outcomes.deliveredNetSales();
    long firstUses = outcomes.firstUseNetSales();
    boolean deliveryVerified = netSales > 0 && deliveries == netSales;
    boolean useVerified = netSales > 0 && firstUses == netSales;
    boolean satisfactionVerified =
        netSales > 0 && outcomes.positiveSatisfactionResponses() == netSales;
    String fingerprint =
        fingerprint(
            experiment.getId(),
            funnel.currentExperienceVersion(),
            funnel.totalEvents(),
            funnel.sessions(),
            funnel.lastEventAt(),
            outcomes.purchases(),
            outcomes.refunds(),
            revenue,
            media.spendBrl(),
            media.observedAt(),
            auditableCost,
            outcomes.accessReleasedNetSales(),
            deliveries,
            firstUses,
            outcomes.positiveSatisfactionResponses(),
            !now.isBefore(cycle.getWindowEnd()));

    ObjectNode evidence = json.createObjectNode();
    evidence.put("contractVersion", CONTRACT);
    evidence.put("automatic", true);
    evidence.put("sourceFingerprint", fingerprint);
    evidence.put("experimentId", experiment.getId());
    evidence.put("currency", "BRL");
    evidence.put(
        "source",
        "PDE Analytics atribuído ao experimento + métricas oficiais do canal + ledger de custos auditáveis");
    evidence.put("periodStart", cycle.getWindowStart().toString());
    evidence.put("periodEnd", periodEnd.toString());
    evidence.put("observedAt", now.toString());
    evidence.put("sessions", funnel.sessions());
    evidence.put("starts", starts);
    evidence.put("firstResults", firstResults);
    evidence.put("checkouts", funnel.checkoutStarted());
    evidence.put("netSales", netSales);
    evidence.put("refunds", outcomes.refunds());
    evidence.put("spendBrl", media.spendBrl());
    evidence.put("revenueBrl", revenue);
    evidence.put("contributionBrl", contribution);
    evidence.put("dataValid", true);
    evidence.put("testDataExcluded", true);
    evidence.put("deliveryVerified", deliveryVerified);
    evidence.put("useVerified", useVerified);
    evidence.put("satisfactionVerified", satisfactionVerified);

    ObjectNode sources = evidence.putObject("sources");
    ObjectNode pde = sources.putObject("pdeAnalytics");
    pde.put("productSlug", funnel.productSlug());
    pde.put("experienceVersion", funnel.currentExperienceVersion());
    pde.put("attribution", "EXPERIMENT_ID_OR_LINKED_CAMPAIGN_IDENTIFIERS");
    pde.put("trafficQualityIncluded", "HUMAN");
    pde.put("humanEvents", funnel.totalEvents());
    pde.put("rawEvents", funnel.rawTotalEvents());
    pde.put("humanSessions", funnel.humanSessions());
    pde.put("rawSessions", funnel.rawSessions());
    putNullable(pde, "lastEventAt", funnel.lastEventAt());
    ObjectNode channel = sources.putObject("acquisition");
    channel.put("platform", String.valueOf(experiment.getPlatform()));
    channel.put("mode", media.mode());
    channel.put("spendBrl", media.spendBrl());
    putNullable(channel, "observedAt", media.observedAt());
    channel.put("finalSnapshot", media.finalSnapshot());
    channel.put("impressions", media.impressions());
    channel.put("clicks", media.clicks());
    ObjectNode financial = sources.putObject("financialOutcomes");
    financial.put("purchaseEvents", outcomes.purchases());
    financial.put("refundEvents", outcomes.refunds());
    financial.put("grossRevenueBrl", outcomes.grossRevenueBrl());
    financial.put("refundedRevenueBrl", outcomes.refundedRevenueBrl());
    financial.put("referencesComplete", outcomes.financialReferencesComplete());
    financial.put("amountsComplete", outcomes.financialAmountsComplete());
    financial.put("refundsMatchPurchases", outcomes.refundReferencesMatchPurchases());
    ObjectNode cost = sources.putObject("costLedger");
    cost.put("auditableTotalBrl", auditableCost);
    cost.put("legacyTotalBrl", money(costDto.getLegacyTotalCost()));
    cost.put("unreconciledLegacyBrl", money(costDto.getUnreconciledLegacyCost()));
    ObjectNode value = sources.putObject("valueDelivery");
    value.put("accessReleasedNetSales", outcomes.accessReleasedNetSales());
    value.put("deliveries", deliveries);
    value.put("firstUses", firstUses);
    value.put("referencesComplete", outcomes.valueReferencesComplete());
    value.put("satisfactionResponses", outcomes.satisfactionResponses());
    value.put("positiveSatisfactionResponses", outcomes.positiveSatisfactionResponses());

    String summary =
        "Conciliação automática: "
            + funnel.sessions()
            + " sessões, "
            + firstResults
            + " primeiros resultados, "
            + netSales
            + " vendas líquidas, R$ "
            + revenue
            + " de receita e R$ "
            + contribution
            + " de contribuição.";
    return new Result(
        true,
        evidence,
        summary,
        "internal://learning-cycles/"
            + cycle.getId()
            + "/experiments/"
            + experiment.getId()
            + "/measurements/"
            + fingerprint);
  }

  /** Confere a fonte de aquisição sem tratar sincronização ausente como gasto zero. */
  private MediaEvidence media(Experiment experiment, Instant now) {
    if (experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE)
      return new MediaEvidence(BigDecimal.ZERO.setScale(2), "DIRECT_ONE_TO_ONE", null, true, 0, 0);
    requireSource(
        experiment.getPlatform() == ExperimentPlatform.FACEBOOK,
        "O canal deste experimento ainda não possui uma fonte automática de gasto.");
    ExperimentAcquisitionMetricsSnapshot metric = acquisition.read(experiment);
    requireSource(
        metric.campaignLinked(),
        "A campanha ainda não possui métricas persistidas para conciliação.");
    Instant observedAt =
        experiment.getStatus() == ExperimentStatus.RUNNING
            ? metric.lastSyncedAt()
            : metric.finalSyncedAt();
    requireSource(observedAt != null, "As métricas da campanha ainda não foram sincronizadas.");
    requireSource(
        metric.spendBrl() != null
            && metric.impressions() != null
            && metric.clicks() != null
            && metric.spendBrl().signum() >= 0
            && metric.impressions() >= 0
            && metric.clicks() >= 0,
        "O snapshot da campanha está incompleto ou possui métricas negativas.");
    requireSource(
        !observedAt.isAfter(now.plusSeconds(60)),
        "O horário das métricas da campanha está no futuro e precisa ser corrigido.");
    boolean finalSnapshot = metric.finalSyncedAt() != null;
    requireSource(
        experiment.getStatus() == ExperimentStatus.RUNNING
            ? !observedAt.isBefore(now.minusSeconds(86400))
            : finalSnapshot,
        experiment.getStatus() == ExperimentStatus.RUNNING
            ? "As métricas da campanha estão desatualizadas há mais de 24 horas."
            : "O experimento encerrado ainda não possui sincronização final da campanha.");
    requireSource(
        metric.lastError() == null || metric.lastError().isBlank(),
        "A última sincronização da campanha terminou com erro.");
    return new MediaEvidence(
        money(metric.spendBrl()),
        "META_INSIGHTS",
        observedAt,
        finalSnapshot,
        value(metric.impressions()),
        value(metric.clicks()));
  }

  /** Conta somente o tipo canônico informado dentro do resumo já atribuído. */
  private long eventCount(PdeAnalyticsSummary summary, String type) {
    return summary.events().stream()
        .filter(event -> type.equals(event.eventType()))
        .mapToLong(PdeAnalyticsSummary.PdeEventMetric::total)
        .sum();
  }

  /** Exige uma condição de fonte sem converter inconsistência em métrica comercial. */
  private void requireSource(boolean condition, String message) {
    if (!condition) throw new MeasurementSourceException(message);
  }

  /** Normaliza valores monetários para o contrato BRL do ciclo. */
  private BigDecimal money(BigDecimal value) {
    return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
  }

  /** Normaliza contagens opcionais persistidas pelo provedor. */
  private long value(Long value) {
    return value == null ? 0 : value;
  }

  /** Preserva ausência explícita sem serializar a palavra textual null. */
  private void putNullable(ObjectNode target, String field, Object value) {
    if (value == null) target.putNull(field);
    else target.put(field, String.valueOf(value));
  }

  /** Gera uma assinatura estável das fontes para impedir nova decisão sem novos dados. */
  private String fingerprint(Object... values) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(java.util.Arrays.toString(values).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      log.error("Ciclos: SHA-256 indisponível para assinatura da medição", ex);
      throw new IllegalStateException("Não foi possível assinar as fontes da medição.", ex);
    }
  }

  /** Produz um evento de bloqueio legível e sem números presumidos. */
  private Result blocked(
      LearningSalesCycle cycle, Experiment experiment, Instant now, String blocker) {
    ObjectNode evidence = json.createObjectNode();
    evidence.put("contractVersion", CONTRACT);
    evidence.put("automatic", true);
    evidence.put("dataValid", false);
    evidence.put("experimentId", experiment.getId());
    evidence.put("observedAt", now.toString());
    evidence.put("blocker", blocker);
    return new Result(
        false,
        evidence,
        "Conciliação automática bloqueada: " + blocker,
        "internal://learning-cycles/"
            + cycle.getId()
            + "/experiments/"
            + experiment.getId()
            + "/measurement-blocker");
  }

  /** Entrega ao orquestrador o snapshot ou o bloqueio que deve ser persistido. */
  public record Result(
      boolean ready, JsonNode evidence, String summary, String evidenceReference) {}

  /** Representa uma inconsistência funcional esperada em uma das fontes oficiais. */
  private static final class MeasurementSourceException extends RuntimeException {
    /** Mantém a causa funcional pronta para auditoria e orientação na tela. */
    private MeasurementSourceException(String message) {
      super(message);
    }
  }

  /** Conserva o snapshot do canal sem misturá-lo com o funil ou o ledger de custos. */
  private record MediaEvidence(
      BigDecimal spendBrl,
      String mode,
      Instant observedAt,
      boolean finalSnapshot,
      long impressions,
      long clicks) {}
}
