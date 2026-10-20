package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Responsabilidade: impedir que falhas transitórias de leitura interrompam a revisão criativa. */
class CreativeReviewDownloadRecoveryTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Reproduz o timeout de Alcyone e recupera a prova uma vez, inclusive em outro contexto. */
  @ParameterizedTest
  @ValueSource(longs = {778, 991778})
  void recoversTransientSourceDownloadAndReusesVerifiedCapture(long taskId) throws Exception {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var backend = builder.build();
    byte[] render = png(1080, 1350);
    byte[] source = png(400, 500);
    String endpoint = endpoint(taskId);
    var inputs = inputs(render, source);
    server.expect(requestTo(endpoint)).andRespond(withSuccess(inputs, MediaType.APPLICATION_JSON));
    server.expect(requestTo(endpoint + "/770/1060/content"))
        .andRespond(withSuccess(render, MediaType.IMAGE_PNG));
    server.expect(requestTo(endpoint + "/765/1047/content"))
        .andRespond(withException(new HttpTimeoutException("Request timed out")));
    server.expect(requestTo(endpoint + "/765/1047/content"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    server.expect(requestTo(endpoint + "/765/1047/content"))
        .andRespond(withSuccess(source, MediaType.IMAGE_PNG));
    var task = task(taskId);
    List<Path> files;
    try (var images = CreativeReviewImages.load(backend, task)) {
      var command = images.attach(List.of("codex", "exec"));
      files = command.stream().filter(value -> value.endsWith(".png")).map(Path::of).toList();
      assertThat(files).hasSize(3);
      var checks = json.valueToTree(task.get("creativeEvidenceChecks")).get(0);
      assertThat(checks.path("source").path("hashVerified").asBoolean()).isTrue();
      assertThat(checks.path("currentDestination").path("captures").get(0)
          .path("attachmentIndex").asInt()).isEqualTo(checks.path("source").path("attachmentIndex").asInt());
    }
    assertThat(files).allSatisfy(file -> assertThat(file).doesNotExist());
    server.verify();
  }

  /** Limita falhas persistentes sem publicar contexto parcial para o modelo. */
  @Test
  void stopsAfterThreeTransportFailures() throws Exception {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    for (int attempt = 0; attempt < 3; attempt++)
      server.expect(requestTo(endpoint(778)))
          .andRespond(withException(new HttpTimeoutException("Request timed out")));
    var task = task(778);
    assertThatThrownBy(() -> CreativeReviewImages.load(builder.build(), task))
        .isInstanceOf(java.io.IOException.class).hasMessageContaining("778");
    assertThat(task).doesNotContainKeys("creativeVisualInputs", "creativeEvidenceChecks");
    server.verify();
  }

  /** Preserva recusas de autorização, ausência e contrato sem repetir requisições. */
  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 404, 409, 429, 500})
  void doesNotRetryFunctionalOrAuthorizationFailure(int status) {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(endpoint(778))).andRespond(withStatus(HttpStatus.valueOf(status)));
    assertThatThrownBy(() -> CreativeReviewImages.load(builder.build(), task(778)))
        .isInstanceOf(java.io.IOException.class);
    server.verify();
  }

  /** Recusa pixels divergentes sem buscar outra resposta para tentar aprová-los. */
  @Test
  void doesNotRetryHashMismatch() throws Exception {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    byte[] render = png(1080, 1350);
    byte[] source = png(400, 500);
    server.expect(requestTo(endpoint(778)))
        .andRespond(withSuccess(inputs(render, source), MediaType.APPLICATION_JSON));
    server.expect(requestTo(endpoint(778) + "/770/1060/content"))
        .andRespond(withSuccess(source, MediaType.IMAGE_PNG));
    assertThatThrownBy(() -> CreativeReviewImages.load(builder.build(), task(778)))
        .isInstanceOf(java.io.IOException.class).hasMessageContaining("hash");
    server.verify();
  }

  /** Preserva cancelamento da thread sem iniciar outra leitura ou inferência. */
  @Test
  void doesNotRetryInterruptedRead() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(endpoint(778)))
        .andRespond(request -> {
          Thread.currentThread().interrupt();
          throw new java.io.IOException("Leitura interrompida", new InterruptedException());
        });
    try {
      assertThatThrownBy(() -> CreativeReviewImages.load(builder.build(), task(778)))
          .isInstanceOf(java.io.IOException.class);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      server.verify();
    } finally {
      Thread.interrupted();
    }
  }

  /** Recupera os códigos temporários permitidos sem ampliar retentativas a outros erros. */
  @ParameterizedTest
  @ValueSource(ints = {502, 503, 504})
  void retriesTemporaryGatewayFailure(int status) throws Exception {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(endpoint(778))).andRespond(withStatus(HttpStatus.valueOf(status)));
    server.expect(requestTo(endpoint(778))).andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));
    assertThat(CreativeEvidenceReader.read(builder.build(), endpoint(778), 778, String.class))
        .isEqualTo("ok");
    server.verify();
  }

  /** Não insiste em falha de identidade TLS nem substitui a verificação de segurança. */
  @Test
  void doesNotRetryTlsFailure() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(endpoint(778)))
        .andRespond(withException(new javax.net.ssl.SSLException("Certificado inválido")));
    assertThatThrownBy(
        () -> CreativeEvidenceReader.read(builder.build(), endpoint(778), 778, String.class))
        .isInstanceOf(org.springframework.web.client.ResourceAccessException.class);
    server.verify();
  }

  /** Reconfere o hash mesmo quando uma referência reaproveita bytes já baixados nesta tarefa. */
  @Test
  void rejectsChangedHashOnRepeatedReference() throws Exception {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    byte[] render = png(1080, 1350);
    byte[] source = png(400, 500);
    var inputs = json.readTree(inputs(render, source));
    ((com.fasterxml.jackson.databind.node.ObjectNode) inputs.get(0).path("creativeProof")
        .path("currentDestination").path("captures").get(0).path("evidence"))
        .put("sha256", "0".repeat(64));
    server.expect(requestTo(endpoint(778)))
        .andRespond(withSuccess(inputs.toString(), MediaType.APPLICATION_JSON));
    server.expect(requestTo(endpoint(778) + "/770/1060/content"))
        .andRespond(withSuccess(render, MediaType.IMAGE_PNG));
    server.expect(requestTo(endpoint(778) + "/765/1047/content"))
        .andRespond(withSuccess(source, MediaType.IMAGE_PNG));
    assertThatThrownBy(() -> CreativeReviewImages.load(builder.build(), task(778)))
        .isInstanceOf(java.io.IOException.class).hasMessageContaining("hash");
    server.verify();
  }

  /** Comprova HTTP, anexos e executor local sem chamar modelo quando a prova não chega. */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void invokesSimulatedModelOnlyAfterCompleteRecovery(
      boolean persistentFailure, @org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
    byte[] render = png(1080, 1350);
    byte[] source = png(400, 500);
    String inputs = inputs(render, source);
    var reads = new java.util.concurrent.atomic.AtomicInteger();
    var audits = new java.util.concurrent.atomic.AtomicInteger();
    var server = com.sun.net.httpserver.HttpServer.create(
        new java.net.InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      exchange.getRequestBody().readAllBytes();
      String path = exchange.getRequestURI().getPath();
      byte[] body;
      if ("PUT".equals(exchange.getRequestMethod())) {
        audits.incrementAndGet();
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return;
      }
      if (path.endsWith("/765/1047/content")) {
        if (reads.incrementAndGet() == 1 || persistentFailure) {
          exchange.sendResponseHeaders(503, -1);
          exchange.close();
          return;
        }
        body = source;
      } else if (path.endsWith("/content")) body = render;
      else body = inputs.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type",
          path.endsWith("/content") ? "image/png" : "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    Path counter = directory.resolve("model-calls");
    Path executable = directory.resolve("codex-fixture.cjs");
    Files.writeString(executable, """
        #!/usr/bin/env node
        const fs = require('node:fs');
        const args = process.argv.slice(2);
        const prompt = fs.readFileSync(0, 'utf8');
        if (!prompt.includes('creativeEvidenceChecks')) process.exit(2);
        const images = args.flatMap((value, i) => value === '--image' ? [args[i + 1]] : []);
        if (images.length !== 3 || images.some(file => !fs.existsSync(file))) process.exit(3);
        fs.appendFileSync(%s, '1');
        fs.writeFileSync(args[args.indexOf('--output-last-message') + 1], JSON.stringify({
          decision: 'APPROVED', commercialRationale: 'Provas locais verificadas',
          evidence: ['Simulação técnica sem efeito comercial'], requiredChanges: [],
          renderedAssetAudit: [{artifactId: 1060, sha256: '%s', assessment: 'Fixture íntegra'}]
        }));
        console.log(JSON.stringify({type:'turn.completed',usage:{
          input_tokens:20,cached_input_tokens:0,output_tokens:5}}));
        """.formatted(json.writeValueAsString(counter.toString()), sha(render)));
    executable.toFile().setExecutable(true);
    var properties = new MetaAdApproverProperties();
    properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
    var consumer = new CommercialBpmTaskConsumer(properties, executable.toString(),
        "gpt-6.1-sol", directory.toString(), "", json);
    try {
      if (persistentFailure) {
        assertThatThrownBy(() -> consumer.execute(task(778))).isInstanceOf(java.io.IOException.class);
        assertThat(counter).doesNotExist();
        assertThat(audits).hasValue(0);
        assertThat(reads).hasValue(3);
      } else {
        var execution = consumer.execute(task(778));
        assertThat(execution.result().path("decision").asText()).isEqualTo("APPROVED");
        assertThat(Files.readString(counter)).isEqualTo("1");
        assertThat(audits).hasValue(1);
        assertThat(reads).hasValue(2);
        assertThat(execution.usage().inputTokens()).isEqualTo(20L);
      }
    } finally {
      server.stop(0);
    }
  }

  /** Monta uma prova sintética com a mesma captura referida como origem e destino. */
  private String inputs(byte[] render, byte[] source) throws Exception {
    return """
        [{"sourceTaskId":770,"prototypeVersion":"local-v3",
          "evidence":{"id":1060,"evidenceType":"CREATIVE_RENDER","sha256":"%s"},
          "creativeProof":{
            "source":{"sourceTaskId":765,"prototypeVersion":"local-v3",
              "evidence":{"id":1047,"sha256":"%s"}},
            "crop":{"x":0,"y":0,"width":200,"height":250},
            "currentDestination":{"taskId":765,
              "technicalProof":{"prototypeVersion":"local-v3","decision":"APPROVED"},
              "captures":[{"sourceTaskId":765,"prototypeVersion":"local-v3",
                "evidence":{"id":1047,"sha256":"%s"}}]}}}]
        """.formatted(sha(render), sha(source), sha(source));
  }

  /** Cria contexto isolado sem custos, efeitos comerciais ou consulta a produto real. */
  private HashMap<String, Object> task(long id) {
    return new HashMap<>(Map.of("taskId", id, "processCode", "creative-production-approval"));
  }

  /** Usa somente a rota canônica de leitura, mantendo a reserva sob controle do backend. */
  private String endpoint(long id) {
    return "/api/internal/agent-tasks/meta-ad-approver/stage-executions/" + id + "/visual-inputs";
  }

  /** Gera pixels sintéticos para testar transporte e identidade sem provedor externo. */
  private byte[] png(int width, int height) throws Exception {
    var output = new ByteArrayOutputStream();
    javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(width, height,
        java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
    return output.toByteArray();
  }

  /** Calcula o hash esperado de cada prova local. */
  private String sha(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
