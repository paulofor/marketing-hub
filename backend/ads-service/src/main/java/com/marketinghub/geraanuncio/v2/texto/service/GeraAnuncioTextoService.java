package com.marketinghub.geraanuncio.v2.texto.service;

import com.marketinghub.geraanuncio.v2.texto.service.detailStageExecution.GeraAnuncioTextoDetailResponse;
import com.marketinghub.geraanuncio.v2.texto.service.listStageExecutions.GeraAnuncioTextoExecutionSummaryResponse;
import com.marketinghub.geraanuncio.v2.texto.service.pending.GeraAnuncioTextoPendingResponse;
import com.marketinghub.geraanuncio.v2.texto.service.recebePrompt.GeraAnuncioTextoPromptRequest;
import com.marketinghub.geraanuncio.v2.texto.service.recebeResposta.GeraAnuncioTextoRespostaRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Responsabilidade: expor contratos de leitura, escrita e auditoria da etapa Texto do pipeline GeraAnuncio v2. */
@Service
public class GeraAnuncioTextoService {
    private final ExperimentService experimentService;

    /** Inicializa o service com o serviço canônico de experimentos. */
    public GeraAnuncioTextoService(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    /** Inicia uma solicitação da etapa Texto para um experimento. */
    public GeraAnuncioTextoExecutionSummaryResponse start(Long experimentId) {
        Experiment experiment = experimentService.requestPipelineCreatives(experimentId);
        return new GeraAnuncioTextoExecutionSummaryResponse(
                null,
                experiment.getId(),
                null,
                experiment.getCreativeGenerationStatus().name(),
                experiment.getCreativeGenerationRequestedAt());
    }

    /** Lista execuções da etapa Texto para relatório operacional. */
    public List<GeraAnuncioTextoExecutionSummaryResponse> listStageExecutions(Long experimentId) {
        return List.of();
    }

    /** Publica pendências canônicas para consumo do AI Worker. */
    public List<GeraAnuncioTextoPendingResponse> pending() {
        return List.of();
    }

    /** Registra o prompt enviado ao modelo para auditoria da etapa. */
    public void recebePrompt(String stageExecutionId, GeraAnuncioTextoPromptRequest request) {}

    /** Registra a resposta do modelo e a saída estruturada retornada pelo worker. */
    public void recebeResposta(String stageExecutionId, GeraAnuncioTextoRespostaRequest request) {}

    /** Busca o detalhe auditável de uma execução da etapa. */
    public GeraAnuncioTextoDetailResponse detailStageExecution(String stageExecutionId) {
        return new GeraAnuncioTextoDetailResponse(stageExecutionId, null, "NOT_FOUND", Instant.now(), Map.of());
    }
}
