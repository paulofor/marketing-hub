package com.marketinghub.pde.mira.privateprototype.v1.service.contract;

import com.marketinghub.pde.mira.privateprototype.v1.service.MiraRoutinePolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** Responsabilidade: declarar entradas limitadas da candidata privada, sem identidade pessoal. */
public final class MiraPrivateContract {
  /** Impede instanciação do catálogo de contratos. */
  private MiraPrivateContract() {}

  /** Solicita um pacote sintético vinculado à identidade real do ciclo. */
  public record Create(
      @NotNull @Positive Long cycleId,
      @NotBlank String sourceReference,
      @NotBlank String prototypeVersion,
      @NotBlank String condition,
      @NotBlank String scenarioCode,
      @NotBlank String deviceProfile) {}

  /** Representa um produto cuja orientação documental será preservada integralmente. */
  public record ProductInput(
      @NotBlank @Size(max = 160) String name,
      @NotBlank @Size(max = 600) String labelDirections,
      @Size(max = 1200) String sourceUrl) {
    /** Preserva a entrada legada quando a cliente informa somente o próprio rótulo. */
    public ProductInput(String name, String labelDirections) {
      this(name, labelDirections, null);
    }
  }

  /** Valida também os elementos da lista, incluindo os produtos opcionais acrescentados. */
  public record Input(
      @NotBlank @Size(max = 500) String objective,
      @NotEmpty @Size(max = 12) List<@Valid ProductInput> products) {}

  /** Registra somente uma ação funcional autorizada no estado persistido. */
  public record Event(@NotBlank String eventType) {}

  /** Descreve capacidades privadas sem implicar autorização comercial. */
  public record Contract(
      String productSlug,
      String prototypeVersion,
      String generationMode,
      int organizationsLimit,
      int productsLimit,
      int valueDeadlineSeconds,
      String checkoutMode,
      boolean paymentEnabled,
      boolean published,
      boolean externalAiEnabled,
      int mediaSpendBrl) {}

  /** Retorna a credencial uma única vez, separada da projeção auditável. */
  public record CreatedSession(String sessionToken, SessionView session) {}

  /** Expõe dados funcionais persistidos e explicitamente segregados de mercado. */
  public record SessionView(
      String id,
      Long productId,
      Long cycleId,
      Long experimentId,
      String prototypeVersion,
      String condition,
      String scenarioCode,
      String deviceProfile,
      String trafficClass,
      boolean syntheticEvaluation,
      String status,
      String objective,
      List<ProductInput> products,
      List<MiraRoutinePolicy.RoutineCard> routine,
      List<ReadyResult> previousResults,
      String blocker,
      int organizationsUsed,
      int organizationsLimit,
      List<String> events,
      String firstInteractionAt,
      String generatedAt,
      String generationMode,
      String checkoutMode,
      boolean paymentEnabled,
      boolean published,
      int mediaSpendBrl) {}

  /** Preserva um resultado útil consumível sem confundi-lo com a entrada atual. */
  public record ReadyResult(
      int organization,
      String objective,
      List<ProductInput> products,
      List<MiraRoutinePolicy.RoutineCard> routine,
      String generatedAt) {}

  /** Agrupa evidências do ciclo sem segredo de sessão ou alegação comercial. */
  public record Report(
      List<SessionView> sessions,
      List<com.fasterxml.jackson.databind.JsonNode> audit,
      String trafficClass,
      boolean humanEvidenceClaimed,
      boolean commercialEvidenceClaimed) {}
}
