package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Valida que Psique use pesquisa somente como critério consultivo rastreável. */
final class ResearchIntelligenceUsageValidator {
  private static final Pattern CARD_ID = Pattern.compile("RI1-[0-9A-F]{12}");

  /** Impede instanciação de um validador sem estado. */
  private ResearchIntelligenceUsageValidator() {}

  /** Exige cartões entregues de todas as coleções quando o parecer não está bloqueado. */
  static void validate(
      Map<String, Object> task, String agentKey, List<String> evidence, boolean completed) {
    if (!completed || task.get("researchIntelligence") == null) return;
    JsonNode intelligence = new ObjectMapper().valueToTree(task.get("researchIntelligence"));
    JsonNode route = route(intelligence, agentKey);
    if (route == null) {
      throw new IllegalArgumentException("Biblioteca de pesquisa sem a rota esperada de Psique.");
    }
    Map<String, String> collectionByCard = deliveredCards(route);
    if (collectionByCard.isEmpty()) {
      throw new IllegalArgumentException("Biblioteca de pesquisa sem cartões entregues a Psique.");
    }
    Set<String> mentioned = mentionedCards(String.join("\n", evidence));
    if (mentioned.isEmpty()) {
      throw new IllegalArgumentException("Psique não declarou os cartões de pesquisa aplicados.");
    }
    if (!collectionByCard.keySet().containsAll(mentioned)) {
      throw new IllegalArgumentException("Psique citou cartão de pesquisa não entregue.");
    }
    Set<String> coveredCollections = new HashSet<>();
    mentioned.forEach(cardId -> coveredCollections.add(collectionByCard.get(cardId)));
    if (!coveredCollections.containsAll(new HashSet<>(collectionByCard.values()))) {
      throw new IllegalArgumentException(
          "Psique não aplicou ao menos um cartão de cada coleção entregue.");
    }
  }

  /** Localiza a rota do agente sem aceitar cartões de outra responsabilidade. */
  private static JsonNode route(JsonNode intelligence, String agentKey) {
    for (JsonNode route : intelligence.path("routes")) {
      if (agentKey.equals(route.path("agentKey").asText())) return route;
    }
    return null;
  }

  /** Indexa os IDs e coleções autorizados no contrato recebido. */
  private static Map<String, String> deliveredCards(JsonNode route) {
    Map<String, String> cards = new HashMap<>();
    for (JsonNode card : route.path("cards")) {
      cards.put(card.path("cardId").asText(), card.path("collection").asText());
    }
    return cards;
  }

  /** Extrai somente IDs estruturados mencionados pelo parecer. */
  private static Set<String> mentionedCards(String evidence) {
    Set<String> cards = new HashSet<>();
    Matcher matcher = CARD_ID.matcher(evidence);
    while (matcher.find()) cards.add(matcher.group());
    return cards;
  }
}
