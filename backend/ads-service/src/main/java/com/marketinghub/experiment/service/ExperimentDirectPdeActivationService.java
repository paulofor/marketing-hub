package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunGateCodes;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.valuechainposition.ProductProcessPeriodService;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Ativa de forma atômica experimentos PDE executados por abordagem individual consentida. */
@Service
public class ExperimentDirectPdeActivationService {
  private static final String ACTIVE_PRODUCT_STATUS = "ATIVO";
  private static final String PDE_PRODUCT_TYPE_CODE = "PDE";
  private static final Set<String> LOW_TICKET_ACTIVATION_GATES =
      Set.of(
          ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED,
          ExperimentRunGateCodes.CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED,
          ExperimentRunGateCodes.DIRECT_CHANNEL_READINESS_CONFIRMED,
          ExperimentRunGateCodes.DATA_FRESHNESS_VALID);

  private final ExperimentRunRepository experimentRunRepository;
  private final ExperimentRunGateResultRepository gateResultRepository;
  private final ProductProcessPeriodService productProcessPeriodService;
  private final Clock clock;

  /** Configura as fontes persistidas usadas para ativar o experimento, o run e o produto. */
  @Autowired
  public ExperimentDirectPdeActivationService(
      ExperimentRunRepository experimentRunRepository,
      ExperimentRunGateResultRepository gateResultRepository,
      ProductProcessPeriodService productProcessPeriodService) {
    this(
        experimentRunRepository,
        gateResultRepository,
        productProcessPeriodService,
        Clock.systemUTC());
  }

  /** Permite testar a ativação com instante determinístico. */
  ExperimentDirectPdeActivationService(
      ExperimentRunRepository experimentRunRepository,
      ExperimentRunGateResultRepository gateResultRepository,
      ProductProcessPeriodService productProcessPeriodService,
      Clock clock) {
    this.experimentRunRepository = experimentRunRepository;
    this.gateResultRepository = gateResultRepository;
    this.productProcessPeriodService = productProcessPeriodService;
    this.clock = clock;
  }

  /** Informa se o experimento segue o contrato de ativação PDE direta. */
  public boolean appliesTo(Experiment experiment) {
    return experiment != null
        && experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE
        && (experiment.getExperimentType() == ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL
            || isLowTicketPde(experiment));
  }

  /** Confirma que existe um run produtivo homologado e um produto vinculável. */
  public boolean isReadyForActivation(Experiment experiment) {
    if (!appliesTo(experiment) || experiment.getId() == null || experiment.getProduct() == null) {
      return false;
    }
    return latestProductionRun(experiment)
        .filter(run -> isActivationReadyStatus(run.getStatus()))
        .filter(run -> hasRequiredActivationEvidence(experiment, run))
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
          "Run produtivo precisa chegar a READY_TO_PUBLISH com os gates comerciais auditáveis antes de RUNNING");
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
    if (isLowTicketPde(experiment)) {
      productProcessPeriodService.recordAuditedPreflightTransition(
          product, previousCommercialStatus);
    } else {
      productProcessPeriodService.recordTransition(product, previousCommercialStatus);
    }
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

  /** Exige evidência gate a gate no low-ticket PDE e preserva o contrato legado do clube. */
  private boolean hasRequiredActivationEvidence(Experiment experiment, ExperimentRun run) {
    if (!isLowTicketPde(experiment)) {
      return true;
    }
    if (run.getId() == null) {
      return false;
    }
    List<ExperimentRunGateResult> gates =
        gateResultRepository.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(run.getId());
    boolean noBlockingGate = gates.stream().allMatch(this::isApprovedGate);
    boolean requiredGatesAudited =
        LOW_TICKET_ACTIVATION_GATES.stream()
            .allMatch(requiredCode -> hasAuditedPass(gates, requiredCode));
    return noBlockingGate && requiredGatesAudited;
  }

  /** Confirma um gate aprovado ou explicitamente não aplicável sem aceitar pendência ou alerta. */
  private boolean isApprovedGate(ExperimentRunGateResult gate) {
    return gate.getStatus() == ExperimentRunGateStatus.PASS
        || gate.getStatus() == ExperimentRunGateStatus.NOT_APPLICABLE;
  }

  /** Confirma que o gate operacional obrigatório possui aprovação e referência de evidência. */
  private boolean hasAuditedPass(List<ExperimentRunGateResult> gates, String requiredCode) {
    return gates.stream()
        .filter(gate -> requiredCode.equals(gate.getGateCode()))
        .anyMatch(
            gate ->
                gate.getStatus() == ExperimentRunGateStatus.PASS
                    && StringUtils.hasText(gate.getEvidenceReference()));
  }

  /** Identifica venda direta low-ticket vinculada ao tipo de produto PDE do catálogo. */
  private boolean isLowTicketPde(Experiment experiment) {
    if (experiment == null
        || experiment.getExperimentType() != ExperimentType.LOW_TICKET_PRODUCT
        || experiment.getProduct() == null) {
      return false;
    }
    Product product = experiment.getProduct();
    if (product.getProductTypeDefinition() != null
        && PDE_PRODUCT_TYPE_CODE.equalsIgnoreCase(product.getProductTypeDefinition().getCode())) {
      return true;
    }
    return StringUtils.hasText(product.getProductType())
        && PDE_PRODUCT_TYPE_CODE.equalsIgnoreCase(product.getProductType().trim());
  }
}
