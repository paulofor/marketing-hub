package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.leadportal.dto.RegisterLeadPortalSubmissionRequest;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa o registro e a consolidação de submissões públicas no funil do experimento.
 */
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

    /**
     * Valida que uma submissão pública grava evento da etapa de envio do formulário.
     */
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

    /**
     * Valida que o resumo automático contabiliza eventos públicos de submissão gravados no funil.
     */
    @Test
    void summarizeCountsPublicSubmissionEventsAsFormSubmissions() {
        Experiment experiment = Experiment.builder().id(37L).build();
        when(experimentRepository.findById(37L)).thenReturn(Optional.of(experiment));
        when(eventRepository.aggregateManualByExperiment(37L, null)).thenReturn(List.of());

        service.summarize(37L);

        verify(jdbcTemplate).query(
                eq("""
                        SELECT COUNT(DISTINCT canonical_submission_id) AS total,
                               COUNT(DISTINCT lead_id) AS unique_count,
                               MAX(submitted_at) AS last_event
                        FROM (
                            SELECT CAST(CONCAT('legacy:', lps.id) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS canonical_submission_id,
                                   lps.lead_id,
                                   lps.submitted_at
                            FROM lead_portal_submission lps
                            WHERE lps.experiment_id = ?
                            UNION ALL
                            SELECT CAST(fs.id AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS canonical_submission_id,
                                   NULL AS lead_id,
                                   fs.created_at AS submitted_at
                            FROM flow_submissions fs
                            JOIN lead_portal_flow f ON f.slug = fs.flow_slug
                            WHERE %s
                            UNION ALL
                            SELECT CAST(SUBSTRING(efe.payload, CHAR_LENGTH('submissionId=') + 1) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS canonical_submission_id,
                                   efe.lead_id,
                                   efe.occurred_at AS submitted_at
                            FROM experiment_funnel_event efe
                            WHERE efe.experiment_id = ?
                              AND efe.stage = 'ENVIO_FORM'
                              AND efe.source = ?
                              AND efe.payload LIKE 'submissionId=%%'
                        ) submissions
                        WHERE canonical_submission_id IS NOT NULL
                          AND canonical_submission_id <> ''
                          AND (? IS NULL OR submitted_at > ?)
                        """.formatted(ExperimentFunnelService.FLOW_SCOPE_CONDITION)),
                any(ResultSetExtractor.class),
                eq(37L),
                eq(37L),
                eq(37L),
                eq(37L),
                eq(ExperimentFunnelEventRepository.SUBMISSION_SOURCE),
                eq(null),
                eq(null));
    }

    /**
     * Valida que a submissão pública exige identificador idempotente.
     */
    @Test
    void registerFormSubmissionRequiresSubmissionId() {
        RegisterLeadPortalSubmissionRequest request = new RegisterLeadPortalSubmissionRequest("   ", null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registerFormSubmission("flow-slug", request));

        assertEquals("ID da submissão é obrigatório", ex.getMessage());
    }
}
