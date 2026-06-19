package com.marketinghub.oprm.nichocnae.evidencelevelgate.web;

import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.BackendEvidenceLevelGateService;
import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.completeStageExecution.CompleteEvidenceLevelGateRequest;
import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.completeStageExecution.CompleteEvidenceLevelGateResponse;
import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.detailStageExecution.EvidenceLevelGateDetailResponse;
import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.failStageExecution.FailEvidenceLevelGateRequest;
import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.pending.RecordEvidenceLevelGatePending;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe contratos de leitura e escrita da etapa onze E0-E5 sem executar regra de negócio no backend. */
@RestController
@RequestMapping("/api")
public class BackendEvidenceLevelGateController {
  private final BackendEvidenceLevelGateService service;

  /** Inicializa o controller com o serviço de persistência da etapa E0-E5. */
  public BackendEvidenceLevelGateController(BackendEvidenceLevelGateService service) {
    this.service = service;
  }

  /** Lista pendências para o executor externo calcular o nível comercial E0-E5. */
  @GetMapping("/internal/oprm/nichocnae/evidence-level-gate/stage-executions/pending")
  public List<RecordEvidenceLevelGatePending> pending() {
    return service.listPending();
  }

  /** Persiste o resultado E0-E5 calculado pelo executor externo. */
  @PostMapping("/internal/oprm/nichocnae/evidence-level-gate/stage-executions/{researchCycleId}/complete")
  public ResponseEntity<CompleteEvidenceLevelGateResponse> complete(@PathVariable Long researchCycleId, @RequestBody CompleteEvidenceLevelGateRequest request) {
    return ResponseEntity.ok(service.complete(researchCycleId, request));
  }

  /** Registra falha técnica informada pelo executor externo. */
  @PostMapping("/internal/oprm/nichocnae/evidence-level-gate/stage-executions/{researchCycleId}/fail")
  public ResponseEntity<Void> fail(@PathVariable Long researchCycleId, @RequestBody FailEvidenceLevelGateRequest request) {
    service.fail(researchCycleId, request);
    return ResponseEntity.noContent().build();
  }

  /** Consulta o resultado E0-E5 persistido para relatório. */
  @GetMapping("/oprm/nichocnae/evidence-level-gate/stage-executions/{researchCycleId}")
  public ResponseEntity<EvidenceLevelGateDetailResponse> detail(@PathVariable Long researchCycleId) {
    return ResponseEntity.ok(service.detail(researchCycleId));
  }
}
