package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Reconcilia um experimento parado com a regra financeira e seus registros operacionais. */
@Service
public class ExperimentTerminalReconciliationService {
  private final ExperimentRepository experimentRepository;
  private final ExperimentFunnelAutoStopService autoStopService;

  /** Configura a persistência do experimento e a política canônica de parada financeira. */
  public ExperimentTerminalReconciliationService(
      ExperimentRepository experimentRepository, ExperimentFunnelAutoStopService autoStopService) {
    this.experimentRepository = experimentRepository;
    this.autoStopService = autoStopService;
  }

  /**
   * Reavalia um encerramento manual sem reativar mídia e retorna se a regra comercial foi aplicada.
   */
  @Transactional
  public TerminalReconciliationResponse reconcile(Long experimentId) {
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Experimento não encontrado."));
    if (experiment.getStatus() == ExperimentStatus.INVALIDATED) {
      return new TerminalReconciliationResponse(experiment.getId(), experiment.getStatus(), true);
    }
    if (experiment.getStatus() != ExperimentStatus.USER_STOPPED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente experimento parado pelo usuário pode ser reconciliado.");
    }
    boolean invalidated = autoStopService.stopIfNoPrimaryResultAfterMinimumSpend(experiment);
    experimentRepository.save(experiment);
    return new TerminalReconciliationResponse(
        experiment.getId(), experiment.getStatus(), invalidated);
  }

  /** Informa ao contrato administrativo se o comando de reconciliação pode ser oferecido. */
  public boolean isAvailable(Experiment experiment) {
    return autoStopService.isFinancialReconciliationAvailable(experiment);
  }

  /** Resume o resultado persistido da reconciliação administrativa. */
  public record TerminalReconciliationResponse(
      Long experimentId, ExperimentStatus status, boolean invalidated) {}
}
