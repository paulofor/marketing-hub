package com.marketinghub.opportunitydossier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.marketinghub.opportunitydossier.*;
import com.marketinghub.opportunitydossier.service.review.CompleteOpportunityReviewRequest;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityAgentReviewRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityEvidenceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar reserva exclusiva e conclusão auditável da estratégia de Atena. */
@ExtendWith(MockitoExtension.class)
class OpportunityReviewExecutionServiceTest {
  @Mock OpportunityAgentReviewRepository reviews;
  @Mock OpportunityEvidenceRepository evidence;
  @Mock OpportunityDossierService dossiers;
  OpportunityReviewExecutionService service;

  /** Prepara a fila isolada usada pelos cenários. */
  @BeforeEach
  void setUp() {
    service = new OpportunityReviewExecutionService(reviews, evidence, dossiers);
    lenient().when(reviews.save(any())).thenAnswer(call -> call.getArgument(0));
  }

  /** Reserva somente trabalho de Atena e entrega o contexto completo do dossiê. */
  @Test
  void claimsAgentSpecificReviewWithContext() {
    OpportunityAgentReview review = review("ATENA", OpportunityReviewExecutionStatus.PENDING);
    when(reviews.findByAgentKeyAndExecutionStatusAndUpdatedAtBefore(any(), any(), any()))
        .thenReturn(List.of());
    when(reviews.findByAgentKeyAndExecutionStatusOrderByRequestedAtAsc(any(), any(), any()))
        .thenReturn(List.of(review));
    when(evidence.findByDossierIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

    var job = service.claim("atena");

    assertThat(job.agentKey()).isEqualTo("ATENA");
    assertThat(job.dossierId()).isEqualTo(10L);
    assertThat(review.getExecutionStatus()).isEqualTo(OpportunityReviewExecutionStatus.RUNNING);
  }

  /** Persiste auditoria e usa a governança canônica para concluir a estratégia de Atena. */
  @Test
  void completesThroughDossierGovernance() {
    OpportunityAgentReview review = review("ATENA", OpportunityReviewExecutionStatus.RUNNING);
    when(reviews.findById(20L)).thenReturn(Optional.of(review));

    service.complete(
        "ATENA",
        20L,
        new CompleteOpportunityReviewRequest(
            OpportunityReviewDecision.ADJUST,
            "Margem ainda precisa de teste.",
            "CAC ausente.",
            "Executar pré-venda controlada.",
            "{\"decision\":\"ADJUST\"}",
            "codex"));

    assertThat(review.getExecutionStatus()).isEqualTo(OpportunityReviewExecutionStatus.COMPLETED);
    verify(dossiers).submitReview(eq(10L), eq("ATENA"), any());
  }

  /** Reenfileira a execução falha de Atena no registro realmente consumido pelo worker. */
  @Test
  void requeuesFailedAtenaReviewInCanonicalQueue() {
    OpportunityAgentReview review = review("ATENA", OpportunityReviewExecutionStatus.FAILED);
    review.setErrorMessage("Backend temporariamente indisponível");
    review.setRetryCount(1);
    review.setStartedAt(Instant.now());
    when(reviews.findByDossierIdAndAgentKey(10L, "ATENA")).thenReturn(Optional.of(review));

    service.requeue(10L, "atena");

    assertThat(review.getExecutionStatus()).isEqualTo(OpportunityReviewExecutionStatus.PENDING);
    assertThat(review.getErrorMessage()).isNull();
    assertThat(review.getStartedAt()).isNull();
    assertThat(review.getRetryCount()).isZero();
    verify(reviews).save(review);
  }

  /** Impede duplicar um parecer que o executor já está processando. */
  @Test
  void refusesToRequeueRunningReview() {
    OpportunityAgentReview review = review("ATENA", OpportunityReviewExecutionStatus.RUNNING);
    when(reviews.findByDossierIdAndAgentKey(10L, "ATENA")).thenReturn(Optional.of(review));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requeue(10L, "ATENA"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("execução");

    verify(reviews, never()).save(any());
  }

  /** Retoma uma lease órfã de Atena após indisponibilidade do backend. */
  @Test
  void recoversExpiredLeaseForAtena() {
    String agent = "ATENA";
    OpportunityAgentReview expired = review(agent, OpportunityReviewExecutionStatus.RUNNING);
    expired.setStartedAt(Instant.now().minusSeconds(3000));
    expired.setUpdatedAt(Instant.now().minusSeconds(3000));
    OpportunityAgentReview pending = review(agent, OpportunityReviewExecutionStatus.PENDING);
    when(reviews.findByAgentKeyAndExecutionStatusAndUpdatedAtBefore(
            eq(agent), eq(OpportunityReviewExecutionStatus.RUNNING), any()))
        .thenReturn(List.of(expired));
    when(reviews.findByAgentKeyAndExecutionStatusOrderByRequestedAtAsc(any(), any(), any()))
        .thenReturn(List.of(pending));
    when(evidence.findByDossierIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

    service.claim(agent);

    assertThat(expired.getExecutionStatus()).isEqualTo(OpportunityReviewExecutionStatus.PENDING);
    assertThat(expired.getErrorMessage()).isEqualTo("LEASE_RECOVERED_ONCE");
    assertThat(expired.getRetryCount()).isEqualTo(1);
  }

  /** Rejeita consumo estratégico por agentes com responsabilidade posterior no fluxo. */
  @ParameterizedTest
  @ValueSource(strings = {"PSIQUE", "PLUTUS", "HERMES"})
  void rejectsLegacyOpportunityReviewers(String agent) {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.claim(agent))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("Somente Atena");

    verify(reviews, never())
        .findByAgentKeyAndExecutionStatusOrderByRequestedAtAsc(any(), any(), any());
  }

  /** Cria um parecer operacional vinculado a um dossiê em avaliação. */
  private OpportunityAgentReview review(String agent, OpportunityReviewExecutionStatus status) {
    OpportunityDossier dossier =
        OpportunityDossier.builder()
            .id(10L)
            .title("Produto IA")
            .status(OpportunityDossierStatus.UNDER_REVIEW)
            .targetAudience("Profissionais")
            .mainPain("Esforço")
            .referenceProduct("Referência")
            .aiAdvantage("Menos esforço")
            .build();
    return OpportunityAgentReview.builder()
        .id(20L)
        .dossier(dossier)
        .agentKey(agent)
        .executionStatus(status)
        .requestedAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }
}
