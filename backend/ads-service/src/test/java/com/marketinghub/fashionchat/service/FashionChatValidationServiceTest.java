package com.marketinghub.fashionchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.fashionchat.service.login.StartFashionChatLoginResponse;
import com.marketinghub.fashionchat.service.status.FashionChatValidationStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/** Valida a integração administrativa do backend com o serviço Chat Moda. */
class FashionChatValidationServiceTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate = new RestTemplate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
  private final FashionChatValidationService service =
      new FashionChatValidationService(restTemplate, objectMapper, "http://fashion-chat.test");

  /**
   * Deve informar login necessário quando o health ready está degradado e a conta não está
   * autenticada.
   */
  @Test
  void statusReturnsNotAuthenticatedWhenCodexSessionIsMissing() {
    server
        .expect(requestTo("http://fashion-chat.test/health/ready"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withServiceUnavailable()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"degraded\"}"));
    server
        .expect(requestTo("http://fashion-chat.test/codex-app-server/account/read"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"code\":\"CODEX_NOT_AUTHENTICATED\",\"authenticated\":false}",
                MediaType.APPLICATION_JSON));

    FashionChatValidationStatusResponse response = service.status();

    assertThat(response.ready()).isFalse();
    assertThat(response.readyHttpStatus()).isEqualTo(503);
    assertThat(response.accountStatus()).isEqualTo("NOT_AUTHENTICATED");
    assertThat(response.authenticated()).isFalse();
    assertThat(response.connected()).isNull();
    assertThat(response.executable()).isNull();
    server.verify();
  }

  /**
   * Deve reconhecer sessão conectada e executável como autenticada no contrato real do Chat Moda.
   */
  @Test
  void statusReturnsAuthenticatedWhenCodexSessionIsConnectedAndExecutable() {
    server
        .expect(requestTo("http://fashion-chat.test/health/ready"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://fashion-chat.test/codex-app-server/account/read"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                        {
                          "connected": true,
                          "status": "connected",
                          "executable": true,
                          "blockReason": null
                        }
                        """,
                MediaType.APPLICATION_JSON));

    FashionChatValidationStatusResponse response = service.status();

    assertThat(response.ready()).isTrue();
    assertThat(response.accountStatus()).isEqualTo("AUTHENTICATED");
    assertThat(response.authenticated()).isTrue();
    assertThat(response.connected()).isTrue();
    assertThat(response.executable()).isTrue();
    assertThat(response.blockReason()).isNull();
    server.verify();
  }

  /** Deve retornar link e código quando o serviço externo inicia o fluxo de device code. */
  @Test
  void startLoginReturnsDeviceCodePayload() {
    server
        .expect(requestTo("http://fashion-chat.test/codex-app-server/account/login/start"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("{\"type\":\"chatgptDeviceCode\"}"))
        .andRespond(
            withSuccess(
                """
                        {
                          "verification_uri": "https://chatgpt.com/activate",
                          "user_code": "ABCD-EFGH",
                          "expires_in": 600,
                          "interval": 5
                        }
                        """,
                MediaType.APPLICATION_JSON));

    StartFashionChatLoginResponse response = service.startLogin();

    assertThat(response.verificationUri()).isEqualTo("https://chatgpt.com/activate");
    assertThat(response.userCode()).isEqualTo("ABCD-EFGH");
    assertThat(response.expiresIn()).isEqualTo(600);
    assertThat(response.errorMessage()).isNull();
    server.verify();
  }
}
