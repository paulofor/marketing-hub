package com.marketinghub.product.modeling.v1.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.repository.jpa.opportunitydossier.*;
import com.marketinghub.repository.jpa.productmodeling.ProductModelingRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Transporta fontes factuais e intenção à cadeia, isolando adoções e versões históricas. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ModelingHandoff {
  private final ProductModelingRepository modelings;
  private final ObjectMapper json;
  private final OpportunityDossierRepository dossiers;
  private final OpportunityEvidenceRepository evidence;
  private final com.marketinghub.repository.jpa.agenttask.AgentTaskRepository tasks;

  /** Evita materializar outro produto quando Argos pesquisou uma evolução do existente. */
  public boolean preservesExistingProduct(Long cycleId) {
    return modelings
        .findByDiscoveryCycleId(cycleId)
        .filter(m -> m.getProductId() != null)
        .isPresent();
  }

  /** Retorna a referência oficial do experimento adotado ou da pesquisa ainda preparatória. */
  public String sourceReference(Long cycleId) {
    return modelings
        .findByDiscoveryCycleId(cycleId)
        .filter(m -> m.getExperimentId() != null)
        .map(m -> "experiment:" + m.getExperimentId())
        .orElse("product-discovery-cycle:" + cycleId);
  }

  /** Resolve apenas a referência exata; tarefas anteriores à adoção preservam seu contexto. */
  public JsonNode snapshot(String reference, Instant createdAt) {
    if (reference == null) return json.nullNode();
    var experiment =
        java.util.regex.Pattern.compile("^experiment:([1-9][0-9]{0,17})$").matcher(reference);
    if (experiment.matches())
      return modelings
          .findByExperimentId(Long.valueOf(experiment.group(1)))
          .filter(
              m ->
                  createdAt != null
                      && m.getAdoptedAt() != null
                      && !createdAt.isBefore(m.getAdoptedAt()))
          .map(
              m ->
                  withDecision(
                      read(m.getAdoptedContextJson()), "experiment:" + m.getExperimentId()))
          .orElse(json.nullNode());
    var discovery =
        java.util.regex.Pattern.compile("^product-discovery-cycle:([1-9][0-9]{0,17})$")
            .matcher(reference);
    return discovery.matches() ? context(Long.valueOf(discovery.group(1))) : json.nullNode();
  }

  /** Entrega a mesma adoção às etapas privadas cuja referência canônica pertence ao produto. */
  public JsonNode forLearningCycle(Long cycleId, Long productId, Instant createdAt) {
    return modelings
        .findByLearningCycleId(cycleId)
        .filter(
            m ->
                Objects.equals(m.getProductId(), productId)
                    && createdAt != null
                    && m.getAdoptedAt() != null
                    && !createdAt.isBefore(m.getAdoptedAt()))
        .map(
            m -> withDecision(read(m.getAdoptedContextJson()), "experiment:" + m.getExperimentId()))
        .orElse(json.nullNode());
  }

  /** Expõe intenção, dossiês e evidências realmente persistidas; concorrentes não são compras. */
  public JsonNode context(Long cycleId) {
    var m = modelings.findByDiscoveryCycleId(cycleId).orElse(null);
    if (m == null) return json.nullNode();
    ObjectNode result = json.createObjectNode();
    result.put("contractVersion", "MARKET_REFERENCE_MODELING_V1");
    result.put("modelingId", m.getId());
    result.put("discoveryCycleId", cycleId);
    if (m.getProductId() != null) result.put("productId", m.getProductId());
    result.put("identityMode", m.getProductId() == null ? "CREATE" : "PRESERVE");
    result.put("desiredOutcome", m.getDesiredOutcome());
    result.set("referenceUrls", read(m.getReferencesJson()));
    result.set(
        "bigIdeaAtPreparation",
        m.getBigIdeaSnapshot() == null ? json.nullNode() : read(m.getBigIdeaSnapshot()));
    var facts = result.putArray("factualSources");
    var candidates = result.putArray("candidates");
    for (var dossier : dossiers.findAllByProductDiscoveryCycleIdOrderByIdAsc(cycleId)) {
      var candidate = candidates.addObject();
      candidate.put("dossierId", dossier.getId());
      candidate.put("title", dossier.getTitle());
      candidate.put("mainPain", dossier.getMainPain());
      candidate.put("targetAudience", dossier.getTargetAudience());
      candidate.put("limitations", dossier.getKnownRisks());
      if (dossier.getProductDiscoveryOpportunity() != null) {
        candidate.put("opportunityId", dossier.getProductDiscoveryOpportunity().getId());
        candidate.put("maturity", dossier.getProductDiscoveryOpportunity().getMaturity().name());
      }
      for (var source : evidence.findByDossierIdOrderByCreatedAtAsc(dossier.getId())) {
        var fact = facts.addObject();
        fact.put("evidenceId", source.getId());
        fact.put("dossierId", dossier.getId());
        fact.put("url", source.getSourceUrl());
        fact.put("observation", source.getSummary());
        fact.put(
            "recordedAt", source.getCreatedAt() == null ? null : source.getCreatedAt().toString());
      }
    }
    result.put("demandStatus", "HYPOTHESIS");
    result.put("commercialAuthorizationInherited", false);
    result.put(
        "guidance",
        "Relacionar fonte observada e limitações → aprendizado → três alternativas de vantagem percebida → decisão de Atena → entrega funcionando de Dédalo → compras, entrega e contribuição. Preservar identidade e Big Idea; propor evolução explícita sem sobrescrever histórico. Comparar entrada gratuita, esforço inicial, experiência sensorial e personalizada e continuidade paga. Diferenciação própria sem copiar marcas, materiais ou implementação. Concorrentes não comprovam demanda deste produto. As passagens e os limites continuam controlados pelo backend na cadeia existente.");
    return withDecision(result, sourceReference(cycleId));
  }

  /** Preserva a decisão vigente de Atena junto às fontes para construção, comunicação e revisão. */
  private JsonNode withDecision(JsonNode snapshot, String reference) {
    var latest =
        tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc(reference).stream()
            .filter(t -> "marketStrategy".equals(t.getProcessActivityId()))
            .reduce((first, second) -> second)
            .orElse(null);
    if (latest == null) return snapshot;
    var result = (ObjectNode) snapshot.deepCopy();
    var decision = result.putObject("strategicDecision");
    decision.put("taskId", latest.getId());
    decision.put("status", latest.getStatus());
    if ("COMPLETED".equals(latest.getStatus()) && latest.getResultJson() != null) {
      var output = read(latest.getResultJson());
      decision.put("decision", output.path("decision").asText());
      if ("APPROVE".equals(output.path("decision").asText()))
        decision.set("trace", output.path("referenceModelingTrace"));
    }
    return result;
  }

  /** Recusa snapshot inválido sem produzir contexto substituto ou ocultar o erro. */
  private JsonNode read(String value) {
    try {
      return json.readTree(value);
    } catch (Exception ex) {
      log.error("Falha ao ler snapshot de modelagem persistido.", ex);
      throw new IllegalStateException("Contexto de modelagem inválido.", ex);
    }
  }
}
