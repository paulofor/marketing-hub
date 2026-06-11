package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

  /** Deve listar ciclos em execução e falhas retryáveis sem seed para a etapa dois. */
  @Test
  void listPendingUsesSeedBuilderRepositoryFilter() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findSeedBuilderPendingOrRetryable(
            eq("RUNNING"),
            eq("FAILED"),
            eq(""),
            eq("niche-research-seed-builder/stage-executions"),
            any(Pageable.class)))
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
    assertThat(response.totalQueries()).isEqualTo(12);
    assertThat(response.queries()).extracting("status").containsOnly("PENDING");
    assertThat(response.queries()).extracting("resultCount").containsOnly(0);

    ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
    verify(routineResearchCycleRepository).save(cycleCaptor.capture());
    assertThat(cycleCaptor.getValue().getTotalQueries()).isEqualTo(12);
  }

  /** Deve reabrir automaticamente ciclo falho retryável quando a conclusão da etapa dois passa a funcionar. */
  @Test
  void completeReactivatesRetryableFailedCycleAfterSuccess() {
    OprmRoutineResearchCycle cycle = failedCycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(nicheResearchSeedRepository.existsByResearchCycleId(1001L)).thenReturn(false);
    when(nicheResearchSeedRepository.save(any(OprmNicheResearchSeed.class)))
        .thenAnswer(invocation -> {
          OprmNicheResearchSeed seed = invocation.getArgument(0);
          seed.setId(44L);
          return seed;
        });
    when(researchQueryRepository.saveAll(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.complete(1001L, validRequest());

    ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
    verify(routineResearchCycleRepository).save(cycleCaptor.capture());
    assertThat(cycleCaptor.getValue().getStatus()).isEqualTo("RUNNING");
    assertThat(cycleCaptor.getValue().getFinishedAt()).isNull();
    assertThat(cycleCaptor.getValue().getErrorMessage()).isNull();
    assertThat(cycleCaptor.getValue().getTotalQueries()).isEqualTo(12);
  }

  /** Deve aceitar objetivo retornado pelo modelo sem bloquear semanticamente a etapa preparatória. */
  @Test
  void completeAcceptsModelReturnedQueryGoal() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    stubSuccessfulPersistence();
    CompleteNicheResearchSeedBuilderRequest request = new CompleteNicheResearchSeedBuilderRequest(
        "Cabeleireiros, manicures e pedicures",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        "depende de agenda cheia",
        "INFERRED_FROM_CNAE",
        "AI",
        validQueryRequestsWithFirst("manicure MEI serviços mais procurados Brasil", "PRODUCT_SERVICE_DISCOVERY"));

    CompleteNicheResearchSeedBuilderResponse response = service.complete(1001L, request);

    assertThat(response.totalQueries()).isEqualTo(12);
    assertThat(response.queries()).extracting("queryGoal").contains("PRODUCT_SERVICE_DISCOVERY");
  }

  /** Deve aceitar linguagem de solução retornada pelo modelo sem bloquear a criação das queries iniciais. */
  @Test
  void completeAcceptsSolutionLanguageQuery() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    stubSuccessfulPersistence();
    CompleteNicheResearchSeedBuilderRequest request = new CompleteNicheResearchSeedBuilderRequest(
        "Cabeleireiros, manicures e pedicures",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        "depende de agenda cheia",
        "INFERRED_FROM_CNAE",
        "AI",
        validQueryRequestsWithFirst("IA para crescimento de manicure MEI Brasil", "MEI_ROUTINE_DISCOVERY"));

    CompleteNicheResearchSeedBuilderResponse response = service.complete(1001L, request);

    assertThat(response.totalQueries()).isEqualTo(12);
  }

  /** Deve aceitar query sem marcador literal de MEI/autônomo porque a etapa passa a confiar no modelo. */
  @Test
  void completeAcceptsQueryWithoutAudienceMarker() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    stubSuccessfulPersistence();
    CompleteNicheResearchSeedBuilderRequest request = new CompleteNicheResearchSeedBuilderRequest(
        "Cabeleireiros, manicures e pedicures",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        "depende de agenda cheia",
        "INFERRED_FROM_CNAE",
        "AI",
        validQueryRequestsWithFirst("manicure responsabilidades rotina Brasil", "MEI_ROUTINE_DISCOVERY"));

    CompleteNicheResearchSeedBuilderResponse response = service.complete(1001L, request);

    assertThat(response.totalQueries()).isEqualTo(12);
  }

  /** Deve aceitar query sem marcador literal brasileiro porque a localização será tratada pelas próximas etapas. */
  @Test
  void completeAcceptsQueryWithoutBrazilMarker() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    stubSuccessfulPersistence();
    CompleteNicheResearchSeedBuilderRequest request = new CompleteNicheResearchSeedBuilderRequest(
        "Cabeleireiros, manicures e pedicures",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        "depende de agenda cheia",
        "INFERRED_FROM_CNAE",
        "AI",
        validQueryRequestsWithFirst("manicure MEI responsabilidades rotina", "MEI_ROUTINE_DISCOVERY"));

    CompleteNicheResearchSeedBuilderResponse response = service.complete(1001L, request);

    assertThat(response.totalQueries()).isEqualTo(12);
  }

  /** Deve aceitar queries com palavras comuns repetidas sem gerar duplicate element na tokenização. */
  @Test
  void completeAcceptsRepeatedCommonWordsInQueryText() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    stubSuccessfulPersistence();
    CompleteNicheResearchSeedBuilderRequest request = new CompleteNicheResearchSeedBuilderRequest(
        "Cabeleireiros, manicures e pedicures",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        "depende de agenda cheia",
        "INFERRED_FROM_CNAE",
        "AI",
        validQueryRequestsWithFirst(
            "rotina de manicure MEI e atendimento e organização diária Brasil", "MEI_ROUTINE_DISCOVERY"));

    CompleteNicheResearchSeedBuilderResponse response = service.complete(1001L, request);

    assertThat(response.totalQueries()).isEqualTo(12);
    verify(nicheResearchSeedRepository).save(any());
  }

  /** Deve aceitar mais de quinze queries quando o modelo decidir ampliar a cobertura da pesquisa inicial. */
  @Test
  void completeAcceptsMoreThanFifteenQueries() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    stubSuccessfulPersistence();
    List<NicheResearchQueryRequest> manyQueries = java.util.stream.IntStream.rangeClosed(1, 16)
        .mapToObj(index -> new NicheResearchQueryRequest(
            "manicure MEI rotina Brasil " + index, "MEI_ROUTINE_DISCOVERY", "web", index))
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

    CompleteNicheResearchSeedBuilderResponse response = service.complete(1001L, request);

    assertThat(response.totalQueries()).isEqualTo(16);
  }

  /** Deve aceitar ausência total de queries criando uma query padrão para manter o ciclo avançando. */
  @Test
  void completeCreatesDefaultQueryWhenModelReturnsEmptyQueries() {
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
        .thenAnswer(invocation -> invocation.getArgument(0));
    CompleteNicheResearchSeedBuilderRequest request = new CompleteNicheResearchSeedBuilderRequest(
        "Cabeleireiros, manicures e pedicures",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        "depende de agenda cheia",
        "INFERRED_FROM_CNAE",
        "AI",
        List.of());

    CompleteNicheResearchSeedBuilderResponse response = service.complete(1001L, request);

    assertThat(response.totalQueries()).isEqualTo(1);
    assertThat(response.queries().getFirst().queryGoal()).isEqualTo("ROUTINE_DISCOVERY");
    assertThat(response.queries().getFirst().queryText()).contains("rotina dificuldades atendimento clientes Brasil");
  }

  /** Deve limitar campos textuais variáveis ao contrato físico do banco antes da persistência. */
  @Test
  void completeLimitsModelTextFieldsToDatabaseContract() {
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
        .thenAnswer(invocation -> invocation.getArgument(0));
    String longText = "x".repeat(600);
    CompleteNicheResearchSeedBuilderRequest request = new CompleteNicheResearchSeedBuilderRequest(
        longText,
        longText,
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        "depende de agenda cheia",
        "INFERRED_FROM_CNAE",
        longText,
        List.of(new NicheResearchQueryRequest(longText, longText, longText, 1)));

    CompleteNicheResearchSeedBuilderResponse response = service.complete(1001L, request);

    assertThat(response.nicheName()).hasSize(255);
    assertThat(response.businessType()).hasSize(255);
    assertThat(response.createdBy()).hasSize(32);
    assertThat(response.queries().getFirst().queryText()).hasSize(500);
    assertThat(response.queries().getFirst().queryGoal()).hasSize(64);
    assertThat(response.queries().getFirst().sourceGroup()).hasSize(64);
  }

  /** Deve marcar o ciclo como falho e registrar a mensagem operacional da falha da etapa dois. */
  @Test
  void failMarksCycleAsFailed() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));

    service.fail(1001L, new FailNicheResearchSeedBuilderRequest("IA retornou queries genéricas.", null));

    ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
    verify(routineResearchCycleRepository).save(cycleCaptor.capture());
    assertThat(cycleCaptor.getValue().getStatus()).isEqualTo("FAILED");
    assertThat(cycleCaptor.getValue().getErrorMessage()).isEqualTo("IA retornou queries genéricas.");
    assertThat(cycleCaptor.getValue().getFinishedAt()).isNotNull();
  }

  /** Deve preservar detalhe técnico da falha para revelar causa-raiz em reprocessamentos do seed. */
  @Test
  void failPersistsTechnicalDetailWhenProvided() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));

    service.fail(
        1001L,
        new FailNicheResearchSeedBuilderRequest(
            "Falha ao gerar seed da etapa dois OPRM nichocnae.",
            "java.lang.IllegalStateException: OpenAI retornou corpo vazio"));

    ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
    verify(routineResearchCycleRepository).save(cycleCaptor.capture());
    assertThat(cycleCaptor.getValue().getErrorMessage())
        .contains("Falha ao gerar seed da etapa dois OPRM nichocnae.")
        .contains("OpenAI retornou corpo vazio");
  }

  /** Monta um payload válido de etapa dois com doze objetivos de pesquisa orientados a MEI/autônomo. */
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
        validQueryRequests());
  }

  /** Cria a lista padrão de queries da etapa dois usada pelos cenários de persistência. */
  private List<NicheResearchQueryRequest> validQueryRequests() {
    return List.of(
        new NicheResearchQueryRequest("manicure MEI responsabilidades rotina Brasil", "MEI_ROUTINE_DISCOVERY", "web", 1),
        new NicheResearchQueryRequest(
            "profissional autônomo agenda manicure horários vazios Brasil", "DAILY_OPERATION_PAIN_DISCOVERY", "web", 2),
        new NicheResearchQueryRequest(
            "trabalhador por conta própria manicure consegue clientes Brasil",
            "CUSTOMER_ACQUISITION_BEHAVIOR_DISCOVERY",
            "web",
            3),
        new NicheResearchQueryRequest(
            "dono-operador salão beleza modo de trabalho Brasil", "AUTONOMOUS_WORK_MODE_DISCOVERY", "web", 4),
        new NicheResearchQueryRequest(
            "MEI manicure dor emocional agenda vazia Brasil", "EMOTIONAL_PAIN_DISCOVERY", "web", 5),
        new NicheResearchQueryRequest(
            "profissional autônomo manicure sonhos objetivos Brasil", "DREAM_DISCOVERY", "web", 6),
        new NicheResearchQueryRequest("MEI manicure medos inseguranças Brasil", "FEAR_DISCOVERY", "web", 7),
        new NicheResearchQueryRequest(
            "profissional autônomo manicure canais WhatsApp clientes Brasil", "CHANNEL_BEHAVIOR_DISCOVERY", "web", 8),
        new NicheResearchQueryRequest("MEI manicure linguagem real clientes pt-BR", "LANGUAGE_DISCOVERY", "web", 9),
        new NicheResearchQueryRequest(
            "MEI manicure fontes recentes rotina Brasil", "SOURCE_FRESHNESS_DISCOVERY", "web", 10),
        new NicheResearchQueryRequest(
            "profissional autônomo pedicure compra materiais Brasil", "DAILY_OPERATION_PAIN_DISCOVERY", "web", 11),
        new NicheResearchQueryRequest(
            "trabalhador por conta própria cabeleireiro retrabalho Brasil", "MEI_ROUTINE_DISCOVERY", "web", 12));
  }

  /** Substitui a primeira query válida para testar validações específicas sem quebrar o contrato mínimo. */
  private List<NicheResearchQueryRequest> validQueryRequestsWithFirst(String queryText, String queryGoal) {
    List<NicheResearchQueryRequest> queries = new java.util.ArrayList<>(validQueryRequests());
    queries.set(0, new NicheResearchQueryRequest(queryText, queryGoal, "web", 1));
    return queries;
  }


  /** Configura os repositórios para simular persistência bem-sucedida da etapa dois. */
  private void stubSuccessfulPersistence() {
    when(nicheResearchSeedRepository.existsByResearchCycleId(1001L)).thenReturn(false);
    when(nicheResearchSeedRepository.save(any(OprmNicheResearchSeed.class)))
        .thenAnswer(invocation -> {
          OprmNicheResearchSeed seed = invocation.getArgument(0);
          seed.setId(44L);
          return seed;
        });
    when(researchQueryRepository.saveAll(any())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      List<OprmResearchQuery> queries = invocation.getArgument(0);
      for (int i = 0; i < queries.size(); i++) {
        queries.get(i).setId((long) i + 1);
      }
      return queries;
    });
  }

  /** Monta um ciclo falho pelo contrato legado para validar recuperação automática da etapa dois. */
  private OprmRoutineResearchCycle failedCycle() {
    OprmRoutineResearchCycle cycle = cycle();
    cycle.setStatus("FAILED");
    cycle.setFinishedAt(Instant.parse("2026-06-03T11:15:13Z"));
    cycle.setErrorMessage(
        "500 Internal Server Error: nicheName is required path=/api/internal/oprm/nichocnae/niche-research-seed-builder/stage-executions/1/complete");
    return cycle;
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
