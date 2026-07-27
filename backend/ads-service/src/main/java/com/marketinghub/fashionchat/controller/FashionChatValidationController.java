package com.marketinghub.fashionchat.controller;

import com.marketinghub.fashionchat.service.FashionChatValidationService;
import com.marketinghub.fashionchat.service.login.StartFashionChatLoginResponse;
import com.marketinghub.fashionchat.service.status.FashionChatValidationStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a validação administrativa da autenticação do Chat Moda. */
@RestController
@RequestMapping("/api/fashion-chat/validation")
public class FashionChatValidationController {
  private final FashionChatValidationService service;

  /** Inicializa o controller com o serviço de validação do Chat Moda. */
  public FashionChatValidationController(FashionChatValidationService service) {
    this.service = service;
  }

  /** Consulta a prontidão e a autenticação real do serviço de Chat Moda. */
  @GetMapping("/status")
  public FashionChatValidationStatusResponse status() {
    return service.status();
  }

  /** Inicia o fluxo de login por device code no serviço de Chat Moda. */
  @PostMapping("/login/start")
  public StartFashionChatLoginResponse startLogin() {
    return service.startLogin();
  }
}
