package com.marketinghub.oprm.nichocnae.signalextractor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.CompleteSignalExtractorRequest;
import com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.CompleteSignalExtractorResponse;
import com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.SignalExtractionItemRequest;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** Valida a persistência dos sinais estruturados da etapa cinco do OPRM nichocnae. */
@ExtendWith(MockitoExtension.class)
class BackendSignalExtractorServiceTest {
  @Mock private OprmSourceSnapshotRepository sourceSnapshotRepository;
  @Mock private OprmExtractedSignalRepository extractedSignalRepository;
  @Mock private OprmRoutineResearchCycleRepository routineResearchCycleRepository;

  @InjectMocks private BackendSignalExtractorService service;

  /** Deve listar snapshots coletados e ainda pendentes de extração. */
  @Test
  void listPendingUsesCompletedAndPendingFilters() {
    when(sourceSnapshotRepository.findByFetchStatusAndSignalExtractionStatusOrderByResearchCycleIdAscIdAsc(
            eq("COMPLETED"), eq("PENDING"), any(Pageable.class)))
        .thenReturn(List.of(snapshot()));
    when(extractedSignalRepository.existsBySourceSnapshotId(901L)).thenReturn(false);

    var result = service.listPending();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().sourceSnapshotId()).isEqualTo(901L);
    assertThat(result.getFirst().shortExcerpt()).contains("agenda");
  }

  /** Deve persistir sinais, marcar snapshot como concluído e atualizar total do ciclo. */
  @Test
  void completePersistsSignalsAndUpdatesSnapshotAndCycleTotals() {
    OprmSourceSnapshot snapshot = snapshot();
    OprmRoutineResearchCycle cycle = cycle();
    when(sourceSnapshotRepository.findById(901L)).thenReturn(Optional.of(snapshot));
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(extractedSignalRepository.existsBySourceSnapshotId(901L)).thenReturn(false);
    when(extractedSignalRepository.save(any(OprmExtractedSignal.class)))
        .thenAnswer(invocation -> {
          OprmExtractedSignal signal = invocation.getArgument(0);
          signal.setId(signal.getSignalType().equals("ROUTINE_TASK") ? 7001L : 7002L);
          return signal;
        });
    when(extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(1001L))
        .thenReturn(List.of(signal(7001L), signal(7002L)));

    CompleteSignalExtractorResponse response = service.complete(901L, validRequest());

    assertThat(response.sourceSnapshotId()).isEqualTo(901L);
    assertThat(response.signalExtractionStatus()).isEqualTo("COMPLETED");
    assertThat(response.extractedSignalCount()).isEqualTo(2);
    assertThat(response.cycleTotalExtractedSignals()).isEqualTo(2);
    assertThat(response.signals()).extracting("signalType").contains("ROUTINE_TASK", "PAIN_POINT");

    ArgumentCaptor<OprmSourceSnapshot> snapshotCaptor = ArgumentCaptor.forClass(OprmSourceSnapshot.class);
    verify(sourceSnapshotRepository).save(snapshotCaptor.capture());
    assertThat(snapshotCaptor.getValue().getSignalExtractionStatus()).isEqualTo("COMPLETED");
    assertThat(snapshotCaptor.getValue().getSignalExtractionError()).isNull();
  }

  /** Deve rejeitar payload sem sinais para não avançar a etapa cinco com saída inútil. */
  @Test
  void completeRejectsEmptySignals() {
    CompleteSignalExtractorRequest request = new CompleteSignalExtractorRequest(
        1001L, 301L, "exemplo.com", "COMPLETED", "test", List.of());

    assertThatThrownBy(() -> service.complete(901L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("signals must contain at least one item");
    verify(extractedSignalRepository, never()).save(any(OprmExtractedSignal.class));
  }

  /** Cria um snapshot curto coletado para os testes da etapa cinco. */
  private OprmSourceSnapshot snapshot() {
    OprmSourceSnapshot snapshot = new OprmSourceSnapshot();
    snapshot.setId(901L);
    snapshot.setResearchCycleId(1001L);
    snapshot.setSourceCandidateId(301L);
    snapshot.setSourceUrl("https://exemplo.com/clientes-manicure");
    snapshot.setSourceDomain("exemplo.com");
    snapshot.setSourceTitle("Como lotar agenda de manicure");
    snapshot.setSourceType("PUBLIC_CONTENT");
    snapshot.setSnippet("Resumo sobre agenda, clientes e WhatsApp");
    snapshot.setShortExcerpt("Profissionais usam WhatsApp para reduzir faltas na agenda e fidelizar clientes.");
    snapshot.setFetchedAt(Instant.parse("2026-06-04T00:00:00Z"));
    snapshot.setFetchStatus("COMPLETED");
    snapshot.setHttpStatus(200);
    snapshot.setStoragePolicy("SHORT_EXCERPT_ALLOWED");
    snapshot.setLicenseState("PUBLIC_PAGE");
    snapshot.setSignalExtractionStatus("PENDING");
    snapshot.setCreatedAt(Instant.parse("2026-06-04T00:00:00Z"));
    return snapshot;
  }

  /** Cria um ciclo mínimo para atualização dos contadores da etapa cinco. */
  private OprmRoutineResearchCycle cycle() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(1001L);
    cycle.setSourceNicheId(501L);
    cycle.setCnaeCode("9602501");
    cycle.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    cycle.setNicheName("Manicure");
    cycle.setSourceScore(new BigDecimal("90.00"));
    cycle.setTriggerSource("SCHEDULER");
    cycle.setStatus("RUNNING");
    cycle.setTotalQueries(15);
    cycle.setTotalSourceCandidates(70);
    cycle.setTotalSourceSnapshots(1);
    cycle.setTotalExtractedSignals(0);
    cycle.setStartedAt(Instant.parse("2026-06-04T00:00:00Z"));
    cycle.setCreatedAt(Instant.parse("2026-06-04T00:00:00Z"));
    cycle.setUpdatedAt(Instant.parse("2026-06-04T00:00:00Z"));
    return cycle;
  }

  /** Cria um payload válido com sinais comerciais úteis. */
  private CompleteSignalExtractorRequest validRequest() {
    return new CompleteSignalExtractorRequest(
        1001L,
        301L,
        "exemplo.com",
        "COMPLETED",
        "test",
        List.of(
            new SignalExtractionItemRequest(
                "ROUTINE_TASK", "Confirmar agenda pelo WhatsApp", "Profissionais usam WhatsApp para reduzir faltas", 88),
            new SignalExtractionItemRequest(
                "PAIN_POINT", "Faltas quebram previsibilidade da agenda", "reduzir faltas na agenda e fidelizar clientes", 84)));
  }

  /** Cria um sinal persistido para cálculo de total em detalhe. */
  private OprmExtractedSignal signal(Long id) {
    OprmExtractedSignal signal = new OprmExtractedSignal();
    signal.setId(id);
    signal.setResearchCycleId(1001L);
    signal.setSourceSnapshotId(901L);
    signal.setSourceCandidateId(301L);
    signal.setSignalType(id.equals(7001L) ? "ROUTINE_TASK" : "PAIN_POINT");
    signal.setSignalText(id.equals(7001L) ? "Confirmar agenda pelo WhatsApp" : "Faltas quebram previsibilidade da agenda");
    signal.setEvidenceExcerpt("Profissionais usam WhatsApp para reduzir faltas");
    signal.setSourceDomain("exemplo.com");
    signal.setConfidenceScore(88);
    signal.setCreatedBy("test");
    signal.setCreatedAt(Instant.parse("2026-06-04T00:00:00Z"));
    return signal;
  }
}
