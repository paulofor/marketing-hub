package com.marketinghub.fashionchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.marketinghub.fashionchat.service.message.FashionChatMessageRequest;
import com.marketinghub.fashionchat.service.message.FashionChatMessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Valida a ponte do backend para o executor do Chat Moda. */
class FashionChatMessageServiceTest {
    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    private final FashionChatMessageService service = new FashionChatMessageService(
            restTemplate,
            "http://fashion-chat.test/");

    /** Deve encaminhar a mensagem ao executor e preservar a resposta funcional. */
    @Test
    void answerForwardsMessageToFashionChatExecutor() {
        server.expect(requestTo("http://fashion-chat.test/api/fashion-chat/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "customerId": "marketing-hub-pilot",
                          "message": "Que roupa usar em uma reuniao casual?"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "answer": "Use alfaiataria leve.",
                          "mode": "codex_app_server",
                          "sandboxId": "fashion-chat-test",
                          "research": {"query": "moda reuniao"}
                        }
                        """, MediaType.APPLICATION_JSON));

        FashionChatMessageResponse response = service.answer(new FashionChatMessageRequest(
                " marketing-hub-pilot ",
                " Que roupa usar em uma reuniao casual? "));

        assertThat(response.answer()).isEqualTo("Use alfaiataria leve.");
        assertThat(response.mode()).isEqualTo("codex_app_server");
        assertThat(response.sandboxId()).isEqualTo("fashion-chat-test");
        assertThat(response.research().get("query").asText()).isEqualTo("moda reuniao");
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
