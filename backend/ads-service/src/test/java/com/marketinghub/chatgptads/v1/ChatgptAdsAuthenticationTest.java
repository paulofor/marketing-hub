package com.marketinghub.chatgptads.v1;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.chatgptads.v1.controller.ChatgptAdsController;
import com.marketinghub.chatgptads.v1.service.ChatgptAdsRequestAuth;
import com.marketinghub.chatgptads.v1.service.ChatgptAdsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/** Comprova que a fila exige assinatura de corpo e rejeita alteração, replay e credencial vazia. */
class ChatgptAdsAuthenticationTest {
  private static final String KEY = "synthetic-internal-secret-at-least-32-chars";
  private static final String PATH =
      "/api/internal/chatgpt-ads/v1/delivery/stage-executions/pending";

  /** Produz assinatura sintética independente para conferir o contrato de transporte. */
  private HttpHeaders headers(String body, long timestamp) throws Exception {
    String time = Long.toString(timestamp), nonce = UUID.randomUUID().toString();
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String hash =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
    String signature =
        HexFormat.of()
            .formatHex(
                mac.doFinal(
                    ("POST\n" + PATH + "\n" + time + "\n" + nonce + "\n" + hash)
                        .getBytes(StandardCharsets.UTF_8)));
    var h = new HttpHeaders();
    h.add("X-Mkt-Ads-Time", time);
    h.add("X-Mkt-Ads-Nonce", nonce);
    h.add("X-Mkt-Ads-Signature", signature);
    return h;
  }

  /** O endpoint real aceita assinatura válida sem transmitir segredo em Authorization. */
  @Test
  void realControllerRequiresValidSignature() throws Exception {
    var service = mock(ChatgptAdsService.class);
    var auth = new ChatgptAdsRequestAuth(KEY);
    var mvc =
        MockMvcBuilders.standaloneSetup(new ChatgptAdsController(service, auth, new ObjectMapper()))
            .build();
    mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post(PATH)
                .headers(headers("{}", Instant.now().getEpochSecond()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isNoContent());
    verify(service, times(1)).pending();
  }

  /** A resposta real contém assinatura do corpo em UTF-8 vinculada ao request. */
  @Test
  void realControllerSignsReturnedCommand() throws Exception {
    var service = mock(ChatgptAdsService.class);
    var auth = new ChatgptAdsRequestAuth(KEY);
    var mapper = new ObjectMapper();
    when(service.pending())
        .thenReturn(
            new com.marketinghub.chatgptads.v1.service.managecampaign.ChatgptAdsContracts.Work(
                71L,
                1L,
                "lease",
                "CAMPAIGN",
                "POST",
                "/campaigns",
                mapper.createObjectNode().put("name", "Divulgação útil"),
                "idempotency",
                "test_account"));
    var mvc =
        MockMvcBuilders.standaloneSetup(new ChatgptAdsController(service, auth, mapper)).build();
    var headers = headers("{}", Instant.now().getEpochSecond());
    var response =
        mvc.perform(
                post(PATH).headers(headers).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    String hash =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256").digest(response.getContentAsByteArray()));
    var mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    assertEquals(
        HexFormat.of()
            .formatHex(
                mac.doFinal(
                    ("RESPONSE\n200\n" + headers.getFirst("X-Mkt-Ads-Nonce") + "\n" + hash)
                        .getBytes(StandardCharsets.UTF_8))),
        response.getHeader("X-Mkt-Ads-Response"));
  }

  /** Uma assinatura capturada não permite outro corpo nem nova reserva. */
  @Test
  void rejectsReplayAndModifiedBody() throws Exception {
    var auth = new ChatgptAdsRequestAuth(KEY);
    var headers = headers("{}", Instant.now().getEpochSecond());
    assertThrows(
        ResponseStatusException.class, () -> auth.verify(PATH, "{\"changed\":true}", headers));
    auth.verify(PATH, "{}", headers);
    assertThrows(ResponseStatusException.class, () -> auth.verify(PATH, "{}", headers));
  }

  /** Relógio tolera pequena diferença operacional e recusa requests antigos. */
  @Test
  void rejectsExpiredAndMissingSecret() throws Exception {
    var old = headers("{}", Instant.now().minusSeconds(601).getEpochSecond());
    assertThrows(
        ResponseStatusException.class,
        () -> new ChatgptAdsRequestAuth(KEY).verify(PATH, "{}", old));
    assertFalse(new ChatgptAdsRequestAuth("").configured());
  }
}
