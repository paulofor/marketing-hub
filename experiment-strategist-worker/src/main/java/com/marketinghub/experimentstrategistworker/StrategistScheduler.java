package com.marketinghub.experimentstrategistworker;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: consumir pesquisas pendentes e reportar seus resultados ao backend. */
@Component
public class StrategistScheduler {
  private static final Logger log = LoggerFactory.getLogger(StrategistScheduler.class);
  private final StrategistBackendClient backend;
  private final CodexStrategistRunner runner;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Configura a fila canônica e o executor somente leitura. */
  public StrategistScheduler(StrategistBackendClient backend, CodexStrategistRunner runner) {
    this.backend = backend;
    this.runner = runner;
  }

  /** Processa em PLAY no máximo uma pesquisa por ciclo para preservar custo e rastreabilidade. */
  @Scheduled(fixedDelay = 60000)
  public void processOne() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
    StrategistJob job = null;
    try {
      job = backend.claim();
      if (job == null) return;
      if (!"READ_ONLY_RESEARCH".equals(job.authorityMode())
          && !"COMMERCIAL_ASSUMPTIONS_PROPOSAL".equals(job.authorityMode()))
        throw new IllegalStateException("Autoridade da pesquisa não é somente leitura.");
      log.info(
          "Iniciando pesquisa do Estrategista. executionId={} planId={}",
          job.id(),
          job.commercialPlanId());
      backend.complete(job.id(), runner.run(job));
      log.info(
          "Pesquisa do Estrategista concluída. executionId={} planId={}",
          job.id(),
          job.commercialPlanId());
    } catch (CodexActivityTimeoutException ex) {
      log.error(
          "Pesquisa sem atividade aguardará retomada da lease. executionId={} planId={}",
          job == null ? null : job.id(),
          job == null ? null : job.commercialPlanId(),
          ex);
    } catch (Exception ex) {
      log.error(
          "Falha no experiment-strategist-worker. executionId={} planId={}",
          job == null ? null : job.id(),
          job == null ? null : job.commercialPlanId(),
          ex);
      if (job != null) backend.fail(job.id(), detailed(ex));
    }
  }

  /** Converte a exceção completa em diagnóstico persistível. */
  private String detailed(Exception ex) {
    StringWriter value = new StringWriter();
    ex.printStackTrace(new PrintWriter(value));
    return value.toString();
  }
}
