package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.kit.privateprototype.v1.controller.KitPrivateController;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitPrivateService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: reproduzir o limite de JSON e provar o transporte binário autenticado. */
class KitPrivateBinaryCallbackTest {
  /** O pacote fotográfico excede uma string JSON padrão, mas atravessa o callback binário. */
  @Test
  void acceptsLargeBinaryWithoutIncreasingGlobalJsonLimit() throws Exception {
    int size = 21_000_000;
    assertThatThrownBy(
            () -> new ObjectMapper().readTree("{\"zipBase64\":\"" + "A".repeat(size) + "\"}"))
        .isInstanceOf(com.fasterxml.jackson.core.exc.StreamConstraintsException.class);
    var service = mock(KitPrivateService.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new KitPrivateController(
                    service, "local-agent", "local-worker", "https://media.invalid"))
            .build();
    var file =
        new MockMultipartFile("artifact", "kit.zip", "application/octet-stream", new byte[size]);
    var metadata =
        new MockMultipartFile(
            "metadata",
            "",
            "application/json",
            ("{\"claimKey\":\""
                    + UUID.randomUUID()
                    + "\",\"zipSha256\":\""
                    + "a".repeat(64)
                    + "\",\"providerCalls\":0}")
                .getBytes(StandardCharsets.UTF_8));
    mvc.perform(
            multipart("/api/pde/kit/private/v1/stage-executions/local/result")
                .file(file)
                .file(metadata)
                .header("X-Payments-Auth", "local-worker"))
        .andExpect(status().isOk());
    verify(service).completeBinary(eq("local"), any(), argThat(bytes -> bytes.length == size));
    mvc.perform(
            multipart("/api/pde/kit/private/v1/stage-executions/other/result")
                .file(file)
                .file(metadata)
                .header("X-Payments-Auth", "wrong"))
        .andExpect(status().isUnauthorized());
    verifyNoMoreInteractions(service);
  }
}
