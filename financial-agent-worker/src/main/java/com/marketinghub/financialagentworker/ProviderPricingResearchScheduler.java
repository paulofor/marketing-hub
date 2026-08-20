package com.marketinghub.financialagentworker;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: pesquisar periodicamente um preço pendente sem bloquear as demais filas de
 * Plutus.
 */
@Component
public class ProviderPricingResearchScheduler {
  private static final Logger log = LoggerFactory.getLogger(ProviderPricingResearchScheduler.class);
  private final FinancialAgentBackendClient backend;
  private final FinancialCodexRunner runner;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Configura a pesquisa com acesso exclusivo aos contratos oficiais do backend. */
  public ProviderPricingResearchScheduler(
      FinancialAgentBackendClient backend, FinancialCodexRunner runner) {
    this.backend = backend;
    this.runner = runner;
  }

  /** Pesquisa em PLAY no máximo um modelo por hora para controlar custo e preservar auditoria. */
  @Scheduled(cron = "0 7 * * * *")
  public void researchOne() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
    ProviderPricingCandidate candidate = null;
    try {
      candidate = backend.pendingProviderPricing();
      if (candidate == null) return;
      Map<String, Object> result = runner.researchProviderPricing(candidate);
      backend.updateProviderPricing(candidate.id(), result);
    } catch (Exception ex) {
      log.error(
          "Falha na pesquisa de preço de Plutus. modelId={} provider={}",
          candidate == null ? null : candidate.id(),
          candidate == null ? null : candidate.providerName(),
          ex);
    }
  }
}
