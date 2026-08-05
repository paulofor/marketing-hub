package com.marketinghub.mois.metaads.v1.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Reúne os contratos versionados da investigação de anúncios da Meta. */
public final class MoisMetaAdDtos {

  /** Impede instanciação do agrupador de contratos. */
  private MoisMetaAdDtos() {}

  /** Solicita uma investigação recorrente sem autorizar publicação ou gasto. */
  public record CreateInvestigationRequest(
      @NotBlank String workspaceId, @NotBlank String searchTerms, String countryCode) {}

  /** Expõe o estado, as evidências e as lacunas de uma investigação. */
  public record InvestigationResponse(
      long id,
      String workspaceId,
      String searchTerms,
      String countryCode,
      String status,
      String gateDecision,
      List<String> evidences,
      List<String> gaps,
      EthicalModelingCard ethicalModeling,
      int adsObserved,
      Instant createdAt,
      Instant updatedAt) {}

  /** Lista investigações para a tela administrativa. */
  public record InvestigationListResponse(List<InvestigationResponse> items) {}

  /** Entrega ao coletor uma investigação pendente. */
  public record PendingInvestigationResponse(
      long id, String workspaceId, String searchTerms, String countryCode) {}

  /** Representa uma observação bruta obtida pela API oficial da Meta. */
  public record MetaAdObservation(
      @NotBlank String metaAdId,
      String advertiserId,
      String advertiserName,
      @NotBlank String status,
      List<String> formatTypes,
      List<String> texts,
      List<String> mediaUrls,
      String destinationUrl,
      String snapshotUrl,
      boolean pageActive,
      boolean commercialSignal,
      @NotBlank String rawPayload) {}

  /** Recebe um lote real de observações do coletor. */
  public record ObservationBatchRequest(
      @NotBlank String collectorRunId,
      @NotEmpty List<MetaAdObservation> observations,
      Instant observedAt) {}

  /** Confirma a persistência do lote e devolve o gate recalculado. */
  public record ObservationBatchResponse(
      long investigationId, int accepted, String gateDecision, List<String> gaps) {}

  /** Recebe uma evidência comercial observada por uma pessoa na Biblioteca pública. */
  public record SupervisedObservationRequest(
      @NotBlank String adReference,
      @NotBlank String advertiserName,
      @NotBlank @Pattern(regexp = "https://(www\\.)?facebook\\.com/ads/library/.*")
          String adLibraryUrl,
      @NotBlank String adText,
      String formatType,
      String mediaUrl,
      String destinationUrl,
      boolean pageActive,
      boolean commercialSignal,
      Instant observedAt) {}

  /** Recebe a conclusão técnica do coletor sem mascarar falhas. */
  public record CompleteInvestigationRequest(boolean success, String errorMessage) {}

  /** Separa padrões modeláveis dos elementos que não podem ser copiados. */
  public record EthicalModelingCard(
      String pain,
      String audience,
      String mechanism,
      String offerStructure,
      List<String> angles,
      List<String> patterns,
      List<String> prohibitedCopies) {

    /** Cria uma ficha vazia enquanto ainda faltam evidências reais. */
    public static EthicalModelingCard empty() {
      return new EthicalModelingCard(
          "Ainda não comprovada",
          "Ainda não comprovado",
          "Ainda não comprovado",
          "Ainda não comprovada",
          List.of(),
          List.of(),
          List.of("marca", "texto", "personagem", "criativo"));
    }
  }

  /** Resume sinais usados pelos três diagnósticos independentes. */
  public record DiagnosticSignals(
      boolean longRunningAd,
      boolean plausiblyValidatedProduct,
      boolean marketingHubOpportunity,
      Map<String, Object> measurements) {}
}
