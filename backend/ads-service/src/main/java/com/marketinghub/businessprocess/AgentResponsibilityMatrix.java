package com.marketinghub.businessprocess;

import com.fasterxml.jackson.databind.JsonNode;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Responsabilidade: validar a autoria única das atividades contra os oito domínios dos agentes. */
final class AgentResponsibilityMatrix {
  private static final Map<String, Responsibility> RESPONSIBILITIES = responsibilities();

  private AgentResponsibilityMatrix() {}

  /** Valida identidade, domínio e rótulo do único agente responsável por uma atividade. */
  static void validate(JsonNode node, String nodeType) {
    JsonNode responsibleAgentKeys = node.get("responsibleAgentKeys");
    String responsibilityDomain = node.path("responsibilityDomain").asText("").trim();
    String owner = node.path("owner").asText("").trim();
    List<Responsibility> matchedOwners = matchedOwners(owner);

    if (responsibleAgentKeys == null || responsibleAgentKeys.isNull()) {
      if (!responsibilityDomain.isEmpty()) {
        throw new IllegalArgumentException(
            "Domínio de agente exige uma responsibleAgentKey explícita.");
      }
      if (!matchedOwners.isEmpty()) {
        throw new IllegalArgumentException(
            "Atividade de agente exige uma responsibleAgentKey e um domínio explícitos.");
      }
      return;
    }
    if (!"TASK".equals(nodeType)) {
      throw new IllegalArgumentException("Somente atividades podem declarar agente responsável.");
    }
    if (!responsibleAgentKeys.isArray() || responsibleAgentKeys.size() != 1) {
      throw new IllegalArgumentException(
          "Atividade de agente deve possuir exatamente uma responsibleAgentKey.");
    }
    String agentKey = responsibleAgentKeys.get(0).asText("").trim();
    Responsibility responsibility = RESPONSIBILITIES.get(agentKey);
    if (responsibility == null) {
      throw new IllegalArgumentException("A atividade declara um agente fora da matriz canônica.");
    }
    if (!responsibility.domain().equals(responsibilityDomain)) {
      throw new IllegalArgumentException(
          "O domínio da atividade é incompatível com o agente responsável.");
    }
    if (matchedOwners.size() != 1 || !matchedOwners.get(0).agentKey().equals(agentKey)) {
      throw new IllegalArgumentException(
          "O rótulo do responsável deve identificar somente o agente declarado.");
    }
    if (hasSharedOwnershipMarker(owner)) {
      throw new IllegalArgumentException(
          "Atividade de agente não pode combinar coautores no campo owner.");
    }
  }

  /** Localiza identidades canônicas e legadas citadas no rótulo de responsabilidade. */
  private static List<Responsibility> matchedOwners(String owner) {
    String normalizedOwner = normalize(owner);
    return RESPONSIBILITIES.values().stream()
        .filter(
            responsibility ->
                responsibility.ownerAliases().stream()
                    .map(AgentResponsibilityMatrix::normalize)
                    .anyMatch(normalizedOwner::contains))
        .toList();
  }

  /** Detecta texto que combina um agente com outro agente ou função na mesma atividade. */
  private static boolean hasSharedOwnershipMarker(String owner) {
    String normalizedOwner = normalize(owner);
    return normalizedOwner.contains(" e ")
        || normalizedOwner.contains(" ou ")
        || normalizedOwner.contains(",")
        || normalizedOwner.contains("/");
  }

  /** Remove variações de acento e caixa usadas apenas na apresentação dos responsáveis. */
  private static String normalize(String value) {
    return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .trim();
  }

  /** Monta a fonte única de identidades e domínios aceitos pelo cadastro de processos. */
  private static Map<String, Responsibility> responsibilities() {
    Map<String, Responsibility> values = new LinkedHashMap<>();
    add(values, "market-radar", "MARKET_EVIDENCE", "Argos", "Agente Radar de Mercado");
    add(
        values,
        "experiment-strategist",
        "MARKET_STRATEGY",
        "Atena",
        "Estrategista de Experimentos",
        "Estrategista-Chefe de Mercado");
    add(values, "financial-agent", "FINANCIAL_VALIDATION", "Plutus", "Agente Financeiro");
    add(
        values,
        "landing-generator",
        "PDE_CONSTRUCTION",
        "Dédalo",
        "Agente Gerador de Landing",
        "Gerador de Landing");
    add(values, "videomaker", "AUDIOVISUAL_PRODUCTION", "Apolo", "Agente Videomaker");
    add(values, "customer-agent", "HUMAN_EXPERIENCE_REVIEW", "Psique", "Agente Cliente");
    add(
        values,
        "meta-ad-approver",
        "COMMERCIAL_INTEGRITY_REVIEW",
        "Têmis",
        "Agente de Integridade Comercial",
        "Aprovador Meta");
    add(values, "growth-operator", "GROWTH_OPERATION", "Hermes", "Operador de Crescimento");
    return Map.copyOf(values);
  }

  /** Registra uma identidade canônica sem permitir domínios duplicados por chave técnica. */
  private static void add(
      Map<String, Responsibility> values, String agentKey, String domain, String... ownerAliases) {
    values.put(agentKey, new Responsibility(agentKey, domain, List.of(ownerAliases)));
  }

  /** Representa a fronteira imutável de um agente no catálogo de processos. */
  private record Responsibility(String agentKey, String domain, List<String> ownerAliases) {}
}
