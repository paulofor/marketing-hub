package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.kit.privateprototype.v1.controller.KitPrivateController;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitPrivateService;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: provar identidade, integridade e ausência de efeitos das amostras públicas. */
class KitPreviewSamplesTest {
  /**
   * Confere os seis PNG e sua origem sem consultar sessões, compor pacotes ou cobrar provedores.
   */
  @Test
  void servesOnlyVerifiedVersionedSamplesWithoutSessionEffects() throws Exception {
    var service = mock(KitPrivateService.class);
    var controller = new KitPrivateController(service, "fixture", "fixture", "");
    var manifest =
        new ObjectMapper()
            .readTree(
                new ClassPathResource("private-kit-v1/samples/manifest.json").getInputStream());
    assertThat(manifest.path("samples").size()).isEqualTo(6);
    for (var sample : manifest.path("samples")) {
      var response = controller.sample(sample.path("profileCode").asText());
      byte[] bytes = response.getBody();
      assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
          .isEqualTo(sample.path("sha256").asText());
      var image = ImageIO.read(new ByteArrayInputStream(bytes));
      assertThat(image.getWidth()).isEqualTo(1080);
      assertThat(image.getHeight()).isEqualTo(1080);
      assertThat(sample.path("sourceSha256").asText()).matches("[a-f0-9]{64}");
      assertThat(sample.path("licenseUrl").asText()).isEqualTo("https://www.pexels.com/license/");
      assertThat(response.getHeaders().getCacheControl()).contains("no-store");
    }
    for (String invalid : new String[] {"barber-v1", "beauty-v2", "../manifest", "other-v2"})
      assertThatThrownBy(() -> controller.sample(invalid))
          .isInstanceOf(ResponseStatusException.class);
    verifyNoInteractions(service);
  }
}
