package com.marketinghub.pde.vega.privatevalidation.v1;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Comprova transporte contextual com cookie protegido, escopo fechado e rejeição entre origens. */
class VegaStyleGatewayTest {
  private HttpServer backend;
  private MockMvc mvc;
  private final AtomicInteger calls = new AtomicInteger();
  private final AtomicReference<String> session = new AtomicReference<>();
  private final AtomicReference<String> internal = new AtomicReference<>();

  /** Simula somente a dependência HTTP e mantém o gateway real sob teste. */
  @BeforeEach
  void prepare() throws Exception {
    backend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    backend.createContext(
        "/api/pde/vega/style/v1",
        exchange -> {
          calls.incrementAndGet();
          session.set(exchange.getRequestHeaders().getFirst("X-Vega-Style-Session"));
          internal.set(exchange.getRequestHeaders().getFirst("X-PDE-Internal-Token"));
          boolean material = exchange.getRequestURI().getPath().contains("/materials/");
          byte[] body =
              (material ? "Meu registro preservado" : "{\"status\":\"BLOCKED\"}")
                  .getBytes(java.nio.charset.StandardCharsets.UTF_8);
          exchange
              .getResponseHeaders()
              .set("Content-Type", material ? "text/plain;charset=UTF-8" : "application/json");
          exchange.sendResponseHeaders(
              material && !exchange.getRequestHeaders().getFirst("Accept").contains("text/plain")
                  ? 406
                  : 200,
              body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    backend.start();
    mvc =
        MockMvcBuilders.standaloneSetup(
                new VegaStyleGatewayController(
                    new VegaPrivateGatewayService(
                        "http://127.0.0.1:" + backend.getAddress().getPort()),
                    new InternalApiAuthorizer("test-internal")))
            .build();
  }

  /** Encerra apenas o servidor efêmero do teste. */
  @AfterEach
  void close() {
    backend.stop(0);
  }

  /** A sessão recebida segue ao backend, sem elevar o navegador às permissões internas. */
  @Test
  void forwardsCookieAndPreservesTextMaterials() throws Exception {
    mvc.perform(
            get("/api/pde/vega/style/v1/materials/mapa-do-meu-estilo.txt")
                .cookie(new Cookie("vega-style-session", "synthetic-session")))
        .andExpect(status().isOk())
        .andExpect(content().string("Meu registro preservado"))
        .andExpect(header().string("Cache-Control", "private, no-store"));
    assertThat(session.get()).isEqualTo("synthetic-session");
    assertThat(internal.get()).isNull();
  }

  /** Fixture e origem estrangeira são recusadas antes de qualquer chamada remota. */
  @Test
  void rejectsUnauthorizedFixtureAndCrossOrigin() {
    assertThatThrownBy(
            () -> mvc.perform(post("/api/pde/vega/style/v1/internal/fixtures/full-access")))
        .hasRootCauseInstanceOf(SecurityException.class);
    assertThatThrownBy(
            () ->
                mvc.perform(
                    post("/api/pde/vega/style/v1/start")
                        .header("Host", "vega.example")
                        .header("Origin", "https://other.example")
                        .cookie(new Cookie("vega-style-session", "synthetic-session"))))
        .hasRootCauseInstanceOf(SecurityException.class);
    assertThat(calls.get()).isZero();
  }

  /** O executor mantém as duas credenciais separadas sem acrescentar acesso do servidor. */
  @Test
  void forwardsBothExplicitCredentialsToInternalReport() throws Exception {
    mvc.perform(
            get("/api/pde/vega/style/v1/internal/report")
                .header("X-PDE-Internal-Token", "test-internal")
                .header("X-Vega-Style-Session", "synthetic-session"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BLOCKED"));
    assertThat(session.get()).isEqualTo("synthetic-session");
    assertThat(internal.get()).isEqualTo("test-internal");
  }
}
