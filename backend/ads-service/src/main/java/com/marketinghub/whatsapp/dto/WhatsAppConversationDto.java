package com.marketinghub.whatsapp.dto;

import java.time.Instant;

/** Resumo comercial de uma conversa com um contato no WhatsApp. */
public record WhatsAppConversationDto(
    Long id,
    Long accountId,
    String accountDisplayName,
    String contactNumber,
    Instant lastMessageAt,
    long inboundCount,
    long outboundCount,
    long pendingInboundCount) {}
