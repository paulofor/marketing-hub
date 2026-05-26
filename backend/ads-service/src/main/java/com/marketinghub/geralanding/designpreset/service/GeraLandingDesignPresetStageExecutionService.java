package com.marketinghub.geralanding.designpreset.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.GeraLandingStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsável por registrar execuções iniciais da etapa no domínio local do GeraLanding. */
@Service
public class GeraLandingDesignPresetStageExecutionService {
  private static final String STATUS_STARTED = "INICIADO";

  private final ExperimentRepository experimentRepository;
  private final GeraLandingStageExecutionRepository executionRepository;

  public GeraLandingDesignPresetStageExecutionService(
      ExperimentRepository experimentRepository,
      GeraLandingStageExecutionRepository executionRepository) {
    this.experimentRepository = experimentRepository;
    this.executionRepository = executionRepository;
  }

  /** Registra a execução inicial da etapa e devolve os dados para acompanhamento. */
  @Transactional
  public GeraLandingDesignPresetStartResponse registerInitialExecution(Long experimentId, String stageCode) {
    Instant now = Instant.now();
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));

    GeraLandingStageExecution execution =
        GeraLandingStageExecution.builder()
            .experimentId(experiment.getId())
            .experiment(experiment)
            .stageCode(stageCode)
            .executionRequestedAt(now)
            .createdAt(now)
            .promptTemplateId("manual/start")
            .promptContent("Início manual via interface do experimento.")
            .status(STATUS_STARTED)
            .idJob(toDatabaseIdJob(UUID.randomUUID().toString()))
            .build();

    GeraLandingStageExecution saved = executionRepository.save(execution);
    return new GeraLandingDesignPresetStartResponse(fromDatabaseIdJob(saved.getIdJob()), saved.getStatus());
  }

  /** Remove prefixo interno de jobId antes de expor o valor para os clientes. */
  private byte[] toDatabaseIdJob(String idJob) {
    return idJob.getBytes(StandardCharsets.UTF_8);
  }

  /** Converte o id do formato persistido para o formato textual da API. */
  private String fromDatabaseIdJob(byte[] idJob) {
    return new String(idJob, StandardCharsets.UTF_8);
  }
}
