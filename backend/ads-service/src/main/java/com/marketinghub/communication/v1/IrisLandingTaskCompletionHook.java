package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskCompletionHook;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.geralanding.agent.v1.GovernedLandingHtmlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Responsabilidade: aplicar o HTML de Íris e aguardar o Quality Review antes da conclusão. */
@Service
public class IrisLandingTaskCompletionHook implements AgentTaskCompletionHook {
  private static final Logger log = LoggerFactory.getLogger(IrisLandingTaskCompletionHook.class);
  private final IrisCommunicationMaterializationContextProvider contextProvider;
  private final GovernedLandingHtmlService landingHtmlService;
  private final ObjectMapper objectMapper;

  /** Configura a resolução segregada do experimento e o aplicador governado de HTML. */
  public IrisLandingTaskCompletionHook(
      IrisCommunicationMaterializationContextProvider contextProvider,
      GovernedLandingHtmlService landingHtmlService,
      ObjectMapper objectMapper) {
    this.contextProvider = contextProvider;
    this.landingHtmlService = landingHtmlService;
    this.objectMapper = objectMapper;
  }

  /** Restringe o efeito ao HTML da versão de landing atribuída a Íris. */
  @Override
  public boolean supports(AgentTask task) {
    return "communication-director".equals(task.getAssignedAgent().getAgentKey())
        && task.getProcessDefinition() != null
        && "landing-page-generation".equals(task.getProcessDefinition().getProcessCode())
        && "html".equals(task.getProcessActivityId());
  }

  /** Persiste o rascunho e mantém a tarefa ativa até o parecer técnico assíncrono. */
  @Override
  public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
    try {
      JsonNode result = objectMapper.readTree(request.resultJson());
      String html = result.path("functionalOutput").path("landingHtml").asText();
      Long experimentId =
          contextProvider
              .experimentId(task.getSourceReference())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Landing de Íris sem experimento segregado no plano."));
      landingHtmlService.apply(experimentId, html, "agent-task:" + task.getId());
      return CompletionDisposition.DEFERRED;
    } catch (Exception ex) {
      log.error(
          "Falha ao aplicar landing de Íris. taskId={} sourceReference={}",
          task.getId(),
          task.getSourceReference(),
          ex);
      throw new IllegalArgumentException("Resultado de landing de Íris não pôde ser aplicado.", ex);
    }
  }
}
