package com.marketinghub.geraanuncio.v2.imagem.service;

import com.marketinghub.geraanuncio.v2.imagem.service.detailStageExecution.GeraAnuncioImagemDetailResponse;
import com.marketinghub.geraanuncio.v2.imagem.service.listStageExecutions.GeraAnuncioImagemExecutionSummaryResponse;
import com.marketinghub.geraanuncio.v2.imagem.service.pending.GeraAnuncioImagemPendingResponse;
import com.marketinghub.geraanuncio.v2.imagem.service.recebePrompt.GeraAnuncioImagemPromptRequest;
import com.marketinghub.geraanuncio.v2.imagem.service.recebeResposta.GeraAnuncioImagemRespostaRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Responsabilidade: expor contratos de leitura, escrita e auditoria da etapa Imagem do pipeline GeraAnuncio v2. */
@Service
public class GeraAnuncioImagemService {
    private final ExperimentService experimentService;

    /** Inicializa o service com o serviço canônico de experimentos. */
    public GeraAnuncioImagemService(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    /** Inicia uma solicitação da etapa Imagem para um experimento. */
    public GeraAnuncioImagemExecutionSummaryResponse start(Long experimentId) {
        Experiment experiment = experimentService.requestPipelineCreatives(experimentId);
        return new GeraAnuncioImagemExecutionSummaryResponse(
                null,
                experiment.getId(),
                null,
                experiment.getCreativeGenerationStatus().name(),
                experiment.getCreativeGenerationRequestedAt());
    }

    /** Lista execuções da etapa Imagem para relatório operacional. */
    public List<GeraAnuncioImagemExecutionSummaryResponse> listStageExecutions(Long experimentId) {
        return List.of();
    }

    /** Publica pendências canônicas para consumo do AI Worker. */
    public List<GeraAnuncioImagemPendingResponse> pending() {
        return List.of();
    }

    /** Registra o prompt enviado ao modelo para auditoria da etapa. */
    public void recebePrompt(String stageExecutionId, GeraAnuncioImagemPromptRequest request) {}

    /** Registra a resposta do modelo e a saída estruturada retornada pelo worker. */
    public void recebeResposta(String stageExecutionId, GeraAnuncioImagemRespostaRequest request) {}

    /** Busca o detalhe auditável de uma execução da etapa. */
    public GeraAnuncioImagemDetailResponse detailStageExecution(String stageExecutionId) {
        return new GeraAnuncioImagemDetailResponse(stageExecutionId, null, "NOT_FOUND", Instant.now(), Map.of());
    }
}
