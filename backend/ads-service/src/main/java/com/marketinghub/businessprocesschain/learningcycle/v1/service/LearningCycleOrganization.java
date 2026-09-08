package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleEntry;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: resolver a hierarquia e os caminhos do ciclo a partir do BPM persistido. */
@Component
@RequiredArgsConstructor
public class LearningCycleOrganization {
  private final LearningCycleJson json;

  /** Expõe somente o pai e os retornos que pertencem à versão exata da cadeia. */
  public LearningCycleEntry describe(
      BusinessProcessChainDefinition chain,
      BusinessProcessDefinition cycleProcess,
      Long productId,
      LearningSalesCycle activeCycle) {
    var parent =
        chain.getItems().stream()
            .filter(
                item ->
                    item.getProcessDefinition()
                        .getProcessCode()
                        .equals(cycleProcess.getParentProcessCode()))
            .findFirst()
            .orElse(null);
    if (parent == null) return null;
    var diagram = json.read(parent.getProcessDefinition().getDiagramJson());
    String activityId = null;
    for (var node : diagram.path("nodes")) {
      if (cycleProcess.getProcessCode().equals(node.path("subprocessCode").asText())
          && "TASK".equals(node.path("type").asText())) activityId = node.path("id").asText();
    }
    boolean incoming = false, outgoing = false;
    for (var flow : diagram.path("flows")) {
      if (flow.path("to").asText().equals(activityId)) incoming = true;
      if (flow.path("from").asText().equals(activityId)) outgoing = true;
    }
    boolean integrated = activityId != null && incoming && outgoing;
    var routes = new ArrayList<LearningCycleEntry.ReturnRoute>();
    for (var route : diagram.path("learningCycleReturns")) {
      chain.getItems().stream()
          .filter(
              item ->
                  item.getProcessDefinition()
                      .getProcessCode()
                      .equals(route.path("processCode").asText()))
          .findFirst()
          .ifPresent(
              item ->
                  routes.add(
                      new LearningCycleEntry.ReturnRoute(
                          route.path("label").asText(), route.path("condition").asText(),
                          item.getProcessDefinition().getId(), item.getSequenceNumber(),
                          item.getProcessDefinition().getName(),
                              diagramUrl(item, productId, chain.getId()))));
    }
    String workspace =
        "/business-process-chains/learning-cycles?chainId="
            + (activeCycle == null ? chain.getId() : activeCycle.getChainDefinitionId());
    if (productId != null) workspace += "&productId=" + productId;
    if (activeCycle != null) workspace += "&cycleId=" + activeCycle.getId();
    return new LearningCycleEntry(
        chain.getId(),
        chain.getName(),
        parent.getProcessDefinition().getId(),
        parent.getProcessDefinition().getName(),
        parent.getSequenceNumber(),
        activityId,
        cycleProcess.getId(),
        cycleProcess.getName(),
        integrated,
        integrated
            && "PUBLISHED".equals(chain.getStatus())
            && "PUBLISHED".equals(parent.getProcessDefinition().getStatus())
            && "PUBLISHED".equals(cycleProcess.getStatus()),
        integrated
            ? "Após consolidar os resultados, abra o ciclo deste produto e experimento. Registre a decisão e execute a próxima atividade orientada. Uma mudança comercial abre um sucessor; uma correção técnica preserva o experimento e exige revalidação."
            : "Esta versão histórica da cadeia ainda não chama o ciclo no BPM. Consulte o histórico ou selecione a cadeia vigente para iniciar uma nova iteração.",
        workspace,
        activeCycle == null
            ? "Abrir ciclo por produto e experimento"
            : "Retomar ciclo #" + activeCycle.getId(),
        processUrl(parent, productId, chain.getId()),
        List.copyOf(routes));
  }

  /** Abre a definição do destino; a execução da correção usa a atividade orientada do ciclo. */
  private String diagramUrl(BusinessProcessChainItem item, Long productId, Long chainId) {
    return ("RETIRED".equals(item.getProcessDefinition().getStatus())
            ? "/business-processes/retired?processId="
            : "/business-processes?processId=")
        + item.getProcessDefinition().getId()
        + "&chainId="
        + chainId
        + (productId == null ? "" : "&productId=" + productId);
  }

  /** Conserva o contexto da cadeia e, quando disponível, o produto nos destinos do BPM. */
  private String processUrl(BusinessProcessChainItem item, Long productId, Long chainId) {
    Long id = item.getProcessDefinition().getId();
    return productId == null
        ? diagramUrl(item, null, chainId)
        : "/products/"
            + productId
            + "/value-chain-history/processes/"
            + id
            + "/activities?chainId="
            + chainId;
  }
}
