package com.marketinghub.experiment.service;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityHandler;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityRequirement;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: validar e aplicar a autorização humana da ativação comercial de um produto PDE.
 */
@Service
public class PdeCommercialActivationHumanActivityHandler
    implements HumanProductProcessActivityHandler {
  static final String PROCESS_CODE = "pde-commercial-homologation-activation";
  static final String ACTIVITY_ID = "authorization";
  private static final String CONFIRMATION_TOKEN =
      "CONFIRM:pde-commercial-homologation-activation:authorization";
  private static final Pattern EXPERIMENT_REFERENCE = Pattern.compile("^experiment:(\\d+)$");

  private final ExperimentRepository experimentRepository;
  private final ExperimentRunRepository experimentRunRepository;
  private final CommercialPlanRepository commercialPlanRepository;
  private final ExperimentReadinessService readinessService;
  private final ExperimentService experimentService;

  /** Configura as fontes de experimento, teto financeiro, prontidão e mudança de estado. */
  public PdeCommercialActivationHumanActivityHandler(
      ExperimentRepository experimentRepository,
      ExperimentRunRepository experimentRunRepository,
      CommercialPlanRepository commercialPlanRepository,
      ExperimentReadinessService readinessService,
      ExperimentService experimentService) {
    this.experimentRepository = experimentRepository;
    this.experimentRunRepository = experimentRunRepository;
    this.commercialPlanRepository = commercialPlanRepository;
    this.readinessService = readinessService;
    this.experimentService = experimentService;
  }

  /** Reconhece exclusivamente o gate humano da homologação comercial do PDE. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITY_ID.equals(activityDefinition.getActivityId());
  }

  /** Consolida os gates do experimento e exige um teto financeiro persistido. */
  @Override
  @Transactional(readOnly = true)
  public HumanProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    Experiment experiment = referencedExperiment(product, sourceReference);
    ExperimentReadinessSummaryDto readiness = readinessService.summarize(experiment.getId());
    CommercialPlan plan = currentPlan(product.getId(), experiment.getId());
    ExperimentRun productionRun = latestProductionRun(experiment.getId());
    BigDecimal budgetLimit = plan == null ? null : plan.getMaxBudget();
    boolean budgetDefined = budgetLimit != null && budgetLimit.compareTo(BigDecimal.ZERO) > 0;
    boolean auditContextReady =
        plan != null
            && plan.getId() != null
            && productionRun != null
            && productionRun.getId() != null
            && productionRun.getRunNumber() != null;
    List<HumanProductProcessActivityRequirement> requirements = new ArrayList<>();
    readiness
        .runningGateRequirements()
        .forEach(
            requirement ->
                requirements.add(
                    new HumanProductProcessActivityRequirement(
                        requirement.code(),
                        requirement.title(),
                        requirement.ready(),
                        requirement.detail(),
                        requirement.recommendation())));
    requirements.add(
        new HumanProductProcessActivityRequirement(
            "BUDGET_LIMIT_DEFINED",
            "Teto financeiro definido",
            budgetDefined,
            budgetDefined
                ? "O plano limita a operação a " + brl(budgetLimit) + "."
                : "O plano comercial ainda não possui teto financeiro positivo.",
            budgetDefined
                ? "Não ultrapasse o teto persistido sem uma nova decisão humana."
                : "Defina o teto no plano comercial antes de autorizar a ativação."));
    requirements.add(
        new HumanProductProcessActivityRequirement(
            "AUDIT_CONTEXT_READY",
            "Evidências vinculadas",
            auditContextReady,
            auditContextReady
                ? "Run #"
                    + productionRun.getRunNumber()
                    + " e plano #"
                    + plan.getId()
                    + " serão registrados automaticamente."
                : "O run produtivo e o plano comercial ainda não possuem identidade auditável.",
            auditContextReady
                ? "A tela registrará essas referências sem exigir digitação."
                : "Reconcilie o run produtivo e o plano comercial antes da decisão."));
    boolean ready = readiness.eligibleForRunning() && budgetDefined && auditContextReady;
    String reason =
        ready
            ? "Preflight, requisitos comerciais e teto financeiro estão prontos para decisão."
            : requirements.stream()
                .filter(requirement -> !requirement.satisfied())
                .findFirst()
                .map(HumanProductProcessActivityRequirement::recommendation)
                .orElse("A ativação ainda possui requisito pendente.");
    String sample =
        experiment.getSampleSize() == null
            ? "amostra ainda não definida"
            : "amostra de " + experiment.getSampleSize() + " contatos";
    String budget = budgetDefined ? brl(budgetLimit) : "teto financeiro ainda não definido";
    String auditEvidenceReference =
        auditContextReady
            ? "experiment:"
                + experiment.getId()
                + "; experiment-run:"
                + productionRun.getId()
                + "/run-number:"
                + productionRun.getRunNumber()
                + "; commercial-plan:"
                + plan.getId()
            : null;
    return new HumanProductProcessActivityReadiness(
        ready,
        reason,
        "Li, entendi e autorizo",
        "Revise o resumo abaixo e autorize com um único comando. O sistema registra as evidências e inicia a janela comercial, sem criar campanha paga.",
        "Revise e autorize",
        "O experimento "
            + experiment.getName()
            + " está pronto, com "
            + sample
            + " e teto total de "
            + budget
            + ".",
        CONFIRMATION_TOKEN,
        "EXPERIMENT_ACTIVATION",
        experiment.getId(),
        requirements,
        HumanProductProcessActivityReadiness.REVIEW_AND_ACCEPT,
        auditEvidenceReference);
  }

  /** Ativa ou reconcilia os estados comerciais pelo serviço canônico após a confirmação humana. */
  @Override
  @Transactional
  public void approve(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request) {
    Experiment experiment = referencedExperiment(product, sourceReference);
    experimentService.updateStatus(experiment.getId(), ExperimentStatus.RUNNING);
  }

  /** Resolve o experimento declarado pela referência sem misturar outro ciclo ou produto. */
  private Experiment referencedExperiment(Product product, String sourceReference) {
    Matcher matcher = EXPERIMENT_REFERENCE.matcher(sourceReference == null ? "" : sourceReference);
    if (!matcher.matches()) {
      throw new IllegalStateException(
          "A ativação exige uma referência operacional no formato experiment:<id>.");
    }
    Long experimentId = Long.valueOf(matcher.group(1));
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "O experimento da referência operacional não foi encontrado."));
    if (experiment.getProduct() == null
        || experiment.getProduct().getId() == null
        || !experiment.getProduct().getId().equals(product.getId())) {
      throw new IllegalStateException(
          "O experimento da referência operacional não pertence ao produto selecionado.");
    }
    return experiment;
  }

  /** Prefere o plano ligado ao experimento e usa o plano mais recente do produto como fallback. */
  private CommercialPlan currentPlan(Long productId, Long experimentId) {
    return commercialPlanRepository.findByExperimentReference(experimentId).stream()
        .findFirst()
        .or(() -> commercialPlanRepository.findByProductId(productId).stream().findFirst())
        .orElse(null);
  }

  /** Retorna o run produtivo exato que sustenta o resumo apresentado ao operador. */
  private ExperimentRun latestProductionRun(Long experimentId) {
    return experimentRunRepository
        .findTopByExperimentIdAndModeOrderByRunNumberDesc(
            experimentId, ExperimentRunMode.PRODUCTION)
        .orElse(null);
  }

  /** Formata o teto financeiro em linguagem de negócio para a confirmação. */
  private String brl(BigDecimal value) {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value);
  }
}
