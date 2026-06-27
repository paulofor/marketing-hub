package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.controller;

import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.GeraAnuncioImagemService;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.detailStageExecution.GeraAnuncioImagemDetailResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.listStageExecutions.GeraAnuncioImagemExecutionSummaryResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.pending.GeraAnuncioImagemPendingResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebePrompt.GeraAnuncioImagemPromptRequest;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebeRequest.GeraAnuncioImagemRecebeRequestRequest;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebeResposta.GeraAnuncioImagemRespostaRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: disponibilizar a borda HTTP canônica da etapa Imagem do pipeline GeraAnuncio v2. */
@RestController
@RequestMapping("/api/internal/aiworker/geracaoanuncios/v1/imagem/stage-executions")
public class GeraAnuncioImagemController {
    private static final Logger log = LoggerFactory.getLogger(GeraAnuncioImagemController.class);
    private final GeraAnuncioImagemService service;

    /** Inicializa o controller com o service canônico da etapa. */
    public GeraAnuncioImagemController(GeraAnuncioImagemService service) {
        this.service = service;
    }

    /** Inicia uma execução da etapa Imagem para o identificador externo informado no caminho. */
    @PostMapping("/{idExterno}/start")
    public GeraAnuncioImagemExecutionSummaryResponse startPorIdExterno(@PathVariable String idExterno) {
        return service.start(idExterno);
    }

    /** Inicia uma execução da etapa Imagem para o experimento informado. */
    @PostMapping("/experiments/{experimentId}/start")
    public GeraAnuncioImagemExecutionSummaryResponse start(@PathVariable Long experimentId) {
        return service.start(experimentId);
    }

    /** Lista execuções existentes da etapa para um experimento. */
    @GetMapping("/experiments/{experimentId}")
    public List<GeraAnuncioImagemExecutionSummaryResponse> listStageExecutions(@PathVariable Long experimentId) {
        return service.listStageExecutions(experimentId);
    }

    /** Entrega ao AI Worker as execuções pendentes da etapa Imagem. */
    @PostMapping("/pending")
    public List<GeraAnuncioImagemPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe o request operacional gerado pelo AI Worker para a etapa usando o jobId da execução. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public ResponseEntity<Void> recebeRequest(
            @PathVariable String idExterno, @PathVariable String jobId, @RequestBody GeraAnuncioImagemRecebeRequestRequest request) {
        service.recebeRequest(idExterno, jobId, request);
        return ResponseEntity.accepted().build();
    }

    /** Recebe o callback final do AI Worker com a resposta da etapa e retorna a próxima etapa, quando existir. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public ResponseEntity<String> recebeResponse(
            @PathVariable String idExterno, @PathVariable String jobId, @RequestBody GeraAnuncioImagemRespostaRequest request) {
        log.info(
                "Recebendo response do pipeline geracaoanuncios; etapa=imagem; idExterno={}; jobId={}; payload={}",
                idExterno,
                jobId,
                request);
        String nextStageCode = service.recebeResponse(idExterno, jobId, request);
        return ResponseEntity.accepted().body(nextStageCode);
    }

    /** Recebe o prompt operacional enviado ao modelo pelo AI Worker. */
    @PostMapping("/{stageExecutionId}/prompt")
    public ResponseEntity<Void> recebePrompt(@PathVariable String stageExecutionId, @RequestBody GeraAnuncioImagemPromptRequest request) {
        service.recebePrompt(stageExecutionId, request);
        return ResponseEntity.accepted().build();
    }

    /** Recebe a resposta bruta do modelo e a saída funcional estruturada da etapa. */
    @PostMapping("/{stageExecutionId}/response")
    public ResponseEntity<Void> recebeResposta(@PathVariable String stageExecutionId, @RequestBody GeraAnuncioImagemRespostaRequest request) {
        service.recebeResposta(stageExecutionId, request);
        return ResponseEntity.accepted().build();
    }

    /** Retorna o detalhe auditável de uma execução da etapa. */
    @GetMapping("/{stageExecutionId}")
    public GeraAnuncioImagemDetailResponse detailStageExecution(@PathVariable String stageExecutionId) {
        return service.detailStageExecution(stageExecutionId);
    }
}
