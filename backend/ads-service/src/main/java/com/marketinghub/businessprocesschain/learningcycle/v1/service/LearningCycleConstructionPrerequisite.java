package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.communication.v1.IrisLearningCycleContext;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Responsabilidade: exigir a homologação atual da construção antes de liberar comunicação. */
@Component
public class LearningCycleConstructionPrerequisite {
  private final BusinessProcessChainDefinitionRepository chains;
  private final BusinessProcessActivityInstanceRepository instances;
  private final IrisLearningCycleContext proof;

  /** Reutiliza o verificador canônico de provas sem executar agentes nem criar aprovações. */
  public LearningCycleConstructionPrerequisite(
      BusinessProcessChainDefinitionRepository chains,
      BusinessProcessActivityInstanceRepository instances,
      @Lazy IrisLearningCycleContext proof) {
    this.chains = chains;
    this.instances = instances;
    this.proof = proof;
  }

  /** Responsabilidade: informar a pendência e o caminho oficial do predecessor no mesmo ciclo. */
  public record Pending(Long processId, Integer processNumber, String reason, String url) {}

  /**
   * Aceita somente gate vigente da própria referência e a prova integral já exigida por Íris;
   * artefatos históricos não dispensam a nova homologação independente.
   */
  public Pending pending(LearningSalesCycle cycle) {
    var construction =
        chains.findById(cycle.getChainDefinitionId()).orElseThrow().getItems().stream()
            .filter(
                item ->
                    "pde-construction-approval"
                        .equals(item.getProcessDefinition().getProcessCode()))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Processo de construção ausente da cadeia do ciclo."));
    Long processId = construction.getProcessDefinition().getId();
    String reference = "experiment:" + cycle.getExperimentId();
    String url =
        "/products/"
            + cycle.getProductId()
            + "/value-chain-history/processes/"
            + processId
            + "/activities?learningCycleId="
            + cycle.getId()
            + "&chainId="
            + cycle.getChainDefinitionId();
    var pending =
        new Pending(processId, construction.getSequenceNumber(), pendingReason(cycle), url);
    var gate =
        instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                processId, reference)
            .stream()
            .filter(
                value ->
                    value.getActivityDefinition() != null
                        && "agentValidationGate"
                            .equals(value.getActivityDefinition().getActivityId())
                        && Objects.equals(
                            processId, value.getActivityDefinition().getProcessDefinition().getId())
                        && reference.equals(value.getSourceReference()))
            .max(Comparator.comparing(value -> value.getOccurrenceNumber()))
            .orElse(null);
    if (gate == null || !"COMPLETED".equals(gate.getStatus()) || !gate.isObjectiveAchieved())
      return pending;
    Map<String, Object> verified = proof.resolve(reference).orElse(Map.of());
    Map<?, ?> product = verified.get("product") instanceof Map<?, ?> value ? value : Map.of();
    return "AVAILABLE".equals(verified.get("availability"))
            && "READY".equals(verified.get("inputReadiness"))
            && Objects.equals(cycle.getId(), verified.get("cycleId"))
            && Objects.equals(reference, verified.get("sourceReference"))
            && Objects.equals(cycle.getProductId(), product.get("id"))
            && Objects.equals(cycle.getChainDefinitionId(), verified.get("chainDefinitionId"))
            && Objects.equals(cycle.getProductVersion(), verified.get("prototypeVersion"))
            && Objects.equals(gate.getId(), verified.get("gateInstanceId"))
        ? null
        : pending;
  }

  /** Explica a dependência funcional sem atribuir a falta de homologação à estratégia de Atena. */
  public static String pendingReason(LearningSalesCycle cycle) {
    return "Processo 3 pendente: conclua a construção, a validação multiagente e a aprovação da versão "
        + cycle.getProductVersion()
        + " neste ciclo #"
        + cycle.getId()
        + " antes de iniciar Íris no Processo 4. Os resultados e custos do planejamento foram preservados; reutilizar artefatos não dispensa a homologação.";
  }
}
