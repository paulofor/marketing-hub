package com.marketinghub.product.bigidea.v1.service;

import com.fasterxml.jackson.databind.*;
import com.marketinghub.agenttask.*;
import com.marketinghub.product.bigidea.v1.service.save.BigIdeaContent;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Materializa a ideia da estratégia aprovada sem permitir que Atena sobrescreva uma decisão. */
@Component
@RequiredArgsConstructor
@Slf4j
public class BigIdeaStrategyCompletion implements AgentTaskCompletionHook {
  private final BigIdeaService service;
  private final BigIdeaTaskContext context;
  private final AgentTaskTargetContextProvider targets;
  private final ObjectMapper json;
  private final Validator validator;

  /** Aplica o contrato apenas a novas tarefas de estratégia de Atena. */
  @Override
  public boolean supports(AgentTask task) {
    return task.getProductBigIdeaJson() != null
        && task.getProcessDefinition() != null
        && "pde-commercial-plan-offer".equals(task.getProcessDefinition().getProcessCode())
        && task.getProcessDefinition().getVersionNumber() >= 10
        && "marketStrategy".equals(task.getProcessActivityId())
        && "experiment-strategist".equals(task.getAssignedAgent().getAgentKey());
  }

  /** Valida a referência de entrada e cria somente a primeira Big Idea após aprovação. */
  @Override
  public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
    try {
      JsonNode result = json.readTree(request.resultJson());
      if (!"APPROVE".equals(result.path("decision").asText()))
        return CompletionDisposition.COMPLETE;
      BigIdeaContent content = json.treeToValue(result.required("bigIdea"), BigIdeaContent.class);
      if (content == null || !validator.validate(content).isEmpty())
        throw new IllegalArgumentException("Big Idea incompleta no resultado da estratégia.");
      JsonNode pinned = context.snapshot(task).path("revision").path("content");
      if (!pinned.isMissingNode() && !pinned.equals(json.valueToTree(content)))
        throw new IllegalArgumentException(
            "Atena deve preservar a Big Idea fixada; evolução exige nova revisão explícita.");
      var target = targets.resolve(task.getSourceReference()).orElse(null);
      if (target != null && target.productId() != null) {
        if (pinned.isMissingNode())
          service.initializeFromStrategy(target.productId(), content, task.getId());
        service.pin(
            target.productId(),
            target.experimentId() == null
                ? task.getSourceReference()
                : "experiment:" + target.experimentId());
      }
      return CompletionDisposition.COMPLETE;
    } catch (Exception ex) {
      log.error(
          "Falha no contrato Big Idea de Atena. taskId={} sourceReference={}",
          task.getId(),
          task.getSourceReference(),
          ex);
      throw new IllegalArgumentException(
          "Big Idea ausente, inválida ou divergente da revisão fixada.", ex);
    }
  }
}
