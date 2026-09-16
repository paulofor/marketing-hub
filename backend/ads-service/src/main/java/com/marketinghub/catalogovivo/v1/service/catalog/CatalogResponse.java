package com.marketinghub.catalogovivo.v1.service.catalog;

import java.time.Instant;
import java.util.List;

/** Responsabilidade: apresentar cobertura, versões, auditoria e métricas operacionais do piloto. */
public record CatalogResponse(
    String project,
    long processId,
    String origin,
    boolean ready,
    List<String> issues,
    List<Item> items,
    long pinnedTasks,
    long resolutionFailures) {
  /** Responsabilidade: reunir atividade e histórico de versões textuais. */
  public record Item(
      CatalogBinding binding,
      List<CatalogVersion> versions,
      List<Event> events,
      List<Usage> usages) {
    /** Mantém o harness leve quando a tela não solicita histórico de utilização. */
    public Item(CatalogBinding binding, List<CatalogVersion> versions, List<Event> events) {
      this(binding, versions, events, List.of());
    }
  }

  /**
   * Responsabilidade: relacionar versões utilizadas com tarefas e origens sem expor prompts
   * extensos.
   */
  public record Usage(
      long taskId,
      long versionId,
      int versionNumber,
      String sourceReference,
      String status,
      Instant fixedAt) {}

  /** Responsabilidade: preservar autoria, motivo e horário de cada decisão do catálogo. */
  public record Event(
      long id,
      long versionId,
      String action,
      String operatorName,
      String note,
      Instant createdAt) {}
}
