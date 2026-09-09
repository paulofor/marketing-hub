package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Valida a procedência registrada pelo ledger nas ocorrências automáticas do ciclo. */
class LearningCycleBpmLedgerTest {

  /** Persiste a fotografia automática como saída funcional e não como declaração humana. */
  @Test
  void identifiesAutomaticMeasurementEvidence() {
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var activities = mock(BusinessProcessActivityDefinitionRepository.class);
    var ledger = new LearningCycleBpmLedger(instances, activities);
    var cycle = new LearningSalesCycle();
    cycle.setCurrentInstanceId(73L);
    var instance = new BusinessProcessActivityInstance();
    when(instances.findById(73L)).thenReturn(Optional.of(instance));
    Instant now = Instant.parse("2026-09-09T02:00:00Z");

    ledger.finishAutomaticMeasurement(
        cycle, "{\"automatic\":true,\"sessions\":4}", "COMPLETED", "Fonte conciliada", now);

    assertThat(instance.getObjectiveEvidenceJson())
        .isEqualTo("{\"automatic\":true,\"sessions\":4}");
    assertThat(instance.getEvidenceQuality()).isEqualTo("AUTOMATIC");
    assertThat(instance.isObjectiveAchieved()).isTrue();
    assertThat(instance.getExitedAt()).isEqualTo(now);
    verify(instances).save(instance);
  }
}
