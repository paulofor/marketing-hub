package com.marketinghub.geralanding.wireframe.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.GeraLandingStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/** Responsável por registrar execuções iniciais da etapa de wireframe. */
@Service("geraLandingWireframeStageExecutionService")
public class GeraLandingWireframeStageExecutionService {

  private static final String STATUS_STARTED = "INICIADO";
  private final ExperimentRepository experimentRepository;
  private final GeraLandingStageExecutionRepository executionRepository;

  public GeraLandingWireframeStageExecutionService(
      ExperimentRepository experimentRepository,
      GeraLandingStageExecutionRepository executionRepository) {
    this.experimentRepository = experimentRepository;
    this.executionRepository = executionRepository;
  }

  /** Registra a execução inicial da etapa e retorna o contrato local para o módulo wireframe. */
  @Transactional
  public GeraLandingStageExecution registerInitialExecution(Long experimentId, String stageName) {
    Instant now = Instant.now();
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));

    GeraLandingStageExecution execution = new GeraLandingStageExecution();
    execution.setExperimentId(experiment.getId());
    execution.setExperiment(experiment);
    execution.setStageCode(stageName);
    execution.setExecutionRequestedAt(now);
    execution.setCreatedAt(now);
    execution.setPromptTemplateId("manual/start");
    execution.setPromptContent("Início manual via interface do experimento.");
    execution.setStatus(STATUS_STARTED);
    execution.setIdJob(toDatabaseIdJob(UUID.randomUUID().toString()));

    return executionRepository.save(execution);
  }

  /** Converte o identificador textual para formato binário persistido no banco. */
  private byte[] toDatabaseIdJob(String jobId) {
    return UUID.fromString(jobId).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  /** Converte o identificador binário persistido no banco para formato textual. */
  private String fromDatabaseIdJob(byte[] idJob) {
    return new String(idJob, java.nio.charset.StandardCharsets.UTF_8);
  }
}
