package com.marketinghub.pde.vega.privatevalidation.v1;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Confere que o gateway preserva cookies protegidos e nunca injeta a autorização interna. */
class MusaConsultorGatewayTest {
  private HttpServer backend;
  private MockMvc mvc;
  private final AtomicReference<String> session = new AtomicReference<>();

  /** Simula a fronteira HTTP para observar o transporte real do contrato. */
  @BeforeEach
  void prepare() throws Exception {
    backend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    backend.createContext(
        "/api/pde/vega/consultor/v1",
        exchange -> {
          session.set(exchange.getRequestHeaders().getFirst("X-Musa-Session"));
          assertThat(exchange.getRequestHeaders().getFirst("X-PDE-Internal-Token")).isNull();
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange
              .getResponseHeaders()
              .add(
                  "Set-Cookie",
                  "musa-consultor-session=synthetic; HttpOnly; Secure; SameSite=Strict;"
                      + " Path=/api/pde/vega/consultor/v1");
          byte[] body = "{}".getBytes();
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    backend.start();
    mvc =
        MockMvcBuilders.standaloneSetup(
                new MusaConsultorGatewayController(
                    new VegaPrivateGatewayService(
                        "http://127.0.0.1:" + backend.getAddress().getPort()),
                    new InternalApiAuthorizer("local-only")))
            .build();
  }

  /** Encerra somente o servidor sintético criado por este teste. */
  @AfterEach
  void close() {
    backend.stop(0);
  }

  /** Propaga a sessão e o Set-Cookie sem colocá-los no corpo ou armazenamento JavaScript. */
  @Test
  void transportsHttpOnlyCookie() throws Exception {
    mvc.perform(
            get("/api/pde/vega/consultor/v1/workspace")
                .cookie(new Cookie("musa-consultor-session", "test-session")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "private, no-store"))
        .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));
    assertThat(session.get()).isEqualTo("test-session");
  }

  /** Falha antes do transporte em tentativa administrativa sem token ou mutação estrangeira. */
  @Test
  void rejectsPrivilegeEscalationAndForeignOrigin() throws Exception {
    mvc.perform(post("/api/pde/vega/consultor/v1/internal/cycles/22/access"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/pde/vega/consultor/v1/commands")
                .header("Origin", "https://foreign.invalid")
                .header("Host", "local.invalid")
                .content("{}"))
        .andExpect(status().isForbidden());
  }
}
