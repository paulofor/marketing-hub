package com.marketinghub.pde.kit.privateprototype.v1;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleVideoBinding;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitArtifactContract;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: fornecer recibos e mídias sintéticos apenas à topologia local já existente. */
@TestConfiguration
@Profile("private-kit-local")
@Import(KitPrivateVideoLocalFixture.Media.class)
class KitPrivateVideoLocalFixture {
  /**
   * Simula apenas o recibo posterior ao aceite humano, sem gravar aprovação em ambiente externo.
   */
  @Bean
  LearningCycleVideoBinding videoBinding(LearningSalesCycleRepository cycles, ObjectMapper json)
      throws IOException {
    var binding = mock(LearningCycleVideoBinding.class);
    String path = System.getenv("KIT_LOCAL_VIDEO_FIXTURE");
    if (path == null) return binding;
    ObjectNode source = (ObjectNode) json.readTree(Files.readString(Path.of(path)));
    when(binding.presentation(anyLong(), anyString()))
        .thenAnswer(
            invocation -> {
              var cycle = cycles.findById(invocation.getArgument(0, Long.class)).orElseThrow();
              ObjectNode proof = source.deepCopy();
              proof.put("productId", cycle.getProductId());
              proof.put("cycleId", cycle.getId());
              proof.put("experimentId", cycle.getExperimentId());
              proof.put("productVersion", cycle.getProductVersion());
              proof.put(
                  "integrationFingerprint",
                  KitArtifactContract.hash(proof.toString().getBytes(StandardCharsets.UTF_8)));
              return Optional.of(proof);
            });
    return binding;
  }

  /** Responsabilidade: servir somente os dois MP4s sintéticos pelo backend local de homologação. */
  @RestController
  @Profile("private-kit-local")
  static class Media {
    /** Entrega bytes locais conhecidos; nenhuma credencial ou conteúdo produtivo é consultado. */
    @GetMapping("/__kit-test-media/{role}.mp4")
    ResponseEntity<byte[]> media(@PathVariable String role) throws IOException {
      String directory = System.getenv("KIT_LOCAL_VIDEO_MEDIA_DIR");
      if (directory == null || !java.util.Set.of("ad", "hero").contains(role))
        return ResponseEntity.notFound().build();
      return ResponseEntity.ok()
          .header("Content-Type", "video/mp4")
          .body(Files.readAllBytes(Path.of(directory).resolve(role + ".mp4")));
    }
  }
}
