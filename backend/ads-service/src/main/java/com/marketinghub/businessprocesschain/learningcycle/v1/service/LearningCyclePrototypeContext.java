package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: vincular a prova de implementação privada exclusivamente à versão do ciclo. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCyclePrototypeContext {
  private final LearningSalesCycleEventRepository events;
  private final ObjectMapper json;
  private static final List<String> CHECKS =
      List.of(
          "desktopValidated",
          "mobileValidated",
          "firstResultValidated",
          "resumeValidated",
          "failuresValidated",
          "testDataExcluded",
          "noExternalSideEffects");

  /** Valida o handoff opcional sem transformar plano futuro em protótipo disponível. */
  public void validate(JsonNode proof, String version) {
    if (proof == null || proof.isMissingNode() || proof.isNull()) return;
    try {
      URI uri = URI.create(proof.path("privateAccessUrl").asText());
      Instant observed = Instant.parse(proof.path("observedAt").asText());
      if (!proof.isObject()
          || !version.equals(proof.path("prototypeVersion").asText())
          || !"https".equals(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getRawQuery() != null
          || uri.getRawFragment() != null
          || proof.path("image").asText().isBlank()
          || proof.path("evidenceReference").asText().length() < 20
          || observed.isAfter(Instant.now().plusSeconds(60))
          || observed.isBefore(Instant.now().minusSeconds(7 * 86400))
          || CHECKS.stream().anyMatch(k -> !proof.path(k).asBoolean(false)))
        throw new IllegalArgumentException(
            "Prova incompleta, antiga ou incompatível com a versão privada.");
    } catch (Exception ex) {
      log.warn("Handoff privado inválido no ciclo version={}", version, ex);
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Registre URL HTTPS, imagem, evidência recente e todos os testes da mesma versão.",
          ex);
    }
  }

  /** Lê a última prova da mesma versão sem recorrer ao cadastro comercial ou a outro ciclo. */
  public Optional<JsonNode> resolve(LearningSalesCycle cycle) {
    var history = events.findByCycleIdOrderByRevisionAsc(cycle.getId());
    for (int i = history.size() - 1; i >= 0; i--) {
      var event = history.get(i);
      try {
        JsonNode evidence = json.readTree(event.getEvidenceJson());
        JsonNode proof = evidence.path("privatePrototype");
        if (!"REWORK".equals(event.getAction())
            || !cycle.getProductVersion().equals(evidence.path("productVersion").asText())
            || !proof.isObject()) continue;
        if (!cycle.getProductVersion().equals(proof.path("prototypeVersion").asText()))
          return Optional.empty();
        ObjectNode acceptance = proof.deepCopy();
        acceptance.put("status", "READY");
        acceptance.put("acceptedAt", event.getCreatedAt().toString());
        acceptance.put("eventSource", "FIRST_PARTY_EVENTS");
        acceptance.put("paymentEnabled", false);
        acceptance.put("published", false);
        acceptance.put("mediaSpendBrl", 0);
        acceptance.put("testMarker", "PRIVATE_PROTOTYPE");
        acceptance.put("evidenceOrigin", "OPERATOR_VALIDATION");
        return Optional.of(acceptance);
      } catch (Exception ex) {
        log.error(
            "Falha ao ler handoff privado cycleId={} eventId={}", cycle.getId(), event.getId(), ex);
        return Optional.empty();
      }
    }
    return Optional.empty();
  }
}
