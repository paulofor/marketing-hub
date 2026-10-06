package com.marketinghub.pde.mira.privateprototype.v1.controller;

import com.marketinghub.pde.mira.privateprototype.v1.service.MiraPrivateService;
import com.marketinghub.pde.mira.privateprototype.v1.service.contract.MiraPrivateContract.*;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: proteger e expor a candidata privada de Mira pelo backend principal. */
@RestController
@RequestMapping("/api/pde/mira/candidate/v1")
public class MiraPrivateController {
  private final MiraPrivateService service;
  private final String internalToken;

  /** Recebe o serviço canônico e a credencial já existente da integração PDE. */
  public MiraPrivateController(
      MiraPrivateService service,
      @Value("${integrations.pde-platform.internal-token:${PDE_INTERNAL_API_TOKEN:}}")
          String internalToken) {
    this.service = service;
    this.internalToken = internalToken;
  }

  /** Descreve capacidades públicas sem criar pacote nem emitir sinal de funil. */
  @GetMapping("/contract")
  public Contract contract() {
    return service.contract();
  }

  /** Recupera somente o pacote associado à credencial opaca apresentada. */
  @GetMapping("/session")
  public SessionView session(@RequestHeader("X-Mira-Session") String secret) {
    return service.session(secret);
  }

  /** Valida recursivamente cada produto incluído antes de gravar a entrada. */
  @PutMapping("/input")
  public SessionView input(
      @RequestHeader("X-Mira-Session") String secret, @Valid @RequestBody Input input) {
    return service.input(secret, input);
  }

  /** Organiza a entrada persistida sem integrar provedor pago ou criar compra. */
  @PostMapping("/generate")
  public SessionView generate(@RequestHeader("X-Mira-Session") String secret) {
    return service.generate(secret);
  }

  /** Registra somente ações funcionais previstas para o pacote autorizado. */
  @PostMapping("/events")
  public SessionView event(
      @RequestHeader("X-Mira-Session") String secret, @Valid @RequestBody Event event) {
    return service.event(secret, event);
  }

  /** Emite um acesso sintético vinculado ao ciclo e à versão, após autenticação interna. */
  @PostMapping("/internal/sessions")
  public CreatedSession create(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @Valid @RequestBody Create input) {
    authorize(token);
    return service.create(input);
  }

  /** Revoga o acesso mantendo sua prova histórica. */
  @DeleteMapping("/internal/sessions/{id}")
  public void revoke(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable String id) {
    authorize(token);
    service.revoke(id);
  }

  /** Entrega relatório persistido somente a executor ou operador autenticado. */
  @GetMapping("/internal/cycles/{cycleId}/report")
  public Report report(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable Long cycleId) {
    authorize(token);
    return service.report(cycleId);
  }

  /** Compara credenciais em tempo constante sem aceitar sessão como autorização interna. */
  private void authorize(String token) {
    if (internalToken == null
        || internalToken.isBlank()
        || token == null
        || !MessageDigest.isEqual(
            internalToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial interna necessária.");
  }
}
