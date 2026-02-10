package com.marketinghub.facebookads.playbook.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJob;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJobApiLog;
import com.marketinghub.facebookads.playbook.ExperimentAdSetSpec;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflow;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflowStatus;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetJobApiLogDto;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetJobDetailDto;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetJobDto;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetSpecDto;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetWorkflowDto;
import com.marketinghub.facebookads.playbook.dto.StartExperimentAdSetWorkflowRequest;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetJobApiLogRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetJobRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetSpecRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetWorkflowRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Facade used by the REST layer to inspect and (re)start workflows.
 */
@Service
public class ExperimentAdSetWorkflowService {

    private final ExperimentRepository experimentRepository;
    private final ExperimentAdSetWorkflowRepository workflowRepository;
    private final ExperimentAdSetJobRepository jobRepository;
    private final ExperimentAdSetSpecRepository specRepository;
    private final ExperimentAdSetJobApiLogRepository jobApiLogRepository;
    private final ExperimentAdSetWorkflowJobCoordinator coordinator;

    public ExperimentAdSetWorkflowService(ExperimentRepository experimentRepository,
                                          ExperimentAdSetWorkflowRepository workflowRepository,
                                          ExperimentAdSetJobRepository jobRepository,
                                          ExperimentAdSetSpecRepository specRepository,
                                          ExperimentAdSetJobApiLogRepository jobApiLogRepository,
                                          ExperimentAdSetWorkflowJobCoordinator coordinator) {
        this.experimentRepository = experimentRepository;
        this.workflowRepository = workflowRepository;
        this.jobRepository = jobRepository;
        this.specRepository = specRepository;
        this.jobApiLogRepository = jobApiLogRepository;
        this.coordinator = coordinator;
    }

    @Transactional
    public ExperimentAdSetWorkflowDto start(Long experimentId, StartExperimentAdSetWorkflowRequest request) {
        ExperimentAdSetWorkflow workflow = getOrCreateWorkflow(experimentId);
        boolean restart = request != null && request.restart();
        if (restart) {
            jobRepository.deleteByWorkflowId(workflow.getId());
            specRepository.deleteByWorkflowId(workflow.getId());
            workflow.resetForRestart();
        }
        if (workflow.getStatus() == ExperimentAdSetWorkflowStatus.NOT_STARTED || restart
            || workflow.getStatus() == ExperimentAdSetWorkflowStatus.FAILED) {
            workflowRepository.save(workflow);
            coordinator.initializeWorkflow(workflow);
        }
        return buildDto(workflow);
    }

    @Transactional
    public ExperimentAdSetWorkflowDto getDetails(Long experimentId) {
        ExperimentAdSetWorkflow workflow = getOrCreateWorkflow(experimentId);
        return buildDto(workflow);
    }

    private ExperimentAdSetWorkflow getOrCreateWorkflow(Long experimentId) {
        return workflowRepository.findByExperimentId(experimentId)
                .orElseGet(() -> createWorkflow(experimentId));
    }

    private ExperimentAdSetWorkflow createWorkflow(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experimento %d não encontrado".formatted(experimentId)));
        ExperimentAdSetWorkflow workflow = ExperimentAdSetWorkflow.builder()
                .experiment(experiment)
                .status(ExperimentAdSetWorkflowStatus.NOT_STARTED)
                .build();
        return workflowRepository.save(workflow);
    }

    @Transactional
    public ExperimentAdSetJobDetailDto getJobDetail(Long experimentId, Long jobId) {
        ExperimentAdSetWorkflow workflow = getOrCreateWorkflow(experimentId);
        ExperimentAdSetJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job %d não encontrado".formatted(jobId)));
        if (job.getWorkflow() == null || !job.getWorkflow().getId().equals(workflow.getId())) {
            throw new EntityNotFoundException("Job %d não pertence ao experimento %d".formatted(jobId, experimentId));
        }
        List<ExperimentAdSetJobApiLogDto> apiLogs = jobApiLogRepository.findByJobIdOrderByCreatedAtAsc(jobId)
                .stream()
                .map(this::toApiLogDto)
                .collect(Collectors.toList());
        return new ExperimentAdSetJobDetailDto(toJobDto(job), apiLogs);
    }

    private ExperimentAdSetWorkflowDto buildDto(ExperimentAdSetWorkflow workflow) {
        List<ExperimentAdSetJob> jobs = jobRepository.findByWorkflowId(workflow.getId());
        List<ExperimentAdSetSpec> specs = specRepository.findByWorkflowId(workflow.getId());
        List<ExperimentAdSetJobDto> jobDtos = jobs.stream()
                .sorted(Comparator.comparing(ExperimentAdSetJob::getId))
                .map(this::toJobDto)
                .toList();
        List<ExperimentAdSetSpecDto> specDtos = specs.stream()
                .sorted(Comparator.comparing(spec -> spec.getSlot() != null ? spec.getSlot().ordinal() : Integer.MAX_VALUE))
                .map(spec -> new ExperimentAdSetSpecDto(
                        spec.getId(),
                        spec.getSlot(),
                        spec.getLabel(),
                        spec.getAgeMin(),
                        spec.getAgeMax(),
                        spec.getTargetingSpec(),
                        spec.getValidationStatus(),
                        spec.getValidationResponse(),
                        spec.getReachStatus(),
                        spec.getReachLowerBound(),
                        spec.getReachUpperBound(),
                        spec.getCreatedAt(),
                        spec.getUpdatedAt()
                ))
                .toList();
        return new ExperimentAdSetWorkflowDto(
                workflow.getId(),
                workflow.getExperiment() != null ? workflow.getExperiment().getId() : null,
                workflow.getStatus(),
                workflow.getSeedKeyword(),
                workflow.getSeedLocale(),
                workflow.getSeedInterestId(),
                workflow.getSeedInterestName(),
                workflow.getSeedAudienceLower(),
                workflow.getSeedAudienceUpper(),
                workflow.getAiNotes(),
                workflow.getLastError(),
                workflow.getCompletedAt(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt(),
                jobDtos,
                specDtos
        );
    }

    private ExperimentAdSetJobDto toJobDto(ExperimentAdSetJob job) {
        return new ExperimentAdSetJobDto(
                job.getId(),
                job.getType(),
                job.getWorker(),
                job.getStatus(),
                job.getAttemptCount(),
                job.getLockedBy(),
                job.getLockedAt(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getErrorMessage(),
                job.getResourceId(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private ExperimentAdSetJobApiLogDto toApiLogDto(ExperimentAdSetJobApiLog log) {
        return new ExperimentAdSetJobApiLogDto(
                log.getId(),
                log.getProvider(),
                log.getEndpoint(),
                log.getHttpMethod(),
                log.getStatusCode(),
                log.getRequestPayload(),
                log.getResponsePayload(),
                log.getErrorMessage(),
                log.getRequestedAt(),
                log.getRespondedAt(),
                log.getCreatedAt()
        );
    }

}
