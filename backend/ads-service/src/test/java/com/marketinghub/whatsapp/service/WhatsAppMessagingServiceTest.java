package com.marketinghub.whatsapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.journey.execution.config.WhatsAppProperties;
import com.marketinghub.whatsapp.*;
import com.marketinghub.repository.jpa.whatsapp.WhatsAppMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class WhatsAppMessagingServiceTest {
    @Mock
    private WhatsAppAccountService accountService;
    @Mock
    private WhatsAppMessageRepository messageRepository;

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private ObjectMapper objectMapper;
    private WhatsAppProperties properties;
    private WhatsAppMessagingService service;
    private WhatsAppAccount account;

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        objectMapper = new ObjectMapper();
        properties = new WhatsAppProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://graph.facebook.com/v18.0");
        service = new WhatsAppMessagingService(restTemplate, accountService, messageRepository, objectMapper, properties);
        account = WhatsAppAccount.builder()
                .id(1L)
                .displayName("Account")
                .phoneNumber("+5511987654321")
                .phoneNumberId("1234")
                .accessToken("token")
                .build();
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendTextMessagePersistsAndReturnsMessage() {
        server.expect(once(), requestTo("https://graph.facebook.com/v18.0/1234/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.555\"}]}", MediaType.APPLICATION_JSON));

        WhatsAppMessage message = service.sendTextMessage(account, "+551100000000", "Body", Map.of("source", "test"));

        server.verify();
        assertThat(message.getStatus()).isEqualTo("SENT");
        assertThat(message.getMessageId()).isEqualTo("wamid.555");
        assertThat(message.getTextBody()).isEqualTo("Body");
    }

    @Test
    void handleWebhookPersistsInboundMessage() throws Exception {
        when(accountService.findByPhoneNumberId("1234")).thenReturn(Optional.of(account));
        when(messageRepository.findByMessageId("wamid.123")).thenReturn(Optional.empty());

        String payload = "{" +
                "\"entry\":[{" +
                "\"changes\":[{" +
                "\"value\":{" +
                "\"metadata\":{\"phone_number_id\":\"1234\"}," +
                "\"contacts\":[{\"wa_id\":\"5511999999999\"}]," +
                "\"messages\":[{" +
                "\"id\":\"wamid.123\"," +
                "\"from\":\"5511999999999\"," +
                "\"timestamp\":\"1700000000\"," +
                "\"type\":\"text\"," +
                "\"text\":{\"body\":\"hello world\"}" +
                "}]" +
                "}" +
                "}]}]}";

        service.handleWebhook(objectMapper.readTree(payload));

        ArgumentCaptor<WhatsAppMessage> captor = ArgumentCaptor.forClass(WhatsAppMessage.class);
        verify(messageRepository).save(captor.capture());
        WhatsAppMessage saved = captor.getValue();
        assertThat(saved.getDirection()).isEqualTo(WhatsAppMessageDirection.INBOUND);
        assertThat(saved.getTextBody()).isEqualTo("hello world");
        assertThat(saved.getFromNumber()).isEqualTo("5511999999999");
    }

    @Test
    void handleWebhookUpdatesStatus() throws Exception {
        when(accountService.findByPhoneNumberId("1234")).thenReturn(Optional.of(account));
        WhatsAppMessage existing = WhatsAppMessage.builder()
                .id(20L)
                .account(account)
                .direction(WhatsAppMessageDirection.OUTBOUND)
                .messageType(WhatsAppMessageType.TEXT)
                .messageId("wamid.456")
                .status("SENT")
                .build();
        when(messageRepository.findByMessageId("wamid.456")).thenReturn(Optional.of(existing));

        String payload = "{" +
                "\"entry\":[{" +
                "\"changes\":[{" +
                "\"value\":{" +
                "\"metadata\":{\"phone_number_id\":\"1234\"}," +
                "\"statuses\":[{" +
                "\"id\":\"wamid.456\"," +
                "\"status\":\"delivered\"," +
                "\"timestamp\":\"1700000500\"" +
                "}]" +
                "}" +
                "}]}]}";

        service.handleWebhook(objectMapper.readTree(payload));

        assertThat(existing.getStatus()).isEqualTo("delivered");
        verify(messageRepository).save(existing);
    }
}
