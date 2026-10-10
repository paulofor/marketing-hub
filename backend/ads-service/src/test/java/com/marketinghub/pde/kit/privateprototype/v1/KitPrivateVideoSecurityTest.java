package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.pde.kit.privateprototype.v1.controller.KitPrivateController;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitPrivateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: permitir o CDN canônico dos vídeos sem relaxar scripts ou acesso privado. */
class KitPrivateVideoSecurityTest {
  /**
   * Expõe somente a origem do armazenamento e mantém script, conexão, cache e referência restritos.
   */
  @Test
  void permitsOnlyCanonicalMediaOrigin() {
    var service = mock(KitPrivateService.class);
    var page =
        new KitPrivateController(
                service, "local-only", "local-worker", "https://cdn.fixture.invalid/objects")
            .page();
    assertThat(page.getHeaders().getFirst("Content-Security-Policy"))
        .contains("media-src 'self' https://cdn.fixture.invalid;")
        .contains("script-src 'self';", "connect-src 'self';", "frame-ancestors 'none';")
        .doesNotContain("unsafe-eval", "unsafe-inline", "https:;", "/objects");
    assertThat(page.getHeaders().getCacheControl()).isEqualTo("no-store");
    assertThat(page.getHeaders().getFirst("Referrer-Policy")).isEqualTo("no-referrer");
    verifyNoInteractions(service);
  }

  /** Recusa origem insegura, credencial, query, fragmento ou diretiva injetada na configuração. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "http://cdn.fixture.invalid",
        "https://operator:secret@cdn.fixture.invalid",
        "https://cdn.fixture.invalid?key=invalid",
        "https://cdn.fixture.invalid#fragment",
        "https://cdn.fixture.invalid; script-src *",
        "https://cdn.fixture.invalid:70000"
      })
  void preservesRestrictedCspWithInvalidOrigin(String value) {
    var page =
        new KitPrivateController(mock(KitPrivateService.class), "qa", "worker", value).page();
    assertThat(page.getHeaders().getFirst("Content-Security-Policy"))
        .contains("media-src 'self';", "script-src 'self';")
        .doesNotContain("media-src 'self' https", "unsafe-eval", "unsafe-inline");
  }
}
