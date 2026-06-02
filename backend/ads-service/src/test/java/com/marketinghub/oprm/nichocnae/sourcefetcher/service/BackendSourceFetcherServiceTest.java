package com.marketinghub.oprm.nichocnae.sourcefetcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceCandidate;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution.CompleteSourceFetcherRequest;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution.CompleteSourceFetcherResponse;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.failStageExecution.FailSourceFetcherRequest;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceCandidateRepository;
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

/** Valida a persistência dos snapshots curtos da etapa quatro do OPRM nichocnae. */
@ExtendWith(MockitoExtension.class)
class BackendSourceFetcherServiceTest {
  @Mock private OprmSourceCandidateRepository sourceCandidateRepository;
  @Mock private OprmSourceSnapshotRepository sourceSnapshotRepository;
  @Mock private OprmRoutineResearchCycleRepository routineResearchCycleRepository;

  @InjectMocks private BackendSourceFetcherService service;

  /** Deve listar fontes encontradas ainda não selecionadas para coleta curta. */
  @Test
  void listPendingUsesFoundAndNotSelectedFilter() {
    when(sourceCandidateRepository
            .findByStatusAndSelectedForFetchFalseOrderByResearchCycleIdAscResearchQueryIdAscSearchPositionAscIdAsc(
                eq("FOUND"), any(Pageable.class)))
        .thenReturn(List.of(candidate()));

    var result = service.listPending();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().sourceCandidateId()).isEqualTo(301L);
    assertThat(result.getFirst().sourceUrl()).isEqualTo("https://exemplo.com/clientes-manicure");
  }

  /** Deve gravar snapshot, marcar a candidata como coletada e atualizar o total do ciclo. */
  @Test
  void completePersistsSnapshotAndUpdatesCandidateAndCycleTotals() {
    OprmSourceCandidate candidate = candidate();
    OprmRoutineResearchCycle cycle = cycle();
    when(sourceCandidateRepository.findById(301L)).thenReturn(Optional.of(candidate));
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(sourceSnapshotRepository.existsBySourceCandidateId(301L)).thenReturn(false);
    when(sourceSnapshotRepository.save(any(OprmSourceSnapshot.class)))
        .thenAnswer(invocation -> {
          OprmSourceSnapshot snapshot = invocation.getArgument(0);
          snapshot.setId(901L);
          return snapshot;
        });
    when(sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(1001L))
        .thenAnswer(invocation -> List.of(snapshot(901L)));

    CompleteSourceFetcherResponse response = service.complete(301L, validRequest());

    assertThat(response.sourceCandidateId()).isEqualTo(301L);
    assertThat(response.selectedForFetch()).isTrue();
    assertThat(response.relevanceScore()).isEqualTo(88);
    assertThat(response.cycleTotalSourceSnapshots()).isEqualTo(1);
    assertThat(response.snapshot().fetchStatus()).isEqualTo("COMPLETED");
    assertThat(response.snapshot().shortExcerpt()).contains("WhatsApp");

    ArgumentCaptor<OprmSourceCandidate> candidateCaptor = ArgumentCaptor.forClass(OprmSourceCandidate.class);
    verify(sourceCandidateRepository).save(candidateCaptor.capture());
    assertThat(candidateCaptor.getValue().getStatus()).isEqualTo("FETCHED");
    assertThat(candidateCaptor.getValue().getSelectedForFetch()).isTrue();
  }

  /** Deve rejeitar trecho longo para impedir persistência de HTML completo no MVP. */
  @Test
  void completeRejectsLongExcerpt() {
    CompleteSourceFetcherRequest request = new CompleteSourceFetcherRequest(
        "https://exemplo.com/clientes-manicure",
        "exemplo.com",
        "Como conseguir mais clientes para manicure",
        "PUBLIC_CONTENT",
        "Resumo",
        "x".repeat(1201),
        "COMPLETED",
        200,
        "SHORT_EXCERPT_ALLOWED",
        "PUBLIC_PAGE",
        88);

    assertThatThrownBy(() -> service.complete(301L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortExcerpt must contain at most 1200 characters");
    verify(sourceSnapshotRepository, never()).save(any());
  }

  /** Deve registrar rejeição da fonte candidata quando ela não merece coleta. */
  @Test
  void failMarksCandidateAsRejected() {
    OprmSourceCandidate candidate = candidate();
    when(sourceCandidateRepository.findById(301L)).thenReturn(Optional.of(candidate));

    service.fail(301L, new FailSourceFetcherRequest("domínio bloqueado", 10));

    ArgumentCaptor<OprmSourceCandidate> candidateCaptor = ArgumentCaptor.forClass(OprmSourceCandidate.class);
    verify(sourceCandidateRepository).save(candidateCaptor.capture());
    assertThat(candidateCaptor.getValue().getStatus()).isEqualTo("REJECTED");
    assertThat(candidateCaptor.getValue().getRejectionReason()).isEqualTo("domínio bloqueado");
    assertThat(candidateCaptor.getValue().getRelevanceScore()).isEqualTo(10);
  }

  /** Monta uma fonte candidata padrão para os testes da etapa quatro. */
  private OprmSourceCandidate candidate() {
    OprmSourceCandidate candidate = new OprmSourceCandidate();
    candidate.setId(301L);
    candidate.setResearchCycleId(1001L);
    candidate.setResearchQueryId(2001L);
    candidate.setSourceUrl("https://exemplo.com/clientes-manicure");
    candidate.setSourceTitle("Como conseguir mais clientes para manicure");
    candidate.setSourceSnippet("Veja formas de divulgar serviços e preencher horários.");
    candidate.setSourceDomain("exemplo.com");
    candidate.setSourceGroup("PUBLIC_CONTENT");
    candidate.setSearchProvider("BRAVE_SEARCH");
    candidate.setSearchPosition(1);
    candidate.setSelectedForFetch(false);
    candidate.setStatus("FOUND");
    candidate.setCreatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    candidate.setUpdatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    return candidate;
  }

  /** Monta um ciclo em execução para receber totais da etapa quatro. */
  private OprmRoutineResearchCycle cycle() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(1001L);
    cycle.setSourceNicheId(77L);
    cycle.setCnaeCode("9602501");
    cycle.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    cycle.setNicheName("Cabeleireiros, manicures e pedicures");
    cycle.setSourceScore(new BigDecimal("91.50"));
    cycle.setTriggerSource("AUTO_SCORE_QUEUE");
    cycle.setStatus("RUNNING");
    cycle.setTotalQueries(2);
    cycle.setTotalSourceCandidates(2);
    cycle.setTotalSourceSnapshots(0);
    cycle.setTotalExtractedSignals(0);
    cycle.setStartedAt(Instant.parse("2026-06-02T10:00:00Z"));
    cycle.setCreatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    cycle.setUpdatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    return cycle;
  }

  /** Monta um payload válido de snapshot curto coletado de página pública. */
  private CompleteSourceFetcherRequest validRequest() {
    return new CompleteSourceFetcherRequest(
        "https://exemplo.com/clientes-manicure",
        "exemplo.com",
        "Como conseguir mais clientes para manicure",
        "PUBLIC_CONTENT",
        "Veja formas de divulgar serviços e preencher horários.",
        "Manicures podem atrair mais clientes usando indicação, redes sociais, WhatsApp, pacotes mensais e lembretes de retorno.",
        "COMPLETED",
        200,
        "SHORT_EXCERPT_ALLOWED",
        "PUBLIC_PAGE",
        88);
  }

  /** Monta um snapshot persistido para simular a contagem agregada do ciclo. */
  private OprmSourceSnapshot snapshot(Long id) {
    OprmSourceSnapshot snapshot = new OprmSourceSnapshot();
    snapshot.setId(id);
    snapshot.setResearchCycleId(1001L);
    snapshot.setSourceCandidateId(301L);
    snapshot.setSourceUrl("https://exemplo.com/clientes-manicure");
    snapshot.setSourceDomain("exemplo.com");
    snapshot.setSourceTitle("Como conseguir mais clientes para manicure");
    snapshot.setSourceType("PUBLIC_CONTENT");
    snapshot.setSnippet("Veja formas de divulgar serviços e preencher horários.");
    snapshot.setShortExcerpt("Manicures podem atrair mais clientes usando indicação e WhatsApp.");
    snapshot.setFetchedAt(Instant.parse("2026-06-02T10:05:00Z"));
    snapshot.setFetchStatus("COMPLETED");
    snapshot.setHttpStatus(200);
    snapshot.setStoragePolicy("SHORT_EXCERPT_ALLOWED");
    snapshot.setLicenseState("PUBLIC_PAGE");
    snapshot.setCreatedAt(Instant.parse("2026-06-02T10:05:00Z"));
    return snapshot;
  }
}
