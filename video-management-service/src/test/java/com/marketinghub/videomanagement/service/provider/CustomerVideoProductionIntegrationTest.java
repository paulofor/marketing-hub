package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.*;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import okhttp3.mockwebserver.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.reactive.function.client.WebClient;

/** Homologa a edição real com fontes locais, prova recortada e zero chamada paga. */
class CustomerVideoProductionIntegrationTest {
  /** Mantém os planos em movimento e uma prova legível sem narrar texto do sistema. */
  @Test
  @EnabledIfSystemProperty(named="video.customer.media-dir", matches=".+")
  void rendersCustomerCandidateWithoutExternalProviders() throws Exception {
    Path dir = Path.of(System.getProperty("video.customer.media-dir"));
    byte[] source = Files.readAllBytes(dir.resolve("motion.mp4"));
    byte[] proof = Files.readAllBytes(dir.resolve("proof.png"));
    var json = new ObjectMapper();
    var metadata = json.createObjectNode().put("tenantId","default").put("videoProjectId",91013)
        .put("productId",91010).put("experimentId",91102).put("targetDurationSeconds",20);
    metadata.put("captionText","Quer organizar seus cuidados com a pele? | Mira reúne as instruções dos produtos que você já tem. | Veja um exemplo com fontes. | Conheça o resultado. Decida se quer continuar.");
    var cuts=metadata.putArray("cut_plan");
    for(String role:java.util.List.of("HOOK_DOR","MECANISMO","RESULTADO","PROVA","CTA"))
      cuts.addObject().put("role",role).put("duration_seconds",4);
    var p=metadata.putObject("post_production").putObject("product_proof");
    p.put("tenantId","default").put("projectId",91013).put("productId",91010).put("experimentId",91102)
        .put("contractVersion","PDE_PRIVATE_VIDEO_PROOF_V1").put("commercialEvidenceClaimed",false)
        .put("contentPath","/api/sales-videos/projects/91013/product-proof")
        .put("sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(proof)));
    p.putArray("crop").add(90).add(2025).add(990).add(1200);
    try(var server=new MockWebServer()) {
      server.setDispatcher(new Dispatcher() {
        /** Autoriza somente as duas fontes locais; qualquer síntese paga falha nesta homologação. */
        @Override public MockResponse dispatch(RecordedRequest request) {
          if("/motion.mp4".equals(request.getPath())) return new MockResponse().setBody(new Buffer().write(source));
          if(request.getPath().endsWith("/product-proof")) return new MockResponse().setBody(new Buffer().write(proof));
          return new MockResponse().setResponseCode(503);
        }
      });
      server.start();
      metadata.put("sourceVideoUrl",server.url("/motion.mp4").toString());
      var settings = new VideoManagementProperties();settings.setBackendBaseUrl(server.url("/").uri());
      var job=new SalesVideoJob(91099L,91066L,91067L,"default",SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
          "MUSA_POST_PRODUCTION",null,SalesVideoJobType.POST_PRODUCTION,SalesVideoStatus.VIDEO_REQUESTED,
          1,null,91098L,null,0,null,null,"fixture@sandbox.local",Instant.now(),null,null,null,
          null,null,null,metadata.toString(),Instant.now(),Instant.now());
      var output=new PostProductionVideoProvider(settings,json,WebClient.builder()).render(job,null,(percent,status,message)->{});
      assertThat(server.getRequestCount()).isEqualTo(2);
      assertThat(output.metadata()).containsEntry("customerQualityContract","CUSTOMER_VIDEO_QUALITY_V1");
      assertThat(output.metadata().get("product_reference_overlay").toString()).contains("12.000,16.000");
      assertThat(output.captionFile().content()).asString().doesNotContain("acesso privado","Demonstração interna");
      Files.createDirectories(Path.of("target"));
      Files.write(Path.of("target/customer-video-candidate-local.mp4"),output.videoFile().content());
      Files.writeString(Path.of("target/customer-video-candidate-local.json"),json.writeValueAsString(output.metadata()));
    }
  }
  /** Reproduz entrada, resultado e continuidade com fontes reais locais e voz simulada sem cobrança. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings={"ad","hero"})
  @EnabledIfSystemProperty(named="video.sequence.media-dir", matches=".+")
  void rendersNarratedProofSequenceWithLocalDoubles(String role) throws Exception {
    var dir = Path.of(System.getProperty("video.sequence.media-dir")).resolve(role);
    var json = new ObjectMapper();
    var metadata = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(Files.readString(dir.resolve("metadata.json")));
    var captions = json.readTree(Files.readString(dir.resolve("captions.json")));
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    var unknown = new java.util.concurrent.atomic.AtomicInteger();
    try(var server = new MockWebServer()) {
      server.setDispatcher(new Dispatcher() {
        /** Aceita somente arquivos da fixture e síntese simulada, sem saída para serviços externos. */
        @Override public MockResponse dispatch(RecordedRequest request) {
          try {
            String path = request.getPath(); Path file = null;
            if ("/motion.mp4".equals(path)) file = dir.resolve("motion.mp4");
            else if (path.contains("/product-proof/627/")) file = dir.resolve(path.substring(path.lastIndexOf('/')+1) + ".png");
            else if (path.endsWith("/audio/speech")) {
              var body = json.readTree(request.getBody().readUtf8());
              for (int i=0;i<captions.size();i++) if(captions.get(i).asText().equals(body.path("input").asText())) file = dir.resolve("tone-"+i+".mp3");
              calls.incrementAndGet();
            }
            if (file == null) { unknown.incrementAndGet(); return new MockResponse().setResponseCode(503); }
            return new MockResponse().setBody(new Buffer().write(Files.readAllBytes(file)));
          } catch (java.io.IOException ex) {
            org.slf4j.LoggerFactory.getLogger(CustomerVideoProductionIntegrationTest.class).error("Falha na fixture audiovisual; role={} path={}", role, request.getPath(), ex);
            throw new java.io.UncheckedIOException(ex);
          }
        }
      }); server.start();
      metadata.put("sourceVideoUrl",server.url("/motion.mp4").toString());
      var settings = new VideoManagementProperties();settings.setBackendBaseUrl(server.url("/").uri());
      settings.getProviders().getPostProduction().setOpenAiTtsEnabled(true);
      settings.getProviders().getPostProduction().setOpenAiApiKey("local-fixture-never-billable");
      settings.getProviders().getPostProduction().setOpenAiBaseUrl(server.url("/").uri());
      var job = new SalesVideoJob(91099L,91066L,91067L,"default",SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
          "MUSA_POST_PRODUCTION",null,SalesVideoJobType.POST_PRODUCTION,SalesVideoStatus.VIDEO_REQUESTED,
          1,null,91098L,null,0,null,null,"fixture@sandbox.local",Instant.now(),null,null,null,
          null,null,null,metadata.toString(),Instant.now(),Instant.now());
      var output = new PostProductionVideoProvider(settings,json,WebClient.builder()).render(job,
          json.readValue("{\"id\":91066}",SalesVideoProfile.class),(percent,status,message)->{});
      assertThat(unknown.get()).isZero(); assertThat(calls.get()).isEqualTo(captions.size());
      var audit = json.valueToTree(output.metadata()).path("product_reference_overlay");
      assertThat(audit.path("compositionMode").asText()).isEqualTo("NARRATED_PROOF_SEQUENCE_V1");
      assertThat(audit.path("shots").size()).isEqualTo(role.equals("ad")?3:4);
      assertThat(output.captionFile().content()).asString().doesNotContain("acesso privado","prova interna");
      Files.createDirectories(Path.of("target"));
      Files.write(Path.of("target/customer-sequence-"+role+"-local.mp4"),output.videoFile().content());
      Files.writeString(Path.of("target/customer-sequence-"+role+"-local.json"),json.writeValueAsString(output.metadata()));
    }
  }

}
