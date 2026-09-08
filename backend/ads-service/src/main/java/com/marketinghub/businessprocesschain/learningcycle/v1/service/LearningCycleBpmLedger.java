package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: materializar cada passagem do ciclo como uma instância BPM auditável. */
@Component
@RequiredArgsConstructor
public class LearningCycleBpmLedger {
  private final BusinessProcessActivityInstanceRepository instances;
  private final BusinessProcessActivityDefinitionRepository activities;

  /** Abre a etapa liberada pelo backend sem fabricar tarefa de agente ou gasto. */
  public void open(LearningSalesCycle cycle, Instant now) {
    var definition =
        activities
            .findByProcessDefinitionIdAndActivityId(
                cycle.getProcessDefinitionId(), cycle.getStage())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Atividade do ciclo não foi instalada: " + cycle.getStage()));
    var instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(definition);
    instance.setSourceReference("experiment:" + cycle.getExperimentId());
    instance.setOccurrenceNumber((int) cycle.getRevision() + 1);
    instance.setStatus("PENDING");
    instance.setEnteredAt(now);
    instance.setObjectiveAchieved(false);
    instance.setKnownCostUsd(BigDecimal.ZERO);
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("NOT_RECORDED");
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    cycle.setCurrentInstanceId(instances.saveAndFlush(instance).getId());
  }

  /** Fecha a ocorrência atual preservando evidência, reprovação e horário da decisão. */
  public void finish(
      LearningSalesCycle cycle, String evidence, String status, String reason, Instant now) {
    var instance = instances.findById(cycle.getCurrentInstanceId()).orElseThrow();
    instance.setStatus(status);
    instance.setObjectiveAchieved("COMPLETED".equals(status));
    instance.setObjectiveEvidenceJson(evidence);
    instance.setBlockedReason("BLOCKED".equals(status) ? reason : null);
    instance.setEvidenceQuality("HUMAN_RECORDED");
    instance.setExitedAt(now);
    instance.setUpdatedAt(now);
    instances.save(instance);
  }
}
