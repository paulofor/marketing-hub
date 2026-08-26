package com.marketinghub.moismeta.metaadlibraryv1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Protege o coletor contra reserva de trabalho sem autorização oficial comprovada. */
class MetaAdLibraryV1RunnerTest {

  /** Deve reservar e concluir a pendência somente após o preflight oficial ser aprovado. */
  @Test
  void shouldExecutePendingInvestigationWhenAccessIsAuthorized() {
    MarketingHubBackendClient backendClient = mock(MarketingHubBackendClient.class);
    MetaAdLibraryApiClient metaClient = mock(MetaAdLibraryApiClient.class);
    MetaAdLibraryAccessHealthIndicator health = new MetaAdLibraryAccessHealthIndicator();
    MetaAdLibraryContracts.PendingInvestigation pending =
        new MetaAdLibraryContracts.PendingInvestigation(
            81L, "workspace-001", "treino entrevista emprego", "PT", "INSTAGRAM");
    when(metaClient.preflight())
        .thenReturn(
            new MetaAdLibraryContracts.AccessPreflight(
                true,
                "AUTHORIZED",
                null,
                null,
                "Acesso oficial confirmado",
                Instant.parse("2026-08-26T12:00:00Z")));
    when(backendClient.pending()).thenReturn(Optional.of(pending));
    when(metaClient.search(pending)).thenReturn(List.of());
    MetaAdLibraryV1Runner runner = new MetaAdLibraryV1Runner(backendClient, metaClient, health);

    runner.poll();

    verify(backendClient).pending();
    verify(metaClient).search(pending);
    verify(backendClient).complete(81L, true, null);
    assertThat(health.health().getDetails())
        .containsEntry("apiAccess", "AUTHORIZED")
        .containsEntry("authorized", true);
  }

  /** Deve manter a fila intacta quando o preflight real for rejeitado pela Meta. */
  @Test
  void shouldNotClaimPendingInvestigationWhenAccessIsRejected() {
    MarketingHubBackendClient backendClient = mock(MarketingHubBackendClient.class);
    MetaAdLibraryApiClient metaClient = mock(MetaAdLibraryApiClient.class);
    MetaAdLibraryAccessHealthIndicator health = new MetaAdLibraryAccessHealthIndicator();
    when(metaClient.preflight())
        .thenReturn(
            new MetaAdLibraryContracts.AccessPreflight(
                false,
                "UNAUTHORIZED",
                10,
                2332002,
                "Application does not have permission",
                Instant.parse("2026-08-26T12:00:00Z")));
    MetaAdLibraryV1Runner runner = new MetaAdLibraryV1Runner(backendClient, metaClient, health);

    runner.poll();

    verifyNoInteractions(backendClient);
    assertThat(health.health().getDetails())
        .containsEntry("apiAccess", "UNAUTHORIZED")
        .containsEntry("authorized", false)
        .containsEntry("errorSubcode", 2332002);
  }
}
