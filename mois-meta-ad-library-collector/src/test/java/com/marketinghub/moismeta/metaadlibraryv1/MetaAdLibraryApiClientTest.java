package com.marketinghub.moismeta.metaadlibraryv1;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
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
                        81L, "workspace-001", "agenda cheia", "BR", "INSTAGRAM")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("META_AD_LIBRARY_ACCESS_TOKEN");
  }

  /** Deve distinguir token configurado de autorização real no `ads_archive`. */
  @Test
  void shouldExposeRejectedOfficialAccessWithoutLeakingToken() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    AtomicReference<String> requestedUri = new AtomicReference<>();
    server.createContext(
        "/v26.0/ads_archive",
        exchange -> {
          requestedUri.set(exchange.getRequestURI().toString());
          byte[] body =
              "{\"error\":{\"message\":\"Application does not have permission\",\"code\":10,\"error_subcode\":2332002}}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(400, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      MetaAdLibraryApiClient client =
          new MetaAdLibraryApiClient(
              new ObjectMapper(),
              "http://127.0.0.1:" + server.getAddress().getPort() + "/v26.0",
              "segredo-de-teste",
              new ExternalCommercialSignalInspector());

      MetaAdLibraryContracts.AccessPreflight result = client.preflight();

      assertThat(result.authorized()).isFalse();
      assertThat(result.status()).isEqualTo("UNAUTHORIZED");
      assertThat(result.errorCode()).isEqualTo(10);
      assertThat(result.errorSubcode()).isEqualTo(2332002);
      assertThat(requestedUri.get()).doesNotContain("segredo-de-teste", "access_token");
    } finally {
      server.stop(0);
    }
  }

  /** Deve enviar o filtro Instagram e preservar a plataforma devolvida pela Meta. */
  @Test
  void shouldRequestAndNormalizeInstagramAds() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    AtomicReference<String> requestedQuery = new AtomicReference<>();
    server.createContext(
        "/v26.0/ads_archive",
        exchange -> {
          requestedQuery.set(
              URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8));
          byte[] body =
              """
              {"data":[{"id":"ad-1","page_id":"page-1","page_name":"Marca",
              "ad_delivery_stop_time":null,"publisher_platforms":["instagram"],
              "ad_creative_bodies":["Treine sua entrevista"]}]}
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      MetaAdLibraryApiClient client =
          new MetaAdLibraryApiClient(
              new ObjectMapper(),
              "http://127.0.0.1:" + server.getAddress().getPort() + "/v26.0",
              "segredo-de-teste",
              new ExternalCommercialSignalInspector());

      var observations =
          client.search(
              new MetaAdLibraryContracts.PendingInvestigation(
                  81L,
                  "workspace-001",
                  "entrevista emprego",
                  "PT",
                  "INSTAGRAM"));

      assertThat(observations).hasSize(1);
      assertThat(observations.getFirst().publisherPlatforms()).containsExactly("instagram");
      assertThat(observations.getFirst().status()).isEqualTo("ACTIVE");
      assertThat(requestedQuery.get()).contains("publisher_platforms=[\"INSTAGRAM\"]");
      assertThat(requestedQuery.get()).contains("search_terms=entrevista emprego");
    } finally {
      server.stop(0);
    }
  }
}
