package com.marketinghub.experiment.web;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.service.ExperimentCampaignDestinationPolicy;
import com.marketinghub.experiment.service.ExperimentDiagnosticsService;
import com.marketinghub.experiment.service.ExperimentPromiseGenerationService;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.experiment.service.ExperimentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Valida a política de bloqueio no endpoint administrativo de liberação para Facebook Ads.
 */
class ExperimentControllerReleasePolicyTest {

    @Test
    // Garante que o botão de liberação bloqueia tráfego frio com compra direta.
    void releaseForFacebookRejectsPurchaseIntentBypassingSalesPage() {
        ExperimentService service = mock(ExperimentService.class);
        ExperimentCampaignDestinationPolicy policy = mock(ExperimentCampaignDestinationPolicy.class);
        Experiment experiment = new Experiment();
        when(service.get(60L)).thenReturn(experiment);
        when(policy.missingConfiguration(experiment)).thenReturn(List.of("salesPageAdDestination"));
        ExperimentController controller = new ExperimentController(
                service,
                mock(ExperimentMapper.class),
                mock(ExperimentDiagnosticsService.class),
                mock(ExperimentReadinessService.class),
                mock(ExperimentPromiseGenerationService.class),
                policy);

        assertThatThrownBy(() -> controller.releaseForFacebook(60L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("checkout direto");
        verify(service, never()).releaseForFacebook(60L);
    }
}
