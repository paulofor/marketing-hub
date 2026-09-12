package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Responsabilidade: proteger a revisão de Têmis contra troca de peça e ausência de pixels. */
class CreativeReviewImagesTest {
  /** Usa o PNG persistido, exige o mesmo hash no parecer e remove o arquivo após a execução. */
  @Test
  void receivesExactPixelsAndRejectsDifferentArtifact() throws Exception {
    var mapper = new ObjectMapper();
    var pixels =
        new java.awt.image.BufferedImage(1080, 1350, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var output = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(pixels, "png", output);
    byte[] png = output.toByteArray();
    String hash =
        HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(png));
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          boolean content = exchange.getRequestURI().getPath().endsWith("/content");
          byte[] response =
              content
                  ? png
                  : ("[{\"sourceTaskId\":910403,\"prototypeVersion\":\"sandbox-v12\",\"evidence\":{\"id\":910130,\"evidenceType\":\"CREATIVE_RENDER\",\"sha256\":\""
                          + hash
                          + "\"}}]")
                      .getBytes(StandardCharsets.UTF_8);
          exchange
              .getResponseHeaders()
              .set("Content-Type", content ? "image/png" : "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    Path file;
    try {
      var task =
          new HashMap<String, Object>(
              Map.of("taskId", 910405L, "processCode", "creative-production-approval"));
      try (var images =
          CreativeReviewImages.load(
              RestClient.builder()
                  .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                  .build(),
              task)) {
        var command = images.attach(List.of("codex", "exec"));
        assertThat(command).contains("--image");
        file = Path.of(command.getLast());
        assertThat(Files.readAllBytes(file)).isEqualTo(png);
        var result =
            mapper.readTree(
                "{\"renderedAssetAudit\":[{\"artifactId\":910130,\"sha256\":\""
                    + hash
                    + "\",\"assessment\":\"Promessa coerente com a demonstração privada\"}]}");
        CreativeReviewImages.validate(result, mapper.valueToTree(task.get("creativeVisualInputs")));
        assertThatThrownBy(
                () ->
                    CreativeReviewImages.validate(
                        mapper.readTree(result.toString().replace("910130", "999999")),
                        mapper.valueToTree(task.get("creativeVisualInputs"))))
            .hasMessageContaining("peças exatas");
      }
      assertThat(file).doesNotExist();
    } finally {
      server.stop(0);
    }
  }
}
