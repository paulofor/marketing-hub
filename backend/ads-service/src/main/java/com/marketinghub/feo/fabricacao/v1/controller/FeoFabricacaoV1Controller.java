package com.marketinghub.feo.fabricacao.v1.controller;

import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1CompleteRequest;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1ExecutionSummaryResponse;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1FailureRequest;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1PendingResponse;
import com.marketinghub.feo.fabricacao.v1.dto.FeoFabricacaoV1StartResponse;
import com.marketinghub.feo.fabricacao.v1.service.FeoFabricacaoV1Service;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor comandos de experimento e callbacks internos da FEO v1. */
@RestController
@RequestMapping("/api")
public class FeoFabricacaoV1Controller {

  private final FeoFabricacaoV1Service service;

  /** Inicializa o controller com o serviço de fabricação FEO. */
  public FeoFabricacaoV1Controller(FeoFabricacaoV1Service service) {
    this.service = service;
  }

  /** Cria ou reutiliza uma solicitação de fabricação FEO para o experimento. */
  @PostMapping("/experiments/{experimentId}/feo/fabricacao/v1/start")
  public ResponseEntity<FeoFabricacaoV1StartResponse> start(@PathVariable Long experimentId) {
    return ResponseEntity.accepted().body(service.startForExperiment(experimentId));
  }

  /** Lista execuções FEO vinculadas ao experimento. */
  @GetMapping("/experiments/{experimentId}/feo/fabricacao/v1/stage-executions")
  public ResponseEntity<List<FeoFabricacaoV1ExecutionSummaryResponse>> list(
      @PathVariable Long experimentId) {
    return ResponseEntity.ok(service.listByExperiment(experimentId));
  }

  /** Lista pendências canônicas de uma etapa para consumo pelo worker FEO. */
  @GetMapping("/internal/feo/fabricacao/v1/{stageCode}/stage-executions/pending")
  public List<FeoFabricacaoV1PendingResponse> pending(
      @PathVariable String stageCode, @RequestParam(defaultValue = "10") int limit) {
    return service.listPending(stageCode, limit);
  }

  /** Recebe conclusão funcional de uma etapa executada pelo worker FEO. */
  @PostMapping("/internal/feo/fabricacao/v1/{stageCode}/stage-executions/{executionId}/complete")
  public ResponseEntity<Void> complete(
      @PathVariable String stageCode,
      @PathVariable Long executionId,
      @RequestBody FeoFabricacaoV1CompleteRequest request) {
    service.complete(stageCode, executionId, request);
    return ResponseEntity.accepted().build();
  }

  /** Recebe falha técnica de uma etapa executada pelo worker FEO. */
  @PostMapping("/internal/feo/fabricacao/v1/{stageCode}/stage-executions/{executionId}/fail")
  public ResponseEntity<Void> fail(
      @PathVariable String stageCode,
      @PathVariable Long executionId,
      @RequestBody FeoFabricacaoV1FailureRequest request) {
    service.fail(stageCode, executionId, request);
    return ResponseEntity.accepted().build();
  }
}
