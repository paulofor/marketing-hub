package com.marketinghub.experiment.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ReactivateExperimentRequest;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.ExperimentTerminalReconciliationService;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.salespageab.service.ExperimentSalesPageAbTestService;
import com.marketinghub.experiment.service.ExperimentCampaignDestinationPolicy;
import com.marketinghub.experiment.service.ExperimentCockpitService;
import com.marketinghub.experiment.service.ExperimentConstructionService;
import com.marketinghub.experiment.service.ExperimentCostReconciliationService;
import com.marketinghub.experiment.service.ExperimentDeliverablesZipService;
import com.marketinghub.experiment.service.ExperimentDiagnosticsService;
import com.marketinghub.experiment.service.ExperimentPromiseGenerationService;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.experiment.service.ExperimentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Valida a política de bloqueio no endpoint administrativo de liberação para Facebook Ads. */
class ExperimentControllerReleasePolicyTest {

  @Test
  // Garante que o botão de liberação bloqueia tráfego frio com compra direta.
  void releaseForFacebookRejectsPurchaseIntentBypassingSalesPage() {
    ExperimentService service = mock(ExperimentService.class);
    ExperimentCampaignDestinationPolicy policy = mock(ExperimentCampaignDestinationPolicy.class);
    Experiment experiment = new Experiment();
    when(service.get(60L)).thenReturn(experiment);
    when(policy.missingConfiguration(experiment)).thenReturn(List.of("salesPageAdDestination"));
    ExperimentController controller =
        controller(service, policy, mock(ExperimentTerminalReconciliationService.class));

    assertThatThrownBy(() -> controller.releaseForFacebook(60L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("checkout direto");
    verify(service, never()).releaseForFacebook(60L);
  }

  /** Impede que um experimento com trava financeira seja reativado pela rota administrativa. */
  @Test
  void reactivationRejectsExperimentAwaitingFinancialReconciliation() {
    ExperimentService service = mock(ExperimentService.class);
    ExperimentTerminalReconciliationService reconciliationService =
        mock(ExperimentTerminalReconciliationService.class);
    Experiment experiment = new Experiment();
    when(service.get(88L)).thenReturn(experiment);
    when(reconciliationService.isAvailable(experiment)).thenReturn(true);
    ExperimentController controller =
        controller(service, mock(ExperimentCampaignDestinationPolicy.class), reconciliationService);
    ReactivateExperimentRequest request =
        new ReactivateExperimentRequest("Reativação não deve ser permitida");

    assertThatThrownBy(() -> controller.reactivate(88L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("trava financeira");
    verify(service, never()).reactivate(88L, request);
  }

  /** Monta o controller com colaboradores controlados para validar políticas administrativas. */
  private ExperimentController controller(
      ExperimentService service,
      ExperimentCampaignDestinationPolicy policy,
      ExperimentTerminalReconciliationService reconciliationService) {
    return new ExperimentController(
        service,
        mock(ExperimentMapper.class),
        mock(ExperimentDiagnosticsService.class),
        mock(ExperimentReadinessService.class),
        mock(ExperimentPromiseGenerationService.class),
        policy,
        mock(ExperimentFunnelService.class),
        mock(ExperimentSalesPageAbTestService.class),
        mock(ExperimentDeliverablesZipService.class),
        mock(ExperimentConstructionService.class),
        mock(ExperimentCostReconciliationService.class),
        mock(ExperimentCockpitService.class),
        mock(com.marketinghub.experiment.service.DedaloCreativeTaskOrchestrationService.class),
        reconciliationService);
  }
}
