package com.marketinghub.geralanding;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeraLandingStageExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingStageExecutionService.class);

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
                .idJob(UUID.randomUUID().toString())
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
                .idJob(UUID.randomUUID().toString())
                .build();
        executionRepository.save(execution);
    }

    @Transactional
    public void receivePrompt(String idJob, GeraLandingPromptReceiveRequest request) {
        log.info(
                "Receiving gera-landing prompt. idJob={}, experimentId={}, stageCode={}, promptLength={}",
                idJob,
                request.experimentId(),
                request.stageCode(),
                request.prompt() != null ? request.prompt().length() : 0);

        GeraLandingStageExecution execution = executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(idJob)
                .orElseThrow(() -> {
                    log.error(
                            "GeraLanding execution not found before prompt persistence. idJob={}, experimentId={}, stageCode={}",
                            idJob,
                            request.experimentId(),
                            request.stageCode());
                    return new EntityNotFoundException("GeraLanding execution not found for idJob: " + idJob);
                });

        log.info(
                "GeraLanding execution found. idJob={}, persistedExperimentId={}, persistedStageCode={}, previousStatus={}",
                idJob,
                execution.getExperimentId(),
                execution.getStageCode(),
                execution.getStatus());

        Instant now = Instant.now();
        execution.setPrompt(request.prompt());
        execution.setProcessingStartedAt(now);
        execution.setStatus("EM_PROCESSAMENTO");
        executionRepository.save(execution);
        log.info("GeraLanding prompt persisted. idJob={}, newStatus={}", idJob, execution.getStatus());
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
