package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.funnel.ExperimentFinancialGuardrailPolicy;
import com.marketinghub.experiment.service.cockpit.ExperimentCockpitSampleDecisionDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/** Calcula a leitura progressiva da amostra sem alterar campanha, orçamento ou experimento. */
@Service
public class ExperimentSampleDecisionService {

  private static final double CONFIDENCE_Z_95 = 1.96d;
  private static final double ONE_SIDED_ALPHA_95 = 0.05d;
  private static final double MAX_PRECISION_HALF_WIDTH = 0.02d;
  private static final int PRECISION_SAMPLE_BLOCK = 50;
  private static final long MINIMUM_VISITORS_FOR_STABLE_COST_PROJECTION = 20L;
  private final ExperimentBinomialConfidenceService binomialConfidenceService;

  /** Inicializa a decisão progressiva com o cálculo binomial exato de confiança. */
  public ExperimentSampleDecisionService(
      ExperimentBinomialConfidenceService binomialConfidenceService) {
    this.binomialConfidenceService = binomialConfidenceService;
  }

  /** Representa visitantes e vendas líquidas da mesma coorte humana atribuída. */
  public record SampleMeasurement(
      boolean available, String source, long humanVisitors, Long attributedPurchases) {

    /** Cria uma leitura disponível quando a fonte ainda não entrega compras na mesma coorte. */
    public static SampleMeasurement available(String source, long humanVisitors) {
      return new SampleMeasurement(true, source, Math.max(0L, humanVisitors), null);
    }

    /** Cria uma leitura disponível com visitantes e compras da mesma coorte atribuída. */
    public static SampleMeasurement available(
        String source, long humanVisitors, long attributedPurchases) {
      return new SampleMeasurement(
          true, source, Math.max(0L, humanVisitors), Math.max(0L, attributedPurchases));
    }

    /** Cria uma leitura indisponível que não pode sustentar conclusão comercial. */
    public static SampleMeasurement unavailable(String source) {
      return new SampleMeasurement(false, source, 0L, null);
    }
  }

  /** Informa se o experimento usa a estratégia de visitantes pagos para decisão de vendas. */
  public boolean isApplicable(Experiment experiment) {
    return experiment != null
        && experiment.getCampaignObjective() == ExperimentCampaignObjective.SALES
        && experiment.getExperimentType() == ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL
        && experiment.getPlatform() == ExperimentPlatform.FACEBOOK;
  }

  /** Consolida meta inicial, precisão, intervalo e compatibilidade financeira da amostra. */
  public ExperimentCockpitSampleDecisionDto evaluate(
      Experiment experiment,
      SampleMeasurement measurement,
      long purchases,
      BigDecimal currentSpend) {
    if (!isApplicable(experiment)) {
      return notApplicable(
          measurement, purchases, experiment != null ? experiment.getMediaSpendLimit() : null);
    }
    if (!measurement.available()) {
      return unavailableMeasurement(measurement, purchases, experiment.getMediaSpendLimit());
    }

    long humanVisitors = measurement.humanVisitors();
    long safePurchases =
        measurement.attributedPurchases() != null
            ? measurement.attributedPurchases()
            : Math.max(0L, purchases);
    Integer configuredSampleSize = experiment.getSampleSize();
    BigDecimal configuredTargetCvr = experiment.getTargetCvr();
    if (configuredSampleSize == null
        || configuredSampleSize <= 0
        || configuredTargetCvr == null
        || configuredTargetCvr.compareTo(BigDecimal.ZERO) <= 0
        || configuredTargetCvr.compareTo(BigDecimal.valueOf(100)) > 0) {
      return configurationRequired(
          measurement, safePurchases, experiment.getMediaSpendLimit(), configuredSampleSize);
    }

    int initialTarget = configuredSampleSize;
    int targetPurchases = targetPurchases(initialTarget, configuredTargetCvr);
    int precisionTarget = precisionTarget(initialTarget, configuredTargetCvr);
    Projection projection =
        projection(
            currentSpend,
            humanVisitors,
            initialTarget,
            precisionTarget,
            experiment.getMediaSpendLimit());

    if (safePurchases > humanVisitors) {
      return decision(
          measurement,
          "MEASUREMENT_INVALID",
          "Compras e visitantes humanos não fecham",
          "A quantidade de compras atribuídas é maior que a quantidade de visitantes humanos distintos da mesma amostra.",
          initialTarget,
          targetPurchases,
          precisionTarget,
          safePurchases,
          StatisticalReading.empty(),
          projection,
          experiment.getMediaSpendLimit(),
          "Reconciliar atribuição de visitantes e compras antes de interpretar conversão ou escala.");
    }

    StatisticalReading statistics = statistics(humanVisitors, safePurchases);
    SampleStatus status =
        resolveStatus(
            humanVisitors,
            safePurchases,
            initialTarget,
            targetPurchases,
            precisionTarget,
            configuredTargetCvr,
            statistics);
    return decision(
        measurement,
        status.code(),
        status.headline(),
        status.explanation(),
        initialTarget,
        targetPurchases,
        precisionTarget,
        safePurchases,
        statistics,
        projection,
        experiment.getMediaSpendLimit(),
        status.recommendation());
  }

