package com.marketinghub.geraanuncio.v2.criativo.service;

import com.marketinghub.geraanuncio.v2.criativo.service.detailStageExecution.GeraAnuncioCriativoDetailResponse;
import com.marketinghub.geraanuncio.v2.criativo.service.listStageExecutions.GeraAnuncioCriativoExecutionSummaryResponse;
import com.marketinghub.geraanuncio.v2.criativo.service.pending.GeraAnuncioCriativoPendingResponse;
import com.marketinghub.geraanuncio.v2.criativo.service.recebePrompt.GeraAnuncioCriativoPromptRequest;
import com.marketinghub.geraanuncio.v2.criativo.service.recebeResposta.GeraAnuncioCriativoRespostaRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Responsabilidade: expor contratos de leitura, escrita e auditoria da etapa Criativo do pipeline GeraAnuncio v2. */
@Service
public class GeraAnuncioCriativoService {
    private final ExperimentService experimentService;

    /** Inicializa o service com o serviço canônico de experimentos. */
    public GeraAnuncioCriativoService(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    /** Inicia uma solicitação de geração de criativos para um experimento. */
    public GeraAnuncioCriativoExecutionSummaryResponse start(Long experimentId) {
        Experiment experiment = experimentService.requestPipelineCreatives(experimentId);
        return new GeraAnuncioCriativoExecutionSummaryResponse(
                null,
                experiment.getId(),
                null,
                experiment.getCreativeGenerationStatus().name(),
                experiment.getCreativeGenerationRequestedAt());
    }

    /** Lista execuções da etapa Criativo para relatório operacional. */
    public List<GeraAnuncioCriativoExecutionSummaryResponse> listStageExecutions(Long experimentId) {
        return List.of();
    }

    /** Publica pendências canônicas para consumo do AI Worker. */
    public List<GeraAnuncioCriativoPendingResponse> pending() {
        return List.of();
    }

    /** Registra o prompt enviado ao modelo para auditoria da etapa. */
    public void recebePrompt(String stageExecutionId, GeraAnuncioCriativoPromptRequest request) {}

    /** Registra a resposta do modelo e a saída estruturada retornada pelo worker. */
    public void recebeResposta(String stageExecutionId, GeraAnuncioCriativoRespostaRequest request) {}

    /** Busca o detalhe auditável de uma execução da etapa. */
    public GeraAnuncioCriativoDetailResponse detailStageExecution(String stageExecutionId) {
        return new GeraAnuncioCriativoDetailResponse(stageExecutionId, null, "NOT_FOUND", Instant.now(), Map.of());
    }
}
