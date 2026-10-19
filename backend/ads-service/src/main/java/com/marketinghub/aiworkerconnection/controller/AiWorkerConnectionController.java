package com.marketinghub.aiworkerconnection.controller;

import com.marketinghub.aiworkerconnection.service.AiWorkerConnectionService;
import com.marketinghub.aiworkerconnection.service.deviceCode.DeviceCodeRequest;
import com.marketinghub.aiworkerconnection.service.session.SessionReport;
import com.marketinghub.aiworkerconnection.service.status.ConnectionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Expõe a conexão administrativa e os callbacks exclusivos do Worker IA. */
@RestController
@RequestMapping("/api/ai/worker-connection")
public class AiWorkerConnectionController {
  private final AiWorkerConnectionService service;

  /** Recebe o serviço canônico, sem acesso direto a executor ou provedor. */
  public AiWorkerConnectionController(AiWorkerConnectionService service) {
    this.service = service;
  }

  /** Entrega ao painel o último estado seguro de conexão. */
  @GetMapping
  public ConnectionResponse current() {
    return service.current();
  }

  /** Solicita uma conexão ou reutiliza a tentativa que já está ativa. */
  @PostMapping
  public ConnectionResponse request() {
    return service.request();
  }

  /** Entrega ao executor o único comando pendente do próprio módulo. */
  @GetMapping("/internal/pending")
  public ResponseEntity<ConnectionResponse> pending() {
    ConnectionResponse item = service.pending();
    return item == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(item);
  }

  /** Recebe a página oficial e o código de uso único emitidos pelo App Server. */
  @PostMapping("/internal/{id}/device-code")
  public ConnectionResponse deviceCode(
      @PathVariable Long id, @Valid @RequestBody DeviceCodeRequest body) {
    return service.deviceCode(id, body);
  }

  /** Encerra o comando após confirmação explícita da conta no executor. */
  @PostMapping("/internal/{id}/completion")
  public ConnectionResponse complete(
      @PathVariable Long id, @Valid @RequestBody SessionReport body) {
    return service.complete(id, body);
  }

  /** Registra a prova periódica de disponibilidade sem receber identidade ou token da conta. */
  @PostMapping("/internal/session")
  public ConnectionResponse session(@Valid @RequestBody SessionReport body) {
    return service.session(body);
  }
}
