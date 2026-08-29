package com.marketinghub.agenttask;

import java.util.List;

/** Responsabilidade: converter a auditoria vinculada à tarefa em contratos públicos tipados. */
public final class AgentTaskAuditView {
  private static final String ACCESSED_URL = "ACCESSED_URL";
  private static final String BLOCKER_HELP = "BLOCKER_HELP";

  /** Impede instanciação de um conversor sem estado. */
  private AgentTaskAuditView() {}

  /** Lista somente as URLs realmente acessadas durante esta tarefa. */
  public static List<AgentTaskAuditLinkResponse> accessedUrls(AgentTask task) {
    return links(task, ACCESSED_URL);
  }

  /** Lista os snapshots privados vinculados exclusivamente à tarefa consultada. */
  public static List<AgentTaskVisualEvidenceResponse> visualEvidence(AgentTask task) {
    if (task.getVisualEvidence() == null) return List.of();
    return task.getVisualEvidence().stream()
        .sorted(
            java.util.Comparator.comparing(
                    AgentTaskVisualEvidence::getCapturedAt,
                    java.util.Comparator.nullsLast(java.time.Instant::compareTo))
                .thenComparing(
                    AgentTaskVisualEvidence::getId,
                    java.util.Comparator.nullsLast(Long::compareTo)))
        .map(AgentTaskVisualEvidenceService::response)
        .toList();
  }

  /** Monta a orientação do bloqueio quando categoria e ação foram persistidas. */
  public static AgentTaskBlockerGuidanceResponse blockerGuidance(AgentTask task) {
    if (task.getBlockerCategory() == null || task.getBlockerAction() == null) return null;
    return new AgentTaskBlockerGuidanceResponse(
        task.getBlockerCategory(), task.getBlockerAction(), links(task, BLOCKER_HELP));
  }

  /** Filtra e ordena os links de um único papel de auditoria. */
  private static List<AgentTaskAuditLinkResponse> links(AgentTask task, String linkType) {
    if (task.getAuditLinks() == null) return List.of();
    return task.getAuditLinks().stream()
        .filter(link -> linkType.equals(link.getLinkType()))
        .sorted(
            java.util.Comparator.comparing(
                    AgentTaskAuditLink::getDisplayOrder,
                    java.util.Comparator.nullsLast(Integer::compareTo))
                .thenComparing(
                    AgentTaskAuditLink::getId, java.util.Comparator.nullsLast(Long::compareTo)))
        .map(
            link ->
                new AgentTaskAuditLinkResponse(
                    link.getLabel(), link.getUrl(), link.getAccessMethod(), link.getAccessedAt()))
        .toList();
  }
}
