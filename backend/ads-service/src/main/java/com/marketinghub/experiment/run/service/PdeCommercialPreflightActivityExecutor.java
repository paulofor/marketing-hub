package com.marketinghub.experiment.run.service;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityRequirementResponse;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.experiment.run.ExperimentRunStopPolicy;
import com.marketinghub.experiment.run.service.create.CreateExperimentRunRequest;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: criar, executar e reconciliar o preflight técnico solicitado pelo processo PDE.
 */
@Service
public class PdeCommercialPreflightActivityExecutor
    implements BackendProductProcessActivityExecutor {
  static final String PROCESS_CODE = PdeCommercialPreflightActivityProjector.PROCESS_CODE;
  static final String ACTIVITY_ID = PdeCommercialPreflightActivityProjector.ACTIVITY_ID;
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
  private static final Set<ExperimentRunStatus> RETRY_WITH_NEW_RUN_STATUSES =
      Set.of(
          ExperimentRunStatus.PREFLIGHT_FAILED,
          ExperimentRunStatus.FAILED,
          ExperimentRunStatus.CANCELLED);
  private static final Pattern EXPERIMENT_REFERENCE = Pattern.compile("^experiment:(\\d+)$");

  private final ExperimentRepository experimentRepository;
  private final ExperimentRunRepository experimentRunRepository;
  private final BackendExperimentRunService experimentRunService;
  private final ProductProcessActivityPredecessorService predecessorService;

  /** Configura experimento, runs, comando de preflight e validação da ordem BPM. */
  public PdeCommercialPreflightActivityExecutor(
      ExperimentRepository experimentRepository,
      ExperimentRunRepository experimentRunRepository,
      BackendExperimentRunService experimentRunService,
      ProductProcessActivityPredecessorService predecessorService) {
    this.experimentRepository = experimentRepository;
    this.experimentRunRepository = experimentRunRepository;
    this.experimentRunService = experimentRunService;
    this.predecessorService = predecessorService;
  }

  /** Reconhece exclusivamente o preflight do processo comercial PDE publicado. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITY_ID.equals(activityDefinition.getActivityId());
  }

  /** Expõe o run atual e bloqueia qualquer atalho antes das revisões predecessoras. */
  @Override
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    ProductProcessActivityPredecessorReadiness predecessor =
        predecessorService.readiness(process, activityDefinition, sourceReference);
    Experiment experiment = referencedExperiment(product, sourceReference);
    Optional<ExperimentRun> run = latestProductionRun(experiment.getId());
    String actionLabel = run.map(this::actionLabel).orElse("Criar e executar preflight");
    String reason =
        predecessor.ready()
            ? run.map(this::runReason)
                .orElse("As revisões estão concluídas; o backend pode criar o run produtivo.")
            : predecessor.reason();
    boolean executable = predecessor.ready() && run.map(this::canExecute).orElse(true);
    List<ProductProcessActivityRequirementResponse> requirements =
        List.of(
            new ProductProcessActivityRequirementResponse(
                "PREDECESSORS_COMPLETED",
                "Revisões anteriores concluídas",
                predecessor.ready(),
                predecessor.reason(),
                predecessor.ready()
                    ? "Preserve as evidências já aprovadas."
                    : predecessor.reason()),
            new ProductProcessActivityRequirementResponse(
                "PRODUCTION_RUN",
                "Run produtivo de homologação",
                run.isPresent(),
                run.map(value -> "Run #" + value.getRunNumber() + " em " + value.getStatus())
                    .orElse("Nenhum run produtivo foi criado."),
                run.isPresent()
                    ? "Use o painel abaixo para concluir gates e evidências."
                    : "Execute o comando para criar a tentativa produtiva."));
    return new BackendProductProcessActivityReadiness(
        executable,
        reason,
        actionLabel,
        "Cria uma tentativa produtiva quando necessário, preserva tentativas anteriores e executa"
            + " os gates determinísticos. O painel abaixo registra a homologação funcional no run"
            + " atual.",
        "EXPERIMENT_PREFLIGHT",
        experiment.getId(),
        requirements);
  }

  /** Cria uma nova tentativa após falha, executa o preflight e devolve o estado persistido. */
  @Override
  @Transactional
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    BackendProductProcessActivityReadiness readiness =
        readiness(process, activityDefinition, product, sourceReference);
    if (!readiness.ready()) {
      throw new IllegalStateException(readiness.reason());
    }
    Experiment experiment = referencedExperiment(product, sourceReference);
    ExperimentRun run =
        latestProductionRun(experiment.getId())
            .filter(value -> !RETRY_WITH_NEW_RUN_STATUSES.contains(value.getStatus()))
            .orElseGet(() -> createProductionRun(experiment.getId()));
    if (run.getStatus() == ExperimentRunStatus.DRAFT) {
      experimentRunService.runPreflight(run.getId());
      run = latestProductionRun(experiment.getId()).orElseThrow();
    }
    experimentRunService.synchronizePreflightActivity(run.getId());
    String resolvedReference = "experiment:" + experiment.getId();
    if (COMPLETED_STATUSES.contains(run.getStatus())) {
      return new BackendProductProcessActivityExecutionResult(
          resolvedReference,
          "COMPLETED",
          true,
          "Preflight produtivo concluído e atividade comercial reconciliada automaticamente.");
    }
    if (run.getStatus() == ExperimentRunStatus.PREFLIGHT_FAILED) {
      return new BackendProductProcessActivityExecutionResult(
          resolvedReference,
          "BLOCKED",
          false,
          "O preflight encontrou bloqueadores. Corrija as causas e reexecute pela mesma atividade.");
    }
    return new BackendProductProcessActivityExecutionResult(
        resolvedReference,
        "PENDING",
        false,
        "Run criado e gates iniciais avaliados. Registre no painel as evidências funcionais pendentes.");
  }

  /** Abre o próximo run produtivo e devolve a entidade persistida para avaliação. */
  private ExperimentRun createProductionRun(Long experimentId) {
    experimentRunService.create(
        experimentId,
        new CreateExperimentRunRequest(
            ExperimentRunMode.PRODUCTION,
            ExperimentRunStopPolicy.MANUAL_ONLY,
            "BUSINESS_PROCESS_UI"));
    return latestProductionRun(experimentId).orElseThrow();
  }

  /** Resolve o experimento declarado pela referência sem misturar outro ciclo ou produto. */
  private Experiment referencedExperiment(Product product, String sourceReference) {
    Matcher matcher = EXPERIMENT_REFERENCE.matcher(sourceReference == null ? "" : sourceReference);
    if (!matcher.matches()) {
      throw new IllegalStateException(
          "O preflight exige uma referência operacional no formato experiment:<id>.");
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

  /** Busca o run produtivo mais recente sem misturar tentativas técnicas. */
  private Optional<ExperimentRun> latestProductionRun(Long experimentId) {
    return experimentRunRepository.findTopByExperimentIdAndModeOrderByRunNumberDesc(
        experimentId, ExperimentRunMode.PRODUCTION);
  }

  /** Define o comando coerente com o estado persistido do run. */
  private String actionLabel(ExperimentRun run) {
    if (run.getStatus() == ExperimentRunStatus.DRAFT) {
      return "Executar preflight";
    }
    if (RETRY_WITH_NEW_RUN_STATUSES.contains(run.getStatus())) {
      return "Reexecutar preflight";
    }
    if (COMPLETED_STATUSES.contains(run.getStatus())) {
      return "Reconciliar conclusão";
    }
    return "Continuar homologação";
  }

  /** Explica o próximo movimento sem inferência no frontend. */
  private String runReason(ExperimentRun run) {
    if (run.getStatus() == ExperimentRunStatus.PREFLIGHT_PENDING) {
      return "O run aguarda evidências funcionais no painel desta atividade.";
    }
    if (run.getStatus() == ExperimentRunStatus.PREFLIGHT_RUNNING) {
      return "O backend ainda está avaliando o preflight atual.";
    }
    if (COMPLETED_STATUSES.contains(run.getStatus())) {
      return "O run atingiu prontidão técnica e pode reconciliar a atividade.";
    }
    return "O run pode executar novamente os gates determinísticos.";
  }

  /** Impede reexecução que apagaria evidências enquanto a homologação está pendente ou ativa. */
  private boolean canExecute(ExperimentRun run) {
    return run.getStatus() != ExperimentRunStatus.PREFLIGHT_PENDING
        && run.getStatus() != ExperimentRunStatus.PREFLIGHT_RUNNING;
  }
}
