package com.marketinghub.landinggeneratoragent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: controlar o polling operacional do Agente Gerador de Landing. */
@Component
public class LandingGeneratorAgentScheduler {
  private static final Logger log = LoggerFactory.getLogger(LandingGeneratorAgentScheduler.class);
  private final LandingGeneratorBackendClient backend;
  private final LandingGeneratorCodexRunner runner;

  /** Configura o ciclo local sem controlar o avanço do pipeline. */
  public LandingGeneratorAgentScheduler(
      LandingGeneratorBackendClient backend, LandingGeneratorCodexRunner runner) {
    this.backend = backend;
    this.runner = runner;
  }

  /** Processa uma pendência por ciclo e isola a falha. */
  @Scheduled(cron = "20 */1 * * * *")
  public void processPending() {
    try {
      backend.claimPending().forEach(this::process);
    } catch (RuntimeException ex) {
      log.error("Falha ao consultar fila do Agente Gerador de Landing", ex);
    }
  }

  /** Executa uma landing com correlação completa. */
  private void process(LandingAgentJob job) {
    try {
      backend.report(job, runner.run(job));
    } catch (CodexActivityTimeoutException ex) {
      log.error(
          "Execução sem atividade será retomada pela lease canônica. executionId={} experimentId={}",
          job.executionId(),
          job.experimentId(),
          ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Execução interrompida. experimentId={}", job.experimentId(), ex);
      backend.fail(job, new IllegalStateException("Execução interrompida", ex));
    } catch (Exception ex) {
      log.error("Execução falhou. experimentId={}", job.experimentId(), ex);
      backend.fail(job, new IllegalStateException("Falha na correção da landing", ex));
    }
  }
}