  /** Classifica a etapa da amostra sem confundir sinal inicial com precisão para escala. */
  private SampleStatus resolveStatus(
      long visitors,
      long purchases,
      int initialTarget,
      int targetPurchases,
      int precisionTarget,
      BigDecimal targetCvr,
      StatisticalReading statistics) {
    if (visitors < initialTarget) {
      String headline =
          purchases > 0
              ? "Há sinal de venda, mas a amostra ainda é insuficiente"
              : "Amostra comercial ainda insuficiente";
      return new SampleStatus(
          "INSUFFICIENT_DATA",
          headline,
          "%d de %d visitantes humanos distintos atribuídos; ainda faltam %d para a primeira decisão."
              .formatted(visitors, initialTarget, initialTarget - visitors),
          "Preservar versão, preço, público e oferta até a primeira decisão, respeitando as travas financeiras e corrigindo apenas falhas técnicas comprovadas.");
    }

    BigDecimal observedRate = statistics.observedRatePercent();
    if (visitors >= precisionTarget) {
      if (observedRate != null && observedRate.compareTo(targetCvr) >= 0) {
        return new SampleStatus(
            "PRECISION_TARGET_REACHED",
            "Meta atingida na rodada de precisão",
            "A conversão observada atingiu a meta depois do volume planejado para uma estimativa mais estável.",
            "Revisar contribuição líquida, entrega, primeiro uso e satisfação antes de solicitar autorização para escalar.");
      }
      return new SampleStatus(
          "PRECISION_TARGET_NOT_REACHED",
          "Meta não atingida na rodada de precisão",
          "A amostra de precisão foi concluída, mas a conversão observada ficou abaixo da meta configurada.",
          "Não escalar; revisar a causa do funil e criar sucessor com uma única variável comercial alterada.");
    }

    if (purchases >= targetPurchases) {
      return new SampleStatus(
          "INITIAL_TARGET_REACHED",
          "Sinal comercial inicial atingido",
          "%d compras líquidas em %d visitantes atingem a meta inicial, mas ainda não entregam precisão suficiente para escala."
              .formatted(purchases, visitors),
          "Confirmar margem, entrega, primeiro uso e satisfação; somente então planejar a rodada de precisão com nova autorização financeira.");
    }

    if (purchases == 0
        && statistics.zeroPurchaseUpper95Percent() != null
        && statistics.zeroPurchaseUpper95Percent().compareTo(targetCvr) < 0) {
      return new SampleStatus(
          "INITIAL_ZERO_SALES_REJECTED",
          "A versão ficou abaixo da meta inicial sem vendas",
          "Com zero compras, o limite superior unilateral de 95% ficou abaixo da conversão-alvo.",
          "Não ampliar gasto nesta versão; investigar o gargalo e criar sucessor alterando uma variável por vez.");
    }

    return new SampleStatus(
        "INITIAL_TARGET_NOT_REACHED",
        "Meta inicial ainda não atingida",
        "%d compras líquidas em %d visitantes ficaram abaixo da meta inicial de %d; o intervalo permanece explícito para evitar falsa certeza."
            .formatted(purchases, visitors, targetPurchases),
        "Não escalar; confrontar o funil, a utilidade entregue e o limite financeiro antes de decidir entre ajustar ou financiar amostra adicional.");
  }

  /** Calcula a quantidade mínima inteira de compras coerente com a conversão-alvo. */
  private int targetPurchases(int sampleSize, BigDecimal targetCvr) {
    return targetCvr
        .multiply(BigDecimal.valueOf(sampleSize))
        .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
        .intValueExact();
  }

  /** Planeja o volume de precisão e o arredonda para blocos de cinquenta visitantes. */
  private int precisionTarget(int initialTarget, BigDecimal targetCvr) {
    double targetRate = targetCvr.doubleValue() / 100.0d;
    double halfWidth = Math.min(MAX_PRECISION_HALF_WIDTH, targetRate / 2.0d);
    double rawTarget =
        CONFIDENCE_Z_95
            * CONFIDENCE_Z_95
            * targetRate
            * (1.0d - targetRate)
            / (halfWidth * halfWidth);
    int rounded =
        (int) (Math.ceil(Math.ceil(rawTarget) / PRECISION_SAMPLE_BLOCK) * PRECISION_SAMPLE_BLOCK);
    return Math.max(initialTarget, rounded);
  }

