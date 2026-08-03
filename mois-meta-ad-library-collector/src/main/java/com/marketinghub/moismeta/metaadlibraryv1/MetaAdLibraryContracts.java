package com.marketinghub.moismeta.metaadlibraryv1;

import java.time.Instant;
import java.util.List;

/** Reúne contratos enxutos trocados entre coletor, Meta e backend. */
public final class MetaAdLibraryContracts {

  /** Impede instanciação do agrupador de contratos. */
  private MetaAdLibraryContracts() {}

  /** Representa uma investigação reservada no backend. */
  public record PendingInvestigation(long id, String workspaceId, String searchTerms, String countryCode) {}

  /** Representa um anúncio normalizado sem perder seu payload bruto. */
  public record Observation(
      String metaAdId,
      String advertiserId,
      String advertiserName,
      String status,
      List<String> formatTypes,
      List<String> texts,
      List<String> mediaUrls,
      String destinationUrl,
      String snapshotUrl,
      boolean pageActive,
      boolean commercialSignal,
      String rawPayload) {}

  /** Envia um lote correlacionado ao backend. */
  public record ObservationBatch(String collectorRunId, List<Observation> observations, Instant observedAt) {}

  /** Registra o resultado técnico sem alterar a decisão comercial. */
  public record Completion(boolean success, String errorMessage) {}
}
