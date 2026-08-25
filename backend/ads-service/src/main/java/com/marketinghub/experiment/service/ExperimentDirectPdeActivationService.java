package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.valuechainposition.ProductProcessPeriodService;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Ativa de forma atômica experimentos PDE executados por abordagem individual consentida. */
@Service
public class ExperimentDirectPdeActivationService {
  private static final String ACTIVE_PRODUCT_STATUS = "ATIVO";

  private final ExperimentRunRepository experimentRunRepository;
  private final ProductProcessPeriodService productProcessPeriodService;
  private final Clock clock;

  /** Configura as fontes persistidas usadas para ativar o experimento, o run e o produto. */
  @Autowired
  public ExperimentDirectPdeActivationService(
      ExperimentRunRepository experimentRunRepository,
      ProductProcessPeriodService productProcessPeriodService) {
    this(experimentRunRepository, productProcessPeriodService, Clock.systemUTC());
  }

  /** Permite testar a ativação com instante determinístico. */
  ExperimentDirectPdeActivationService(
      ExperimentRunRepository experimentRunRepository,
      ProductProcessPeriodService productProcessPeriodService,
      Clock clock) {
    this.experimentRunRepository = experimentRunRepository;
    this.productProcessPeriodService = productProcessPeriodService;
    this.clock = clock;
  }

  /** Informa se o experimento segue o contrato de ativação PDE direta. */
  public boolean appliesTo(Experiment experiment) {
    return experiment != null
        && experiment.getExperimentType() == ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL
        && experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE;
  }

  /** Confirma que existe um run produtivo homologado e um produto vinculável. */
  public boolean isReadyForActivation(Experiment experiment) {
    if (!appliesTo(experiment) || experiment.getId() == null || experiment.getProduct() == null) {
      return false;
    }
    return latestProductionRun(experiment)
        .map(ExperimentRun::getStatus)
        .filter(this::isActivationReadyStatus)
        .isPresent();
  }

  /**
   * Bloqueia a transição quando a homologação produtiva ou o vínculo do produto estiver ausente.
   */
  public void validateReadyForActivation(Experiment experiment) {
    if (!appliesTo(experiment)) {
      return;
    }
    if (experiment.getProduct() == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Produto não vinculado ao experimento PDE direto");
    }
    if (!isReadyForActivation(experiment)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Run produtivo precisa concluir o preflight e chegar a READY_TO_PUBLISH antes de RUNNING");
    }
  }

  /** Abre a janela comercial e avança o produto sem deixar estados parciais persistidos. */
  public void activate(Experiment experiment) {
    if (!appliesTo(experiment)) {
      return;
    }
    validateReadyForActivation(experiment);
    ExperimentRun run = latestProductionRun(experiment).orElseThrow();
    Instant activatedAt = Instant.now(clock);
    if (run.getPublicationRequestedAt() == null) {
      run.setPublicationRequestedAt(activatedAt);
    }
    if (run.getPublishedAt() == null) {
      run.setPublishedAt(activatedAt);
    }
    if (run.getCommercialWindowStartedAt() == null) {
      run.setCommercialWindowStartedAt(activatedAt);
    }
    run.setStatus(ExperimentRunStatus.RUNNING);
    experimentRunRepository.save(run);

    Product product = experiment.getProduct();
    String previousCommercialStatus = product.getCommercialStatus();
    product.setCommercialStatus(ACTIVE_PRODUCT_STATUS);
    productProcessPeriodService.recordTransition(product, previousCommercialStatus);
  }

  /** Busca o run produtivo mais recente que governa a ativação comercial direta. */
  private Optional<ExperimentRun> latestProductionRun(Experiment experiment) {
    return experimentRunRepository.findTopByExperimentIdAndModeOrderByRunNumberDesc(
        experiment.getId(), ExperimentRunMode.PRODUCTION);
  }

  /** Aceita o estado homologado e preserva a idempotência quando a ativação já ocorreu. */
  private boolean isActivationReadyStatus(ExperimentRunStatus status) {
    return status == ExperimentRunStatus.READY_TO_PUBLISH || status == ExperimentRunStatus.RUNNING;
  }
}
