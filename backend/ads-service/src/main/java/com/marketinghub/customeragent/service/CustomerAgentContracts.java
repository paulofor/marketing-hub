package com.marketinghub.customeragent.service;

import java.time.Instant;

/**
 * Responsabilidade: concentrar os contratos imutaveis da Biblioteca de Personas e do Agente
 * Cliente.
 */
public final class CustomerAgentContracts {
  private CustomerAgentContracts() {}

  /** Dados aceitos ao cadastrar uma hipotese de persona. */
  public record SavePersonaRequest(
      Long productId,
      String personaKey,
      String name,
      String confidenceLevel,
      String lifeContext,
      String pain,
      String desiredProgress,
      String awarenessLevel,
      String objections,
      String trustCriteria,
      String languageSamples,
      String evidenceJson) {}

  /** Representacao publica de uma persona versionada. */
  public record PersonaResponse(
      Long id,
      Long productId,
      String personaKey,
      String name,
      Integer version,
      String confidenceLevel,
      String lifeContext,
      String pain,
      String desiredProgress,
      String awarenessLevel,
      String objections,
      String trustCriteria,
      String languageSamples,
      String evidenceJson,
      Instant updatedAt) {}

  /** Solicitacao de avaliacao simulada de um ativo. */
  public record StartEvaluationRequest(Long personaId, String assetType, String assetReference) {}

  /** Resultado devolvido pelo worker sem qualquer dado humano inferido. */
  public record CompleteEvaluationRequest(
      String assessment, String hypothesisJson, String rawModelResponse, String model) {}

  /** Falha técnica terminal reportada pelo worker. */
  public record FailExecutionRequest(String error) {}

  /** Resultado humano observado posteriormente por uma fonte real. */
  public record RecordHumanResultRequest(String humanResultJson) {}

  /** Representacao auditavel da avaliacao e do resultado real separado. */
  public record EvaluationResponse(
      Long id,
      Long personaId,
      PersonaResponse persona,
      String assetType,
      String assetReference,
      String status,
      String simulatedAssessment,
      String hypothesisJson,
      String humanResultJson,
      String model,
      Instant startedAt,
      Instant finishedAt,
      Instant createdAt) {}

  /** Solicita uma navegacao observacional limitada a fontes publicas autorizadas. */
  public record StartDigitalObservationRequest(
      Long personaId, String objective, String authorizedSourcesJson, String deviceProfile) {}

  /** Resultado do worker separado em observacao, simulacao e hipotese. */
  public record CompleteDigitalObservationRequest(
      String observationJson,
      String simulatedReactionJson,
      String commercialHypothesisJson,
      String rawModelResponse,
      String model) {}

  /** Confirmacao humana posterior, proveniente de fonte oficial. */
  public record RecordObservationHumanConfirmationRequest(String humanConfirmationJson) {}

  /** Representacao auditavel de uma experiencia digital observacional. */
  public record DigitalObservationResponse(
      Long id,
      PersonaResponse persona,
      String objective,
      String authorizedSourcesJson,
      String status,
      String deviceProfile,
      String observationJson,
      String simulatedReactionJson,
      String commercialHypothesisJson,
      String humanConfirmationJson,
      String model,
      Instant startedAt,
      Instant finishedAt,
      Instant createdAt) {}
}
