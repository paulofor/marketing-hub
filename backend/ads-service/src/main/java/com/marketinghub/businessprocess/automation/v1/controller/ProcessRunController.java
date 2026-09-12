package com.marketinghub.businessprocess.automation.v1.controller;

import com.marketinghub.businessprocess.automation.v1.service.ProcessRunService;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.automation.v1.service.status.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: expor comandos administrativos e conciliação autenticada dos processos. */
@RestController
@Tag(name = "Execução automática de processos v1")
public class ProcessRunController {
  private static final String ROOT =
      "/api/business-processes/{processId}/products/{productId}/automation/v1";
  private final ProcessRunService service;
  private final String workerToken;

  /** Configura o serviço único e a credencial exclusiva do conciliador externo. */
  public ProcessRunController(
      ProcessRunService service,
      @Value("${process-execution.worker-token:}") String workerToken,
      @Value("${process-execution.worker-token-file:}") String tokenFile) {
    this.service = service;
    try {
      this.workerToken =
          workerToken.isBlank() && !tokenFile.isBlank()
              ? java.nio.file.Files.readString(java.nio.file.Path.of(tokenFile)).trim()
              : workerToken;
    } catch (java.io.IOException ex) {
      org.slf4j.LoggerFactory.getLogger(ProcessRunController.class)
          .error("Falha ao ler credencial do conciliador de processos", ex);
      throw new IllegalStateException("Credencial do conciliador indisponível.", ex);
    }
  }

  /**
   * Retorna progresso e relações; sem referência oferece somente consulta, sem permitir execução.
   */
  @Operation(
      summary =
          "Consulta execução, progresso e relações do processo, inclusive antes do primeiro ciclo")
  @GetMapping(ROOT)
  public ProcessRunResponse status(
      @PathVariable Long productId,
      @PathVariable Long processId,
      @RequestParam Long chainId,
      @RequestParam(required = false) Long learningCycleId,
      @RequestParam(required = false) String sourceReference) {
    return service.status(
        productId, processId, new ProcessRunCommand(chainId, learningCycleId, sourceReference));
  }

  /** Autoriza uma execução idempotente com os limites de identidade do processo. */
  @Operation(summary = "Inicia o processo completo sem executar atividades pelo navegador")
  @PostMapping(ROOT)
  public ProcessRunResponse start(
      @PathVariable Long productId,
      @PathVariable Long processId,
      @Valid @RequestBody ProcessRunCommand command) {
    return service.start(productId, processId, command);
  }

  /** Pausa novos disparos sem cancelar tarefas já iniciadas. */
  @Operation(summary = "Pausa a execução automática do processo")
  @PostMapping(ROOT + "/{runId}/pause")
  public ProcessRunResponse pause(
      @PathVariable Long productId, @PathVariable Long processId, @PathVariable Long runId) {
    return service.pause(productId, processId, runId);
  }

  /** Retoma o processo preservando tarefas e decisões anteriores. */
  @Operation(summary = "Retoma o processo e revalida o impedimento atual")
  @PostMapping(ROOT + "/{runId}/resume")
  public ProcessRunResponse resume(
      @PathVariable Long productId, @PathVariable Long processId, @PathVariable Long runId) {
    return service.resume(productId, processId, runId);
  }

  /** Entrega o diário paginado com decisões e tarefas vinculadas. */
  @Operation(summary = "Consulta o diário auditável da execução")
  @GetMapping(ROOT + "/{runId}/events")
  public List<ProcessRunEventResponse> events(
      @PathVariable Long productId,
      @PathVariable Long processId,
      @PathVariable Long runId,
      @RequestParam(required = false) Long beforeId) {
    return service.history(productId, processId, runId, beforeId);
  }

  /** Entrega pendências ao conciliador, sem aceitar seleção externa de atividade. */
  @Operation(summary = "Consulta pendências do conciliador externo")
  @GetMapping("/api/internal/business-processes/automation/v1/stage-executions/pending")
  public List<Long> pending(
      @RequestHeader(value = "X-Process-Worker-Token", required = false) String token,
      @RequestParam(defaultValue = "20") int limit) {
    authorize(token);
    return service.pending(limit);
  }

  /** Solicita ao backend a decisão de avanço de uma execução já autorizada. */
  @Operation(summary = "Concilia uma execução de processo já autorizada")
  @PostMapping("/api/internal/business-processes/automation/v1/stage-executions/{runId}/reconcile")
  public ProcessRunResponse reconcile(
      @RequestHeader(value = "X-Process-Worker-Token", required = false) String token,
      @PathVariable Long runId) {
    authorize(token);
    return service.reconcile(runId);
  }

  /** Recusa chamadas internas sem configuração e compara credenciais sem revelar seu conteúdo. */
  private void authorize(String token) {
    if (workerToken.isBlank())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "O conciliador de processos ainda não foi configurado.");
    if (token == null
        || !MessageDigest.isEqual(
            workerToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Credencial do conciliador inválida.");
  }
}
