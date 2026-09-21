package com.marketinghub.gerasalespage.v1.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.gerasalespage.v1.web.GeraSalesPageController;
import com.marketinghub.leadportal.integration.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

/**
 * Responsabilidade: expor o controller real com fontes locais e publicação no Lead Portal local.
 */
public class PublicationRecoveryLocalApplication {
  /** Inicia somente contratos sintéticos, sem configuração nem credencial de produção. */
  public static void main(String[] args) throws Exception {
    var fixture = new GeraSalesPagePublicationRecoveryTest();
    fixture.setup();
    String html =
        """
        <!doctype html><html lang="pt-BR"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Kit de teste local</title>
        <style>body{margin:0;font:20px sans-serif}main{padding:24px}section{min-height:850px}a{display:inline-block;padding:16px}</style>
        </head><body><main><section><h1>Kit para divulgar seu trabalho</h1>
        <p>Arquivos personalizados e suporte conforme o contrato.</p>
        <a href="/checkout-local">Comprar kit por R$ 67</a></section>
        <section><h2>O que você recebe</h2><p>Posts, stories e calendário para usar no negócio.</p></section>
        </main></body></html>
        """
            .trim();
    fixture.audit.setHtml(html);
    fixture.flow.setCustomFormHtml(html);
    String publicUrl =
        "http://127.0.0.1:18081/api/flows/" + fixture.flow.getSlug() + "/page?mh_test=1";
    fixture.audit.setSalesPageUrl(publicUrl);
    fixture.experiment.setFollowUpActionUrl(publicUrl);
    when(fixture.urls.resolve(fixture.flow)).thenReturn(publicUrl);
    var properties = new LeadPortalIntegrationProperties();
    properties.setEnabled(true);
    properties.setBaseUrl("http://127.0.0.1:18081");
    var hero = mock(ExperimentHeroImageResolver.class);
    when(hero.resolve(any())).thenReturn(Optional.empty());
    var publisher = new LeadPortalFlowPublisher(new RestTemplate(), properties, hero);
    var service =
        new GeraSalesPagePublicationAuditService(
            fixture.experiments,
            fixture.executions,
            fixture.publications,
            fixture.stages,
            fixture.flows,
            publisher,
            fixture.urls,
            fixture.assets,
            new ObjectMapper());
    publisher.publish(fixture.flow);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new GeraSalesPageController(mock(GeraSalesPageStageService.class), service))
            .build();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 18096), 0);
    server.createContext(
        "/api/",
        exchange -> {
          try {
            var request =
                "POST".equals(exchange.getRequestMethod())
                    ? post(exchange.getRequestURI().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exchange.getRequestBody().readAllBytes())
                    : get(exchange.getRequestURI().toString());
            var response = mvc.perform(request).andReturn().getResponse();
            byte[] body = response.getContentAsByteArray();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.getStatus(), body.length);
            exchange.getResponseBody().write(body);
          } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(PublicationRecoveryLocalApplication.class)
                .error("Fixture local: falha na requisição uri={}", exchange.getRequestURI(), ex);
            exchange.sendResponseHeaders(500, -1);
          } finally {
            exchange.close();
          }
        });
    server.setExecutor(Executors.newFixedThreadPool(4));
    server.start();
    var output = Path.of("../../artifacts/publication-recovery/local-source.json");
    Files.createDirectories(output.getParent());
    Files.writeString(
        output,
        new ObjectMapper()
            .writeValueAsString(
                Map.of(
                    "publicUrl",
                    publicUrl,
                    "sourceHtml",
                    html,
                    "experimentId",
                    fixture.experiment.getId(),
                    "publicationId",
                    fixture.audit.getId())),
        StandardCharsets.UTF_8);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
  }
}