  /** Calcula conversão, intervalo binomial exato e limite unilateral para zero compras. */
  private StatisticalReading statistics(long visitors, long purchases) {
    if (visitors <= 0) {
      return StatisticalReading.empty();
    }
    double rate = Math.min(1.0d, purchases / (double) visitors);
    ExperimentBinomialConfidenceService.Interval interval =
        binomialConfidenceService.exact95(visitors, purchases);
    BigDecimal zeroUpper =
        purchases == 0 ? percentage(1.0d - Math.pow(ONE_SIDED_ALPHA_95, 1.0d / visitors)) : null;
    return new StatisticalReading(
        percentage(rate), percentage(interval.lower()), percentage(interval.upper()), zeroUpper);
  }

  /** Projeta custo observado sem converter a estimativa em autorização de orçamento. */
  private Projection projection(
      BigDecimal currentSpend,
      long visitors,
      int initialTarget,
      int precisionTarget,
      BigDecimal mediaSpendLimit) {
    if (currentSpend == null || currentSpend.compareTo(BigDecimal.ZERO) <= 0 || visitors <= 0) {
      return new Projection(
          null,
          null,
          null,
          null,
          null,
          "UNAVAILABLE",
          mediaSpendLimit == null
              ? "Sem teto de mídia configurado; a estratégia de amostra não autoriza gasto."
              : "O teto financeiro permanece ativo; ainda não há custo por visitante suficiente para projetar a amostra.");
    }
    BigDecimal costPerVisitor =
        currentSpend.divide(BigDecimal.valueOf(visitors), 4, RoundingMode.HALF_UP);
    BigDecimal initialSpend =
        costPerVisitor
            .multiply(BigDecimal.valueOf(initialTarget))
            .setScale(2, RoundingMode.HALF_UP);
    BigDecimal precisionSpend =
        costPerVisitor
            .multiply(BigDecimal.valueOf(precisionTarget))
            .setScale(2, RoundingMode.HALF_UP);
    Boolean initialFits =
        mediaSpendLimit == null ? null : initialSpend.compareTo(mediaSpendLimit) <= 0;
    Boolean precisionFits =
        mediaSpendLimit == null ? null : precisionSpend.compareTo(mediaSpendLimit) <= 0;
    String confidence =
        visitors < MINIMUM_VISITORS_FOR_STABLE_COST_PROJECTION ? "PRELIMINARY" : "OBSERVED";
    String guardrail =
        mediaSpendLimit == null
            ? "A projeção não substitui um teto de mídia nem autoriza gasto."
            : Boolean.FALSE.equals(initialFits)
                ? "O teto atual protege o caixa, mas não financia a primeira amostra no custo observado; qualquer ampliação exige nova autorização."
                : Boolean.FALSE.equals(precisionFits)
                    ? "O teto atual comporta a primeira decisão, mas não a rodada de precisão; qualquer ampliação exige nova autorização."
                    : "O teto comporta a projeção observada, mas continua sendo limite máximo e não autorização automática de gasto.";
    return new Projection(
        costPerVisitor,
        initialSpend,
        precisionSpend,
        initialFits,
        precisionFits,
        confidence,
        guardrail);
  }

  /** Monta o contrato final de uma decisão aplicável. */
  private ExperimentCockpitSampleDecisionDto decision(
      SampleMeasurement measurement,
      String status,
      String headline,
      String explanation,
      int initialTarget,
      int targetPurchases,
      int precisionTarget,
      long purchases,
      StatisticalReading statistics,
      Projection projection,
      BigDecimal mediaSpendLimit,
      String recommendation) {
    return new ExperimentCockpitSampleDecisionDto(
        true,
        measurement.available(),
        measurement.source(),
        status,
        headline,
        explanation,
        measurement.humanVisitors(),
        initialTarget,
        Math.max(0L, initialTarget - measurement.humanVisitors()),
        targetPurchases,
        precisionTarget,
        Math.max(0L, precisionTarget - measurement.humanVisitors()),
        purchases,
        statistics.observedRatePercent(),
        statistics.lower95Percent(),
        statistics.upper95Percent(),
        statistics.zeroPurchaseUpper95Percent(),
        projection.costPerVisitor(),
        projection.initialSpend(),
        projection.precisionSpend(),
        ExperimentFinancialGuardrailPolicy.zeroPrimaryResultMinimumSpend(),
        mediaSpendLimit,
        projection.initialFits(),
        projection.precisionFits(),
        projection.confidence(),
        projection.guardrail(),
        recommendation);
  }

