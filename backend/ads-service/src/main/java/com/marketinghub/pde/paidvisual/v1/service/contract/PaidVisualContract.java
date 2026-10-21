package com.marketinghub.pde.paidvisual.v1.service.contract;

import com.marketinghub.pde.visualpersonalization.v1.service.contract.VisualPreparationContract.Input;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Responsabilidade: declarar entradas da entrega paga sem aceitar autorização vinda da cliente. */
public final class PaidVisualContract {
  /** Impede instanciação do catálogo. */
  private PaidVisualContract() {}

  /** Transporta o estado recém-consultado pelo módulo de pagamentos no provedor. */
  public record Payment(
      @NotBlank @Size(max = 80) String paymentId,
      @com.fasterxml.jackson.annotation.JsonAlias("paymentStatus") @NotBlank String status,
      @NotNull BigDecimal amount,
      @NotBlank String currency,
      @com.fasterxml.jackson.annotation.JsonAlias("buyerEmail") @NotBlank @Email String email,
      @NotBlank String externalReference,
      @NotNull Instant dateApproved,
      @NotNull Map<String, Object> metadata) {}

  /** Solicita recuperação exclusivamente para o e-mail registrado na compra. */
  public record Login(@NotBlank @Email @Size(max = 254) String email) {}

  /** Identifica uma tentativa explícita, com prazo da ocasião e dados limitados. */
  public record Submit(
      @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{16,64}") String operationKey,
      boolean regeneration,
      @NotNull Instant occasionAt,
      @NotNull @Valid Input input) {}
}
