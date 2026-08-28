package com.marketinghub.metaadapproverworker;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: iniciar as filas do recurso visual legado governado por Dédalo. */
@Component
@ConditionalOnProperty(name = "meta-ad-approver.execution-role", havingValue = "image-studio")
public class TemisImageStudioScheduler {
  private static final Logger log = LoggerFactory.getLogger(TemisImageStudioScheduler.class);
  private final TemisImageStudioProcessor imageStudio;
  private final TemisCreativeImprovementProcessor creativeImprovement;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Configura criação e retrabalho no container que não possui responsabilidade de revisão. */
  public TemisImageStudioScheduler(
      TemisImageStudioProcessor imageStudio,
      TemisCreativeImprovementProcessor creativeImprovement) {
    this.imageStudio = imageStudio;
    this.creativeImprovement = creativeImprovement;
  }

  /** Executa em PLAY as filas em série para impedir uploads simultâneos de artefatos grandes. */
  @Scheduled(cron = "30 */1 * * * *")
  public void processPending() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
    runSafely("estúdio de imagens", imageStudio::processPending);
    runSafely("retrabalho de criativos", creativeImprovement::processPending);
    log.debug("Ciclo produtivo do recurso visual de Dédalo concluído");
  }

  /** Preserva a outra fila produtiva quando uma integração falha. */
  private void runSafely(String operation, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException ex) {
      log.error(
          "Falha no ciclo produtivo do recurso visual de Dédalo. operation={}", operation, ex);
    }
  }
}

/** Responsabilidade: executar a fila técnica de criação e edição atribuída a Dédalo. */
@Component
@ConditionalOnProperty(name = "meta-ad-approver.execution-role", havingValue = "image-studio")
class TemisImageStudioProcessor {
  private static final Logger log = LoggerFactory.getLogger(TemisImageStudioProcessor.class);
  private final TemisImageStudioBackendClient backend;
  private final TemisImageStudioOpenAiClient openAi;
  private final MetaAdApproverProperties properties;

  /** Inicializa o executor sem transferir a decisão de avanço para o worker. */
  TemisImageStudioProcessor(
      TemisImageStudioBackendClient backend,
      TemisImageStudioOpenAiClient openAi,
      MetaAdApproverProperties properties) {
    this.backend = backend;
    this.openAi = openAi;
    this.properties = properties;
  }

  /** Processa produções em série para preservar a capacidade do backend compartilhado. */
  public void processPending() {
    List<TemisImageStudioJob> jobs = backend.claimPending(properties.getImageStudioPendingLimit());
    jobs.forEach(this::process);
    log.debug("Ciclo do recurso de imagens de Dédalo concluído");
  }

  /** Executa uma única produção e envia o arquivo somente após resposta válida. */
  private void process(TemisImageStudioJob job) {
    String requestJson = "";
    try {
      TemisImageStudioOpenAiClient.Result result = openAi.execute(job);
      requestJson = result.requestJson();
      backend.complete(job, result);
      log.info(
          "Produção visual de Dédalo concluída. jobId={} commercialPlanId={}",
          job.jobId(),
          job.commercialPlanId());
    } catch (RuntimeException ex) {
      log.error(
          "Falha na produção visual de Dédalo. jobId={} commercialPlanId={}",
          job.jobId(),
          job.commercialPlanId(),
          ex);
      backend.fail(job, ex, requestJson, "");
    }
  }
}
