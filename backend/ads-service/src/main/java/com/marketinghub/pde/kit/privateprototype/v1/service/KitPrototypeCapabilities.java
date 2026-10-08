package com.marketinghub.pde.kit.privateprototype.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.Capability;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: reconhecer composições realmente implementadas a partir de contratos aprovados.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KitPrototypeCapabilities {
  private final AgentTaskRepository tasks;
  private final BusinessProcessChainDefinitionRepository chains;
  private final ObjectMapper json;

  @Value(
      "${pde.kit.private-prototype-url:https://pagamentopalf.site/mh-api/pde/kit/private/v1/prototype}")
  private String prototypeUrl;

  /** Confere predecessoras da mesma cadeia e seleciona somente perfil explícito e inequívoco. */
  public Capability resolve(LearningSalesCycle cycle) {
    if (cycle.isBaseline()
        || !"OPEN".equals(cycle.getStatus())
        || !Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage()))
      return unavailable(
          "A implementação privada exige o mesmo ciclo aberto em ajuste ou validação.");
    try {
      var chain = chains.findById(cycle.getChainDefinitionId()).orElseThrow();
      var planning =
          chain.getItems().stream()
              .map(i -> i.getProcessDefinition())
              .filter(p -> "pde-commercial-plan-offer".equals(p.getProcessCode()))
              .findFirst()
              .orElseThrow();
      var construction =
          chain.getItems().stream()
              .map(i -> i.getProcessDefinition())
              .filter(p -> "pde-construction-approval".equals(p.getProcessCode()))
              .findFirst()
              .orElseThrow();
      var designed =
          tasks.findFunctionalSnapshotsByProcessSince(
              construction.getId(), "experiment:" + cycle.getExperimentId(), cycle.getCreatedAt());
      for (String stage : List.of("journey", "deliverables", "access")) {
        var task = latest(designed, stage);
        if (task == null
            || !"COMPLETED".equals(task.status())
            || !"READY".equals(json.readTree(task.resultJson()).path("decision").asText()))
          return unavailable(
              "Dédalo ainda precisa concluir o contrato de " + stage + " desta versão.");
      }
      var architecture =
          latest(
              tasks.findFunctionalSnapshotsByProcessSince(
                  planning.getId(), "experiment:" + cycle.getExperimentId(), cycle.getCreatedAt()),
              "productArchitecture");
      if (architecture == null || !"COMPLETED".equals(architecture.status()))
        return unavailable("Arquitetura aprovada indisponível.");
      JsonNode result = json.readTree(architecture.resultJson());
      if (!"APPROVE".equals(result.path("decision").asText()))
        return unavailable("A arquitetura não aprovou esta implementação.");
      var definition = result.path("productArchitecture");
      String explicit = definition.path("kitProfileCode").asText("");
      // Alias histórico documentado: a referência estratégica identifica explicitamente o perfil.
      String reference = definition.path("strategyReference").asText("");
      var matches =
          List.of("nails-v1", "barber-v1").stream()
              .filter(
                  code ->
                      code.equals(explicit)
                          || explicit.isBlank() && reference.matches("(?s).*\\b" + code + "\\b.*"))
              .toList();
      if (matches.size() != 1)
        return unavailable(
            "O contrato não identifica um perfil de kit suportado e inequívoco; não será inferido por nome ou ID.");
      return new Capability(
          true,
          matches.getFirst(),
          "O compositor registrado pode materializar este contrato com fixtures de QA, sem chamadas pagas.",
          prototypeUrl);
    } catch (Exception ex) {
      log.error(
          "Falha ao resolver capacidade de kit productId={} cycleId={} experimentId={}",
          cycle.getProductId(),
          cycle.getId(),
          cycle.getExperimentId(),
          ex);
      return unavailable(
          "O contrato de implementação não pôde ser conferido. Preserve a versão e corrija a entrada.");
    }
  }

  /** Usa a última tentativa da atividade, sem recuperar aprovação anterior a um bloqueio novo. */
  private AgentTaskFunctionalSnapshot latest(
      List<AgentTaskFunctionalSnapshot> values, String stage) {
    return values.stream()
        .filter(t -> stage.equals(t.processActivityId()))
        .max(Comparator.comparing(AgentTaskFunctionalSnapshot::id))
        .orElse(null);
  }

  /** Expõe ausência de capacidade sem fabricar entrega ou autorização. */
  private Capability unavailable(String reason) {
    return new Capability(false, null, reason, null);
  }
}
