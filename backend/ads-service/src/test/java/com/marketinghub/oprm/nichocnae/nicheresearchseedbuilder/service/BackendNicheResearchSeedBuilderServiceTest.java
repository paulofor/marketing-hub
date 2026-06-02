package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.OprmNicheResearchSeed;
import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderResponse;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.NicheResearchQueryRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.failStageExecution.FailNicheResearchSeedBuilderRequest;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheResearchSeedRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmResearchQueryRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
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

/** Valida a persistência do seed operacional e das queries da etapa dois do OPRM nichocnae. */
@ExtendWith(MockitoExtension.class)
class BackendNicheResearchSeedBuilderServiceTest {
  @Mock private OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  @Mock private OprmNicheResearchSeedRepository nicheResearchSeedRepository;
  @Mock private OprmResearchQueryRepository researchQueryRepository;

  @InjectMocks private BackendNicheResearchSeedBuilderService service;

  /** Deve listar apenas ciclos em execução que ainda não possuem seed gerado para a etapa dois. */
  @Test
  void listPendingUsesSeedBuilderRepositoryFilter() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findSeedBuilderPendingByStatus(eq("RUNNING"), any(Pageable.class)))
        .thenReturn(List.of(cycle));

    var result = service.listPending();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().researchCycleId()).isEqualTo(1001L);
    assertThat(result.getFirst().cnaeCode()).isEqualTo("9602501");
  }

  /** Deve gravar seed, queries pendentes e total de queries no ciclo quando o payload é válido. */
  @Test
  void completePersistsSeedQueriesAndCycleTotals() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(nicheResearchSeedRepository.existsByResearchCycleId(1001L)).thenReturn(false);
    when(nicheResearchSeedRepository.save(any(OprmNicheResearchSeed.class)))
        .thenAnswer(invocation -> {
          OprmNicheResearchSeed seed = invocation.getArgument(0);
          seed.setId(44L);
          return seed;
        });
    when(researchQueryRepository.saveAll(any()))
        .thenAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          List<OprmResearchQuery> queries = invocation.getArgument(0);
          for (int i = 0; i < queries.size(); i++) {
            queries.get(i).setId((long) i + 1);
          }
          return queries;
        });

    CompleteNicheResearchSeedBuilderResponse response = service.complete(1001L, validRequest());

    assertThat(response.nicheResearchSeedId()).isEqualTo(44L);
    assertThat(response.totalQueries()).isEqualTo(2);
    assertThat(response.queries()).extracting("status").containsOnly("PENDING");
    assertThat(response.queries()).extracting("resultCount").containsOnly(0);

    ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
    verify(routineResearchCycleRepository).save(cycleCaptor.capture());
    assertThat(cycleCaptor.getValue().getTotalQueries()).isEqualTo(2);
  }

  /** Deve rejeitar payloads com mais de quinze queries para preservar o MVP documentado. */
  @Test
  void completeRejectsMoreThanFifteenQueries() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    List<NicheResearchQueryRequest> manyQueries = java.util.stream.IntStream.rangeClosed(1, 16)
        .mapToObj(index -> new NicheResearchQueryRequest(
            "manicure rotina " + index, "ROUTINE_DISCOVERY", "web", index))
        .toList();
    CompleteNicheResearchSeedBuilderRequest request = new CompleteNicheResearchSeedBuilderRequest(
        "Cabeleireiros, manicures e pedicures",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        "depende de agenda cheia",
        "INFERRED_FROM_CNAE",
        "AI",
        manyQueries);

    assertThatThrownBy(() -> service.complete(1001L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("between 1 and 15");
    verify(nicheResearchSeedRepository, never()).save(any());
  }

  /** Deve marcar o ciclo como falho e registrar a mensagem operacional da falha da etapa dois. */
  @Test
  void failMarksCycleAsFailed() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));

    service.fail(1001L, new FailNicheResearchSeedBuilderRequest("IA retornou queries genéricas."));

    ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
    verify(routineResearchCycleRepository).save(cycleCaptor.capture());
    assertThat(cycleCaptor.getValue().getStatus()).isEqualTo("FAILED");
    assertThat(cycleCaptor.getValue().getErrorMessage()).isEqualTo("IA retornou queries genéricas.");
    assertThat(cycleCaptor.getValue().getFinishedAt()).isNotNull();
  }

  /** Monta um payload válido de etapa dois com dois objetivos de pesquisa distintos. */
  private CompleteNicheResearchSeedBuilderRequest validRequest() {
    return new CompleteNicheResearchSeedBuilderRequest(
        "Cabeleireiros, manicures e pedicures",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        "depende de agenda cheia",
        "INFERRED_FROM_CNAE",
        null,
        List.of(
            new NicheResearchQueryRequest("manicure responsabilidades rotina", "ROUTINE_DISCOVERY", "web", 1),
            new NicheResearchQueryRequest("como lotar agenda de manicure", "SALES_PAIN_DISCOVERY", "web", 2)));
  }

  /** Monta um ciclo de pesquisa em execução usado pelos cenários da etapa dois. */
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
    cycle.setTotalQueries(0);
    cycle.setTotalSourceCandidates(0);
    cycle.setTotalSourceSnapshots(0);
    cycle.setTotalExtractedSignals(0);
    cycle.setStartedAt(Instant.parse("2026-06-02T10:00:00Z"));
    cycle.setCreatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    cycle.setUpdatedAt(Instant.parse("2026-06-02T10:00:00Z"));
    return cycle;
  }
}
