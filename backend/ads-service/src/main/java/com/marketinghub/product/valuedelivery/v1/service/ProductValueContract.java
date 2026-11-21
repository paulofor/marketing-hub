package com.marketinghub.product.valuedelivery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

/**
 * Confere rastreabilidade estrutural entre proposta, histórias e provas, sem substituir a revisão
 * independente dos agentes.
 */
@lombok.extern.slf4j.Slf4j
public final class ProductValueContract {
  public static final String VERSION = "TURMALINA_VALUE_DELIVERY_V1";

  /** Impede instanciar regras sem estado. */
  private ProductValueContract() {}

  /**
   * Identifica JSON UTF-8 compacto com chaves ordenadas, sem quebra final e sem reordenar arrays.
   */
  public static String hash(JsonNode value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(canonical(value).toString().getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("Falha ao calcular identidade do contrato de valor", ex);
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }

  /**
   * Mantém hashes estáveis entre persistência e emissores de prova com ordens de campos distintas.
   */
  private static JsonNode canonical(JsonNode value) {
    var factory = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
    if (value.isObject()) {
      var sorted = factory.objectNode();
      var names = new java.util.TreeSet<String>();
      value.fieldNames().forEachRemaining(names::add);
      names.forEach(name -> sorted.set(name, canonical(value.get(name))));
      return sorted;
    }
    if (value.isArray()) {
      var array = factory.arrayNode();
      value.forEach(item -> array.add(canonical(item)));
      return array;
    }
    return value;
  }

  /**
   * Exige cobertura explícita da base, histórias observáveis e estados de tela antes da construção.
   */
  public static void specification(JsonNode baseline, JsonNode spec) {
    require(VERSION.equals(spec.path("contractVersion").asText()), "Contrato de entrega ausente.");
    require(
        hash(baseline).equals(spec.path("baselineSha256").asText()),
        "A especificação usa outra proposta/base de Turmalina.");
    require(
        Set.of("PRODUCT", "PROTOTYPE_SLICE").contains(spec.path("scope").asText()),
        "Declare se a entrega é produto ou recorte de protótipo.");
    require(
        spec.path("stories").isArray() && !spec.path("stories").isEmpty(),
        "Faltam histórias de uso verificáveis.");
    Set<String> sources = new HashSet<>();
    baseline.path("requirements").fieldNames().forEachRemaining(sources::add);
    require(!sources.isEmpty(), "A proposta e a base do tipo não foram fixadas.");
    Set<String> covered = new HashSet<>();
    Set<String> stories = new HashSet<>();
    for (JsonNode story : spec.path("stories")) {
      String id = text(story, "id");
      require(stories.add(id), "História duplicada: " + id);
      text(story, "customerOutcome");
      require(
          story.path("sourceRefs").isArray() && !story.path("sourceRefs").isEmpty(),
          "História sem origem: " + id);
      for (JsonNode source : story.path("sourceRefs")) {
        require(sources.contains(source.asText()), "Referência desconhecida: " + source.asText());
        covered.add(source.asText());
      }
      require(
          Set.of("BUILD", "REUSE", "DEFER").contains(story.path("action").asText()),
          "Declare construir, reutilizar ou adiar: " + id);
      text(story, "implementationNotes");
      if ("REUSE".equals(story.path("action").asText())) text(story, "reuseEvidence");
      if ("DEFER".equals(story.path("action").asText())) {
        text(story, "deferReason");
        require(
            "PROTOTYPE_SLICE".equals(spec.path("scope").asText()),
            "Entrega parcial não pode ser declarada produto completo.");
      }
      for (String state : Set.of("entry", "result", "resume", "empty", "error"))
        text(story.path("screenStates"), state);
      require(
          story.path("criteria").isArray() && !story.path("criteria").isEmpty(),
          "História sem critérios: " + id);
      Set<String> criteria = new HashSet<>();
      for (JsonNode criterion : story.path("criteria")) {
        require(criteria.add(text(criterion, "id")), "Critério duplicado: " + id);
        for (String field : Set.of("given", "when", "then", "testMethod")) text(criterion, field);
      }
    }
    require(
        covered.equals(sources),
        "Há promessas ou capacidades de Turmalina sem história; registre a lacuna sem reduzir a proposta.");
  }

  /** Impede que recibos genéricos ou de outro contexto comprovem a experiência prometida. */
  public static void evidence(JsonNode baseline, JsonNode spec, JsonNode report, String version) {
    specification(baseline, spec);
    require(
        "PRODUCT".equals(spec.path("scope").asText()),
        "O recorte foi especificado; ainda falta entregar a proposta completa antes da comunicação.");
    JsonNode trace = report.path("valueDelivery");
    require(
        hash(spec).equals(trace.path("specificationSha256").asText()),
        "Falta prova da implementação desta especificação; encaminhar a construção a Dédalo.");
    for (String key : Set.of("productId", "sourceReference"))
      require(
          baseline.path(key).equals(trace.path(key)), "A prova pertence a outro produto ou ciclo.");
    require(
        version != null
            && !version.isBlank()
            && version.equals(report.path("prototypeVersion").asText()),
        "A prova pertence a outra versão.");
    require(
        report.path("frontendSourceFingerprint").asText().matches("[a-f0-9]{64}"),
        "Falta identidade da implementação testada.");
    java.util.Map<String, String> methods = new java.util.HashMap<>();
    for (JsonNode story : spec.path("stories"))
      for (JsonNode criterion : story.path("criteria"))
        methods.put(
            story.path("id").asText() + "/" + criterion.path("id").asText(),
            criterion.path("testMethod").asText());
    Set<String> actual = new HashSet<>();
    for (JsonNode mapping : trace.path("criteria")) {
      String key = text(mapping, "storyId") + "/" + text(mapping, "criterionId");
      require(actual.add(key), "Prova repetida: " + key);
      JsonNode proof = null;
      for (JsonNode candidate : report.path("criteria"))
        if (candidate.path("code").asText().equals(text(mapping, "proofCode"))) proof = candidate;
      require(
          proof != null
              && "PASS".equals(proof.path("status").asText())
              && "EXECUTION".equals(proof.path("verificationType").asText())
              && java.util.Objects.equals(methods.get(key), proof.path("testMethod").asText())
              && proof.path("result").isObject()
              && !proof.path("result").isEmpty()
              && hash(proof.path("result")).equals(proof.path("resultSha256").asText()),
          "Critério sem execução comprovada: " + key);
    }
    Set<String> expected = new HashSet<>();
    for (JsonNode story : spec.path("stories"))
      for (JsonNode criterion : story.path("criteria"))
        expected.add(story.path("id").asText() + "/" + criterion.path("id").asText());
    require(
        expected.equals(actual),
        "A prova não cobre exatamente os critérios da experiência prometida.");
  }

  /** Retorna texto obrigatório sem preencher lacunas com valores presumidos. */
  private static String text(JsonNode node, String field) {
    require(
        node.path(field).isTextual() && !node.path(field).asText().isBlank(),
        "Campo obrigatório ausente: " + field);
    return node.path(field).asText();
  }

  /** Produz bloqueio funcional legível para o relatório persistido do processo. */
  private static void require(boolean condition, String reason) {
    if (!condition) throw new IllegalArgumentException(reason);
  }
}
