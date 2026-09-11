package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleExecutionContext;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: resolver o contrato do gate pela identidade persistida do ciclo aberto. */
@Component
@RequiredArgsConstructor
public class PdeAgentValidationCycleContract {
  private final LearningSalesCycleRepository cycles;
  private final LearningCycleExecutionContext executions;
  private final AgentTaskTargetContextProvider targets;

  /**
   * Exige ciclo, cadeia, produto, experimento, versão e aceitação iguais aos usados pelos agentes.
   */
  public JsonNode resolve(Product product, BusinessProcessDefinition process, String reference) {
    if (reference == null || !reference.matches("experiment:[1-9][0-9]*"))
      throw new IllegalArgumentException("Referência de ciclo inválida para o gate multiagente.");
    long experimentId = Long.parseLong(reference.substring("experiment:".length()));
    var cycle =
        cycles
            .findByExperimentId(experimentId)
            .orElseThrow(() -> new IllegalArgumentException("Ciclo da validação não encontrado."));
    if (!Objects.equals(product.getId(), cycle.getProductId())
        || !"OPEN".equals(cycle.getStatus())
        || cycle.isBaseline()
        || !Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage())
        || !reference.equals(executions.source(cycle.getId(), product, process, true)))
      throw new IllegalArgumentException(
          "O gate exige o mesmo ciclo aberto em ajuste ou validação.");
    var target =
        targets
            .resolve(reference, process.getProcessCode())
            .orElseThrow(() -> new IllegalArgumentException("Alvo privado do ciclo indisponível."));
    JsonNode context = target.pdeContext();
    JsonNode lineage = context == null ? null : context.path("lineage");
    if (!Objects.equals(product.getId(), target.productId())
        || !Objects.equals(product.getSlug(), target.productSlug())
        || !Objects.equals(experimentId, target.experimentId())
        || !reference.equals(target.sourceReference())
        || !Objects.equals(cycle.getProductVersion(), target.experienceVersion())
        || lineage == null
        || lineage.path("learningCycleId").asLong() != cycle.getId()
        || lineage.path("productId").asLong() != product.getId()
        || lineage.path("experimentId").asLong() != experimentId
        || !Objects.equals(
            target.publicUrl(),
            context.path("privatePrototypeAcceptance").path("privateAccessUrl").asText())
        || !Objects.equals(
            target.experienceVersion(),
            context.path("privatePrototypeAcceptance").path("prototypeVersion").asText()))
      throw new IllegalArgumentException(
          "A evidência não corresponde à identidade e versão deste ciclo.");
    return context;
  }
}
