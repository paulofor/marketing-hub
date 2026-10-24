package com.marketinghub.product.bigidea.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.bigidea.v1.ProductBigIdea;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Impede aplicar uma direção posterior a um contexto que iniciou sem Big Idea fixada. */
@Component
@RequiredArgsConstructor
@Slf4j
public class BigIdeaHistoricalAuthority {
  private final AgentTaskRepository tasks;
  private final ObjectMapper json;

  /** Normaliza somente a identidade comercial, preservando limites entre IDs diferentes. */
  public static String canonical(String reference) {
    if (reference == null) return "";
    return reference.replaceFirst("^(experiment:[1-9][0-9]*)(?:[@:].*)?$", "$1");
  }

  /** Conserva ausência histórica salvo criação explícita de Atena na estratégia deste contexto. */
  public boolean preservesAbsence(ProductBigIdea idea, String reference) {
    if (idea == null) return false;
    var first = tasks.findFirstBigIdeaHistory(reference).orElse(null);
    if (first == null) return false;
    try {
      String stored = first.getProductBigIdeaJson();
      boolean absent = stored == null || stored.isBlank();
      if (!absent) {
        String status = json.readTree(stored).path("status").asText();
        absent = java.util.Set.of("MISSING", "LEGACY_UNPINNED").contains(status);
      }
      if (!absent) return false;
      if (idea.getSourceTaskId() != null) {
        var strategy = tasks.findById(idea.getSourceTaskId()).orElse(null);
        if (strategy != null
            && reference.equals(canonical(strategy.getSourceReference()))
            && "marketStrategy".equals(strategy.getProcessActivityId())
            && strategy.getAssignedAgent() != null
            && "experiment-strategist".equals(strategy.getAssignedAgent().getAgentKey()))
          return false;
      }
      log.info(
          "Ausência histórica de Big Idea preservada. productId={} sourceReference={} firstTaskId={} laterIdeaId={}",
          idea.getProductId(),
          reference,
          first.getId(),
          idea.getId());
      return true;
    } catch (Exception ex) {
      log.error(
          "Falha ao conferir autoridade histórica da Big Idea. productId={} sourceReference={} firstTaskId={}",
          idea.getProductId(),
          reference,
          first.getId(),
          ex);
      throw new IllegalStateException(
          "Não foi possível comprovar a direção histórica deste contexto.", ex);
    }
  }
}
