package com.marketinghub.moismeta.metaadlibraryv1;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Garante que o coletor não fabrique evidência quando faltar acesso oficial. */
class MetaAdLibraryApiClientTest {

  /** Bloqueia a coleta sem token antes de qualquer chamada externa. */
  @Test
  void shouldFailExplicitlyWhenOfficialAccessTokenIsMissing() {
    MetaAdLibraryApiClient client =
        new MetaAdLibraryApiClient(
            new ObjectMapper(),
            "https://graph.facebook.com/v23.0",
            "",
            new ExternalCommercialSignalInspector());

    assertThatThrownBy(
            () ->
                client.search(
                    new MetaAdLibraryContracts.PendingInvestigation(
                        81L, "workspace-001", "agenda cheia", "BR")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("META_AD_LIBRARY_ACCESS_TOKEN");
  }
}
