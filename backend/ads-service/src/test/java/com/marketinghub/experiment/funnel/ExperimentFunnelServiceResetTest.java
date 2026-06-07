package com.marketinghub.experiment.funnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Testa a limpeza operacional de eventos do funil quando o experimento precisa descartar dados de teste.
 */
@ExtendWith(MockitoExtension.class)
class ExperimentFunnelServiceResetTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ExperimentFunnelEventRepository eventRepository;

    @Mock
    private ExperimentLandingAnalyticsEventRepository landingAnalyticsEventRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExperimentFunnelService service;

    /**
     * Valida que o reset apaga analytics de sessão antes dos demais eventos e atualiza o marco temporal.
     */
    @Test
    void resetFunnelUpdatesTimestamp() {
        Experiment experiment = Experiment.builder().id(9L).build();
        when(experimentRepository.findById(9L)).thenReturn(Optional.of(experiment));

        Instant resetAt = service.resetFunnel(9L);

        InOrder inOrder = inOrder(landingAnalyticsEventRepository, eventRepository, experimentRepository);
        inOrder.verify(landingAnalyticsEventRepository).deleteByExperimentId(9L);
        inOrder.verify(eventRepository).deleteByExperimentIdAndSource(
                9L,
                ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE);
        inOrder.verify(eventRepository).deleteByExperimentId(9L);
        inOrder.verify(experimentRepository).save(experiment);
        assertNotNull(experiment.getFunnelResetAt());
        assertEquals(experiment.getFunnelResetAt(), resetAt);
    }
}
