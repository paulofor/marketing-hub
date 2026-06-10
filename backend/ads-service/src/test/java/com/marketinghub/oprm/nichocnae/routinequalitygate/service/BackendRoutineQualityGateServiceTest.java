package com.marketinghub.oprm.nichocnae.routinequalitygate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.completeStageExecution.CompleteRoutineQualityGateRequest;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.completeStageExecution.CompleteRoutineQualityGateResponse;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.pending.RecordRoutineQualityGatePending;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** Responsabilidade: validar contratos e persistência da etapa sete no backend OPRM NichoCNAE. */
class BackendRoutineQualityGateServiceTest {
  private final OprmRoutineResearchCycleRepository cycleRepository = mock(OprmRoutineResearchCycleRepository.class);
  private final OprmNicheRoutineCardRepository cardRepository = mock(OprmNicheRoutineCardRepository.class);
  private final OprmExtractedSignalRepository signalRepository = mock(OprmExtractedSignalRepository.class);
  private final OprmSourceSnapshotRepository snapshotRepository = mock(OprmSourceSnapshotRepository.class);
  private final OprmMeiAudienceProfileRepository meiAudienceProfileRepository = mock(OprmMeiAudienceProfileRepository.class);
  private final BackendRoutineQualityGateService service =
      new BackendRoutineQualityGateService(cycleRepository, cardRepository, signalRepository, snapshotRepository, meiAudienceProfileRepository);

  /** Deve montar pendência com aliases de tipos de sinais já gerados pela etapa cinco. */
  @Test
  void shouldListPendingWithSignalTypeAliases() {
    OprmNicheRoutineCard card = card();
    when(cardRepository.findByQualityCheckedAtIsNullOrderByCreatedAtAscIdAsc(any(Pageable.class))).thenReturn(List.of(card));
    when(meiAudienceProfileRepository.existsByResearchCycleId(1001L)).thenReturn(true);
    when(snapshotRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of(new OprmSourceSnapshot(), new OprmSourceSnapshot()));
    OprmSourceSnapshot riskSnapshot = new OprmSourceSnapshot();
    riskSnapshot.setSolutionLanguageRisk(true);
    OprmSourceSnapshot routineSnapshot = new OprmSourceSnapshot();
    routineSnapshot.setSolutionLanguageRisk(false);
    when(snapshotRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of(riskSnapshot, routineSnapshot));
    when(signalRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of(
        signal("CUSTOMER_QUESTION"), signal("PAIN_POINT"), signal("OPERATIONAL_FRICTION"), signal("LANGUAGE_MARKER"),
        signal("SOLUTION_LANGUAGE_RISK"), signal("MECHANISM_OPPORTUNITY"), signal("ROUTINE_TASK"), signal("COMMERCIAL_TASK")));

    List<RecordRoutineQualityGatePending> pending = service.listPending();

    assertThat(pending).hasSize(1);
    assertThat(pending.getFirst().sourceCount()).isEqualTo(2);
    assertThat(pending.getFirst().questionSignalCount()).isEqualTo(1);
    assertThat(pending.getFirst().painSignalCount()).isEqualTo(1);
    assertThat(pending.getFirst().commercialObjectCount()).isEqualTo(1);
    assertThat(pending.getFirst().operationalDifficultyCount()).isEqualTo(1);
    assertThat(pending.getFirst().languageMarkerCount()).isEqualTo(1);
    assertThat(pending.getFirst().solutionLanguageRiskCount()).isEqualTo(3);
    assertThat(pending.getFirst().routineEvidenceScore()).isEqualTo(86);
  }

  /** Deve persistir a decisão final no cartão e refletir o status de qualidade no ciclo. */
  @Test
  void shouldCompleteQualityGateAndUpdateCycleStatus() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(1001L);
    cycle.setStatus("ROUTINE_SYNTHESIZED");
    OprmNicheRoutineCard card = card();
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
    when(cardRepository.save(any(OprmNicheRoutineCard.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(cycleRepository.save(any(OprmRoutineResearchCycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CompleteRoutineQualityGateResponse response = service.complete(1001L, new CompleteRoutineQualityGateRequest(
        1001L, 10L, "LIGHTLY_RESEARCHED", true, 75, 80, 10, "Aprovado", "test"));

    assertThat(response.qualityStatus()).isEqualTo("LIGHTLY_RESEARCHED");
    assertThat(response.readyForHypothesis()).isTrue();
    assertThat(cycle.getStatus()).isEqualTo("LIGHTLY_RESEARCHED");
    assertThat(card.getQualityCheckedAt()).isNotNull();
    verify(cardRepository).save(card);
    verify(cycleRepository).save(cycle);
  }

  /** Cria um cartão de rotina mínimo para os testes do serviço da etapa sete. */
  private OprmNicheRoutineCard card() {
    OprmNicheRoutineCard card = new OprmNicheRoutineCard();
    card.setId(10L);
    card.setResearchCycleId(1001L);
    card.setNicheName("Cabeleireiros");
    card.setRoutineSummary("Rotina");
    card.setPainsSummary("Dores");
    card.setResultsSummary("Resultados");
    card.setMechanismOpportunitiesSummary("Mecanismos");
    card.setEvidenceSummary("Evidências");
    card.setSourceDomains("a.com,b.com");
    card.setConfidenceScore(70);
    card.setRoutineEvidenceScore(86);
    card.setDifficultyEvidenceScore(84);
    card.setSourceDiversityScore(72);
    card.setSolutionLanguageRiskScore(10);
    card.setSynthesizedBy("test");
    card.setCreatedAt(Instant.parse("2026-06-04T00:00:00Z"));
    return card;
  }

  /** Cria um sinal mínimo com o tipo informado para contagem de aliases. */
  private OprmExtractedSignal signal(String type) {
    OprmExtractedSignal signal = new OprmExtractedSignal();
    signal.setSignalType(type);
    return signal;
  }
}
