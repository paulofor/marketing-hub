package com.marketinghub.geraanuncio.v2.criativo.controller;

import com.marketinghub.geraanuncio.v2.criativo.service.GeraAnuncioCriativoService;
import com.marketinghub.geraanuncio.v2.criativo.service.detailStageExecution.GeraAnuncioCriativoDetailResponse;
import com.marketinghub.geraanuncio.v2.criativo.service.listStageExecutions.GeraAnuncioCriativoExecutionSummaryResponse;
import com.marketinghub.geraanuncio.v2.criativo.service.pending.GeraAnuncioCriativoPendingResponse;
import com.marketinghub.geraanuncio.v2.criativo.service.recebePrompt.GeraAnuncioCriativoPromptRequest;
import com.marketinghub.geraanuncio.v2.criativo.service.recebeResposta.GeraAnuncioCriativoRespostaRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: disponibilizar a borda HTTP canônica da etapa Criativo do pipeline GeraAnuncio v2. */
@RestController
@RequestMapping("/api/internal/geraanuncio/v2/criativo/stage-executions")
public class GeraAnuncioCriativoController {
    private final GeraAnuncioCriativoService service;

    /** Inicializa o controller com o service canônico da etapa. */
    public GeraAnuncioCriativoController(GeraAnuncioCriativoService service) {
        this.service = service;
    }

    /** Inicia uma execução de geração de criativos para o experimento informado. */
    @PostMapping("/experiments/{experimentId}/start")
    public GeraAnuncioCriativoExecutionSummaryResponse start(@PathVariable Long experimentId) {
        return service.start(experimentId);
    }

    /** Lista execuções existentes da etapa para um experimento. */
    @GetMapping("/experiments/{experimentId}")
    public List<GeraAnuncioCriativoExecutionSummaryResponse> listStageExecutions(@PathVariable Long experimentId) {
        return service.listStageExecutions(experimentId);
    }

    /** Entrega ao AI Worker as execuções pendentes da etapa Criativo. */
    @PostMapping("/pending")
    public List<GeraAnuncioCriativoPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe o prompt operacional enviado ao modelo pelo AI Worker. */
    @PostMapping("/{stageExecutionId}/prompt")
    public ResponseEntity<Void> recebePrompt(@PathVariable String stageExecutionId, @RequestBody GeraAnuncioCriativoPromptRequest request) {
        service.recebePrompt(stageExecutionId, request);
        return ResponseEntity.accepted().build();
    }

    /** Recebe a resposta bruta do modelo e a saída funcional estruturada da etapa. */
    @PostMapping("/{stageExecutionId}/response")
    public ResponseEntity<Void> recebeResposta(@PathVariable String stageExecutionId, @RequestBody GeraAnuncioCriativoRespostaRequest request) {
        service.recebeResposta(stageExecutionId, request);
        return ResponseEntity.accepted().build();
    }

    /** Retorna o detalhe auditável de uma execução da etapa. */
    @GetMapping("/{stageExecutionId}")
    public GeraAnuncioCriativoDetailResponse detailStageExecution(@PathVariable String stageExecutionId) {
        return service.detailStageExecution(stageExecutionId);
    }
}
