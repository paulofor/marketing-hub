package com.marketinghub.experiment.history;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Responsabilidade: definir os contratos públicos do histórico auditável de experimentos. */
public final class ExperimentHistoryEventContracts {
  /** Impede instanciação do agrupador estático de contratos. */
  private ExperimentHistoryEventContracts() {}

  /** Representa a entrada validada para registrar um fato do experimento. */
  public record CreateRequest(
      @NotBlank @Pattern(regexp = "OBSERVACAO|INCIDENTE|DECISAO|CORRECAO|APRENDIZADO")
          String category,
      @NotBlank @Size(max = 191) String title,
      @NotBlank String description,
      String evidenceJson,
      @Size(max = 191) String source,
      Instant occurredAt) {}

  /** Representa um fato já persistido e recuperável pela tela. */
  public record Response(
      Long id,
      String category,
      String title,
      String description,
      String evidenceJson,
      String source,
      Instant occurredAt,
      Instant createdAt) {}
}
