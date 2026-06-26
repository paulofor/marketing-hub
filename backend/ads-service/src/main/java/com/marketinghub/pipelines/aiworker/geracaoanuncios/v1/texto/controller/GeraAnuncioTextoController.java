package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.controller;

import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.GeraAnuncioTextoService;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.detailStageExecution.GeraAnuncioTextoDetailResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.listStageExecutions.GeraAnuncioTextoExecutionSummaryResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.pending.GeraAnuncioTextoPendingResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.recebePrompt.GeraAnuncioTextoPromptRequest;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.recebeResposta.GeraAnuncioTextoRespostaRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: disponibilizar a borda HTTP canônica da etapa Texto do pipeline GeraAnuncio v2. */
@RestController
@RequestMapping("/api/internal/aiworker/geracaoanuncios/v1/texto/stage-executions")
public class GeraAnuncioTextoController {
    private final GeraAnuncioTextoService service;

    /** Inicializa o controller com o service canônico da etapa. */
    public GeraAnuncioTextoController(GeraAnuncioTextoService service) {
        this.service = service;
    }

    /** Inicia uma execução da etapa Texto para o experimento informado. */
    @PostMapping("/experiments/{experimentId}/start")
    public GeraAnuncioTextoExecutionSummaryResponse start(@PathVariable Long experimentId) {
        return service.start(experimentId);
    }

    /** Lista execuções existentes da etapa para um experimento. */
    @GetMapping("/experiments/{experimentId}")
    public List<GeraAnuncioTextoExecutionSummaryResponse> listStageExecutions(@PathVariable Long experimentId) {
        return service.listStageExecutions(experimentId);
    }

    /** Entrega ao AI Worker as execuções pendentes da etapa Texto. */
    @PostMapping("/pending")
    public List<GeraAnuncioTextoPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe o prompt operacional enviado ao modelo pelo AI Worker. */
    @PostMapping("/{stageExecutionId}/prompt")
    public ResponseEntity<Void> recebePrompt(@PathVariable String stageExecutionId, @RequestBody GeraAnuncioTextoPromptRequest request) {
        service.recebePrompt(stageExecutionId, request);
        return ResponseEntity.accepted().build();
    }

    /** Recebe a resposta bruta do modelo e a saída funcional estruturada da etapa. */
    @PostMapping("/{stageExecutionId}/response")
    public ResponseEntity<Void> recebeResposta(@PathVariable String stageExecutionId, @RequestBody GeraAnuncioTextoRespostaRequest request) {
        service.recebeResposta(stageExecutionId, request);
        return ResponseEntity.accepted().build();
    }

    /** Retorna o detalhe auditável de uma execução da etapa. */
    @GetMapping("/{stageExecutionId}")
    public GeraAnuncioTextoDetailResponse detailStageExecution(@PathVariable String stageExecutionId) {
        return service.detailStageExecution(stageExecutionId);
    }
}
