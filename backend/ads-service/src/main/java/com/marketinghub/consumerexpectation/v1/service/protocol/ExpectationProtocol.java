package com.marketinghub.consumerexpectation.v1.service.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/** Carrega o protocolo versionado compartilhado entre ferramenta, tela e contexto dos agentes. */
public final class ExpectationProtocol {
  public static final String VERSION = "CONSUMER_EXPECTATION_V1";
  private static final Logger log = LoggerFactory.getLogger(ExpectationProtocol.class);
  private static final Set<String> AGENTS =
      Set.of(
          "market-radar",
          "experiment-strategist",
          "financial-agent",
          "landing-generator",
          "communication-director",
          "videomaker",
          "customer-agent",
          "meta-ad-approver",
          "growth-operator");
  private static final JsonNode PROTOCOL = load();

  /** Impede instanciação de um catálogo imutável de domínio. */
  private ExpectationProtocol() {}

  /** Devolve cópia para impedir alteração acidental do catálogo global. */
  public static JsonNode catalog() {
    return PROTOCOL.deepCopy();
  }

  /** Preserva o limite de cartões e não oferece rotas administrativas aos executores. */
  public static JsonNode taskContext() {
    var context = (com.fasterxml.jackson.databind.node.ObjectNode) catalog();
    context.remove(java.util.List.of("sources", "surfaces", "variantDefinitions", "tools"));
    context.put(
        "sourcePolicy",
        "Usar os cartões selecionados e os contratos do próprio executor; catálogo completo disponível na interface administrativa. Não inventar ferramentas, fontes ou resultados.");
    return context;
  }

  /** Reconhece as seis dimensões declaradas, preservando o limite dos agentes cadastrados. */
  public static boolean applies(String agent, String context) {
    String text = context == null ? "" : context.toLowerCase(Locale.ROOT);
    return AGENTS.contains(agent == null ? "" : agent)
        && java.util.List.of(
                "surpresa",
                "expectativa",
                "familiaridade",
                "novidade",
                "curiosidade",
                "originalidade",
                "valor percebido",
                "customer delight",
                "surprise",
                "expectation")
            .stream()
            .anyMatch(text::contains);
  }

  /** Falha explicitamente se o recurso obrigatório estiver ausente ou inválido. */
  private static JsonNode load() {
    try (var input =
        new ClassPathResource("consumer-expectation/v1/protocol.json").getInputStream()) {
      return new ObjectMapper().readTree(input);
    } catch (IOException ex) {
      log.error("Falha ao carregar protocolo consumer-expectation/v1", ex);
      throw new IllegalStateException("Protocolo de expectativas indisponível", ex);
    }
  }
}
