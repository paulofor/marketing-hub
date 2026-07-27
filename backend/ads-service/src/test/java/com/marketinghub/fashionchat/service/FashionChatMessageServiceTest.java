package com.marketinghub.fashionchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.marketinghub.fashionchat.service.message.FashionChatMessageRequest;
import com.marketinghub.fashionchat.service.message.FashionChatMessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Valida a ponte do backend para o executor do Chat Moda. */
class FashionChatMessageServiceTest {
  private final RestTemplate restTemplate = new RestTemplate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
  private final FashionChatMessageService service =
      new FashionChatMessageService(restTemplate, "http://fashion-chat.test/");

  /** Deve encaminhar a mensagem ao executor e preservar a resposta funcional. */
  @Test
  void answerForwardsMessageToFashionChatExecutor() {
    server
        .expect(requestTo("http://fashion-chat.test/api/fashion-chat/messages"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("X-Job-Id", "fashion-chat-job-test"))
        .andExpect(header("X-Correlation-Id", "fashion-chat-job-test"))
        .andExpect(
            content()
                .json(
                    """
                        {
                          "customerId": "marketing-hub-pilot",
                          "message": "Que roupa usar em uma reuniao casual?",
                          "jobId": "fashion-chat-job-test"
                        }
                        """))
        .andRespond(
            withSuccess(
                """
                        {
                          "answer": "Use alfaiataria leve.",
                          "mode": "codex_app_server",
                          "sandboxId": "fashion-chat-test",
                          "jobId": "fashion-chat-job-test",
                          "research": {"query": "moda reuniao"}
                        }
                        """,
                MediaType.APPLICATION_JSON));

    FashionChatMessageResponse response =
        service.answer(
            new FashionChatMessageRequest(
                " marketing-hub-pilot ",
                " Que roupa usar em uma reuniao casual? ",
                " fashion-chat-job-test "));

    assertThat(response.answer()).isEqualTo("Use alfaiataria leve.");
    assertThat(response.mode()).isEqualTo("codex_app_server");
    assertThat(response.sandboxId()).isEqualTo("fashion-chat-test");
    assertThat(response.jobId()).isEqualTo("fashion-chat-job-test");
    assertThat(response.research().get("query").asText()).isEqualTo("moda reuniao");
    server.verify();
  }

  /** Deve repetir uma vez quando o executor retorna falha transitória. */
  @Test
  void answerRetriesTransientGatewayFailure() {
    server
        .expect(requestTo("http://fashion-chat.test/api/fashion-chat/messages"))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
    server
        .expect(requestTo("http://fashion-chat.test/api/fashion-chat/messages"))
        .andExpect(header("X-Job-Id", "fashion-chat-retry-test"))
        .andRespond(
            withSuccess(
                """
                        {
                          "answer": "Conversa recuperada apos retry.",
                          "mode": "codex_app_server",
                          "sandboxId": "fashion-chat-retry",
                          "jobId": "fashion-chat-retry-test",
                          "research": {"query": "retry"}
                        }
                        """,
                MediaType.APPLICATION_JSON));

    FashionChatMessageResponse response =
        service.answer(
            new FashionChatMessageRequest("cliente", "Look para festa", "fashion-chat-retry-test"));

    assertThat(response.answer()).contains("retry");
    assertThat(response.jobId()).isEqualTo("fashion-chat-retry-test");
    server.verify();
  }

  /** Deve bloquear chamada vazia antes de acionar o executor. */
  @Test
  void answerRejectsBlankMessage() {
    assertThatThrownBy(() -> service.answer(new FashionChatMessageRequest("cliente", " ")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("message e obrigatorio");
  }
}
