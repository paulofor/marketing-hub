package com.marketinghub.agenttask;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Responsabilidade: comunicar rejeição já persistida sem expor exceção bruta na interface. */
@RestControllerAdvice(
    assignableTypes = {InternalAgentTaskExecutionController.class, AgentTaskController.class})
public class AgentTaskResultRejectionAdvice {
  /**
   * Entrega código estável ao executor e orientação simples à pessoa, sem converter em HTTP 500.
   */
  @ExceptionHandler(AgentTaskResultRejectedException.class)
  public ResponseEntity<Map<String, String>> rejected(AgentTaskResultRejectedException ex) {
    return ResponseEntity.unprocessableEntity()
        .body(
            Map.of(
                "code",
                AgentTaskResultRejectedException.CODE,
                "message",
                AgentTaskResultView.businessReason(ex.getMessage())));
  }
}
