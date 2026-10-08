package com.marketinghub.pde.visualpersonalization.v1.service.contract;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Responsabilidade: transportar entradas privadas e callbacks auditáveis da personalização visual.
 */
public final class VisualPreparationContract {
  /** Impede instanciação do catálogo de contratos. */
  private VisualPreparationContract() {}

  /** Responsabilidade: definir a entrada sintética sem fotografia ou atributos corporais. */
  public record Input(
      @NotBlank @Size(max = 500) String occasion,
      @NotEmpty @Size(min = 2, max = 12) List<@NotBlank @Size(max = 150) String> pieces,
      @NotBlank @Size(max = 500) String preferences,
      @NotNull @Size(max = 500) String constraints) {}

  /** Responsabilidade: abrir uma preparação idempotente no contexto e autorização existentes. */
  public record Create(
      @NotNull @Positive Long commercialPlanId,
      @NotNull @Positive Long experimentId,
      @NotBlank @Size(max = 120) String productVersion,
      @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{32,96}") String accessToken,
      @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{16,64}") String operationKey,
      @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String authorizationHash,
      @AssertTrue boolean syntheticConsent,
      @NotNull @Valid Input input) {}

  /** Responsabilidade: preservar a requisição efetiva enviada ao provedor. */
  public record RequestAudit(@NotNull JsonNode rawRequest) {}

  /** Responsabilidade: transportar a resposta bruta mesmo quando o provedor recusa a geração. */
  public record Result(
      @NotNull JsonNode rawResponse, @Min(100) @Max(599) int httpStatus, boolean providerCalled) {}
}
