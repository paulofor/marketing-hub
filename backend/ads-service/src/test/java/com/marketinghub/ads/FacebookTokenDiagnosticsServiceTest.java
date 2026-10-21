package com.marketinghub.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

/** Evita confundir transporte bem-sucedido com validade e mantém segredos fora do diagnóstico. */
@ExtendWith(OutputCaptureExtension.class)
class FacebookTokenDiagnosticsServiceTest {
  private MockRestServiceServer server;

  /** Constrói cliente com transporte simulado e identidade independente de produtos. */
  private FacebookTokenDiagnosticsService service() {
    var builder =
        new RestTemplateBuilder()
            .additionalCustomizers(
                template -> server = MockRestServiceServer.bindTo(template).build());
    return new FacebookTokenDiagnosticsService(
        builder, new ObjectMapper(), "https://graph.facebook.test", "v23.0");
  }

  /** Mantém somente dados sintéticos para verificar sanitização e identidade. */
  private FacebookAccount account() {
    return FacebookAccount.builder()
        .id(87L)
        .name("Conta QA")
        .appId("123")
        .appSecret("synthetic-secret")
        .accessToken("synthetic-token")
        .adAccountId("456")
        .build();
  }

  /** HTTP 200 com validade falsa, ausente ou string jamais aprova a credencial. */
  @ParameterizedTest
  @ValueSource(strings = {"false", "null", "\"true\"", "true"})
  void distinguishesHttpFromValidity(String value) {
    var service = service();
    server
        .expect(ExpectedCount.once(), request -> {})
        .andRespond(
            withSuccess("{\"data\":{\"is_valid\":" + value + "}}", MediaType.APPLICATION_JSON));
    server
        .expect(ExpectedCount.times(3), request -> {})
        .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
    var result = service.diagnose(account());
    server.verify();
    assertThat(result.tokenDebug().status()).isEqualTo("true".equals(value) ? "SUCCESS" : "FAILED");
  }

  /** Respostas externas e exceções com segredo não contaminam logs nem retorno da tela. */
  @Test
  void omitsSecretsFromProviderErrors(CapturedOutput output) throws Exception {
    var service = service();
    server
        .expect(ExpectedCount.times(4), request -> {})
        .andRespond(
            withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    "{\"error\":{\"code\":190,\"message\":\"synthetic-token synthetic-secret\"}}"));
    var result = service.diagnose(account());
    server.verify();
    assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(result) + output)
        .doesNotContain("synthetic-token", "synthetic-secret");
    assertThat(result.tokenDebug().code()).isEqualTo(190);
  }
}
