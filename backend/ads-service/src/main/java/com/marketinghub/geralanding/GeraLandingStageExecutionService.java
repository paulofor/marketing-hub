package com.marketinghub.geralanding;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeraLandingStageExecutionService {

    private final ExperimentRepository experimentRepository;
    private final GeraLandingStageExecutionRepository executionRepository;

    public GeraLandingStageExecutionService(
            ExperimentRepository experimentRepository,
            GeraLandingStageExecutionRepository executionRepository) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
    }

    @Transactional
    public GeraLandingStartResponse registerInitialExecution(Long experimentId) {
        Instant now = Instant.now();
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode("landing-page-wireframe")
                .executionRequestedAt(now)
                .createdAt(now)
                .promptTemplateId("manual/start")
                .promptContent("Início manual via interface do experimento.")
                .status("INICIADO")
                .idJob(UUID.randomUUID())
                .build();
        GeraLandingStageExecution saved = executionRepository.save(execution);
        return new GeraLandingStartResponse(saved.getIdJob().toString(), saved.getStatus());
    }

    @Transactional
    public void registerWorkerPromptExecution(GeraLandingWorkerPromptRequest request) {
        Instant now = Instant.now();
        Experiment experiment = experimentRepository.findById(request.experimentId())
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + request.experimentId()));

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode(request.stageCode())
                .executionRequestedAt(now)
                .createdAt(now)
                .promptTemplateId(request.executionId())
                .promptContent(request.promptContent())
                .status("INICIADO")
                .idJob(UUID.randomUUID())
                .build();
        executionRepository.save(execution);
    }

    @Transactional
    public void receivePrompt(UUID idJob, GeraLandingPromptReceiveRequest request) {
        GeraLandingStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(idJob)
                .orElseThrow(() -> new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob));

        Instant now = Instant.now();
        execution.setPrompt(request.prompt());
        execution.setProcessingStartedAt(now);
        execution.setStatus("EM_PROCESSAMENTO");
        executionRepository.save(execution);
    }

    @Transactional(readOnly = true)
    public List<GeraLandingPendingExecutionResponse> listPendingExecutions() {
        return executionRepository.findTop20ByStatusOrderByExecutionRequestedAtAsc("INICIADO")
                .stream()
                .map(execution -> new GeraLandingPendingExecutionResponse(
                        execution.getExperimentId(),
                        execution.getIdJob(),
                        execution.getStageCode()))
                .toList();
    }
}
