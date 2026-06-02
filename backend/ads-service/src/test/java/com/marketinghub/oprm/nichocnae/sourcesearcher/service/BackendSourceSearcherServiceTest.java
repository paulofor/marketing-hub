package com.marketinghub.oprm.nichocnae.sourcesearcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceCandidate;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.CompleteSourceSearcherRequest;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.CompleteSourceSearcherResponse;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.SourceCandidateRequest;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.failStageExecution.FailSourceSearcherRequest;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmResearchQueryRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceCandidateRepository;
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

/** Valida a persistência das fontes candidatas da etapa três do OPRM nichocnae. */
@ExtendWith(MockitoExtension.class)
class BackendSourceSearcherServiceTest {
  @Mock private OprmResearchQueryRepository researchQueryRepository;
  @Mock private OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  @Mock private OprmSourceCandidateRepository sourceCandidateRepository;

  @InjectMocks private BackendSourceSearcherService service;

  /** Deve listar queries pendentes em ordem operacional para execução pelo provedor de busca. */
  @Test
  void listPendingUsesResearchQueryPendingFilter() {
    when(researchQueryRepository.findByStatusOrderByPriorityAscIdAsc(eq("PENDING"), any(Pageable.class)))
        .thenReturn(List.of(query()));

    var result = service.listPending();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().researchQueryId()).isEqualTo(2001L);
    assertThat(result.getFirst().queryText()).isEqualTo("como lotar agenda de manicure");
  }

  /** Deve gravar candidatos, concluir a query e atualizar o total de fontes candidatas do ciclo. */
  @Test
  void completePersistsCandidatesAndUpdatesQueryAndCycleTotals() {
    OprmResearchQuery query = query();
    OprmRoutineResearchCycle cycle = cycle();
    when(researchQueryRepository.findById(2001L)).thenReturn(Optional.of(query));
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(sourceCandidateRepository.existsByResearchQueryIdAndSourceUrl(eq(2001L), any())).thenReturn(false);
    when(sourceCandidateRepository.saveAll(any()))
        .thenAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          List<OprmSourceCandidate> candidates = invocation.getArgument(0);
          for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).setId((long) i + 10);
          }
          return candidates;
        });
    when(sourceCandidateRepository.findByResearchCycleIdOrderByResearchQueryIdAscSearchPositionAscIdAsc(1001L))
        .thenAnswer(invocation -> List.of(candidate(10L, 1), candidate(11L, 2)));

    CompleteSourceSearcherResponse response = service.complete(2001L, validRequest());

    assertThat(response.queryStatus()).isEqualTo("COMPLETED");
    assertThat(response.resultCount()).isEqualTo(2);
    assertThat(response.cycleTotalSourceCandidates()).isEqualTo(2);
    assertThat(response.candidates()).extracting("status").containsOnly("FOUND");

    ArgumentCaptor<OprmResearchQuery> queryCaptor = ArgumentCaptor.forClass(OprmResearchQuery.class);
    verify(researchQueryRepository).save(queryCaptor.capture());
    assertThat(queryCaptor.getValue().getStatus()).isEqualTo("COMPLETED");
    assertThat(queryCaptor.getValue().getResultCount()).isEqualTo(2);
  }

  /** Deve rejeitar resultados com URL duplicada para evitar fontes repetidas na mesma query. */
  @Test
  void completeRejectsDuplicatedUrlsInPayload() {
    when(researchQueryRepository.findById(2001L)).thenReturn(Optional.of(query()));
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle()));
    CompleteSourceSearcherRequest request = new CompleteSourceSearcherRequest(
        "BRAVE_SEARCH",
        List.of(
            new SourceCandidateRequest("https://exemplo.com/a", "A", "Resumo", "exemplo.com", null, 1, null),
            new SourceCandidateRequest("https://exemplo.com/a", "A", "Resumo", "exemplo.com", null, 2, null)));

    assertThatThrownBy(() -> service.complete(2001L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("duplicated sourceUrl");
    verify(sourceCandidateRepository, never()).saveAll(any());
  }

  /** Deve marcar apenas a query como falha quando o provedor de busca não consegue executar a frase. */
  @Test
  void failMarksQueryAsFailed() {
    OprmResearchQuery query = query();
    when(researchQueryRepository.findById(2001L)).thenReturn(Optional.of(query));

    service.fail(2001L, new FailSourceSearcherRequest("Brave Search indisponível"));

    ArgumentCaptor<OprmResearchQuery> queryCaptor = ArgumentCaptor.forClass(OprmResearchQuery.class);
    verify(researchQueryRepository).save(queryCaptor.capture());
    assertThat(queryCaptor.getValue().getStatus()).isEqualTo("FAILED");
    assertThat(queryCaptor.getValue().getErrorMessage()).isEqualTo("Brave Search indisponível");
  }

  /** Monta uma query pendente padrão para os testes da etapa três. */
  private OprmResearchQuery query() {
    OprmResearchQuery query = new OprmResearchQuery();
    query.setId(2001L);
    query.setResearchCycleId(1001L);
    query.setNicheResearchSeedId(44L);
    query.setQueryText("como lotar agenda de manicure");
    query.setQueryGoal("SALES_PAIN_DISCOVERY");
    query.setSourceGroup("web");
    query.setPriority(10);
    query.setStatus("PENDING");
    query.setResultCount(0);
    query.setCreatedBy("AI");
    query.setCreatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    query.setUpdatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    return query;
  }

  /** Monta um ciclo de pesquisa em execução para receber os totais da etapa três. */
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
    cycle.setTotalSourceCandidates(0);
    cycle.setTotalSourceSnapshots(0);
    cycle.setTotalExtractedSignals(0);
    cycle.setStartedAt(Instant.parse("2026-06-02T10:00:00Z"));
    cycle.setCreatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    cycle.setUpdatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    return cycle;
  }

  /** Monta um payload válido com dois resultados de busca para uma query. */
  private CompleteSourceSearcherRequest validRequest() {
    return new CompleteSourceSearcherRequest(
        "BRAVE_SEARCH",
        List.of(
            new SourceCandidateRequest(
                "https://exemplo.com/clientes-manicure",
                "Como conseguir mais clientes para manicure",
                "Veja formas de divulgar serviços e preencher horários.",
                "exemplo.com",
                "PUBLIC_CONTENT",
                1,
                "FOUND"),
            new SourceCandidateRequest(
                "https://exemplo.com/agenda-manicure",
                "Como lotar agenda de manicure",
                "Estratégias para manter a agenda cheia.",
                "exemplo.com",
                null,
                2,
                null)));
  }

  /** Monta uma fonte candidata persistida para simular a contagem agregada do ciclo. */
  private OprmSourceCandidate candidate(Long id, int position) {
    OprmSourceCandidate candidate = new OprmSourceCandidate();
    candidate.setId(id);
    candidate.setResearchCycleId(1001L);
    candidate.setResearchQueryId(2001L);
    candidate.setSourceUrl("https://exemplo.com/" + position);
    candidate.setSourceTitle("Fonte " + position);
    candidate.setSourceDomain("exemplo.com");
    candidate.setSourceGroup("PUBLIC_CONTENT");
    candidate.setSearchProvider("BRAVE_SEARCH");
    candidate.setSearchPosition(position);
    candidate.setStatus("FOUND");
    candidate.setCreatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    candidate.setUpdatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    return candidate;
  }
}
