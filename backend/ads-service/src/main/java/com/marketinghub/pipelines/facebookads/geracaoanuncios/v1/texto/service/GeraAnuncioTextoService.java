package com.marketinghub.pipelines.facebookads.geracaoanuncios.v1.texto.service;

import com.marketinghub.pipelines.facebookads.geracaoanuncios.v1.texto.service.detailStageExecution.GeraAnuncioTextoDetailResponse;
import com.marketinghub.pipelines.facebookads.geracaoanuncios.v1.texto.service.listStageExecutions.GeraAnuncioTextoExecutionSummaryResponse;
import com.marketinghub.pipelines.facebookads.geracaoanuncios.v1.texto.service.pending.GeraAnuncioTextoPendingResponse;
import com.marketinghub.pipelines.facebookads.geracaoanuncios.v1.texto.service.recebePrompt.GeraAnuncioTextoPromptRequest;
import com.marketinghub.pipelines.facebookads.geracaoanuncios.v1.texto.service.recebeResposta.GeraAnuncioTextoRespostaRequest;
import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        Experiment experiment = experimentService.get(experimentId);
        if (experiment.getCreativeGenerationMode() != CreativeGenerationMode.PIPELINE_ADS) {
            return List.of();
        }
        return List.of(toSummary(experiment));
    }

    /** Publica pendências canônicas para consumo do AI Worker. */
    public List<GeraAnuncioTextoPendingResponse> pending() {
        return experimentService.listPendingCreativeGeneration(10).stream()
                .filter(experiment -> experiment.getCreativeGenerationMode() == CreativeGenerationMode.PIPELINE_ADS)
                .map(this::toPending)
                .toList();
    }

    /** Registra o prompt enviado ao modelo para auditoria da etapa. */
    public void recebePrompt(String stageExecutionId, GeraAnuncioTextoPromptRequest request) {}

    /** Registra a resposta do modelo e a saída estruturada retornada pelo worker. */
    public void recebeResposta(String stageExecutionId, GeraAnuncioTextoRespostaRequest request) {}

    /** Busca o detalhe auditável de uma execução da etapa. */
    public GeraAnuncioTextoDetailResponse detailStageExecution(String stageExecutionId) {
        Long experimentId = extractExperimentId(stageExecutionId);
        if (experimentId == null) {
            return new GeraAnuncioTextoDetailResponse(stageExecutionId, null, "NOT_FOUND", Instant.now(), Map.of());
        }
        Experiment experiment = experimentService.get(experimentId);
        return new GeraAnuncioTextoDetailResponse(stageExecutionId, stageJobId(experiment), experiment.getCreativeGenerationStatus().name(),
                Objects.requireNonNullElse(experiment.getCreativeGenerationRequestedAt(), Instant.now()), context(experiment));
    }

    /** Converte o experimento em resumo auditável da etapa. */
    private GeraAnuncioTextoExecutionSummaryResponse toSummary(Experiment experiment) {
        return new GeraAnuncioTextoExecutionSummaryResponse(stageExecutionId(experiment), experiment.getId(), stageJobId(experiment),
                experiment.getCreativeGenerationStatus().name(), experiment.getCreativeGenerationRequestedAt());
    }

    /** Converte uma solicitação de criativos em unidade de trabalho fechada para o AI Worker. */
    private GeraAnuncioTextoPendingResponse toPending(Experiment experiment) {
        return new GeraAnuncioTextoPendingResponse(stageExecutionId(experiment), experiment.getId(), stageJobId(experiment),
                experiment.getCreativeGenerationRequestedAt(), context(experiment), previousArtifacts(experiment));
    }

    /** Monta o contexto funcional necessário para geração de anúncio sem consulta adicional. */
    private Map<String, Object> context(Experiment experiment) {
        return Map.of(
                "experimentId", experiment.getId(),
                "adCopy", Objects.toString(experiment.getAdCopy(), ""),
                "adImageBriefing", Objects.toString(experiment.getAdImageBriefing(), ""),
                "creativeGenerationMode", experiment.getCreativeGenerationMode().name());
    }

    /** Monta artefatos anteriores já persistidos no experimento. */
    private Map<String, Object> previousArtifacts(Experiment experiment) {
        return Map.of(
                "adCopy", Objects.toString(experiment.getAdCopy(), ""),
                "adImageBriefing", Objects.toString(experiment.getAdImageBriefing(), ""));
    }

    /** Gera identificador determinístico da execução da etapa a partir do experimento. */
    private String stageExecutionId(Experiment experiment) {
        return "geracaoanuncios-v1-texto-exp-" + experiment.getId();
    }

    /** Gera identificador de job estável para correlação operacional. */
    private String stageJobId(Experiment experiment) {
        return "exp:" + experiment.getId() + "|pipeline:geracaoanuncios|v:1|stage:texto";
    }

    /** Extrai o experimento de um identificador determinístico da etapa. */
    private Long extractExperimentId(String stageExecutionId) {
        String prefix = "geracaoanuncios-v1-texto-exp-";
        if (stageExecutionId == null || !stageExecutionId.startsWith(prefix)) {
            return null;
        }
        try {
            return Long.valueOf(stageExecutionId.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
