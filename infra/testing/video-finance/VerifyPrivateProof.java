package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.reactive.function.client.WebClient;

/** Homologa o contrato exportado no acabamento real, com HTTP/TTS locais e pixels de fixture. */
class VerifyPrivateProof {
  /** Executa prova, áudio, legendas e auditoria sem acessar provedor ou registrar métrica comercial. */
  public static void main(String[] args) throws Exception {
    Path output = Path.of(args[1]);
    Files.createDirectories(output);
    var mapper = new ObjectMapper().findAndRegisterModules();
    ObjectNode metadata = (ObjectNode) mapper.readTree(Files.readString(Path.of(args[0]))).path("metadata");
    Path source = output.resolve("source.mp4");
    Path proofFile = output.resolve("proof.png");
    Path voiceFile = output.resolve("synthetic-test-tone.mp3");
    run("ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "color=c=blue:s=360x640:r=15:d=15",
        "-c:v", "libx264", "-pix_fmt", "yuv420p", source.toString());
    run("ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "color=c=gold:s=720x900", "-frames:v", "1", proofFile.toString());
    run("ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "sine=frequency=440:duration=1",
        "-ar", "44100", "-ac", "1", voiceFile.toString());
    byte[] proofBytes = Files.readAllBytes(proofFile);
    var proof = (ObjectNode) metadata.at("/post_production/product_proof");
    if (!"PDE_PRIVATE_VIDEO_PROOF_V1".equals(proof.path("contractVersion").asText())) {
      throw new IllegalStateException("Backend não exportou o contrato da prova privada.");
    }
    proof.put("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(proofBytes)));
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var proofs = new AtomicInteger();
    var calls = new AtomicInteger();
    server.createContext(proof.path("contentPath").asText(), exchange -> {
      if (!metadata.path("tenantId").asText().equals(exchange.getRequestHeaders().getFirst("X-Tenant-ID"))) {
        exchange.sendResponseHeaders(400, -1); exchange.close(); return;
      }
      proofs.incrementAndGet();
      exchange.getResponseHeaders().set("Content-Type", "image/png");
      exchange.sendResponseHeaders(200, proofBytes.length); exchange.getResponseBody().write(proofBytes); exchange.close();
    });
    server.createContext("/source.mp4", exchange -> {
      byte[] bytes = Files.readAllBytes(source);
      exchange.getResponseHeaders().set("Content-Type", "video/mp4");
      exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
    });
    server.createContext("/v1/audio/speech", exchange -> {
      var request = mapper.readTree(exchange.getRequestBody());
      if (request.path("input").asText().isBlank() || !"marin".equals(request.path("voice").asText())) {
        exchange.sendResponseHeaders(400,-1); exchange.close(); return;
      }
      int call = calls.incrementAndGet();
      Files.writeString(output.resolve("tts-request-" + call + ".json"), request.toPrettyString());
      byte[] bytes = Files.readAllBytes(voiceFile);
      exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
      exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
    });
    server.start();
    try {
      String base = "http://127.0.0.1:" + server.getAddress().getPort();
      var properties = new VideoManagementProperties();
      properties.setBackendBaseUrl(URI.create(base));
      properties.getProviders().getPostProduction().setOpenAiTtsEnabled(true);
      properties.getProviders().getPostProduction().setOpenAiApiKey("fixture-only");
      properties.getProviders().getPostProduction().setOpenAiBaseUrl(URI.create(base + "/v1"));
      metadata.put("sourceVideoUrl", base + "/source.mp4");
      metadata.put("captionText", metadata.at("/premiumFinalization/captionText").asText());
      metadata.put("voiceOverScript", metadata.at("/premiumFinalization/voiceOverScript").asText());
      var job = mapper.createObjectNode().put("id",91009).put("profileId",91001).put("jobType","POST_PRODUCTION")
          .put("providerName","MUSA_POST_PRODUCTION").put("metadataJson", metadata.toString());
      var profile = mapper.createObjectNode().put("id",91001).put("targetDurationSeconds",15);
      var provider = new PostProductionVideoProvider(properties, mapper, WebClient.builder());
      new PdeProductProofOverlay(properties, WebClient.builder()).verify(metadata,91009L);
      var result = provider.render(mapper.treeToValue(job,SalesVideoJob.class), mapper.treeToValue(profile,SalesVideoProfile.class),
          (percent,status,message) -> {});
      var report = mapper.valueToTree(result.metadata());
      if (!"APPLIED".equals(report.at("/product_reference_overlay/status").asText())
          || !"APPROVED".equals(report.at("/caption_narration_sync/timing_status").asText())
          || !report.at("/captions/burned_in").asBoolean() || !report.path("has_audio").asBoolean()
          || calls.get() != 2 || proofs.get() != 2) throw new IllegalStateException("Contrato integrado incompleto: " + report);
      Files.write(output.resolve("final-fixture.mp4"),result.videoFile().content());
      Files.write(output.resolve("final-fixture.vtt"),result.captionFile().content());
      Files.writeString(output.resolve("worker-result.json"),report.toPrettyString());
      Files.writeString(output.resolve("context.json"),metadata.toPrettyString());
      System.out.println("PASS: backend exportado → prova/hash → acabamento FFmpeg → TTS HTTP simulado → legenda/VTT → auditoria. Tom de teste; não é avaliação da voz real.");
    } finally { server.stop(0); }
  }

  /** Executa somente ferramenta local conhecida e interrompe qualquer falha da fixture. */
  private static void run(String... command) throws Exception {
    var process = new ProcessBuilder(command).inheritIO().start();
    if (process.waitFor() != 0) throw new IllegalStateException("Ferramenta local falhou: " + command[0]);
  }
}
