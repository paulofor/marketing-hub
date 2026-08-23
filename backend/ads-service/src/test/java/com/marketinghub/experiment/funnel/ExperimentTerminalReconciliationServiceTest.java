package com.marketinghub.experiment.funnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Comprova o fechamento administrativo sem reativar mídia do experimento. */
@ExtendWith(MockitoExtension.class)
class ExperimentTerminalReconciliationServiceTest {
  @Mock private ExperimentRepository experimentRepository;
  @Mock private ExperimentFunnelAutoStopService autoStopService;

  /** Converte a pausa manual em invalidação quando a regra financeira está comprovada. */
  @Test
  void reconcilesUserStoppedExperimentWithFinancialEvidence() {
    Experiment experiment =
        Experiment.builder().id(88L).status(ExperimentStatus.USER_STOPPED).build();
    when(experimentRepository.findById(88L)).thenReturn(Optional.of(experiment));
    when(autoStopService.stopIfNoPrimaryResultAfterMinimumSpend(experiment))
        .thenAnswer(
            invocation -> {
              experiment.setStatus(ExperimentStatus.INVALIDATED);
              return true;
            });
    ExperimentTerminalReconciliationService service =
        new ExperimentTerminalReconciliationService(experimentRepository, autoStopService);

    var result = service.reconcile(88L);

    assertThat(result.invalidated()).isTrue();
    assertThat(result.status()).isEqualTo(ExperimentStatus.INVALIDATED);
    verify(experimentRepository).save(experiment);
  }

  /** Expõe a disponibilidade calculada pela política canônica sem duplicar a regra na tela. */
  @Test
  void exposesFinancialReconciliationAvailability() {
    Experiment experiment =
        Experiment.builder().id(88L).status(ExperimentStatus.USER_STOPPED).build();
    when(autoStopService.isFinancialReconciliationAvailable(experiment)).thenReturn(true);
    ExperimentTerminalReconciliationService service =
        new ExperimentTerminalReconciliationService(experimentRepository, autoStopService);

    assertThat(service.isAvailable(experiment)).isTrue();
  }

  /** Mantém a reconciliação idempotente quando o experimento já foi invalidado. */
  @Test
  void preservesAlreadyInvalidatedExperimentWithoutRepeatingSideEffects() {
    Experiment experiment =
        Experiment.builder().id(88L).status(ExperimentStatus.INVALIDATED).build();
    when(experimentRepository.findById(88L)).thenReturn(Optional.of(experiment));
    ExperimentTerminalReconciliationService service =
        new ExperimentTerminalReconciliationService(experimentRepository, autoStopService);

    var result = service.reconcile(88L);

    assertThat(result.invalidated()).isTrue();
    assertThat(result.status()).isEqualTo(ExperimentStatus.INVALIDATED);
    verifyNoInteractions(autoStopService);
  }
}
