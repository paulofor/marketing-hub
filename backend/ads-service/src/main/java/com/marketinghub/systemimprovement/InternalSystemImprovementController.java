package com.marketinghub.systemimprovement;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: oferecer a todos os agentes o contrato canônico para sugerir melhorias. */
@RestController
@RequestMapping("/api/internal/system-improvements/v1")
public class InternalSystemImprovementController {
  private final SystemImprovementService service;

  /** Configura o mesmo serviço usado pelo cadastro administrativo. */
  public InternalSystemImprovementController(SystemImprovementService service) {
    this.service = service;
  }

  /** Registra a sugestão identificando obrigatoriamente o agente solicitante. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SystemImprovementResponse create(
      @Valid @RequestBody CreateSystemImprovementRequest request) {
    return service.create(request);
  }
}
