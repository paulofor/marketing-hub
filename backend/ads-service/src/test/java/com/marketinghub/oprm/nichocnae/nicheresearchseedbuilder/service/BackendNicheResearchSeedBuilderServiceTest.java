package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.oprm.nichocnae.OprmNicheResearchSeed;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderResponse;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.NicheResearchQueryRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.failStageExecution.FailNicheResearchSeedBuilderRequest;
import com.marketinghub.repository.jpa.niche.MarketNicheEnrichmentProfileRepository;
import com.marketinghub.repository.jpa.oprm.market.OprmMarketSizeByCnaeRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheResearchSeedRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
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
  @Mock private OprmNicheResearchSeedBuilderConfigurationGateway configurationGateway;
  @Mock private OprmMarketSizeByCnaeRepository marketSizeByCnaeRepository;
  @Mock private OprmNicheRoutineCardRepository routineCardRepository;
  @Mock private MarketNicheEnrichmentProfileRepository enrichmentProfileRepository;

  @InjectMocks private BackendNicheResearchSeedBuilderService service;

  /** Configura preço padrão para os testes que enviam telemetria de OpenAI. */
  @org.junit.jupiter.api.BeforeEach
  void setUpPricing() {
    lenient()
        .when(configurationGateway.estimateCostUsd(eq("gpt-5.4"), any(), any()))
        .thenReturn(new BigDecimal("0.0123"));
    lenient().when(configurationGateway.findConfiguredModel()).thenReturn(Optional.empty());
  }

  /** Deve listar ciclos em execução e falhas retryáveis sem seed para a etapa dois. */
  @Test
  void listPendingUsesSeedBuilderRepositoryFilter() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findSeedBuilderPendingOrRetryable(
            eq("RUNNING"),
            eq("FAILED"),
            eq("nicheName is required"),
            eq("Data too long for column 'query_goal'"),
            eq("niche-research-seed-builder/stage-executions"),
            any(Pageable.class)))
        .thenReturn(List.of(cycle));

    var result = service.listPending();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().researchCycleId()).isEqualTo(1001L);
    assertThat(result.getFirst().cnaeCode()).isEqualTo("9602501");
    assertThat(result.getFirst().meiVolume()).isNull();
  }

  /** Deve expor aprendizado do gate anterior para a etapa de seed não repetir a reprovação dominante. */
  @Test
	  void listPendingIncludesPreviousQualityGateLearning() {
    OprmRoutineResearchCycle cycle = cycle();
    OprmNicheRoutineCard card = new OprmNicheRoutineCard();
    card.setQualityStatus("SOLUTION_CONTAMINATED");
    card.setQualityNotes(
        "status=SOLUTION_CONTAMINATED; proximoMovimentoCodigo=REFAZER_BUSCA_SEM_SOLUCAO; "
            + "proximoMovimento=Reexecutar busca removendo fontes de solucao; riscoLinguagemSolucao=70");
    when(routineResearchCycleRepository.findSeedBuilderPendingOrRetryable(
            eq("RUNNING"),
            eq("FAILED"),
            eq("nicheName is required"),
            eq("Data too long for column 'query_goal'"),
            eq("niche-research-seed-builder/stage-executions"),
            any(Pageable.class)))
        .thenReturn(List.of(cycle));
    when(routineCardRepository.findLatestCheckedCardForLearning(anyLong(), eq(1001L), any(Pageable.class)))
        .thenReturn(List.of(card));

    var result = service.listPending();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().previousQualityStatus()).isEqualTo("SOLUTION_CONTAMINATED");
    assertThat(result.getFirst().previousNextMoveCode()).isEqualTo("REFAZER_BUSCA_SEM_SOLUCAO");
    assertThat(result.getFirst().previousNextMove()).isEqualTo("Reexecutar busca removendo fontes de solucao");
    assertThat(result.getFirst().previousLearningNotes()).contains("riscoLinguagemSolucao=70");
	  }

	  /** Deve enviar subnichos já materializados no CNAE para a IA não repetir o mesmo recorte de mercado. */
	  @Test
	  void listPendingIncludesExistingSubnichesForSameCnae() {
	    OprmRoutineResearchCycle cycle = cycle();
	    MarketNicheEnrichmentProfile firstProfile = new MarketNicheEnrichmentProfile();
	    firstProfile.setNeutralNicheName("Manicure autônoma que atende em domicílio");
	    MarketNicheEnrichmentProfile secondProfile = new MarketNicheEnrichmentProfile();
	    secondProfile.setNeutralNicheName("Nail designer iniciante com agenda pelo Instagram");
	    when(routineResearchCycleRepository.findSeedBuilderPendingOrRetryable(
	            eq("RUNNING"),
	            eq("FAILED"),
	            eq("nicheName is required"),
	            eq("Data too long for column 'query_goal'"),
	            eq("niche-research-seed-builder/stage-executions"),
	            any(Pageable.class)))
	        .thenReturn(List.of(cycle));
	    when(enrichmentProfileRepository.findGeneratedByCnaeCode(eq("9602501"), any(Pageable.class)))
	        .thenReturn(List.of(firstProfile, secondProfile));

	    var result = service.listPending();

	    assertThat(result.getFirst().existingSubnichesForCnae())
	        .containsExactly(
	            "Manicure autônoma que atende em domicílio",
	            "Nail designer iniciante com agenda pelo Instagram");
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
    assertThat(response.model()).isEqualTo("gpt-5.4");
    assertThat(response.inputTokens()).isEqualTo(1200);
    assertThat(response.outputTokens()).isEqualTo(800);
    assertThat(response.costUsd()).isEqualByComparingTo("0.0123");

    ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
    verify(routineResearchCycleRepository).save(cycleCaptor.capture());
    assertThat(cycleCaptor.getValue().getTotalQueries()).isEqualTo(12);
    assertThat(cycleCaptor.getValue().getNicheName()).isEqualTo("Manicures autônomas com agenda instável pelo WhatsApp");
    assertThat(cycleCaptor.getValue().getNeutralNicheName())
        .isEqualTo("Manicures autônomas com agenda instável pelo WhatsApp");
    assertThat(cycleCaptor.getValue().getOriginalNicheName()).isEqualTo("Cabeleireiros, manicures e pedicures");
  }

  /** Deve bloquear resposta que tenta manter o CNAE amplo como se fosse o nicho final. */
  @Test
  void completeRejectsBroadCnaeNameInsteadOfSpecificWinningSubniche() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(nicheResearchSeedRepository.existsByResearchCycleId(1001L)).thenReturn(false);
    CompleteNicheResearchSeedBuilderRequest request = new CompleteNicheResearchSeedBuilderRequest(
        "Cabeleireiros, manicures e pedicures",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        commercialPreGateAssumptions(),
        "INFERRED_FROM_CNAE",
        "AI",
        "gpt-5.4",
        "{\"seed\":true}",
        1200,
        800,
        "resp_seed",
        validQueryRequests());

    assertThatThrownBy(() -> service.complete(1001L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("specific winning subniche");
  }

  /** Deve bloquear seed sem evidência comercial mínima antes de gastar busca, coleta e extração profundas. */
  @Test
  void completeRejectsSeedWithoutCommercialPreGateCoverage() {
    OprmRoutineResearchCycle cycle = cycle();
    when(routineResearchCycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(nicheResearchSeedRepository.existsByResearchCycleId(1001L)).thenReturn(false);
    CompleteNicheResearchSeedBuilderRequest request = new CompleteNicheResearchSeedBuilderRequest(
        "Manicures autônomas com agenda instável pelo WhatsApp",
        "serviço local de beleza",
        "rotina operacional de atendimento",
        "consumidor final",
        "manicure",
        "subnicho escolhido por ser comum no CNAE",
        "INFERRED_FROM_CNAE",
        "AI",
        "gpt-5.4",
        "{\"seed\":true}",
        1200,
        800,
        "resp_seed",
        List.of(new NicheResearchQueryRequest("rotina manicure Brasil", "ROUTINE_DISCOVERY", "web", 1)));

    assertThatThrownBy(() -> service.complete(1001L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Commercial pre-gate rejected seed");
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
        "Manicures autônomas com agenda instável pelo WhatsApp",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        commercialPreGateAssumptions(),
        "INFERRED_FROM_CNAE",
        "AI",
        "gpt-5.4",
        "{\"seed\":true}",
        1200,
        800,
        "resp_seed",
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
        "Manicures autônomas com agenda instável pelo WhatsApp",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        commercialPreGateAssumptions(),
        "INFERRED_FROM_CNAE",
        "AI",
        "gpt-5.4",
        "{\"seed\":true}",
        1200,
        800,
        "resp_seed",
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
        "Manicures autônomas com agenda instável pelo WhatsApp",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        commercialPreGateAssumptions(),
        "INFERRED_FROM_CNAE",
        "AI",
        "gpt-5.4",
        "{\"seed\":true}",
        1200,
        800,
        "resp_seed",
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
        "Manicures autônomas com agenda instável pelo WhatsApp",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        commercialPreGateAssumptions(),
        "INFERRED_FROM_CNAE",
        "AI",
        "gpt-5.4",
        "{\"seed\":true}",
        1200,
        800,
        "resp_seed",
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
        "Manicures autônomas com agenda instável pelo WhatsApp",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        commercialPreGateAssumptions(),
        "INFERRED_FROM_CNAE",
        "AI",
        "gpt-5.4",
        "{\"seed\":true}",
        1200,
        800,
        "resp_seed",
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
        "Manicures autônomas com agenda instável pelo WhatsApp",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        commercialPreGateAssumptions(),
        "INFERRED_FROM_CNAE",
        "AI",
        "gpt-5.4",
        "{\"seed\":true}",
        1200,
        800,
        "resp_seed",
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
        "Manicures autônomas com agenda instável pelo WhatsApp",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        commercialPreGateAssumptions(),
        "INFERRED_FROM_CNAE",
        "AI",
        "gpt-5.4",
        "{\"seed\":true}",
        1200,
        800,
        "resp_seed",
        List.of());

    CompleteNicheResearchSeedBuilderResponse response = service.complete(1001L, request);

    assertThat(response.totalQueries()).isEqualTo(1);
    assertThat(response.queries().getFirst().queryGoal()).isEqualTo("COMMERCIAL_OPERATION_DISCOVERY");
    assertThat(response.queries().getFirst().queryText())
        .contains("WhatsApp Instagram indicação agenda faltas preço cobrança materiais retrabalho Brasil");
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

  /** Monta um payload válido de etapa dois com doze objetivos comerciais-operacionais para MEI/autônomo. */
  private CompleteNicheResearchSeedBuilderRequest validRequest() {
    return new CompleteNicheResearchSeedBuilderRequest(
        "Manicures autônomas com agenda instável pelo WhatsApp",
        "serviço local de beleza",
        "agenda e atendimento recorrente",
        "consumidor final recorrente",
        "manicure, pedicure, escova",
        commercialPreGateAssumptions(),
        "INFERRED_FROM_CNAE",
        null,
        "gpt-5.4",
        "{\"seed\":true}",
        1200,
        800,
        "resp_seed",
        validQueryRequests());
  }

  /** Monta justificativa com cobertura dos critérios comerciais exigidos pelo pré-gate. */
  private String commercialPreGateAssumptions() {
    return "Subnichos avaliados por recorrência, urgência da dor, capacidade de pagar, clareza do resultado "
        + "e compatibilidade com produto digital. Venceu manicure autônoma com agenda instável pelo WhatsApp "
        + "porque sofre com cancelamento, agenda vazia, cobrança de sinal, pacotes mensais, fidelização, "
        + "resultado de agenda cheia, renda previsível e possibilidade de guia/checklist digital.";
  }

  /** Cria a lista padrão de queries da etapa dois usada pelos cenários de persistência. */
  private List<NicheResearchQueryRequest> validQueryRequests() {
    return List.of(
        new NicheResearchQueryRequest(
            "manicure autônoma clientes WhatsApp Instagram indicação Brasil", "CUSTOMER_ACQUISITION", "web", 1),
        new NicheResearchQueryRequest(
            "profissional autônomo manicure agenda faltas remarcações clientes somem", "SCHEDULE_NO_SHOWS", "web", 2),
        new NicheResearchQueryRequest(
            "manicure preço cobrança sinal pacotes recorrência Brasil", "PRICING_BILLING", "web", 3),
        new NicheResearchQueryRequest(
            "manicure materiais tempo atendimento retrabalho Brasil", "MATERIALS_REWORK", "web", 4),
        new NicheResearchQueryRequest(
            "relatos reais manicure autônoma fórum comentários perguntas frequentes", "REAL_REPORTS", "web", 5),
        new NicheResearchQueryRequest(
            "cabeleireiro autônomo clientes indicação Instagram WhatsApp", "CUSTOMER_ACQUISITION", "web", 6),
        new NicheResearchQueryRequest(
            "cabeleireiro agenda vazia cliente falta remarcar horário", "SCHEDULE_NO_SHOWS", "web", 7),
        new NicheResearchQueryRequest(
            "cabeleireiro preço pacote cobrança recorrência salão pequeno", "PRICING_BILLING", "web", 8),
        new NicheResearchQueryRequest(
            "pedicure material esterilização tempo atendimento retrabalho", "MATERIALS_REWORK", "web", 9),
        new NicheResearchQueryRequest(
            "vídeos comentários manicure autônoma dúvidas clientes cobrança", "REAL_REPORTS", "web", 10),
        new NicheResearchQueryRequest(
            "profissional beleza cliente sumiu depois orçamento WhatsApp", "SCHEDULE_NO_SHOWS", "web", 11),
        new NicheResearchQueryRequest(
            "profissional autônomo beleza pacotes mensalidade fidelização clientes", "PRICING_BILLING", "web", 12));
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
    cycle.setOriginalNicheName("Cabeleireiros, manicures e pedicures");
    cycle.setNeutralNicheName("Cabeleireiros, manicures e pedicures");
    cycle.setResearchMode("ROUTINE_REALITY_RESEARCH");
    cycle.setSolutionLanguageRiskScore(BigDecimal.ZERO);
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
