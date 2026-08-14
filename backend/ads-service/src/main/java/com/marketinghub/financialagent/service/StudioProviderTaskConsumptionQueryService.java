package com.marketinghub.financialagent.service;

import com.marketinghub.financialagent.StudioProviderTaskConsumption;
import com.marketinghub.repository.jpa.financialagent.StudioProviderTaskConsumptionRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: oferecer consultas de consumo de providers por contratos imutáveis. */
@Service
public class StudioProviderTaskConsumptionQueryService {
  private final StudioProviderTaskConsumptionRepository repository;

  /** Inicializa a consulta com a fonte canônica de consumo por task. */
  public StudioProviderTaskConsumptionQueryService(
      StudioProviderTaskConsumptionRepository repository) {
    this.repository = repository;
  }

  /** Lista consumos dos jobs na ordem editorial e converte entidades em contratos de leitura. */
  @Transactional(readOnly = true)
  public List<ProviderTaskConsumptionView> findBySalesVideoJobIds(Collection<Long> jobIds) {
    return repository.findBySalesVideoJobIdInOrderBySceneNumberAscAcceptedAtAsc(jobIds).stream()
        .map(this::toView)
        .toList();
  }

  /** Converte uma entidade financeira no contrato mínimo necessário aos consumidores. */
  private ProviderTaskConsumptionView toView(StudioProviderTaskConsumption task) {
    return new ProviderTaskConsumptionView(
        task.getId(),
        task.getSalesVideoJobId(),
        task.getProviderTaskId(),
        task.getSceneNumber(),
        task.getPlannedSceneCount(),
        task.getDurationSeconds(),
        task.getEstimatedCredits(),
        task.getBilledCredits(),
        task.getCommercialEvaluationStatus(),
        task.getCommercialUtilizationPercent(),
        task.getCommercialEvaluationNotes(),
        task.getCommercialEvaluatedBy(),
        task.getCommercialEvaluatedAt(),
        task.getAcceptedAt());
  }

  /** Persiste a avaliação comercial editorial de uma task produzida. */
  @Transactional
  public ProviderTaskConsumptionView evaluate(
      Long taskId,
      String requestedStatus,
      Integer utilizationPercent,
      String notes,
      String evaluatedBy) {
    StudioProviderTaskConsumption task =
        repository
            .findById(taskId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cena do storyboard não encontrada."));
    String status = requestedStatus.trim().toUpperCase(java.util.Locale.ROOT);
    if (!java.util.Set.of("APPROVED", "PARTIAL", "REJECTED").contains(status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avaliação comercial inválida.");
    }
    if (utilizationPercent < 0 || utilizationPercent > 100) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Aproveitamento deve ficar entre 0 e 100.");
    }
    task.setCommercialEvaluationStatus(status);
    task.setCommercialUtilizationPercent(utilizationPercent);
    task.setCommercialEvaluationNotes(notes == null ? null : notes.trim());
    task.setCommercialEvaluatedBy(evaluatedBy.trim());
    task.setCommercialEvaluatedAt(Instant.now());
    return toView(repository.save(task));
  }
}
