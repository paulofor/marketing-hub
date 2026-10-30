package com.marketinghub.payments.service.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.service.AgendaCheiaKitProductionService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.io.TempDir;

/**
 * Responsabilidade: reproduzir falha de callback e garantir replay do arquivo sem nova composição.
 */
class PrivateKitCompositionWorkerTest {
  @TempDir Path directory;

  /**
   * Um reinício entre produção e aceite reaplica bytes e claim duráveis, chamando o compositor uma
   * vez.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"nails-v1", "nails-v2"})
  void replaysSavedZipAfterCallbackFailureAndRestart(String profile) throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var attempts = new AtomicInteger();
    String id = "12345678-1234-1234-1234-123456789abc";
    var mapper = new ObjectMapper();
    var composer = mock(AgendaCheiaKitProductionService.class);
    Path zip = directory.resolve("agenda-cheia-private-" + id + ".zip");
    Files.write(zip, new byte[51000]);
    when(composer.preparePrivateCandidate(any(), eq(profile), eq(id)))
        .thenReturn(new AgendaCheiaKitProductionService.PreparedKit(profile, zip, 100));
    server.createContext(
        "/api/pde/kit/private/v1/stage-executions",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          assertThat(exchange.getRequestHeaders().getFirst("X-Kit-Supported-Profiles"))
              .isEqualTo(String.join(",", new java.util.TreeSet<>(
                  com.marketinghub.payments.service.kit.CapellaKitCatalog.supportedProfiles())));
          String response =
              path.endsWith("pending")
                  ? (attempts.get() == 0 ? "[{\"id\":\"" + id + "\"}]" : "[]")
                  : path.endsWith("claim")
                      ? "{\"profileCode\":\""
                          + profile
                          + "\",\"input\":{\"services\":\"Manicure\"}}"
                      : "";
          if (path.endsWith("result")) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            if (profile.endsWith("-v2")) {
              assertThat(exchange.getRequestHeaders().getFirst("Content-Type"))
                  .startsWith("multipart/form-data");
              assertThat(new String(body, StandardCharsets.ISO_8859_1))
                  .contains("KIT_LICENSED_REFERENCE_V1", "name=\"artifact\"");
            }
          }
          int status = path.endsWith("result") && attempts.incrementAndGet() == 1 ? 503 : 200;
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    try {
      String backend = "http://127.0.0.1:" + server.getAddress().getPort();
      var worker =
          new PrivateKitCompositionWorker(
              composer, mapper, backend, "local-test", directory.toString(), true);
      worker.poll();
      assertThat(attempts).hasValue(1);
      try (var files = Files.list(directory.resolve("private-callbacks-v1"))) {
        assertThat(files.toList()).hasSize(1);
      }
      new PrivateKitCompositionWorker(
              composer, mapper, backend, "local-test", directory.toString(), true)
          .poll();
      assertThat(attempts).hasValue(2);
      verify(composer, times(1)).preparePrivateCandidate(any(), eq(profile), eq(id));
      assertThat(Files.exists(zip)).isFalse();
      try (var files = Files.list(directory.resolve("private-callbacks-v1"))) {
        assertThat(files.toList()).isEmpty();
      }
    } finally {
      server.stop(0);
    }
  }

  /** Confere que todo perfil anunciado resolve um contrato instalado, incluindo a fotografia v2. */
  @org.junit.jupiter.api.Test
  void advertisedProfilesAreInstalledAndVersioned() {
    var profiles = com.marketinghub.payments.service.kit.CapellaKitCatalog.supportedProfiles();
    assertThat(profiles).hasSize(12).contains("hair-v2", "barber-v2", "nails-v1");
    profiles.forEach(profile -> assertThat(
        com.marketinghub.payments.service.kit.CapellaKitCatalog.byCode(profile).code())
        .isEqualTo(profile));
  }
}
