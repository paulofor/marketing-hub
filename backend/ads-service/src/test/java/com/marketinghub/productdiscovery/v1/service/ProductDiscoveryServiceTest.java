package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskExecutionAuditRequest;
import com.marketinghub.opportunitydossier.service.OpportunityDossierResearchSyncService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityMaturity;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryResearchMode;
import com.marketinghub.productdiscovery.v1.service.resumePrivateValidationHandoff.ProductDiscoveryPrivateValidationHandoffResponse;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: valida regras comerciais do serviço de descoberta PDE. */
@ExtendWith(MockitoExtension.class)
class ProductDiscoveryServiceTest {

  @Mock private ProductDiscoveryCycleRepository cycleRepository;

  @Mock private ProductDiscoveryOpportunityRepository opportunityRepository;

  @Mock private OpportunityDossierResearchSyncService dossierResearchSyncService;

  @Mock private ProductDiscoveryBpmAuditService bpmAuditService;

  @Mock private ProductDiscoveryCustomerInterviewService customerInterviewService;

  /** Deve preservar a candidata inicial e abrir o gate comportamental na versão nova. */
  @Test
  void opensCandidateGapDeepeningWithoutSendingPrematureDossierToAtena() {
    ProductDiscoveryCycle cycle = researchCycle(40L, "lease-40", "research");
    ProductDiscoveryOpportunity persisted =
        opportunity(cycle, 801L, "Imagem para ocasião especial");
    when(cycleRepository.findById(40L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(40L))
        .thenReturn(List.of(persisted));
    when(bpmAuditService.supportsCandidateGapDeepening(cycle)).thenReturn(true);
    ProductDiscoveryService service = serviceWithCustomerInterviews();

    ProductDiscoveryCycleDetailResponse response =
        service.complete(
            40L,
            new ProductDiscoveryResultRequest(
                "lease-40",
                "Candidata preservada para aprofundar comportamento passado.",
                List.of(researchableResult("Imagem para ocasião especial", "{}"))));

    assertThat(response.cycle().status())
        .isEqualTo(ProductDiscoveryCycleStatus.AWAITING_CUSTOMER_EVIDENCE);
    assertThat(cycle.getStageCode())
        .isEqualTo(ProductDiscoveryCustomerInterviewService.WAITING_STAGE_CODE);
    verify(bpmAuditService).openCandidateGapDeepening(cycle);
    verify(dossierResearchSyncService, never()).synchronize(eq(40L), any());
  }

  /** Deve preservar a candidata inicial e abrir o gate comportamental na versão nova. */
  @Test
  void automaticallyQueuesPublicDeepeningWithoutInterviewRecruitment() {
    ProductDiscoveryCycle cycle = researchCycle(40L, "lease-40", "research");
    cycle.setEvidencePolicy("PUBLIC_SOURCES_V1");
    ProductDiscoveryOpportunity persisted =
        opportunity(cycle, 801L, "Imagem para ocasião especial");
    when(cycleRepository.findById(40L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(40L))
        .thenReturn(List.of(persisted));
    when(bpmAuditService.supportsCandidateGapDeepening(cycle)).thenReturn(true);
    ProductDiscoveryService service = serviceWithCustomerInterviews();

    ProductDiscoveryCycleDetailResponse response =
        service.complete(
            40L,
            new ProductDiscoveryResultRequest(
                "lease-40",
                "Candidata preservada para aprofundar comportamento passado.",
                List.of(researchableResult("Imagem para ocasião especial", "{}"))));

    assertThat(response.cycle().status()).isEqualTo(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    assertThat(cycle.getStageCode())
        .isEqualTo(ProductDiscoveryCustomerInterviewService.GAP_STAGE_CODE);
    verify(bpmAuditService).openCandidateGapDeepening(cycle);
    verify(dossierResearchSyncService, never()).synchronize(eq(40L), any());
  }

  /** Deve aceitar duas tentativas distintas e preservar seu histórico auditável. */
  @Test
  void acceptsBoundedCandidateGapPlanHistory() {
    ProductDiscoveryCycle cycle = researchCycle(41L, "lease-41", "candidate-gap-deepening");
    ProductDiscoveryOpportunity first = opportunity(cycle, 811L, "Ocasião especial");
    ProductDiscoveryOpportunity second = opportunity(cycle, 812L, "Encontro importante");
    when(cycleRepository.findById(41L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(41L))
        .thenReturn(List.of(first, second));
    ProductDiscoveryService service = serviceWithCustomerInterviews();
    String firstPlan = gapPlan("INITIAL_SCOPE", "Lacunas iniciais", "q1", "q2");
    String secondPlan = gapPlan("ADJACENT_PAID_ALTERNATIVE", "Contrapontos de compra", "q3", "q4");
    String history =
        "{\"strategyCode\":\"BOUNDED_ADJACENT_MARKET_EXPANSION_V1\",\"attempts\":["
            + "{\"attemptNumber\":1,\"plan\":"
            + firstPlan
            + "},{\"attemptNumber\":2,\"plan\":"
            + secondPlan
            + "}]}";

    ProductDiscoveryResearchPlanResponse response =
        service.registerGapDeepeningPlan(
            41L,
            new ProductDiscoveryResearchPlanRequest("lease-41", history, history, "gpt-5.6-sol"));

    assertThat(response.planJson()).contains("Contrapontos de compra");
    assertThat(cycle.getResearchPlanJson()).isEqualTo(history);
  }

  /** Deve rejeitar uma segunda tentativa que repete consulta já autorizada. */
  @Test
  void rejectsRepeatedQueryAcrossGapDeepeningAttempts() {
    ProductDiscoveryCycle cycle = researchCycle(42L, "lease-42", "candidate-gap-deepening");
    ProductDiscoveryOpportunity first = opportunity(cycle, 821L, "Ocasião especial");
    ProductDiscoveryOpportunity second = opportunity(cycle, 822L, "Encontro importante");
    when(cycleRepository.findById(42L)).thenReturn(Optional.of(cycle));
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(42L))
        .thenReturn(List.of(first, second));
    ProductDiscoveryService service = serviceWithCustomerInterviews();
    String history =
        "{\"attempts\":[{\"attemptNumber\":1,\"plan\":"
            + gapPlan("INITIAL_SCOPE", "Lacunas iniciais", "q1", "q2")
            + "},{\"attemptNumber\":2,\"plan\":"
            + gapPlan("ADJACENT_PAID_ALTERNATIVE", "Outra lente", "q1", "q3")
            + "}]}";

    assertThatThrownBy(
            () ->
                service.registerGapDeepeningPlan(
                    42L,
                    new ProductDiscoveryResearchPlanRequest(
                        "lease-42", history, history, "gpt-5.6-sol")))
        .hasMessageContaining("Plano de aprofundamento não comprova");
  }

  /** Deve preservar uma proposta repetida sem autorizá-la para nova coleta ou cobrança. */
  @Test
  void acceptsFinalRepeatedPlanWhenWorkerMarksItAsRejectedBeforeCollection() {
    ProductDiscoveryCycle cycle = researchCycle(49L, "lease-49", "candidate-gap-deepening");
    ProductDiscoveryOpportunity first = opportunity(cycle, 881L, "Ocasião especial");
    ProductDiscoveryOpportunity second = opportunity(cycle, 882L, "Encontro importante");
    when(cycleRepository.findById(49L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(49L))
        .thenReturn(List.of(first, second));
    ProductDiscoveryService service = serviceWithCustomerInterviews();
    String repeatedPlan = gapPlan("INITIAL_SCOPE", "Lacunas iniciais", "q1", "q2");
    String history =
        "{\"attempts\":[{\"attemptNumber\":1,\"planDisposition\":\"AUTHORIZED_FOR_COLLECTION\",\"plan\":"
            + repeatedPlan
            + "},{\"attemptNumber\":2,\"planDisposition\":\"REJECTED_REPEATED_RESEARCH_LENS\",\"plan\":"
            + repeatedPlan
            + "}]}";

    ProductDiscoveryResearchPlanResponse response =
        service.registerGapDeepeningPlan(
            49L,
            new ProductDiscoveryResearchPlanRequest("lease-49", history, history, "gpt-5.6-sol"));

    assertThat(response.planJson()).contains("REJECTED_REPEATED_RESEARCH_LENS");
    assertThat(cycle.getResearchPlanJson()).isEqualTo(history);
  }

  /** Deve atualizar o dossiê final sem trocar o ID vinculado às entrevistas. */
  @Test
  void completesGapDeepeningPreservingCandidateIdentityAndEvidenceLimits() throws Exception {
    ProductDiscoveryCycle cycle = researchCycle(43L, "lease-43", "candidate-gap-deepening");
    ProductDiscoveryOpportunity current = opportunity(cycle, 831L, "Imagem para ocasião especial");
    cycle.setResearchPlanJson(singleCandidateGapPlan(current.getName(), "q1", "q2"));
    when(cycleRepository.findById(43L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(43L)).thenReturn(List.of(current));
    when(customerInterviewService.get(43L)).thenReturn(readyGap(43L, 5));
    ProductDiscoveryService service = serviceWithCustomerInterviews();
    var evidenceReport = singleCandidateGapEvidence(current.getName(), 2);

    ProductDiscoveryCycleDetailResponse response =
        service.completeGapDeepening(
            43L,
            new ProductDiscoveryResultRequest(
                "lease-43",
                "Lacuna ainda aberta; não promover sem evidência.",
                List.of(researchableResult(current.getName(), "{}")),
                evidenceReport,
                null));

    assertThat(response.cycle().status()).isEqualTo(ProductDiscoveryCycleStatus.COMPLETED);
    assertThat(current.getId()).isEqualTo(831L);
    assertThat(current.getMaturity()).isEqualTo(ProductDiscoveryOpportunityMaturity.RESEARCHABLE);
    verify(opportunityRepository, never()).deleteAllByCycleId(43L);
    verify(dossierResearchSyncService).synchronize(43L, List.of(current));
  }

  /** Deve terminar honestamente sem abrir entrevista quando a pesquisa não formar candidata. */
  @Test
  void completesNewFlowWithoutCustomerGateWhenResearchFindsNoCandidate() {
    ProductDiscoveryCycle cycle = researchCycle(44L, "lease-44", "research");
    cycle.setResearchMode(ProductDiscoveryResearchMode.DISCOVER_MARKETS);
    when(cycleRepository.findById(44L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(44L)).thenReturn(List.of());
    ProductDiscoveryService service = serviceWithCustomerInterviews();

    ProductDiscoveryCycleDetailResponse response =
        service.complete(
            44L,
            new ProductDiscoveryResultRequest(
                "lease-44", "Nenhuma candidata factual foi formada.", List.of()));

    assertThat(response.cycle().status()).isEqualTo(ProductDiscoveryCycleStatus.COMPLETED);
    assertThat(cycle.getStageCode()).isEqualTo("opportunity-gate");
    verify(bpmAuditService).complete(cycle, List.of());
    verify(bpmAuditService, never()).openCandidateGapDeepening(any());
    verify(dossierResearchSyncService).synchronize(44L, List.of());
  }

  /** Deve rejeitar consultas executadas que foram atribuídas à candidata errada. */
  @Test
  void rejectsGapReportWithQueriesSwappedBetweenCandidates() throws Exception {
    ProductDiscoveryCycle cycle = researchCycle(45L, "lease-45", "candidate-gap-deepening");
    ProductDiscoveryOpportunity first = opportunity(cycle, 841L, "Ocasião especial");
    ProductDiscoveryOpportunity second = opportunity(cycle, 842L, "Encontro importante");
    cycle.setResearchPlanJson(gapPlan("INITIAL_SCOPE", "Lacunas iniciais", "q1", "q2"));
    when(cycleRepository.findById(45L)).thenReturn(Optional.of(cycle));
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(45L))
        .thenReturn(List.of(first, second));
    when(customerInterviewService.get(45L)).thenReturn(readyGap(45L, 5));
    ProductDiscoveryService service = serviceWithCustomerInterviews();
    var evidenceReport =
        new ObjectMapper()
            .readTree(
                """
                {
                  "gapDeepening": {
                    "customerInterviewCount": 5,
                    "candidateCount": 2,
                    "plannedSearchRequests": 2,
                    "actualSearchRequests": 2,
                    "executedQueries": ["q1", "q2"],
                    "unexecutedQueries": [],
                    "estimatedSearchCostUsd": 0.01000000,
                    "searchCostCoverage": "ESTIMATED_SEARCH_ONLY",
                    "modelInvocationCount": 2,
                    "modelCostCoverage": "AGENT_TASK_AUDIT_AFTER_CALLBACK",
                    "pricingSource": "https://brave.com/search/api/",
                    "pricingObservedOn": "2026-09-23",
                    "resolvedGaps": [
                      {
                        "candidateName": "Ocasião especial",
                        "pendingQuestion": "Quando vira ação?",
                        "appropriateSource": "Relato e oferta",
                        "evidenceNeeded": "Compra e desistência",
                        "contraryEvidenceSought": "Alternativa suficiente",
                        "executedQueries": ["q2"],
                        "resolutionQueries": [],
                        "resolutionEvidenceIds": [],
                        "resolutionBasis": "NO_RESOLUTION_EVIDENCE",
                        "status": "STILL_OPEN"
                      },
                      {
                        "candidateName": "Encontro importante",
                        "pendingQuestion": "Quando vira ação?",
                        "appropriateSource": "Relato e oferta",
                        "evidenceNeeded": "Compra e desistência",
                        "contraryEvidenceSought": "Alternativa suficiente",
                        "executedQueries": ["q1"],
                        "resolutionQueries": [],
                        "resolutionEvidenceIds": [],
                        "resolutionBasis": "NO_RESOLUTION_EVIDENCE",
                        "status": "STILL_OPEN"
                      }
                    ]
                  }
                }
                """);

    assertThatThrownBy(
            () ->
                service.completeGapDeepening(
                    45L,
                    new ProductDiscoveryResultRequest(
                        "lease-45",
                        "Consultas trocadas não comprovam as lacunas.",
                        List.of(
                            researchableResult(first.getName(), "{}"),
                            researchableResult(second.getName(), "{}")),
                        evidenceReport,
                        null)))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("vincular fontes e lacunas");
    verify(opportunityRepository, never()).save(any(ProductDiscoveryOpportunity.class));
  }

  /** Deve bloquear callbacks que excedem o teto de chamadas do modelo. */
  @Test
  void rejectsGapReportAboveModelInvocationLimit() throws Exception {
    ProductDiscoveryCycle cycle = researchCycle(46L, "lease-46", "candidate-gap-deepening");
    ProductDiscoveryOpportunity current = opportunity(cycle, 851L, "Imagem para ocasião especial");
    cycle.setResearchPlanJson(singleCandidateGapPlan(current.getName(), "q1", "q2"));
    when(cycleRepository.findById(46L)).thenReturn(Optional.of(cycle));
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(46L)).thenReturn(List.of(current));
    when(customerInterviewService.get(46L)).thenReturn(readyGap(46L, 5));
    ProductDiscoveryService service = serviceWithCustomerInterviews();

    assertThatThrownBy(
            () ->
                service.completeGapDeepening(
                    46L,
                    new ProductDiscoveryResultRequest(
                        "lease-46",
                        "Consumo acima do limite.",
                        List.of(researchableResult(current.getName(), "{}")),
                        singleCandidateGapEvidence(current.getName(), 5),
                        null)))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("chamadas de modelo");
    verify(opportunityRepository, never()).save(any(ProductDiscoveryOpportunity.class));
  }

  /** Deve separar o plano rejeitado das buscas realmente tentadas e cobradas. */
  @Test
  void acceptsUnexecutedQueriesFromRepeatedLensWithoutChargingThem() throws Exception {
    ProductDiscoveryCycle cycle = researchCycle(48L, "lease-48", "candidate-gap-deepening");
    ProductDiscoveryOpportunity first = opportunity(cycle, 871L, "Ocasião especial");
    ProductDiscoveryOpportunity second = opportunity(cycle, 872L, "Encontro importante");
    cycle.setResearchPlanJson(
        "{\"attempts\":[{\"attemptNumber\":1,\"plan\":"
            + gapPlan("INITIAL_SCOPE", "Lacunas iniciais", "q1", "q2")
            + "},{\"attemptNumber\":2,\"plan\":"
            + gapPlan("ADJACENT_PAID_ALTERNATIVE", "Contraponto repetido", "q3", "q4")
            + "}]}");
    when(cycleRepository.findById(48L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(48L))
        .thenReturn(List.of(first, second));
    when(customerInterviewService.get(48L)).thenReturn(readyGap(48L, 5));
    ProductDiscoveryService service = serviceWithCustomerInterviews();
    var evidenceReport =
        new ObjectMapper()
            .readTree(
                """
                {
                  "gapDeepening": {
                    "customerInterviewCount": 5,
                    "candidateCount": 2,
                    "plannedSearchRequests": 4,
                    "actualSearchRequests": 2,
                    "executedQueries": ["q1", "q2"],
                    "unexecutedQueries": ["q3", "q4"],
                    "estimatedSearchCostUsd": 0.01000000,
                    "searchCostCoverage": "ESTIMATED_SEARCH_ONLY",
                    "modelInvocationCount": 3,
                    "modelCostCoverage": "AGENT_TASK_AUDIT_AFTER_CALLBACK",
                    "pricingSource": "https://brave.com/search/api/",
                    "pricingObservedOn": "2026-09-23",
                    "resolvedGaps": [
                      {
                        "candidateName": "Ocasião especial",
                        "pendingQuestion": "Quando vira ação?",
                        "appropriateSource": "Relato e oferta",
                        "evidenceNeeded": "Compra e desistência",
                        "contraryEvidenceSought": "Alternativa suficiente",
                        "executedQueries": ["q1"],
                        "resolutionQueries": [],
                        "resolutionEvidenceIds": [],
                        "resolutionBasis": "NO_RESOLUTION_EVIDENCE",
                        "status": "STILL_OPEN"
                      },
                      {
                        "candidateName": "Encontro importante",
                        "pendingQuestion": "Quando vira ação?",
                        "appropriateSource": "Relato e oferta",
                        "evidenceNeeded": "Compra e desistência",
                        "contraryEvidenceSought": "Alternativa suficiente",
                        "executedQueries": ["q2"],
                        "resolutionQueries": [],
                        "resolutionEvidenceIds": [],
                        "resolutionBasis": "NO_RESOLUTION_EVIDENCE",
                        "status": "STILL_OPEN"
                      }
                    ]
                  }
                }
                """);

    ProductDiscoveryCycleDetailResponse response =
        service.completeGapDeepening(
            48L,
            new ProductDiscoveryResultRequest(
                "lease-48",
                "A lente repetida não gerou consumo adicional.",
                List.of(
                    researchableResult(first.getName(), "{}"),
                    researchableResult(second.getName(), "{}")),
                evidenceReport,
                null));

    assertThat(response.cycle().status()).isEqualTo(ProductDiscoveryCycleStatus.COMPLETED);
    verify(dossierResearchSyncService).synchronize(48L, List.of(first, second));
  }

  /** Deve converter campo final ausente em resposta contratual 422, nunca erro interno. */
  @Test
  void rejectsMalformedGapReportAsUnprocessableEntity() throws Exception {
    ProductDiscoveryCycle cycle = researchCycle(47L, "lease-47", "candidate-gap-deepening");
    ProductDiscoveryOpportunity current = opportunity(cycle, 861L, "Imagem para ocasião especial");
    cycle.setResearchPlanJson(singleCandidateGapPlan(current.getName(), "q1", "q2"));
    when(cycleRepository.findById(47L)).thenReturn(Optional.of(cycle));
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(47L)).thenReturn(List.of(current));
    when(customerInterviewService.get(47L)).thenReturn(readyGap(47L, 5));
    ProductDiscoveryService service = serviceWithCustomerInterviews();
    var evidenceReport = singleCandidateGapEvidence(current.getName(), 2);
    ((com.fasterxml.jackson.databind.node.ObjectNode)
            evidenceReport.path("gapDeepening").path("resolvedGaps").get(0))
        .remove("appropriateSource");

    assertThatThrownBy(
            () ->
                service.completeGapDeepening(
                    47L,
                    new ProductDiscoveryResultRequest(
                        "lease-47",
                        "Relatório estruturalmente incompleto.",
                        List.of(researchableResult(current.getName(), "{}")),
                        evidenceReport,
                        null)))
        .isInstanceOfSatisfying(
            org.springframework.web.server.ResponseStatusException.class,
            exception -> {
              assertThat(exception.getStatusCode().value()).isEqualTo(422);
              assertThat(exception.getReason()).contains("appropriateSource");
            });
    verify(opportunityRepository, never()).save(any(ProductDiscoveryOpportunity.class));
  }

  /** Deve abrir imediatamente a execução BPM ao criar um novo ciclo de descoberta. */
  @Test
  void opensBpmExecutionWhenCycleIsCreated() {
    when(cycleRepository.save(any(ProductDiscoveryCycle.class)))
        .thenAnswer(
            invocation -> {
              ProductDiscoveryCycle cycle = invocation.getArgument(0);
              cycle.setId(37L);
              return cycle;
            });
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    ProductDiscoveryCycleResponse response =
        service.createCycle(
            new CreateProductDiscoveryCycleRequest(
                "Auditoria de saída de imóvel",
                "Locatários",
                "BR",
                "pt-BR",
                "Instagram",
                "B2C",
                null,
                "Descobrir oportunidade PDE comparável ao Rigel."));

    assertThat(response.id()).isEqualTo(37L);
    verify(bpmAuditService).open(any(ProductDiscoveryCycle.class));
  }

  /** Deve retomar o handoff atual sem recriar ou reexecutar a pesquisa factual de Argos. */
  @Test
  void resumesPrivateValidationFromCompletedFactualCycle() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(22L);
    cycle.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
    ProductDiscoveryOpportunity ready = new ProductDiscoveryOpportunity();
    ready.setCycle(cycle);
    ready.setMaturity(ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    ProductDiscoveryOpportunity researching = new ProductDiscoveryOpportunity();
    researching.setCycle(cycle);
    researching.setMaturity(ProductDiscoveryOpportunityMaturity.RESEARCHABLE);
    when(cycleRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(cycle));
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(22L))
        .thenReturn(List.of(ready, researching));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    ProductDiscoveryPrivateValidationHandoffResponse response =
        service.resumePrivateValidationHandoff(22L);

    assertThat(response.cycleId()).isEqualTo(22L);
    assertThat(response.sourceReference()).isEqualTo("product-discovery-cycle:22");
    assertThat(response.dossierReadyCount()).isEqualTo(1);
    assertThat(response.status()).isEqualTo("QUEUED_FOR_PRIVATE_VALIDATION");
    assertThat(response.nextActivity()).isEqualTo("ATENA_PRIVATE_PROTOTYPE_SELECTION");
    verify(dossierResearchSyncService).synchronize(22L, List.of(ready, researching));
  }

  /** Deve preservar ciclos ainda em pesquisa e impedir um handoff comercial prematuro. */
  @Test
  void blocksPrivateValidationHandoffBeforeResearchCompletion() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(23L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    when(cycleRepository.findByIdForUpdate(23L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    assertThatThrownBy(() -> service.resumePrivateValidationHandoff(23L))
        .hasMessageContaining("pesquisa factual concluída");
  }

  /** Deve impedir a criação de tarefas quando Argos ainda não formou um dossiê pronto. */
  @Test
  void blocksPrivateValidationHandoffWithoutReadyDossier() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(24L);
    cycle.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
    ProductDiscoveryOpportunity researching = new ProductDiscoveryOpportunity();
    researching.setCycle(cycle);
    researching.setMaturity(ProductDiscoveryOpportunityMaturity.RESEARCHABLE);
    when(cycleRepository.findByIdForUpdate(24L)).thenReturn(Optional.of(cycle));
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(24L))
        .thenReturn(List.of(researching));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    assertThatThrownBy(() -> service.resumePrivateValidationHandoff(24L))
        .hasMessageContaining("não possui dossiê factual pronto");
  }

  /** Deve preservar plano, resposta bruta e modelo sem credenciais de marketplace. */
  @Test
  void registerDirectedResearchPlan() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(20L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-20");
    when(cycleRepository.findById(20L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    ProductDiscoveryResearchPlanResponse response =
        service.registerResearchPlan(
            20L,
            new ProductDiscoveryResearchPlanRequest(
                "lease-20",
                "{\"marketplaceRequests\":[{\"marketplace\":\"HOTMART\"}]}",
                "{\"questions\":[\"Quais produtos vendem?\"]}",
                "gpt-5.6-sol"));

    assertThat(response.cycleId()).isEqualTo(20L);
    assertThat(response.model()).isEqualTo("gpt-5.6-sol");
    assertThat(cycle.getResearchPlanRawResponse()).contains("Quais produtos vendem?");
    verify(bpmAuditService).recordPlan(eq(cycle), any(ProductDiscoveryResearchPlanRequest.class));
  }

  /** Deve concluir sem oportunidade quando a busca real não trouxer evidência suficiente. */
  @Test
  void completeWithoutArtificialOpportunityWhenResearchIsEmpty() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(20L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-20");
    when(cycleRepository.findById(20L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(20L)).thenReturn(List.of());
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    ProductDiscoveryCycleDetailResponse response =
        service.complete(
            20L,
            new ProductDiscoveryResultRequest(
                "lease-20", "Nenhuma evidência real encontrada; pesquisar mais.", List.of()));

    assertThat(response.cycle().status()).isEqualTo(ProductDiscoveryCycleStatus.COMPLETED);
    assertThat(response.opportunities()).isEmpty();
    assertThat(cycle.getDecisionSummary()).contains("pesquisar mais");
    verify(bpmAuditService).complete(cycle, List.of());
  }

  /** Deve concluir a pesquisa dirigida sem inventar oportunidade quando faltarem ofertas reais. */
  @Test
  void completesDirectedResearchWithoutOpportunityWhenMarketplaceEvidenceIsInsufficient() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(23L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-23");
    cycle.setResearchPlanJson("{\"marketplaceRequests\":[{\"marketplace\":\"HOTMART\"}]}");
    when(cycleRepository.findById(23L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(23L)).thenReturn(List.of());
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    ProductDiscoveryCycleDetailResponse response =
        service.complete(
            23L,
            new ProductDiscoveryResultRequest(
                "lease-23",
                "Pesquisa dirigida sem ofertas comparáveis suficientes; pesquisar mais.",
                List.of()));

    assertThat(response.cycle().status()).isEqualTo(ProductDiscoveryCycleStatus.COMPLETED);
    assertThat(response.opportunities()).isEmpty();
    assertThat(cycle.getDecisionSummary()).contains("pesquisar mais");
  }

  /** Preserva candidata factual imatura e a auditoria da síntese sem promovê-la a aprovação. */
  @Test
  void persistsResearchMoreCandidateAndAnalysisAudit() throws Exception {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(27L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-27");
    cycle.setResearchMode(ProductDiscoveryResearchMode.DISCOVER_MARKETS);
    cycle.setResearchPlanJson("{\"marketplaceRequests\":[{\"marketplace\":\"HOTMART\"}]}");
    when(cycleRepository.findById(27L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(27L)).thenReturn(List.of());
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Decisão de imagem profissional após retorno ao trabalho",
            "Mulheres de 40 a 55 anos retomando a carreira",
            "Insegurança para traduzir a experiência em presença profissional",
            "Organizar referências e decisões consome tempo",
            "Receio de parecer desatualizada",
            "Duas fontes públicas independentes descrevem a mesma situação",
            "Alternativas existentes ainda exigem montagem manual",
            "Apenas a fronteira factual de redução do esforço, sem desenhar a oferta",
            null,
            "Cobertura Meta e dez ofertas comparáveis ainda ausentes",
            "{\"marketplaceOffers\":[{\"marketplace\":\"HOTMART\",\"referenceId\":\"1\",\"title\":\"Imagem profissional\"}]}",
            new BigDecimal("58"),
            ProductDiscoveryOpportunityMaturity.RESEARCHABLE,
            ProductDiscoveryOpportunityDecision.RESEARCH_MORE);
    ProductDiscoveryAnalysisAuditRequest audit =
        new ProductDiscoveryAnalysisAuditRequest(
            "{\"decisionSummary\":\"Pesquisar mais\",\"candidates\":[]}",
            "gpt-5.6-sol",
            "MODEL",
            "Prompt completo",
            "Constituição de Argos",
            "Contexto factual",
            "high",
            120L,
            20L,
            35L,
            List.of());
    var evidenceReport =
        new ObjectMapper()
            .readTree(
                "{\"researchMode\":\"DISCOVER_MARKETS\",\"sourceCoverage\":{\"publicWeb\":2}}");

    ProductDiscoveryCycleDetailResponse response =
        service.complete(
            27L,
            new ProductDiscoveryResultRequest(
                "lease-27",
                "Candidata factual preservada; pesquisa comercial ainda necessária.",
                List.of(opportunity, opportunity),
                evidenceReport,
                audit));

    assertThat(response.cycle().status()).isEqualTo(ProductDiscoveryCycleStatus.COMPLETED);
    assertThat(cycle.getResearchAnalysisModel()).isEqualTo("gpt-5.6-sol");
    assertThat(cycle.getResearchAnalysisRawResponse()).contains("decisionSummary");
    assertThat(cycle.getResearchEvidenceReportJson()).contains("DISCOVER_MARKETS");
    verify(opportunityRepository, times(2)).save(any(ProductDiscoveryOpportunity.class));
    verify(bpmAuditService).recordAnalysis(cycle, audit);
  }

  /** Deve bloquear uma síntese autônoma que não compare ao menos duas candidatas factuais. */
  @Test
  void blocksAutonomousDiscoveryWithOnlyOneCandidate() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(30L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-30");
    cycle.setResearchMode(ProductDiscoveryResearchMode.DISCOVER_MARKETS);
    when(cycleRepository.findById(30L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Candidata isolada",
            "Público",
            "Dor",
            null,
            null,
            null,
            null,
            null,
            null,
            "Comparação factual ausente",
            "{}",
            new BigDecimal("45"),
            ProductDiscoveryOpportunityMaturity.RESEARCHABLE,
            ProductDiscoveryOpportunityDecision.RESEARCH_MORE);

    assertThatThrownBy(
            () ->
                service.complete(
                    30L,
                    new ProductDiscoveryResultRequest(
                        "lease-30", "Apenas uma candidata", List.of(opportunity))))
        .hasMessageContaining("duas a três candidatas factuais");
  }

  /** Deve rejeitar aprovação que contradiz a maturidade factual declarada pela candidata. */
  @Test
  void blocksApprovalWithoutDossierReadyMaturity() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(28L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-28");
    when(cycleRepository.findById(28L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Sinal imaturo",
            "Público",
            "Dor",
            null,
            null,
            null,
            null,
            null,
            null,
            "Ainda faltam fontes",
            "{}",
            new BigDecimal("45"),
            ProductDiscoveryOpportunityMaturity.RESEARCHABLE,
            ProductDiscoveryOpportunityDecision.APPROVE);

    assertThatThrownBy(
            () ->
                service.complete(
                    28L,
                    new ProductDiscoveryResultRequest(
                        "lease-28", "Aprovação contraditória", List.of(opportunity))))
        .hasMessageContaining("DOSSIER_READY");
  }

  /** Deve impedir que uma candidata sensível contorne o gate humano usando maturidade pronta. */
  @Test
  void blocksDossierReadyThatStillRequiresHumanReview() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(29L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-29");
    when(cycleRepository.findById(29L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Situação sensível",
            "Público",
            "Dor",
            null,
            null,
            null,
            null,
            null,
            null,
            "Revisão humana obrigatória",
            "{}",
            new BigDecimal("70"),
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY,
            ProductDiscoveryOpportunityDecision.HUMAN_REVIEW);

    assertThatThrownBy(
            () ->
                service.complete(
                    29L,
                    new ProductDiscoveryResultRequest(
                        "lease-29", "Revisão obrigatória", List.of(opportunity))))
        .hasMessageContaining("HUMAN_REVIEW exige HUMAN_REVIEW");
  }

  /** Deve bloquear a conclusao dirigida quando faltarem dez ofertas reais comparaveis. */
  @Test
  void blocksDirectedResearchWithoutMinimumMarketplaceEvidence() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(21L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-21");
    cycle.setResearchPlanJson("{\"marketplaceRequests\":[{\"marketplace\":\"HOTMART\"}]}");
    when(cycleRepository.findById(21L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Teste",
            "Publico",
            "Dor",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "{\"marketplaceOffers\":[{\"marketplace\":\"HOTMART\",\"referenceId\":\"1\"}]}",
            new BigDecimal("50"),
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY,
            ProductDiscoveryOpportunityDecision.RESEARCH_MORE);

    assertThatThrownBy(
            () ->
                service.complete(
                    21L,
                    new ProductDiscoveryResultRequest(
                        "lease-21", "Pesquisar mais", List.of(opportunity))))
        .hasMessageContaining("10 ofertas reais comparaveis");
  }

  /** Deve reconhecer dez páginas comerciais públicas depois da normalização canônica. */
  @Test
  void acceptsPublicWebOffersInDirectedMarketplaceGate() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(31L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-31");
    cycle.setResearchPlanJson("{\"marketplaceRequests\":[{\"marketplace\":\"HOTMART\"}]}");
    when(cycleRepository.findById(31L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(31L)).thenReturn(List.of());
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    String offers =
        java.util.stream.IntStream.range(0, 10)
            .mapToObj(
                index ->
                    "{\"marketplace\":\"PUBLIC_WEB\",\"referenceId\":\"public-"
                        + index
                        + "\",\"title\":\"Programa digital "
                        + index
                        + "\",\"producer\":\"Produtor público\"}")
            .collect(java.util.stream.Collectors.joining(","));
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Teste",
            "Publico",
            "Dor",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "{\"marketplaceOffers\":[" + offers + "]}",
            new BigDecimal("70"),
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY,
            ProductDiscoveryOpportunityDecision.RESEARCH_MORE);

    ProductDiscoveryCycleDetailResponse response =
        service.complete(
            31L,
            new ProductDiscoveryResultRequest(
                "lease-31", "Dossiê pronto para Atena", List.of(opportunity)));

    assertThat(response.cycle().status()).isEqualTo(ProductDiscoveryCycleStatus.COMPLETED);
    verify(opportunityRepository).save(any(ProductDiscoveryOpportunity.class));
  }

  /** Deve contar snapshots repetidos do mesmo produto como uma única oferta comparável. */
  @Test
  void blocksDuplicatedMarketplaceSnapshotsFromInflatingEvidence() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(25L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-25");
    cycle.setResearchPlanJson("{\"marketplaceRequests\":[{\"marketplace\":\"HOTMART\"}]}");
    when(cycleRepository.findById(25L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    String offers =
        java.util.stream.IntStream.range(0, 10)
            .mapToObj(
                index ->
                    "{\"marketplace\":\"HOTMART\",\"referenceId\":\"snapshot-"
                        + index
                        + "\",\"title\":\""
                        + (index % 2 == 0 ? "Mesmo produto" : "MESMO PRODUTO!")
                        + "\",\"producer\":\"Mesmo produtor\"}")
            .collect(java.util.stream.Collectors.joining(","));
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Teste",
            "Publico",
            "Dor",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "{\"marketplaceOffers\":[" + offers + "]}",
            new BigDecimal("50"),
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY,
            ProductDiscoveryOpportunityDecision.APPROVE);

    assertThatThrownBy(
            () ->
                service.complete(
                    25L,
                    new ProductDiscoveryResultRequest(
                        "lease-25", "Pesquisar mais", List.of(opportunity))))
        .hasMessageContaining("recebidas 1");
  }

  /** Deve impedir que anúncios sejam contados como ofertas pagas comparáveis. */
  @Test
  void blocksMetaAdsFromInflatingMarketplaceEvidence() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(26L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-26");
    cycle.setResearchPlanJson("{\"marketplaceRequests\":[{\"marketplace\":\"HOTMART\"}]}");
    when(cycleRepository.findById(26L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    String paidOffers =
        java.util.stream.IntStream.range(0, 9)
            .mapToObj(
                index ->
                    "{\"marketplace\":\"HOTMART\",\"referenceId\":\"paid-"
                        + index
                        + "\",\"title\":\"Oferta paga "
                        + index
                        + "\"}")
            .collect(java.util.stream.Collectors.joining(","));
    String ads =
        java.util.stream.IntStream.range(0, 5)
            .mapToObj(
                index ->
                    "{\"marketplace\":\"META_AD_LIBRARY\",\"referenceId\":\"ad-"
                        + index
                        + "\",\"title\":\"Anúncio "
                        + index
                        + "\"}")
            .collect(java.util.stream.Collectors.joining(","));
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Teste",
            "Publico",
            "Dor",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "{\"marketplaceOffers\":[" + paidOffers + "," + ads + "]}",
            new BigDecimal("50"),
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY,
            ProductDiscoveryOpportunityDecision.APPROVE);

    assertThatThrownBy(
            () ->
                service.complete(
                    26L,
                    new ProductDiscoveryResultRequest(
                        "lease-26", "Pesquisar mais", List.of(opportunity))))
        .hasMessageContaining("recebidas 9");
  }

  /** Deve bloquear maturidade pronta quando a cobertura Instagram não foi comprovada. */
  @Test
  void blocksDossierReadyWithoutObservedInstagramMetaEvidence() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(29L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-29");
    cycle.setAcquisitionChannel("Instagram");
    cycle.setCommercialConstraints("B2C e mobile");
    when(cycleRepository.findById(29L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Rotina visual 40+",
            "Mulheres 40+",
            "Decisão visual ainda exige montagem",
            null,
            null,
            null,
            null,
            null,
            null,
            "Meta ainda não observada",
            "{\"purchaseMomentGate\":{\"required\":true},\"metaAdEvidence\":[],\"metaCoverage\":[]}",
            new BigDecimal("72"),
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY,
            ProductDiscoveryOpportunityDecision.RESEARCH_MORE);

    assertThatThrownBy(
            () ->
                service.complete(
                    29L,
                    new ProductDiscoveryResultRequest(
                        "lease-29", "Dossiê declarado pronto", List.of(opportunity))))
        .hasMessageContaining("Biblioteca Meta para Instagram");
  }

  /** Deve impedir que o worker aprove B2C no Instagram antes das duas leituras privadas. */
  @Test
  void blocksInstagramB2cApprovalWithoutPurchaseMomentReadings() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(24L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-24");
    cycle.setAcquisitionChannel("Instagram");
    cycle.setCommercialConstraints("B2C e mobile");
    when(cycleRepository.findById(24L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Ensaio profissional",
            "Pessoa física com entrevista marcada",
            "Receio de travar na entrevista",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "{\"purchaseMomentGate\":{\"required\":true,\"sourceQualityPassed\":true,\"finalPrioritizationEligible\":false}}",
            new BigDecimal("83"),
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY,
            ProductDiscoveryOpportunityDecision.APPROVE);

    assertThatThrownBy(
            () ->
                service.complete(
                    24L,
                    new ProductDiscoveryResultRequest(
                        "lease-24", "Aprovar candidata", List.of(opportunity))))
        .hasMessageContaining("duas leituras válidas do Momento de Compra");
  }

  /** Deve rejeitar booleanos de aprovação quando as leituras auditáveis não foram enviadas. */
  @Test
  void blocksPurchaseMomentBooleanClaimWithoutAuditableReadings() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(25L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-25");
    cycle.setAcquisitionChannel("Instagram");
    cycle.setCommercialConstraints("B2C e mobile");
    when(cycleRepository.findById(25L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Ensaio profissional",
            "Pessoa física com entrevista marcada",
            "Receio de travar na entrevista",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            """
            {"purchaseMomentGate":{"required":true,"status":"PASS","sourceQualityPassed":true,"finalPrioritizationEligible":true,"minimumIndependentReadings":2,"sourceQuality":{"passed":true,"evaluatedAt":"2026-08-26T10:00:00Z","maxAgeDays":30,"reasons":[]},"eligibleCandidateNames":["Ensaio profissional"],"candidates":[]}}
            """,
            new BigDecimal("83"),
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY,
            ProductDiscoveryOpportunityDecision.APPROVE);

    assertThatThrownBy(
            () ->
                service.complete(
                    25L,
                    new ProductDiscoveryResultRequest(
                        "lease-25", "Aprovar candidata", List.of(opportunity))))
        .hasMessageContaining("duas leituras válidas do Momento de Compra");
  }

  /** Deve aceitar a aprovação somente quando o backend recalcula duas leituras privadas válidas. */
  @Test
  void acceptsInstagramB2cApprovalWithCanonicalPurchaseMomentGate() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(26L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-26");
    cycle.setAcquisitionChannel("Instagram");
    cycle.setCommercialConstraints("B2C e mobile");
    when(cycleRepository.findById(26L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(26L)).thenReturn(List.of());
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    ProductDiscoveryOpportunityResultRequest opportunity =
        new ProductDiscoveryOpportunityResultRequest(
            "Ensaio profissional",
            "Pessoa física com entrevista marcada",
            "Receio de travar na entrevista",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            """
            {
              "metaAdEvidence": [{
                "active": true,
                "publisherPlatforms": ["INSTAGRAM"]
              }],
              "metaCoverage": [{
                "publisherPlatform": "INSTAGRAM",
                "sourceStatus": "OBSERVED",
                "activeAds": 1
              }],
              "purchaseMomentGate": {
                "required": true,
                "status": "PASS",
                "sourceQualityPassed": true,
                "finalPrioritizationEligible": true,
                "minimumIndependentReadings": 2,
                "sourceQuality": {
                  "passed": true,
                  "evaluatedAt": "2026-08-26T10:00:00Z",
                  "maxAgeDays": 30,
                  "reasons": []
                },
                "successCriteria": {
                  "declaredAt": "2026-08-25T08:00:00Z",
                  "minimumEligibleParticipantsPerReading": 5,
                  "minimumExperienceStartRate": 0.7,
                  "minimumValueMomentRate": 0.6,
                  "minimumReadyResultUseRate": 0.6,
                  "minimumPrototypePreferenceRate": 0.6,
                  "minimumCheckoutStartRate": 0.2
                },
                "eligibleCandidateNames": ["Ensaio profissional"],
                "candidates": [{
                  "candidateName": "Ensaio profissional",
                  "status": "PASS",
                  "eligibleForFinalPrioritization": true,
                  "scene": {
                    "trigger": "Entrevista marcada",
                    "deadline": "Sete dias",
                    "costOfError": "Perda de renda",
                    "budgetEvidence": "Compara simuladores pagos",
                    "failedAttempt": "Ensaio sem feedback",
                    "currentPaidBehavior": "Compra preparação profissional"
                  },
                  "freeAlternative": {
                    "name": "Ensaio sozinho com ChatGPT",
                    "prototypeAdvantage": "Compara duas respostas faladas"
                  },
                  "humanValueDelivery": {
                    "territories": ["RECOGNITION", "EFFORT_RELIEF"],
                    "desiredTransformation": "Demonstrar a própria capacidade com menos esforço",
                    "evidenceSourceIds": ["study-1", "community-1"],
                    "evidencePathways": ["STRUCTURED_TRAINING", "CURRENT_LANGUAGE"],
                    "readyMadeOutcome": "Diagnóstico visual pronto para novo ensaio",
                    "minimumCustomerInput": "Vaga, pergunta e gravação curta",
                    "requiresPromptEngineering": false,
                    "requiresManualAssembly": false,
                    "usableWithoutAiKnowledge": true,
                    "customerStepsToValue": 3,
                    "timeToUsableResultMinutes": 8,
                    "automationBoundary": "A pessoa revisa e nenhuma experiência é inventada"
                  },
                  "prototype": {
                    "prototypeId": "PRIVATE-1",
                    "private": true,
                    "published": false,
                    "paymentEnabled": false,
                    "mediaSpend": 0,
                    "testMarker": "PRIVATE_PROTOTYPE"
                  },
                  "readings": [
                    {
                      "readingId": "R1",
                      "observedAt": "2026-08-25T12:00:00Z",
                      "eligibleParticipants": 5,
                      "experienceStarted": 5,
                      "valueMoments": 4,
                      "readyResultsUsedWithoutAssembly": 4,
                      "prototypePreferredOverFree": 4,
                      "checkoutStarted": 2,
                      "psiqueDecision": "APPROVE",
                      "temisDecision": "APPROVE",
                      "eventSource": "FIRST_PARTY_EVENTS",
                      "testMarker": "PRIVATE_PROTOTYPE",
                      "passed": true
                    },
                    {
                      "readingId": "R2",
                      "observedAt": "2026-08-26T12:00:00Z",
                      "eligibleParticipants": 5,
                      "experienceStarted": 4,
                      "valueMoments": 3,
                      "readyResultsUsedWithoutAssembly": 3,
                      "prototypePreferredOverFree": 4,
                      "checkoutStarted": 1,
                      "psiqueDecision": "APPROVE",
                      "temisDecision": "APPROVE",
                      "eventSource": "FIRST_PARTY_EVENTS",
                      "testMarker": "PRIVATE_PROTOTYPE",
                      "passed": true
                    }
                  ]
                }]
              }
            }
            """,
            new BigDecimal("83"),
            ProductDiscoveryOpportunityMaturity.DOSSIER_READY,
            ProductDiscoveryOpportunityDecision.APPROVE);

    ProductDiscoveryCycleDetailResponse response =
        service.complete(
            26L,
            new ProductDiscoveryResultRequest(
                "lease-26", "Aprovar candidata", List.of(opportunity)));

    assertThat(response.cycle().status()).isEqualTo(ProductDiscoveryCycleStatus.COMPLETED);
    verify(opportunityRepository).save(any(ProductDiscoveryOpportunity.class));
  }

  /**
   * Recupera ciclo abandonado com novo lease, tentativa auditável e curadoria no contrato de Argos.
   */
  @Test
  void recoversExpiredResearchExecution() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(22L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-expirado");
    cycle.setLeaseExpiresAt(Instant.parse("2026-08-24T08:00:00Z"));
    cycle.setExecutionAttempt(1);
    ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
    when(cycleRepository.findClaimableForUpdate(
            eq("research"),
            eq(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH),
            eq(ProductDiscoveryCycleStatus.RESEARCHING),
            any(Instant.class),
            any(Instant.class),
            eq(false),
            page.capture()))
        .thenReturn(List.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    var research =
        org.mockito.Mockito.mock(
            com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService.class);
    var selection =
        new com.marketinghub.researchintelligence.v1.service.select
            .ResearchIntelligenceSelectionResponse(
            "HARNESS_RESEARCH_INTELLIGENCE_V1",
            "hash-teste",
            1,
            List.of(),
            List.of("Evidência externa não comprova venda."));
    when(research.selectForAgentTask(
            "market-radar", cycle.getTheme(), cycle.getTargetAudience(), cycle.getObjective()))
        .thenReturn(selection);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "researchIntelligenceService", research);
    List<ProductDiscoveryPendingResponse> pending = service.pending();

    assertThat(pending).hasSize(1);
    assertThat(pending.getFirst().researchIntelligence()).isEqualTo(selection);
    assertThat(pending.getFirst().executionAttempt()).isEqualTo(2);
    assertThat(pending.getFirst().executionLeaseId()).isNotBlank().isNotEqualTo("lease-expirado");
    assertThat(cycle.getLeaseExpiresAt()).isAfter(Instant.now());
    assertThat(page.getValue().getPageSize()).isEqualTo(1);
    verify(dossierResearchSyncService).start(22L);
    verify(bpmAuditService).start(cycle);
  }

  /** Deve refletir no BPM a mesma falha aceita pelo contrato do ciclo de descoberta. */
  @Test
  void blocksBpmExecutionWhenWorkerFails() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(24L);
    cycle.setTheme("Sinistro automotivo travado");
    cycle.setCountry("BR");
    cycle.setLanguage("pt-BR");
    cycle.setStageCode("research");
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setExecutionLeaseId("lease-24");
    when(cycleRepository.findById(24L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(cycle)).thenReturn(cycle);
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    AgentTaskExecutionAuditRequest executionAudit =
        new AgentTaskExecutionAuditRequest(
            "MODEL",
            "gpt-5.6-sol",
            "high",
            "Núcleo de Argos.\n\nPesquise a oportunidade.",
            "Núcleo de Argos.",
            "Pesquise a oportunidade.",
            List.of());

    ProductDiscoveryCycleResponse response =
        service.fail(
            24L,
            new ProductDiscoveryFailureRequest(
                "lease-24", "Backend rejeitou a conclusão com status 422.", executionAudit));

    assertThat(response.status()).isEqualTo(ProductDiscoveryCycleStatus.FAILED);
    verify(bpmAuditService).fail(cycle, executionAudit);
  }

  /** Preserva saída inválida e consumo antes de bloquear a tarefa, sem promover candidatas. */
  @Test
  void preservesRejectedSynthesisAuditOnFailure() {
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(306L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode("research");
    cycle.setExecutionLeaseId("lease-306");
    cycle.setLeaseExpiresAt(Instant.now().plusSeconds(120));
    when(cycleRepository.findById(306L)).thenReturn(Optional.of(cycle));
    when(cycleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var audit =
        new ProductDiscoveryAnalysisAuditRequest(
            "{\"candidates\":[]}",
            "modelo-testado",
            "MODEL",
            "Prompt enviado",
            "Responsabilidade",
            "Atividade",
            "medium",
            130L,
            20L,
            45L,
            List.of());

    service.fail(
        306L, new ProductDiscoveryFailureRequest("lease-306", "Síntese recusada", null, audit));

    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.FAILED);
    assertThat(cycle.getResearchAnalysisRawResponse()).isEqualTo(audit.rawResponse());
    assertThat(cycle.getResearchAnalysisModel()).isEqualTo("modelo-testado");
    verify(bpmAuditService).recordAnalysis(cycle, audit);
    verify(bpmAuditService).fail(cycle, null);
    verify(opportunityRepository, never()).saveAll(any());
  }

  /** Deve rejeitar callback atrasado depois que outra tentativa assumiu o ciclo. */
  @Test
  void rejectsCallbackFromReplacedExecution() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(23L);
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setExecutionLeaseId("lease-atual");
    when(cycleRepository.findById(23L)).thenReturn(Optional.of(cycle));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    assertThatThrownBy(
            () ->
                service.fail(
                    23L, new ProductDiscoveryFailureRequest("lease-antigo", "worker interrompido")))
        .hasMessageContaining("Lease da execução");
  }

  /** Deve manter renda extra como primeira trilha de pesquisa recomendada com travas comerciais. */
  @Test
  void getMaturityRanking() {
    when(opportunityRepository.findTop50ByCycleStatusOrderByScoreDesc(
            ProductDiscoveryCycleStatus.COMPLETED))
        .thenReturn(List.of());
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    ProductDiscoveryMaturityRankingResponse ranking = service.getMaturityRanking();

    assertThat(ranking.strategyName()).isEqualTo("Ranking por maturidade comercial");
    assertThat(ranking.items())
        .extracting(ProductDiscoveryMaturityItemResponse::niche)
        .contains("Renda extra");
    assertThat(ranking.recommendedTracks())
        .first()
        .extracting(ProductDiscoveryResearchTrackResponse::name)
        .isEqualTo("Renda extra para autônomos/MEIs");
    assertThat(ranking.recommendedTracks().getFirst().forbiddenCategories())
        .contains("Promessa de renda garantida");
  }

  /** Deve derivar o ranking dos ciclos persistidos quando houver pesquisa real. */
  @Test
  void getMaturityRankingFromPersistedOpportunities() {
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    opportunity.setName("Diagnóstico de decisão para agenda de manicure");
    opportunity.setRootPain("Clientes somem e a agenda perde previsibilidade.");
    opportunity.setScaleEvidence("Três fontes independentes.");
    opportunity.setUnmetnessEvidence("Reviews mostram soluções confusas.");
    opportunity.setCommercialRisk("Validar disposição de compra.");
    opportunity.setScore(new BigDecimal("82.00"));
    opportunity.setDecision(ProductDiscoveryOpportunityDecision.APPROVE);
    when(opportunityRepository.findTop50ByCycleStatusOrderByScoreDesc(
            ProductDiscoveryCycleStatus.COMPLETED))
        .thenReturn(List.of(opportunity));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    ProductDiscoveryMaturityRankingResponse ranking = service.getMaturityRanking();

    assertThat(ranking.items()).hasSize(1);
    assertThat(ranking.items().getFirst().niche()).contains("agenda de manicure");
    assertThat(ranking.recommendedPriority()).contains("agenda de manicure");
  }

  /** Deve arquivar apenas ciclos cujo resultado inteiro seja o fallback artificial legado. */
  @Test
  void archiveArtificialLegacyEvidence() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(17L);
    cycle.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    opportunity.setEvidenceJson(
        "[{\"snippet\":\"Busca brave não retornou resultados estruturados suficientes; pesquisar"
            + " mais.\"}]");
    when(cycleRepository.findTop50ByOrderByUpdatedAtDesc()).thenReturn(List.of(cycle));
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(17L))
        .thenReturn(List.of(opportunity));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    ProductDiscoveryLegacyCleanupResponse response = service.archiveArtificialLegacyEvidence();

    assertThat(response.archivedCycles()).isEqualTo(1);
    assertThat(response.archivedOpportunities()).isEqualTo(1);
    assertThat(response.cycleIds()).containsExactly(17L);
    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.ARCHIVED);
    assertThat(cycle.getDecisionSummary()).contains("não comprova dor");
  }

  /** Deve reconhecer também a frase usada pelo primeiro fallback DuckDuckGo. */
  @Test
  void archiveOldestArtificialLegacyEvidence() {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(1L);
    cycle.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    opportunity.setEvidenceJson(
        "[{\"snippet\":\"Busca pública não retornou tópicos estruturados suficientes; pesquisar"
            + " mais.\"}]");
    when(cycleRepository.findTop50ByOrderByUpdatedAtDesc()).thenReturn(List.of(cycle));
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(1L))
        .thenReturn(List.of(opportunity));
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);

    ProductDiscoveryLegacyCleanupResponse response = service.archiveArtificialLegacyEvidence();

    assertThat(response.cycleIds()).containsExactly(1L);
    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.ARCHIVED);
  }

  /** Cria o serviço e injeta o gate adicional usado apenas pela versão nova do processo. */
  private ProductDiscoveryService serviceWithCustomerInterviews() {
    ProductDiscoveryService service =
        new ProductDiscoveryService(
            cycleRepository, opportunityRepository, dossierResearchSyncService, bpmAuditService);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "customerInterviewService", customerInterviewService);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service,
        "gapResearchContractService",
        new ProductDiscoveryGapResearchContractService(opportunityRepository));
    return service;
  }

  /** Monta um ciclo reservado por uma etapa específica do worker. */
  private ProductDiscoveryCycle researchCycle(Long id, String lease, String stageCode) {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(id);
    cycle.setTheme("Desejo e situação de compra");
    cycle.setCountry("BR");
    cycle.setLanguage("pt-BR");
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    cycle.setStageCode(stageCode);
    cycle.setExecutionLeaseId(lease);
    return cycle;
  }

  /** Monta uma candidata persistida cuja identidade deve sobreviver ao aprofundamento. */
  private ProductDiscoveryOpportunity opportunity(
      ProductDiscoveryCycle cycle, Long id, String name) {
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    ReflectionTestUtils.setField(opportunity, "id", id);
    opportunity.setCycle(cycle);
    opportunity.setName(name);
    opportunity.setPrimaryAudience("Mulheres em uma ocasião concreta");
    opportunity.setRootPain("Decisão difícil antes de uma ocasião importante");
    opportunity.setScore(new BigDecimal("60"));
    opportunity.setMaturity(ProductDiscoveryOpportunityMaturity.RESEARCHABLE);
    opportunity.setDecision(ProductDiscoveryOpportunityDecision.RESEARCH_MORE);
    return opportunity;
  }

  /** Monta um resultado imaturo para comprovar que o fluxo não fabrica aprovação. */
  private ProductDiscoveryOpportunityResultRequest researchableResult(
      String name, String evidenceJson) {
    return new ProductDiscoveryOpportunityResultRequest(
        name,
        "Mulheres em uma ocasião concreta",
        "Decisão difícil antes de uma ocasião importante",
        "Comparar opções exige esforço",
        "Busca sentir segurança sem promessa absoluta",
        "Sinais públicos ainda insuficientes",
        "Alternativas deixam trabalho residual",
        "Orientação digital individualizada ainda como hipótese",
        "Situação concreta antes de qualquer oferta",
        "A disposição de compra continua por comprovar",
        evidenceJson,
        new BigDecimal("60"),
        ProductDiscoveryOpportunityMaturity.RESEARCHABLE,
        ProductDiscoveryOpportunityDecision.RESEARCH_MORE);
  }

  /** Gera uma tentativa válida para as duas candidatas fixadas nos testes de histórico. */
  private String gapPlan(String axis, String lens, String firstQuery, String secondQuery) {
    return "{\"researchLens\":\""
        + lens
        + "\",\"expansionAxis\":\""
        + axis
        + "\",\"publicQueries\":[\""
        + firstQuery
        + "\",\""
        + secondQuery
        + "\"],\"candidateGaps\":["
        + gap("Ocasião especial", firstQuery)
        + ","
        + gap("Encontro importante", secondQuery)
        + "],\"researchLimits\":{\"maxPublicQueries\":12,\"maxEstimatedSearchCostUsd\":0.06}}";
  }

  /** Gera o plano mínimo de uma candidata usado na validação do callback final. */
  private String singleCandidateGapPlan(String name, String firstQuery, String secondQuery) {
    return "{\"researchLens\":\"Lacuna da ocasião\",\"expansionAxis\":\"INITIAL_SCOPE\","
        + "\"publicQueries\":[\""
        + firstQuery
        + "\",\""
        + secondQuery
        + "\"],\"candidateGaps\":[{\"candidateName\":\""
        + name
        + "\",\"pendingQuestion\":\"Quando vira ação?\",\"appropriateSource\":\"Relato e oferta\","
        + "\"evidenceNeeded\":\"Compra e desistência\",\"contraryEvidenceToSeek\":\"Alternativa suficiente\","
        + "\"publicQueries\":[\""
        + firstQuery
        + "\",\""
        + secondQuery
        + "\"],\"maxPublicQueries\":2,\"maxEstimatedSearchCostUsd\":0.01}],"
        + "\"researchLimits\":{\"maxPublicQueries\":12,\"maxEstimatedSearchCostUsd\":0.06}}";
  }

  /** Monta uma lacuna unitária com custo estimado equivalente a uma consulta Brave. */
  private String gap(String candidateName, String query) {
    return "{\"candidateName\":\""
        + candidateName
        + "\",\"pendingQuestion\":\"Quando vira ação?\",\"appropriateSource\":\"Relato e oferta\","
        + "\"evidenceNeeded\":\"Compra e desistência\",\"contraryEvidenceToSeek\":\"Alternativa suficiente\","
        + "\"publicQueries\":[\""
        + query
        + "\"],\"maxPublicQueries\":1,\"maxEstimatedSearchCostUsd\":0.005}";
  }

  /** Gera o relatório final válido de uma candidata para variar apenas o contrato sob teste. */
  private com.fasterxml.jackson.databind.JsonNode singleCandidateGapEvidence(
      String candidateName, int modelInvocations) throws Exception {
    return new ObjectMapper()
        .readTree(
            """
            {
              "gapDeepening": {
                "customerInterviewCount": 5,
                "candidateCount": 1,
                "plannedSearchRequests": 2,
                "actualSearchRequests": 2,
                "executedQueries": ["q1", "q2"],
                "unexecutedQueries": [],
                "estimatedSearchCostUsd": 0.01000000,
                "searchCostCoverage": "ESTIMATED_SEARCH_ONLY",
                "modelInvocationCount": %d,
                "modelCostCoverage": "AGENT_TASK_AUDIT_AFTER_CALLBACK",
                "pricingSource": "https://brave.com/search/api/",
                "pricingObservedOn": "2026-09-23",
                "resolvedGaps": [{
                  "candidateName": "%s",
                  "pendingQuestion": "Quando o desejo vira decisão?",
                  "appropriateSource": "Relato e oferta atual",
                  "evidenceNeeded": "Compra, desistência, preço e entrega",
                  "contraryEvidenceSought": "Alternativa gratuita suficiente",
                  "executedQueries": ["q1", "q2"],
                  "resolutionQueries": [],
                  "resolutionEvidenceIds": [],
                  "resolutionBasis": "NO_RESOLUTION_EVIDENCE",
                  "status": "STILL_OPEN"
                }]
              }
            }
            """
                .formatted(modelInvocations, candidateName));
  }

  /** Declara o gate já atendido sem inventar conteúdo das entrevistas neste teste de integração. */
  private ProductDiscoveryGapDeepeningResponse readyGap(Long cycleId, int interviewCount) {
    return new ProductDiscoveryGapDeepeningResponse(
        cycleId,
        true,
        ProductDiscoveryCycleStatus.RESEARCHING,
        ProductDiscoveryCustomerInterviewService.GAP_STAGE_CODE,
        5,
        8,
        interviewCount,
        3,
        2,
        List.of(831L),
        List.of(),
        true,
        12,
        2,
        4,
        new BigDecimal("0.12000000"),
        "ESTIMATED_SEARCH_ONLY",
        "AGENT_TASK_AUDIT_AFTER_CALLBACK",
        "https://brave.com/search/api/",
        java.time.LocalDate.of(2026, 9, 23),
        "Gate atendido.",
        List.of(),
        "CONSENTED_INTERVIEWS_V1",
        false,
        false);
  }
}
