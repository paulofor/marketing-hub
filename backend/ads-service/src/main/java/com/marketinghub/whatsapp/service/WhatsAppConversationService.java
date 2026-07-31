package com.marketinghub.whatsapp.service;

import com.marketinghub.repository.jpa.whatsapp.WhatsAppMessageRepository;
import com.marketinghub.whatsapp.dto.WhatsAppConversationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serviço que resume mensagens do WhatsApp em conversas acionáveis no cockpit. */
@Service
public class WhatsAppConversationService {
  private final WhatsAppMessageRepository messageRepository;

  /** Cria o serviço usando o repositório de mensagens do WhatsApp. */
  public WhatsAppConversationService(WhatsAppMessageRepository messageRepository) {
    this.messageRepository = messageRepository;
  }

  /** Lista conversas agrupadas por conta e telefone do cliente. */
  @Transactional(readOnly = true)
  public Page<WhatsAppConversationDto> listConversations(Pageable pageable) {
    return messageRepository.findConversationSummaries(pageable).map(this::toDto);
  }

  /** Converte a projeção de banco no contrato usado pelo front-end. */
  private WhatsAppConversationDto toDto(
      WhatsAppMessageRepository.WhatsAppConversationProjection projection) {
    return new WhatsAppConversationDto(
        projection.getId(),
        projection.getAccountId(),
        projection.getAccountDisplayName(),
        projection.getContactNumber(),
        projection.getLastMessageAt(),
        valueOrZero(projection.getInboundCount()),
        valueOrZero(projection.getOutboundCount()),
        valueOrZero(projection.getPendingInboundCount()));
  }

  /** Normaliza contadores nulos retornados por agregações SQL. */
  private long valueOrZero(Long value) {
    return value == null ? 0L : value;
  }
}
