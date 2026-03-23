package com.marketinghub.experiment.funnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.repository.LeadRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ExperimentFunnelServiceResetTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ExperimentFunnelEventRepository eventRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExperimentFunnelService service;

    @Test
    void resetFunnelUpdatesTimestamp() {
        Experiment experiment = Experiment.builder().id(9L).build();
        when(experimentRepository.findById(9L)).thenReturn(Optional.of(experiment));

        Instant resetAt = service.resetFunnel(9L);

        verify(experimentRepository).save(experiment);
        assertNotNull(experiment.getFunnelResetAt());
        assertEquals(experiment.getFunnelResetAt(), resetAt);
    }
}
