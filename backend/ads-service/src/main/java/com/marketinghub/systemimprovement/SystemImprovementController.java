package com.marketinghub.systemimprovement;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor o backlog administrativo de melhorias sugeridas pelos agentes. */
@RestController
@RequestMapping("/api/system-improvements")
public class SystemImprovementController {
  private final SystemImprovementService service;

  /** Configura o serviço central de melhorias. */
  public SystemImprovementController(SystemImprovementService service) {
    this.service = service;
  }

  /** Lista as melhorias registradas por todos os agentes. */
  @GetMapping
  public List<SystemImprovementResponse> list() {
    return service.list();
  }

  /** Permite cadastrar uma melhoria pela tela administrativa. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SystemImprovementResponse create(
      @Valid @RequestBody CreateSystemImprovementRequest request) {
    return service.create(request);
  }
}
