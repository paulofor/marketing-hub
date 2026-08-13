package com.marketinghub.opportunitydossier.service;

import com.marketinghub.opportunitydossier.*;
import com.marketinghub.opportunitydossier.service.review.*;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityAgentReviewRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityEvidenceRepository;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: controlar reserva, conclusão, falha e retomada dos pareceres de oportunidade.
 */
@Service
public class OpportunityReviewExecutionService {
  private static final Set<String> AGENTS = Set.of("ATENA", "PSIQUE", "PLUTUS", "HERMES");
  private static final long LEASE_SECONDS = 2700;
  private final OpportunityAgentReviewRepository reviews;
  private final OpportunityEvidenceRepository evidence;
  private final OpportunityDossierService dossiers;

  /** Configura a fila persistida e a governança funcional do dossiê. */
  public OpportunityReviewExecutionService(
      OpportunityAgentReviewRepository reviews,
      OpportunityEvidenceRepository evidence,
      OpportunityDossierService dossiers) {
    this.reviews = reviews;
    this.evidence = evidence;
    this.dossiers = dossiers;
  }

  /** Reserva o próximo parecer do agente e recupera uma lease órfã uma única vez. */
  @Transactional
  public OpportunityReviewJobResponse claim(String agentKey) {
    String agent = supported(agentKey);
    recoverExpired(agent);
    OpportunityAgentReview review =
        reviews
            .findByAgentKeyAndExecutionStatusOrderByRequestedAtAsc(
                agent, OpportunityReviewExecutionStatus.PENDING, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    review.setExecutionStatus(OpportunityReviewExecutionStatus.RUNNING);
    review.setStartedAt(Instant.now());
    review.setErrorMessage(null);
    return job(reviews.save(review));
  }

  /** Conclui a execução e delega ao serviço canônico a transição funcional do dossiê. */
  @Transactional
  public void complete(String agentKey, Long reviewId, CompleteOpportunityReviewRequest request) {
    OpportunityAgentReview review = running(agentKey, reviewId);
    if (request == null
        || request.decision() == null
        || blank(request.rationale())
        || blank(request.recommendation())
        || blank(request.rawModelResponse())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parecer executado incompleto");
    }
    review.setRawModelResponse(request.rawModelResponse());
    review.setModelName(request.modelName());
    review.setExecutionStatus(OpportunityReviewExecutionStatus.COMPLETED);
    reviews.save(review);
    dossiers.submitReview(
        review.getDossier().getId(),
        review.getAgentKey(),
        new SubmitOpportunityReviewRequest(
            request.decision(), request.rationale(), request.risks(), request.recommendation()));
  }

  /** Registra falha técnica para diagnóstico e retomada humana sem fingir parecer funcional. */
  @Transactional
  public void fail(String agentKey, Long reviewId, FailOpportunityReviewRequest request) {
    OpportunityAgentReview review = running(agentKey, reviewId);
    review.setExecutionStatus(OpportunityReviewExecutionStatus.FAILED);
    review.setErrorMessage(
        request == null || blank(request.errorMessage())
            ? "Falha sem diagnóstico enviada pelo executor."
            : request.errorMessage());
    reviews.save(review);
  }

  /**
   * Recoloca um parecer técnico falho na fila real do agente, preservando seu vínculo ao dossiê.
   */
  @Transactional
  public void requeue(Long dossierId, String agentKey) {
    String agent = supported(agentKey);
    OpportunityAgentReview review =
        reviews
            .findByDossierIdAndAgentKey(dossierId, agent)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (review.getDossier().getStatus() != OpportunityDossierStatus.UNDER_REVIEW) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Dossiê não está recebendo pareceres");
    }
    if (review.getCompletedAt() != null
        || review.getExecutionStatus() == OpportunityReviewExecutionStatus.COMPLETED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Parecer já foi concluído");
    }
    if (review.getExecutionStatus() == OpportunityReviewExecutionStatus.RUNNING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Parecer ainda está em execução");
    }
    review.setExecutionStatus(OpportunityReviewExecutionStatus.PENDING);
    review.setRequestedAt(Instant.now());
    review.setStartedAt(null);
    review.setRetryCount(0);
    review.setErrorMessage(null);
    review.setRawModelResponse(null);
    review.setModelName(null);
    reviews.save(review);
  }

  /** Recupera leases inativas uma vez e encerra reincidência para impedir loop. */
  private void recoverExpired(String agent) {
    Instant cutoff = Instant.now().minusSeconds(LEASE_SECONDS);
    reviews
        .findByAgentKeyAndExecutionStatusAndUpdatedAtBefore(
            agent, OpportunityReviewExecutionStatus.RUNNING, cutoff)
        .stream()
        .forEach(
            item -> {
              if (item.getRetryCount() >= 1) {
                item.setExecutionStatus(OpportunityReviewExecutionStatus.FAILED);
                item.setErrorMessage("Lease expirou novamente após uma retomada automática.");
              } else {
                item.setRetryCount(1);
                item.setExecutionStatus(OpportunityReviewExecutionStatus.PENDING);
                item.setStartedAt(null);
                item.setErrorMessage("LEASE_RECOVERED_ONCE");
              }
              reviews.save(item);
            });
  }

  /** Exige que a execução pertença ao agente e esteja reservada. */
  private OpportunityAgentReview running(String agentKey, Long reviewId) {
    String agent = supported(agentKey);
    OpportunityAgentReview review =
        reviews
            .findById(reviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!agent.equals(review.getAgentKey()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Parecer pertence a outro agente");
    if (review.getExecutionStatus() != OpportunityReviewExecutionStatus.RUNNING)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Parecer não está em execução");
    return review;
  }

  /** Monta o contexto rico e persistido entregue ao executor. */
  private OpportunityReviewJobResponse job(OpportunityAgentReview review) {
    OpportunityDossier dossier = review.getDossier();
    var sources =
        evidence.findByDossierIdOrderByCreatedAtAsc(dossier.getId()).stream()
            .map(
                item ->
                    new OpportunityReviewJobResponse.Evidence(
                        item.getSourceUrl(), item.getSummary(), item.getCreatedBy()))
            .toList();
    return new OpportunityReviewJobResponse(
        review.getId(),
        dossier.getId(),
        review.getAgentKey(),
        "READ_ONLY_OPPORTUNITY_REVIEW",
        dossier.getTitle(),
        dossier.getTargetAudience(),
        dossier.getMainPain(),
        dossier.getReferenceProduct(),
        dossier.getAiAdvantage(),
        dossier.getProposedOffer(),
        dossier.getDeliveryModel(),
        dossier.getKnownRisks(),
        dossier.getExperimentRecommendation(),
        sources);
  }

  /** Normaliza e valida a identidade do agente autorizado. */
  private String supported(String agentKey) {
    String agent = agentKey == null ? "" : agentKey.trim().toUpperCase();
    if (!AGENTS.contains(agent))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agente de parecer inválido");
    return agent;
  }

  /** Verifica texto obrigatório ausente. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
