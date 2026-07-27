package com.marketinghub.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar a configuração HTTP compartilhada das integrações OpenAI. */
class OpenAiConfigurationTest {

  /** Garante que o WebClient OpenAI leia respostas JSON grandes, como imagens em base64. */
  @Test
  void readsLargeOpenAiJsonResponse() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    String largePayload = "a".repeat(1024 * 1024);
    server.createContext(
        "/v1/large",
        exchange -> {
          byte[] body = ("{\"image\":\"" + largePayload + "\"}").getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();

    try {
      OpenAiProperties properties = new OpenAiProperties();
      properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
      properties.setConnectTimeout(Duration.ofSeconds(5));
      properties.setRequestTimeout(Duration.ofSeconds(5));
      properties.setApiKey("test-token");
      WebClient webClient =
          new OpenAiConfiguration()
              .openAiWebClient(WebClient.builder(), properties, new OpenAiApiKeyResolver());

      JsonNode response =
          webClient.get().uri("/large").retrieve().bodyToMono(JsonNode.class).block();

      assertThat(response).isNotNull();
      assertThat(response.path("image").asText()).hasSize(largePayload.length());
    } finally {
      server.stop(0);
    }
  }
}
