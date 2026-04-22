package com.marketinghub.facebookads.playbook.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflow;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflowStatus;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetWorkflowDto;
import com.marketinghub.facebookads.playbook.dto.StartExperimentAdSetWorkflowRequest;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetJobApiLogRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetJobRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetSpecRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetWorkflowRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentAdSetWorkflowServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private ExperimentAdSetWorkflowRepository workflowRepository;
    @Mock
    private ExperimentAdSetJobRepository jobRepository;
    @Mock
    private ExperimentAdSetSpecRepository specRepository;
    @Mock
    private ExperimentAdSetJobApiLogRepository jobApiLogRepository;
    @Mock
    private ExperimentAdSetWorkflowJobCoordinator coordinator;

    private ExperimentAdSetWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentAdSetWorkflowService(
                experimentRepository,
                workflowRepository,
                jobRepository,
                specRepository,
                jobApiLogRepository,
                coordinator);
    }

    @Test
    void startRejectsValidationForInvalidatedExperiment() {
        Experiment experiment = new Experiment();
        experiment.setId(77L);
        experiment.setStatus(ExperimentStatus.INVALIDATED);
        ExperimentAdSetWorkflow workflow = ExperimentAdSetWorkflow.builder()
                .id(901L)
                .experiment(experiment)
                .status(ExperimentAdSetWorkflowStatus.NOT_STARTED)
                .build();

        when(workflowRepository.findByExperimentId(77L)).thenReturn(Optional.of(workflow));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.start(77L, new StartExperimentAdSetWorkflowRequest(false)));

        assertEquals(409, error.getStatusCode().value());
        verify(coordinator, never()).initializeWorkflow(any());
    }

    @Test
    void getDetailsIncludesExperimentStatusForUiGating() {
        Experiment experiment = new Experiment();
        experiment.setId(88L);
        experiment.setStatus(ExperimentStatus.USER_STOPPED);
        ExperimentAdSetWorkflow workflow = ExperimentAdSetWorkflow.builder()
                .id(902L)
                .experiment(experiment)
                .status(ExperimentAdSetWorkflowStatus.FAILED)
                .build();

        when(workflowRepository.findByExperimentId(88L)).thenReturn(Optional.of(workflow));
        when(jobRepository.findByWorkflowId(902L)).thenReturn(List.of());
        when(specRepository.findByWorkflowId(902L)).thenReturn(List.of());

        ExperimentAdSetWorkflowDto details = service.getDetails(88L);

        assertEquals(ExperimentStatus.USER_STOPPED, details.experimentStatus());
    }
}
