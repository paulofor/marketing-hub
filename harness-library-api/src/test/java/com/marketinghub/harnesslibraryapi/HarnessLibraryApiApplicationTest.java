package com.marketinghub.harnesslibraryapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Confirma que o gateway inicia apenas com os contratos e secrets mínimos. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "harness-library.api-key=public-api-key-with-more-than-32-characters",
      "harness-library.internal-signing-key=internal-signing-key-with-more-than-32-characters",
      "harness-library.backend-base-url=http://127.0.0.1:65534"
    })
class HarnessLibraryApiApplicationTest {

  /** Verifica criação do contexto sem banco ou dependência externa no bootstrap. */
  @Test
  void shouldLoadContextWithoutDatabase() {}
}
