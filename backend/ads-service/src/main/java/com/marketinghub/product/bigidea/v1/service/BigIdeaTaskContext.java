package com.marketinghub.product.bigidea.v1.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Transporta a direção estratégica fixada sem reler a versão vigente durante uma retentativa. */
@Component
@RequiredArgsConstructor
@Slf4j
public class BigIdeaTaskContext {
  private final BigIdeaService service;
  private final AgentTaskTargetContextProvider targets;
  private final ObjectMapper json;

  /** Fixa a versão antes de disponibilizar a tarefa, inclusive a ausência explícita. */
  public void pin(AgentTask task) {
    var target = targets.resolve(task.getSourceReference()).orElse(null);
    var snapshot = json.createObjectNode();
    snapshot.put("contractVersion", "PRODUCT_BIG_IDEA_V1");
    snapshot.put("authority", "PRODUCT_STRATEGIC_DIRECTION");
    snapshot.put("downstreamMayRedefine", false);
    snapshot.put("commercialAuthorization", false);
    snapshot.put(
        "guidance",
        "Orientar produto, oferta, comunicação e revisões pela ideia fixada; não ampliar promessas. Divergências exigem ajuste na origem, nunca alteração silenciosa. Big Idea é hipótese, não prova de vendas.");
    if (target != null && target.productId() != null) {
      var idea =
          service.pin(
              target.productId(),
              target.experimentId() == null
                  ? task.getSourceReference()
                  : "experiment:" + target.experimentId());
      snapshot.put("productId", target.productId());
      snapshot.put("status", idea == null ? "MISSING" : "DEFINED");
      snapshot.set("revision", json.valueToTree(idea));
    } else {
      snapshot.put("status", "PRODUCT_NOT_CREATED");
    }
    task.setProductBigIdeaJson(snapshot.toString());
  }

  /** Expõe a fotografia imutável e distingue tarefas históricas anteriores ao contrato. */
  public JsonNode snapshot(AgentTask task) {
    if (task.getProductBigIdeaJson() == null)
      return json.valueToTree(Map.of("status", "LEGACY_UNPINNED"));
    try {
      return json.readTree(task.getProductBigIdeaJson());
    } catch (Exception ex) {
      log.error(
          "Falha ao ler Big Idea da tarefa. taskId={} sourceReference={}",
          task.getId(),
          task.getSourceReference(),
          ex);
      throw new IllegalStateException("Fotografia da Big Idea inválida.", ex);
    }
  }

  /** Leva a mesma fotografia ao alvo de executores determinísticos e consumidores de contexto. */
  public AgentTaskTargetResponse project(AgentTask task, AgentTaskTargetResponse target) {
    if (target == null) return null;
    ObjectNode context =
        target.pdeContext() instanceof ObjectNode object
            ? object.deepCopy()
            : json.createObjectNode();
    context.set("productBigIdea", snapshot(task));
    return new AgentTaskTargetResponse(
        target.sourceReference(),
        target.experimentId(),
        target.productId(),
        target.productSlug(),
        target.productName(),
        target.productInternalName(),
        target.experienceVersion(),
        target.publicUrl(),
        target.commercialCheckoutProvider(),
        target.commercialCheckoutReference(),
        target.commercialCheckoutUrl(),
        target.unitPriceBrl(),
        context);
  }
}