  /** Mantém canais com contrato próprio fora da estratégia de visitantes pagos. */
  private ExperimentCockpitSampleDecisionDto notApplicable(
      SampleMeasurement measurement, long purchases, BigDecimal mediaSpendLimit) {
    return new ExperimentCockpitSampleDecisionDto(
        false,
        measurement.available(),
        measurement.source(),
        "NOT_APPLICABLE",
        "Estratégia de amostra paga não aplicável",
        "Este experimento usa outro objetivo ou canal e conserva seu contrato próprio de decisão.",
        measurement.humanVisitors(),
        0,
        0,
        0,
        0,
        0,
        Math.max(0L, purchases),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        ExperimentFinancialGuardrailPolicy.zeroPrimaryResultMinimumSpend(),
        mediaSpendLimit,
        null,
        null,
        "NOT_APPLICABLE",
        "Nenhuma alteração financeira é produzida por este diagnóstico.",
        "Usar a regra específica do canal e do objetivo deste experimento.");
  }

  /** Bloqueia conclusão quando a fonte canônica de visitantes humanos não responde. */
  private ExperimentCockpitSampleDecisionDto unavailableMeasurement(
      SampleMeasurement measurement, long purchases, BigDecimal mediaSpendLimit) {
    return decision(
        measurement,
        "MEASUREMENT_UNAVAILABLE",
        "Visitantes humanos indisponíveis",
        "A fonte canônica não entregou a contagem atribuída necessária para avaliar a amostra.",
        0,
        0,
        0,
        Math.max(0L, purchases),
        StatisticalReading.empty(),
        Projection.unavailable(mediaSpendLimit),
        mediaSpendLimit,
        "Corrigir a mensuração antes de concluir sobre oferta, página ou escala.");
  }

  /** Bloqueia conclusão quando tamanho de amostra ou conversão-alvo não estão configurados. */
  private ExperimentCockpitSampleDecisionDto configurationRequired(
      SampleMeasurement measurement,
      long purchases,
      BigDecimal mediaSpendLimit,
      Integer configuredSampleSize) {
    return decision(
        measurement,
        "CONFIGURATION_REQUIRED",
        "Estratégia de amostra incompleta",
        "Tamanho da amostra e conversão-alvo válidos são necessários para definir a decisão comercial.",
        configuredSampleSize != null && configuredSampleSize > 0 ? configuredSampleSize : 0,
        0,
        0,
        Math.max(0L, purchases),
        purchases <= measurement.humanVisitors()
            ? statistics(measurement.humanVisitors(), Math.max(0L, purchases))
            : StatisticalReading.empty(),
        Projection.unavailable(mediaSpendLimit),
        mediaSpendLimit,
        "Configurar amostra e conversão-alvo antes de interpretar o resultado.");
  }

  /** Converte uma proporção decimal em percentual com duas casas. */
  private BigDecimal percentage(double value) {
    return BigDecimal.valueOf(value * 100.0d).setScale(2, RoundingMode.HALF_UP);
  }

  /** Mantém textos e ação de cada estado juntos para impedir recomendações divergentes. */
  private record SampleStatus(
      String code, String headline, String explanation, String recommendation) {}

  /** Conserva os cálculos estatísticos usados pelo contrato final. */
  private record StatisticalReading(
      BigDecimal observedRatePercent,
      BigDecimal lower95Percent,
      BigDecimal upper95Percent,
      BigDecimal zeroPurchaseUpper95Percent) {

    /** Representa ausência de denominador sem inventar taxa de conversão. */
    private static StatisticalReading empty() {
      return new StatisticalReading(null, null, null, null);
    }
  }

  /** Conserva a projeção financeira e sua confiança sem alterar o orçamento real. */
  private record Projection(
      BigDecimal costPerVisitor,
      BigDecimal initialSpend,
      BigDecimal precisionSpend,
      Boolean initialFits,
      Boolean precisionFits,
      String confidence,
      String guardrail) {

    /** Representa projeção ainda indisponível e preserva o teto como trava. */
    private static Projection unavailable(BigDecimal mediaSpendLimit) {
      return new Projection(
          null,
          null,
          null,
          null,
          null,
          "UNAVAILABLE",
          mediaSpendLimit == null
              ? "Sem teto de mídia configurado; a estratégia de amostra não autoriza gasto."
              : "O teto financeiro permanece ativo; a projeção ainda está indisponível.");
    }
  }
}
