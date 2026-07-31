package com.marketinghub.whatsapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.repository.jpa.whatsapp.WhatsAppMessageRepository;
import com.marketinghub.whatsapp.dto.WhatsAppConversationDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Testa a montagem de resumos comerciais de conversas do WhatsApp. */
@ExtendWith(MockitoExtension.class)
class WhatsAppConversationServiceTest {
  @Mock private WhatsAppMessageRepository messageRepository;

  /** Verifica que a projeção agregada do banco vira DTO de conversa. */
  @Test
  void listConversationsMapsAggregatedProjection() {
    WhatsAppConversationService service = new WhatsAppConversationService(messageRepository);
    PageRequest pageable = PageRequest.of(0, 25);
    Instant lastMessageAt = Instant.parse("2026-07-31T10:00:00Z");
    WhatsAppMessageRepository.WhatsAppConversationProjection projection =
        new TestConversationProjection(lastMessageAt);
    when(messageRepository.findConversationSummaries(pageable))
        .thenReturn(new PageImpl<>(List.of(projection), pageable, 1));

    Page<WhatsAppConversationDto> result = service.listConversations(pageable);

    WhatsAppConversationDto conversation = result.getContent().get(0);
    assertThat(conversation.accountId()).isEqualTo(10L);
    assertThat(conversation.accountDisplayName()).isEqualTo("Conta MKT");
    assertThat(conversation.contactNumber()).isEqualTo("+5511999999999");
    assertThat(conversation.lastMessageAt()).isEqualTo(lastMessageAt);
    assertThat(conversation.inboundCount()).isEqualTo(3);
    assertThat(conversation.outboundCount()).isEqualTo(2);
    assertThat(conversation.pendingInboundCount()).isEqualTo(1);
  }

  /** Projeção de teste para simular a agregação SQL de conversas. */
  private record TestConversationProjection(Instant lastMessageAt)
      implements WhatsAppMessageRepository.WhatsAppConversationProjection {
    /** Retorna o identificador simulado da conversa. */
    @Override
    public Long getId() {
      return 1L;
    }

    /** Retorna a conta simulada da conversa. */
    @Override
    public Long getAccountId() {
      return 10L;
    }

    /** Retorna o nome simulado da conta. */
    @Override
    public String getAccountDisplayName() {
      return "Conta MKT";
    }

    /** Retorna o telefone simulado do contato. */
    @Override
    public String getContactNumber() {
      return "+5511999999999";
    }

    /** Retorna a data simulada da última mensagem. */
    @Override
    public Instant getLastMessageAt() {
      return lastMessageAt;
    }

    /** Retorna a contagem simulada de entradas. */
    @Override
    public Long getInboundCount() {
      return 3L;
    }

    /** Retorna a contagem simulada de saídas. */
    @Override
    public Long getOutboundCount() {
      return 2L;
    }

    /** Retorna a contagem simulada de mensagens pendentes de resposta. */
    @Override
    public Long getPendingInboundCount() {
      return 1L;
    }
  }
}
