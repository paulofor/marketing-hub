package com.marketinghub.experiment.funnel;

import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.leadportal.dto.RegisterLeadPortalSubmissionRequest;
import com.marketinghub.repository.jpa.core.LeadRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ExperimentFunnelServiceSubmissionTest {

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
    void registerFormSubmissionSavesStageFourEvent() {
        Experiment experiment = Experiment.builder().id(7L).build();
        when(experimentRepository.findFirstByLeadPortalFlowSlug("flow-slug"))
                .thenReturn(Optional.of(experiment));

        Instant submittedAt = Instant.parse("2024-05-01T12:34:56Z");
        RegisterLeadPortalSubmissionRequest request =
                new RegisterLeadPortalSubmissionRequest("submission-123", submittedAt, "ad-xyz");

        service.registerFormSubmission("flow-slug", request);

        ArgumentCaptor<ExperimentFunnelEvent> eventCaptor = ArgumentCaptor.forClass(ExperimentFunnelEvent.class);
        verify(eventRepository).save(eventCaptor.capture());

        ExperimentFunnelEvent saved = eventCaptor.getValue();
        assertEquals(experiment, saved.getExperiment());
        assertEquals(ExperimentFunnelStage.ENVIO_FORM, saved.getStage());
        assertEquals(ExperimentFunnelEventRepository.SUBMISSION_SOURCE, saved.getSource());
        assertEquals("submissionId=submission-123", saved.getPayload());
        assertEquals(submittedAt, saved.getOccurredAt());
        assertEquals("ad-xyz", saved.getCampaignCode());
    }

    @Test
    void registerFormSubmissionRequiresSubmissionId() {
        RegisterLeadPortalSubmissionRequest request = new RegisterLeadPortalSubmissionRequest("   ", null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registerFormSubmission("flow-slug", request));

        assertEquals("ID da submissão é obrigatório", ex.getMessage());
    }
}
