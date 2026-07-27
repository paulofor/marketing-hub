package com.marketinghub.fashionchat.controller;

import com.marketinghub.fashionchat.service.FashionChatMessageService;
import com.marketinghub.fashionchat.service.message.FashionChatMessageRequest;
import com.marketinghub.fashionchat.service.message.FashionChatMessageResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe o contrato de conversa do Chat Moda para o frontend. */
@RestController
@RequestMapping("/api/fashion-chat")
public class FashionChatController {
  private final FashionChatMessageService service;

  /** Inicializa o controller com o serviço de conversa do Chat Moda. */
  public FashionChatController(FashionChatMessageService service) {
    this.service = service;
  }

  /** Recebe a pergunta da tela e devolve a resposta gerada pelo executor. */
  @PostMapping("/messages")
  public FashionChatMessageResponse answer(@RequestBody FashionChatMessageRequest request) {
    return service.answer(request);
  }
}
