package com.marketinghub.pde.paidvisual.v1.service;

import com.marketinghub.pde.paidvisual.v1.controller.PaidVisualController;
import com.marketinghub.pde.paidvisual.v1.service.contract.PaidVisualContract.Payment;
import com.marketinghub.pde.visualpersonalization.v1.controller.VisualPreparationController;
import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Responsabilidade: expor controllers reais em localhost com dependências locais para homologação
 * visual.
 */
public final class PaidVisualLocalApplication {
  /** Impede instanciação do runner de testes. */
  private PaidVisualLocalApplication() {}

  /** Inicia somente no loopback e nunca entra no artefato de produção. */
  public static void main(String[] args) throws Exception {
    var f = new PaidVisualFixture();
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new PaidVisualController(f.service, f.execution, "local-payments"),
                new VisualPreparationController(f.visual, f.execution, "local-worker"))
            .build();
    for (long id : List.of(97L, 709L, 811L))
      f.service.payment(
          new Payment(
              "local-" + id,
              "approved",
              new BigDecimal("79"),
              "BRL",
              "buyer@example.test",
              "produto-" + id,
              Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
              Map.of(
                  "experimentId",
                  id,
                  "productId",
                  id + 100,
                  "deliveryContract",
                  PaidVisualPolicy.VERSION,
                  "experienceVersion",
                  "commercial-v1",
                  "providerLiveMode",
                  true)));
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8189), 0);
    server.createContext(
        "/",
        exchange -> {
          try {
            String path = exchange.getRequestURI().getPath();
            byte[] body;
            if (path.equals("/_fixture/access"))
              body =
                  f.json.writeValueAsBytes(
                      Map.of("access", Objects.toString(f.deliveredToken, "")));
            else if (path.equals("/_fixture/image")) {
              var out = new ByteArrayOutputStream();
              ImageIO.write(new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB), "png", out);
              body =
                  f.json.writeValueAsBytes(
                      Map.of("image", Base64.getEncoder().encodeToString(out.toByteArray())));
            } else {
              var request =
                  MockMvcRequestBuilders.request(
                      HttpMethod.valueOf(exchange.getRequestMethod()), exchange.getRequestURI());
              exchange
                  .getRequestHeaders()
                  .forEach((name, values) -> values.forEach(value -> request.header(name, value)));
              request.content(exchange.getRequestBody().readAllBytes());
              var response = mvc.perform(request).andReturn().getResponse();
              response
                  .getHeaderNames()
                  .forEach(
                      name -> exchange.getResponseHeaders().set(name, response.getHeader(name)));
              body = response.getContentAsByteArray();
              if (body.length == 0 && response.getStatus() >= 400)
                body =
                    f.json.writeValueAsBytes(
                        Map.of(
                            "message",
                            Objects.toString(response.getErrorMessage(), "Operação recusada")));
              exchange.sendResponseHeaders(response.getStatus(), body.length);
              exchange.getResponseBody().write(body);
              exchange.close();
              return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
          } catch (Exception ex) {
            System.err.println("Falha do runner local: " + ex.getClass().getSimpleName());
            exchange.sendResponseHeaders(500, -1);
          } finally {
            exchange.close();
          }
        });
    server.start();
    System.out.println("LOCAL_PAID_VISUAL_READY 8189");
  }
}
