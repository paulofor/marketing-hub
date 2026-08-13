package com.marketinghub.financialagent.service;

import com.marketinghub.financialagent.StudioProviderTaskConsumption;
import com.marketinghub.repository.jpa.financialagent.StudioProviderTaskConsumptionRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        task.getSalesVideoJobId(),
        task.getProviderTaskId(),
        task.getSceneNumber(),
        task.getPlannedSceneCount(),
        task.getDurationSeconds(),
        task.getEstimatedCredits(),
        task.getBilledCredits(),
        task.getAcceptedAt());
  }
}
