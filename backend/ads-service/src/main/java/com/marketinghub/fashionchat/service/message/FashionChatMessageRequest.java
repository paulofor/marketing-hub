package com.marketinghub.fashionchat.service.message;

/** Representa a pergunta enviada pela tela do Chat Moda. */
public record FashionChatMessageRequest(String customerId, String message) {
}
