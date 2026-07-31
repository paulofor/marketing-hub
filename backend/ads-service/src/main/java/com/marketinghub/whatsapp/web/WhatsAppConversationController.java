package com.marketinghub.whatsapp.web;

import com.marketinghub.whatsapp.dto.WhatsAppConversationDto;
import com.marketinghub.whatsapp.service.WhatsAppConversationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller REST que expõe conversas do WhatsApp para operação comercial. */
@RestController
@RequestMapping("/api/whatsapp/conversations")
public class WhatsAppConversationController {
  private final WhatsAppConversationService conversationService;

  /** Cria o controller com o serviço de conversas do WhatsApp. */
  public WhatsAppConversationController(WhatsAppConversationService conversationService) {
    this.conversationService = conversationService;
  }

  /** Lista conversas recentes para atendimento e acompanhamento no cockpit. */
  @GetMapping
  public Page<WhatsAppConversationDto> listConversations(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "25") int size) {
    Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
    return conversationService.listConversations(pageable);
  }
}
