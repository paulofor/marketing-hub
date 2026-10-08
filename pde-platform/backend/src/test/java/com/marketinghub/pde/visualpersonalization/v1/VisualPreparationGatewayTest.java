package com.marketinghub.pde.visualpersonalization.v1;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: garantir transporte privado sem ampliar autorização, fila ou escopo. */
class VisualPreparationGatewayTest {
  private HttpServer main;
  private MockMvc mvc;
  private VisualPreparationGatewayService service;
  private final AtomicInteger calls = new AtomicInteger();
  private final AtomicReference<String> received = new AtomicReference<>();
  private final AtomicReference<String> token = new AtomicReference<>();
  private final String job = "pde-visual-v1-00000000-0000-4000-8000-000000000001";

  /** Usa transporte HTTP real e somente uma origem sintética em loopback. */
  @BeforeEach void setup() throws Exception {
    main = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    main.createContext("/api/pde/visual-personalization/v1", e -> {
      calls.incrementAndGet();
      received.set(e.getRequestMethod() + " " + e.getRequestURI() + " " + new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      token.set(e.getRequestHeaders().getFirst("X-PDE-Internal-Token"));
      if (token.get() == null) token.set(e.getRequestHeaders().getFirst("X-PDE-Visual-Session"));
      byte[] body = "{\"status\":\"PDE_COMPLETED\"}".getBytes(StandardCharsets.UTF_8);
      e.sendResponseHeaders(e.getRequestURI().toString().endsWith("/request") ? 409 : 200, body.length);
      e.getResponseBody().write(body); e.close();
    });
    main.start();
    service = new VisualPreparationGatewayService("http://127.0.0.1:" + main.getAddress().getPort());
    mvc = MockMvcBuilders.standaloneSetup(new VisualPreparationGatewayController(service, new InternalApiAuthorizer("local-only"))).build();
  }

  /** Encerra apenas a dependência efêmera do próprio teste. */
  @AfterEach void stop() { main.stop(0); }

  /** A leitura privada transmite a credencial do usuário, sem injetar segredo interno. */
  @Test void privateReadDoesNotElevateCredentials() throws Exception {
    mvc.perform(get("/api/pde/visual-personalization/v1/preparations/" + job).header("X-PDE-Visual-Session", "session-only"))
        .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "private, no-store"));
    assertThat(token.get()).isEqualTo("session-only");
    assertThat(received.get()).startsWith("GET /api/pde/visual-personalization/v1/preparations/" + job);
  }

  /** A fila só aceita o executor autorizado e preserva o endpoint canônico. */
  @Test void pendingRequiresInternalCredential() throws Exception {
    assertThatThrownBy(() -> mvc.perform(get("/api/pde/visual-personalization/v1/internal/stage-executions/pending")))
        .hasRootCauseInstanceOf(SecurityException.class);
    assertThat(calls.get()).isZero();
    mvc.perform(get("/api/pde/visual-personalization/v1/internal/stage-executions/pending").header("X-PDE-Internal-Token", "local-only"))
        .andExpect(status().isOk());
    assertThat(token.get()).isEqualTo("local-only");
  }

  /** Callback mantém identidade, payload e recusa funcional retornada pelo backend principal. */
  @Test void callbackPreservesPayloadAndStatus() throws Exception {
    mvc.perform(post("/api/pde/visual-personalization/v1/internal/stage-executions/" + job + "/request")
        .header("X-PDE-Internal-Token", "local-only").contentType("application/json").content("{\"rawRequest\":{}}"))
        .andExpect(status().isConflict());
    assertThat(received.get()).endsWith(" {\"rawRequest\":{}}");
  }

  /** Não transforma caminho ou URL arbitrária em consulta de outro módulo. */
  @Test void rejectsForeignPathAndCredentialUrl() {
    assertThatThrownBy(() -> service.forward("GET", "/products/11", null, "X-PDE-Visual-Session", "session-only"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new VisualPreparationGatewayService("https://secret@example.org"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(calls.get()).isZero();
  }
}
