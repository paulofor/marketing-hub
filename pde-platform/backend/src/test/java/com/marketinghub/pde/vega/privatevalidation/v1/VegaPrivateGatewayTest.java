package com.marketinghub.pde.vega.privatevalidation.v1;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Responsabilidade: garantir transporte privado sem perda de contexto ou elevação de credenciais.
 */
class VegaPrivateGatewayTest {
  private HttpServer backend;
  private MockMvc mvc;
  private VegaPrivateGatewayService gateway;
  private final AtomicReference<String> path = new AtomicReference<>();
  private final AtomicReference<String> payload = new AtomicReference<>();
  private final AtomicReference<String> token = new AtomicReference<>();
  private final AtomicInteger calls = new AtomicInteger();

  /** Simula somente o backend remoto e preserva o controller e transporte HTTP reais. */
  @BeforeEach
  void prepare() throws Exception {
    backend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    backend.createContext(
        "/api/pde/vega/private/v1",
        exchange -> {
          calls.incrementAndGet();
          path.set(exchange.getRequestMethod() + " " + exchange.getRequestURI());
          payload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          token.set(exchange.getRequestHeaders().getFirst("X-PDE-Internal-Token"));
          if (token.get() == null)
            token.set(exchange.getRequestHeaders().getFirst("X-Vega-Session"));
          byte[] response =
              "{\"cycleId\":91022,\"origin\":\"AGENT_VALIDATION\"}"
                  .getBytes(StandardCharsets.UTF_8);
          int status = exchange.getRequestURI().getPath().endsWith("/generate") ? 409 : 200;
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    backend.start();
    gateway = new VegaPrivateGatewayService("http://127.0.0.1:" + backend.getAddress().getPort());
    mvc =
        MockMvcBuilders.standaloneSetup(
                new VegaPrivateGatewayController(gateway, new InternalApiAuthorizer("local-token")))
            .build();
  }

  /** Remove a dependência efêmera sem tocar serviços externos. */
  @AfterEach
  void closeBackend() {
    backend.stop(0);
  }

  /** A consulta pública não recebe o segredo interno configurado no servidor. */
  @Test
  void neverElevatesPublicCaller() throws Exception {
    mvc.perform(get("/api/pde/vega/private/v1/contract"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "private, no-store"));
    assertThat(token.get()).isNull();
    assertThatThrownBy(
            () ->
                mvc.perform(
                    post("/api/pde/vega/private/v1/internal/sessions")
                        .contentType("application/json")
                        .content("{}")))
        .hasRootCauseInstanceOf(SecurityException.class);
    assertThat(calls.get()).isEqualTo(1);
  }

  /** Preserva modalidade, identidade e autorização recebida na fila canônica. */
  @Test
  void forwardsOnlyAuthorizedPendingMode() throws Exception {
    mvc.perform(
            get("/api/pde/vega/private/v1/internal/adjustment/stage-executions/pending?mode=FIXTURE")
                .header("X-PDE-Internal-Token", "local-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycleId").value(91022));
    assertThat(path.get()).endsWith("/pending?mode=FIXTURE");
    assertThat(token.get()).isEqualTo("local-token");
    mvc.perform(
            get("/api/pde/vega/private/v1/internal/adjustment/stage-executions/pending?mode=OTHER")
                .header("X-PDE-Internal-Token", "local-token"))
        .andExpect(status().isBadRequest());
    assertThat(calls.get()).isEqualTo(1);
  }

  /** Conserva payload, sessão e rejeição funcional sem converter falha em sucesso. */
  @Test
  void preservesSessionPayloadAndUpstreamStatus() throws Exception {
    String body = "{\"occasion\":\"Almoço\",\"existingSelection\":\"Camisa\"}";
    mvc.perform(
            post("/api/pde/vega/private/v1/generate")
                .header("X-Vega-Session", "session-token")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isConflict());
    assertThat(payload.get()).isEqualTo(body);
    assertThat(token.get()).isEqualTo("session-token");
  }

  /** Transporta callback e relatório sem executar inferência nem controlar avanço. */
  @Test
  void forwardsCallbackAndReportWithExplicitIdentities() throws Exception {
    mvc.perform(
            post("/api/pde/vega/private/v1/internal/adjustment/stage-executions/91001/result")
                .header("X-PDE-Internal-Token", "local-token")
                .contentType("application/json")
                .content("{\"costUsd\":0}"))
        .andExpect(status().isOk());
    assertThat(path.get()).endsWith("/91001/result");
    mvc.perform(
            get("/api/pde/vega/private/v1/internal/cycles/91022/report")
                .header("X-PDE-Internal-Token", "local-token"))
        .andExpect(status().isOk());
    assertThat(path.get()).endsWith("/91022/report");
  }

  /** Recusa configuração ausente e caminhos fora do contrato antes de qualquer integração. */
  @Test
  void rejectsUnavailableOrArbitraryUpstream() {
    assertThatThrownBy(
            () ->
                new VegaPrivateGatewayService("")
                    .forward("GET", "/contract", null, "X-Vega-Session", null))
        .hasMessageContaining("não está configurada");
    assertThatThrownBy(() -> gateway.forward("GET", "/../products", null, "X-Vega-Session", null))
        .hasMessageContaining("fora do contrato");
    assertThatThrownBy(() -> new VegaPrivateGatewayService("https://user:secret@example.invalid"))
        .hasMessageContaining("sem segredo");
    assertThat(calls.get()).isZero();
  }
}
