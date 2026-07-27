package com.marketinghub.fashionchat.service.message;

/** Representa a pergunta enviada pela tela do Chat Moda. */
public record FashionChatMessageRequest(String customerId, String message, String jobId) {
  /** Mantém compatibilidade com chamadas que ainda não informam o job de rastreio. */
  public FashionChatMessageRequest(String customerId, String message) {
    this(customerId, message, null);
  }
}
