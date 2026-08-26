package com.marketinghub.moismeta.metaadlibraryv1;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Executa uma investigação Meta pendente por vez e reporta todo resultado ao backend. */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetaAdLibraryV1Runner {

  private final MarketingHubBackendClient backendClient;
  private final MetaAdLibraryApiClient metaClient;
  private final MetaAdLibraryAccessHealthIndicator accessHealth;

  /** Consulta a fila em intervalo fixo sem transferir a decisão comercial ao coletor. */
  @Scheduled(cron = "0 */15 * * * *")
  public void poll() {
    MetaAdLibraryContracts.AccessPreflight preflight = metaClient.preflight();
    accessHealth.record(preflight);
    if (!preflight.authorized()) {
      log.warn(
          "Coleta Meta aguardando autorização oficial status={} code={} subcode={} message={}",
          preflight.status(),
          preflight.errorCode(),
          preflight.errorSubcode(),
          preflight.message());
      return;
    }
    backendClient.pending().ifPresent(this::execute);
  }

  /** Executa a coleta oficial e preserva falha quando não houver credencial ou contrato válido. */
  void execute(MetaAdLibraryContracts.PendingInvestigation investigation) {
    String runId = UUID.randomUUID().toString();
    try {
      List<MetaAdLibraryContracts.Observation> observations = metaClient.search(investigation);
      if (!observations.isEmpty()) {
        backendClient.observations(
            investigation.id(),
            new MetaAdLibraryContracts.ObservationBatch(runId, observations, Instant.now()));
      }
      backendClient.complete(investigation.id(), true, null);
    } catch (RuntimeException ex) {
      log.error(
          "Falha na investigação Meta runId={} investigationId={}", runId, investigation.id(), ex);
      backendClient.complete(investigation.id(), false, ex.getMessage());
    }
  }
}
