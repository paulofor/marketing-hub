package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.client.VideoAssetClient;
import com.marketinghub.videomanagement.client.payload.JobCompletionPayload;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.service.VideoAssetUploader;
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
    var uploads = new AtomicInteger();
    Path stored = output.resolve("uploaded");
    Files.createDirectories(stored);
    server.createContext("/media/", exchange -> {
      Path file = stored.resolve(Path.of(exchange.getRequestURI().getPath()).getFileName().toString());
      byte[] bytes = Files.readAllBytes(file);
      exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
    });
    server.createContext("/internal/video/assets", exchange -> {
      try {
        byte[] body = exchange.getRequestBody().readAllBytes();
        String raw = new String(body, java.nio.charset.StandardCharsets.ISO_8859_1);
        var match = java.util.regex.Pattern.compile("name=\"file\"; filename=\"([^\"]+)\"").matcher(raw);
        if (!match.find()) throw new IllegalStateException("Upload sem arquivo multipart");
        String name = uploads.incrementAndGet() + "-" + match.group(1);
        int begin = raw.indexOf("\r\n\r\n", match.end()) + 4;
        int end = raw.indexOf("\r\n--", begin);
        Files.write(stored.resolve(name), java.util.Arrays.copyOfRange(body, begin, end));
        byte[] response = mapper.createObjectNode().put("id", uploads.get())
            .put("type", "VIDEO").put("url", "http://127.0.0.1:" + server.getAddress().getPort() + "/media/" + name)
            .toString().getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200,response.length); exchange.getResponseBody().write(response);
      } catch (Exception ex) { throw new RuntimeException(ex); }
      finally { exchange.close(); }
    });
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
          .put("tenantId",metadata.path("tenantId").asText()).put("providerName","MUSA_POST_PRODUCTION").put("metadataJson", metadata.toString());
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
      var uploadsResult = new VideoAssetUploader(new VideoAssetClient(WebClient.builder(), properties), mapper, properties)
          .uploadAssets(mapper.treeToValue(job,SalesVideoJob.class), result);
      if (uploadsResult.streamPlaybackUrl() == null || !uploadsResult.streamPlaybackUrl().endsWith(".m3u8"))
        throw new IllegalStateException("Uploader final não entregou HLS");
      Path manifest;
      try (var paths = Files.list(stored)) { manifest = paths.filter(path -> path.toString().endsWith(".m3u8")).findFirst().orElseThrow(); }
      String originalManifest = Files.readString(manifest);
      for (String line : originalManifest.lines().filter(line -> !line.startsWith("#") && !line.isBlank()).toList()) {
        Path segment = stored.resolve(URI.create(line).getPath().substring("/media/".length()));
        if (Files.size(segment) < 188 || Files.readAllBytes(segment)[0] != 0x47) throw new IllegalStateException("Segmento inválido");
      }
      Files.writeString(stored.resolve("playback.m3u8"), originalManifest.replace(base + "/media/", ""));
      run("ffprobe", "-v", "error", "-show_entries", "format=duration", stored.resolve("playback.m3u8").toString());
      var completed = new java.util.LinkedHashMap<String,Object>(result.metadata());
      completed.putAll(uploadsResult.deliveryMetadata());
      var callback = new JobCompletionPayload(SalesVideoStatus.VIDEO_READY, uploadsResult.videoAssetId(),
          uploadsResult.posterAssetId(), uploadsResult.captionAssetId(), result.providerJobId(), mapper.writeValueAsString(completed),
          null, "Fixture segregada", "Fixture segregada", uploadsResult.streamPlaybackUrl());
      Files.writeString(output.resolve("completion-callback.json"), mapper.writeValueAsString(callback));
      Path sourceVideo;
      Path sourceCaption;
      try (var paths = Files.list(stored)) { sourceVideo = paths.filter(path -> path.toString().endsWith(".mp4")).findFirst().orElseThrow(); }
      try (var paths = Files.list(stored)) { sourceCaption = paths.filter(path -> path.toString().endsWith(".vtt")).findFirst().orElseThrow(); }
      ObjectNode recoveryMetadata = metadata.deepCopy();
      recoveryMetadata.setAll((ObjectNode) mapper.valueToTree(result.metadata()));
      recoveryMetadata.put("deliveryOnly",true);
      recoveryMetadata.putObject("delivery_source").put("contractVersion","VIDEO_FINAL_REUSE_V1").put("jobId",91009)
          .put("videoUrl",base+"/media/"+sourceVideo.getFileName()).put("captionUrl",base+"/media/"+sourceCaption.getFileName())
          .put("videoSha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(result.videoFile().content())))
          .put("captionSha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(result.captionFile().content())));
      var recoveryJob = mapper.treeToValue(job.deepCopy().put("id",91010).put("metadataJson",recoveryMetadata.toString()),SalesVideoJob.class);
      var recovered = provider.render(recoveryJob,mapper.treeToValue(profile,SalesVideoProfile.class),(p,s,m)->{});
      if (!java.util.Arrays.equals(recovered.videoFile().content(),result.videoFile().content()) || calls.get()!=2
          || !java.math.BigDecimal.ZERO.equals(recovered.metadata().get("cost_usd"))) throw new IllegalStateException("Recuperação alterou bytes ou chamou TTS");
      var recoveredUpload = new VideoAssetUploader(new VideoAssetClient(WebClient.builder(),properties),mapper,properties).uploadAssets(recoveryJob,recovered);
      if (recoveredUpload.streamPlaybackUrl()==null) throw new IllegalStateException("Recuperação não entregou HLS");
      Files.writeString(output.resolve("reuse-audit.json"),mapper.writeValueAsString(recovered.metadata()));
      System.out.println("PASS: backend exportado → prova/hash → acabamento → TTS simulado → upload HTTP de MP4/VTT/segmentos/manifesto → callback HLS. Tom de teste; não é avaliação da voz real.");
    } finally { server.stop(0); }
  }

  /** Executa somente ferramenta local conhecida e interrompe qualquer falha da fixture. */
  private static void run(String... command) throws Exception {
    var process = new ProcessBuilder(command).inheritIO().start();
    if (process.waitFor() != 0) throw new IllegalStateException("Ferramenta local falhou: " + command[0]);
  }
}
