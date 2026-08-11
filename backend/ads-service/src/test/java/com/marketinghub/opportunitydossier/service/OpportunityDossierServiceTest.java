package com.marketinghub.opportunitydossier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.marketinghub.opportunitydossier.*;
import com.marketinghub.opportunitydossier.service.convert.ConvertOpportunityRequest;
import com.marketinghub.opportunitydossier.service.review.SubmitOpportunityReviewRequest;
import com.marketinghub.opportunitydossier.service.status.UpdateOpportunityStatusRequest;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.opportunitydossier.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar os gates do ciclo de vida do dossiê. */
@ExtendWith(MockitoExtension.class)
class OpportunityDossierServiceTest {
  @Mock OpportunityDossierRepository dossiers;
  @Mock OpportunityEvidenceRepository evidence;
  @Mock OpportunityAgentReviewRepository reviews;
  @Mock CommercialPlanService plans;
  OpportunityDossierService service;

  /** Prepara o serviço e comportamento mínimo da persistência. */
  @BeforeEach
  void setUp() {
    service = new OpportunityDossierService(dossiers, evidence, reviews, plans);
    lenient().when(dossiers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(reviews.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  /** Impede iniciar pareceres sem evidência verificável. */
  @Test
  void blocksReviewWithoutEvidence() {
    OpportunityDossier dossier = dossier(OpportunityDossierStatus.RESEARCHING);
    when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
    when(evidence.findByDossierIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
    assertThatThrownBy(
            () ->
                service.updateStatus(
                    1L,
                    new UpdateOpportunityStatusRequest(
                        OpportunityDossierStatus.UNDER_REVIEW, "USER")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("evidência");
  }

  /** Libera a decisão humana somente depois dos quatro pareceres concluídos. */
  @Test
  void completesReviewStageAfterAllAgentsRespond() {
    OpportunityDossier dossier = dossier(OpportunityDossierStatus.UNDER_REVIEW);
    OpportunityAgentReview target = review("ATENA", null);
    when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
    when(reviews.findByDossierIdAndAgentKey(1L, "ATENA")).thenReturn(Optional.of(target));
    when(reviews.findByDossierIdOrderByAgentKeyAsc(1L))
        .thenReturn(
            List.of(
                target,
                review("PSIQUE", Instant.now()),
                review("PLUTUS", Instant.now()),
                review("HERMES", Instant.now())));
    when(evidence.findByDossierIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
    var response =
        service.submitReview(
            1L,
            "ATENA",
            new SubmitOpportunityReviewRequest(
                OpportunityReviewDecision.SUPPORT,
                "Demanda plausível",
                "Risco controlável",
                "Testar"));
    assertThat(response.status()).isEqualTo(OpportunityDossierStatus.READY_FOR_TEST);
  }

  /** Converte uma vez sem copiar orçamento ou resultado de outro plano. */
  @Test
  void convertsApprovedDossierIntoFreshPlan() {
    OpportunityDossier dossier = dossier(OpportunityDossierStatus.APPROVED);
    CommercialPlan plan = CommercialPlan.builder().id(77L).name("Nova oportunidade").build();
    when(dossiers.findById(1L)).thenReturn(Optional.of(dossier));
    when(plans.create(any())).thenReturn(plan);
    when(evidence.findByDossierIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
    when(reviews.findByDossierIdOrderByAgentKeyAsc(1L)).thenReturn(List.of());
    var response = service.convert(1L, new ConvertOpportunityRequest("USER"));
    assertThat(response.status()).isEqualTo(OpportunityDossierStatus.CONVERTED_TO_PLAN);
    assertThat(response.convertedPlanId()).isEqualTo(77L);
    verify(plans)
        .create(argThat(request -> request.maxBudget() == null && request.targetRevenue() == null));
  }

  /** Cria um dossiê mínimo válido para os cenários. */
  private OpportunityDossier dossier(OpportunityDossierStatus status) {
    return OpportunityDossier.builder()
        .id(1L)
        .title("Produto IA")
        .ownerAgentKey("ARGOS")
        .status(status)
        .targetAudience("Profissionais")
        .mainPain("Esforço")
        .referenceProduct("Produto validado")
        .aiAdvantage("Automação")
        .proposedOffer("Oferta")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  /** Cria uma solicitação de parecer com conclusão opcional. */
  private OpportunityAgentReview review(String agent, Instant completedAt) {
    return OpportunityAgentReview.builder()
        .id(1L)
        .dossier(dossier(OpportunityDossierStatus.UNDER_REVIEW))
        .agentKey(agent)
        .requestedAt(Instant.now())
        .completedAt(completedAt)
        .build();
  }
}
