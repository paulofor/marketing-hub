package com.marketinghub.pde.vega.privateprototype.v1.service.contract;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Responsabilidade: definir as entradas oficiais da experiência e dos callbacks do Vega. */
public final class VegaPrivateContract {
  /** Impede instanciação do agrupador de contratos imutáveis. */
  private VegaPrivateContract() {}

  /** Transporta o alvo de uma sessão interna segregada. */
  public record InternalSession(
      @NotNull Long cycleId,
      @NotBlank String prototypeVersion,
      @NotBlank String origin,
      Integer readingNumber) {}

  /** Confirma consentimento antes de trocar o convite humano. */
  public record Access(@NotBlank String accessToken, @AssertTrue boolean consentAccepted) {}

  /** Recebe o contexto mínimo sem exigir fotos, prompt ou configuração técnica. */
  public record Input(
      @NotBlank @Size(max = 160) String occasion,
      @NotBlank @Size(max = 300) String existingSelection,
      @Size(max = 300) String optionalNote) {}

  /** Recebe a ação explícita que fundamenta cada sinal da leitura. */
  public record Event(@NotBlank String eventType, @Size(max = 500) String answer) {}

  /** Preserva request antes da chamada ao provedor sem aceitar credenciais no contrato. */
  public record RequestAudit(@NotNull JsonNode request, @NotBlank String model) {}

  /** Recebe saída funcional separada do envelope bruto e do consumo do provedor. */
  public record Result(
      @NotBlank String status,
      JsonNode card,
      JsonNode rawResponse,
      @NotBlank String model,
      Long inputTokens,
      Long outputTokens,
      @DecimalMin("0") BigDecimal costUsd,
      @Size(max = 4000) String error) {}
}
