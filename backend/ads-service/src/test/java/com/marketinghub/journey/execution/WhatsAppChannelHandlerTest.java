package com.marketinghub.journey.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.journey.execution.channel.ChannelDispatchResult;
import com.marketinghub.journey.execution.channel.ChannelDispatchStatus;
import com.marketinghub.journey.execution.channel.WhatsAppChannelHandler;
import com.marketinghub.journey.execution.config.WhatsAppProperties;
import com.marketinghub.journey.model.*;
import com.marketinghub.model.Lead;
import com.marketinghub.whatsapp.WhatsAppAccount;
import com.marketinghub.whatsapp.WhatsAppMessage;
import com.marketinghub.whatsapp.WhatsAppMessageDirection;
import com.marketinghub.whatsapp.WhatsAppMessageType;
import com.marketinghub.whatsapp.service.WhatsAppAccountService;
import com.marketinghub.whatsapp.service.WhatsAppMessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppChannelHandlerTest {
    @Mock
    private WhatsAppMessagingService messagingService;
    @Mock
    private WhatsAppAccountService accountService;

    private WhatsAppChannelHandler handler;
    private ObjectMapper objectMapper;
    private WhatsAppAccount account;

    @BeforeEach
    void setup() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setEnabled(true);
        account = WhatsAppAccount.builder()
                .id(1L)
                .displayName("Test Account")
                .phoneNumber("+5511987654321")
                .phoneNumberId("1234")
                .accessToken("token")
                .build();
        objectMapper = new ObjectMapper();
        handler = new WhatsAppChannelHandler(messagingService, accountService, properties, objectMapper);
        when(accountService.findActiveAccount()).thenReturn(Optional.of(account));
    }

    @Test
    void dispatchesTextMessage() {
        JourneyStep step = JourneyStep.builder()
                .id(1L)
                .template(JourneyTemplate.builder().id(2L).name("template").build())
                .position(1)
                .phase(JourneyPhase.INTEREST)
                .stimulusType(JourneyStimulusType.WHATSAPP)
                .metadata(Map.of("body", "Hello from MarketingHub"))
                .build();
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(10L)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(UUID.randomUUID()).build())
                .build();

        WhatsAppMessage message = WhatsAppMessage.builder()
                .id(55L)
                .account(account)
                .direction(WhatsAppMessageDirection.OUTBOUND)
                .messageType(WhatsAppMessageType.TEXT)
                .messageId("wamid.123")
                .toNumber("+551100000000")
                .build();
        when(messagingService.sendTextMessage(eq(account), eq("+551100000000"), eq("Hello from MarketingHub"), anyMap()))
                .thenReturn(message);

        ChannelDispatchResult result = handler.dispatch(assignment, step, Map.of("phone", "+551100000000"));

        assertThat(result.status()).isEqualTo(ChannelDispatchStatus.OK);
        assertThat(result.providerMessageId()).isEqualTo("wamid.123");

        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingService).sendTextMessage(eq(account), eq("+551100000000"), eq("Hello from MarketingHub"), metadataCaptor.capture());
        Map<String, Object> metadata = metadataCaptor.getValue();
        assertThat(metadata.get("assignmentId")).isEqualTo(10L);
        assertThat(metadata.get("stepId")).isEqualTo(1L);
        assertThat(metadata.get("source")).isEqualTo("journey");
    }

    @Test
    void returnsTransientFailureOnRateLimit() {
        JourneyStep step = JourneyStep.builder()
                .id(1L)
                .template(JourneyTemplate.builder().id(2L).name("template").build())
                .position(1)
                .phase(JourneyPhase.INTEREST)
                .stimulusType(JourneyStimulusType.WHATSAPP)
                .metadata(Map.of("body", "Hello"))
                .build();
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(11L)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(UUID.randomUUID()).build())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "120");
        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many", headers, new byte[0], null);
        when(messagingService.sendTextMessage(eq(account), anyString(), anyString(), anyMap())).thenThrow(tooManyRequests);

        ChannelDispatchResult result = handler.dispatch(assignment, step, Map.of("phone", "+551100000001"));

        assertThat(result.status()).isEqualTo(ChannelDispatchStatus.TRANSIENT_ERROR);
        assertThat(result.nextAttemptAt()).isNotNull();
    }
}
