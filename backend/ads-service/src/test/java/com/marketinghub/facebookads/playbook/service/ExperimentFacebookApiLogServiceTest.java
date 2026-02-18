package com.marketinghub.facebookads.playbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJob;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobApiLog;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobStatus;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobType;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorker;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflow;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetJobApiLogRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentFacebookApiLogEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentFacebookApiLogServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ExperimentAdSetJobApiLogRepository jobApiLogRepository;

    @Mock
    private ExperimentFacebookApiLogEntryRepository apiLogEntryRepository;

    private ExperimentFacebookApiLogService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentFacebookApiLogService(
                experimentRepository,
                jobApiLogRepository,
                apiLogEntryRepository,
                new ObjectMapper()
        );
    }

    @Test
    void shouldFailWhenExperimentDoesNotExist() {
        when(experimentRepository.existsById(6L)).thenReturn(false);

        assertThatThrownBy(() -> service.findLogs(6L, 10))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Experimento 6");
    }

    @Test
    void shouldReturnSanitizedLogs() {
        when(experimentRepository.existsById(6L)).thenReturn(true);
        Experiment experiment = new Experiment();
        experiment.setId(6L);
        ExperimentAdSetWorkflow workflow = ExperimentAdSetWorkflow.builder()
                .id(20L)
                .experiment(experiment)
                .build();
        ExperimentAdSetJob job = ExperimentAdSetJob.builder()
                .id(15L)
                .workflow(workflow)
                .type(ExperimentAdSetJobType.FACEBOOK_SEED_LOOKUP)
                .worker(ExperimentAdSetWorker.FACEBOOK)
                .status(ExperimentAdSetJobStatus.SUCCEEDED)
                .resourceId(99L)
                .build();
        ExperimentAdSetJobApiLog log = ExperimentAdSetJobApiLog.builder()
                .id(55L)
                .job(job)
                .provider("FACEBOOK")
                .endpoint("/v19.0/act_123?access_token=abc123456789")
                .httpMethod("POST")
                .statusCode(400)
                .errorMessage("Invalid token")
                .requestPayload("{\"access_token\":\"abc123456789\",\"name\":\"Test\"}")
                .responsePayload("{\"error\":{\"message\":\"Bad token\"}}")
                .requestedAt(Instant.parse("2024-01-01T10:00:00Z"))
                .respondedAt(Instant.parse("2024-01-01T10:00:01Z"))
                .build();
        when(jobApiLogRepository.findByJobWorkflowExperimentId(eq(6L), any(Pageable.class)))
                .thenReturn(List.of(log));
        when(apiLogEntryRepository.findByExperimentId(eq(6L), any(Pageable.class)))
                .thenReturn(List.of());

        List<ExperimentFacebookApiLogDto> dtos = service.findLogs(6L, 5);

        assertThat(dtos).hasSize(1);
        ExperimentFacebookApiLogDto dto = dtos.getFirst();
        assertThat(dto.jobId()).isEqualTo(15L);
        assertThat(dto.workflowId()).isEqualTo(20L);
        assertThat(dto.endpoint()).doesNotContain("abc123456789");
        assertThat(dto.requestPayload()).doesNotContain("abc123456789");
        assertThat(dto.durationMs()).isEqualTo(1000L);
    }
}
