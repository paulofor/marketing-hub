package com.marketinghub.geralanding;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

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
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode("landing-page-wireframe")
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
        Experiment experiment = experimentRepository.findById(request.experimentId())
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + request.experimentId()));

        GeraLandingStageExecution execution = GeraLandingStageExecution.builder()
                .experimentId(experiment.getId())
                .experiment(experiment)
                .stageCode(request.stageCode())
                .promptTemplateId(request.executionId())
                .promptContent(request.promptContent())
                .status("INICIADO")
                .idJob(UUID.randomUUID())
                .build();
        executionRepository.save(execution);
    }
}
