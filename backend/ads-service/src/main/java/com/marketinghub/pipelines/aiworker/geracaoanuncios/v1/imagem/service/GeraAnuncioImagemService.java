package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service;

import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.detailStageExecution.GeraAnuncioImagemDetailResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.listStageExecutions.GeraAnuncioImagemExecutionSummaryResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.pending.GeraAnuncioImagemPendingResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebePrompt.GeraAnuncioImagemPromptRequest;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebeResposta.GeraAnuncioImagemRespostaRequest;
import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: expor contratos de leitura, escrita e auditoria da etapa Imagem do pipeline GeraAnuncio v2. */
@Service
public class GeraAnuncioImagemService {
    private static final String STAGE_CODE = "imagem";
    private static final String NEXT_STAGE = "fim";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";
    private final ExperimentService experimentService;

    /** Inicializa o service com o serviço canônico de experimentos. */
    public GeraAnuncioImagemService(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    /** Inicia uma solicitação da etapa Imagem usando o código/chave operacional do experimento. */
    public GeraAnuncioImagemExecutionSummaryResponse start(String experimentKey) {
        return start(resolveExperimentId(experimentKey));
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
        Experiment experiment = experimentService.get(experimentId);
        if (experiment.getCreativeGenerationMode() != CreativeGenerationMode.PIPELINE_ADS) {
            return List.of();
        }
        return List.of(toSummary(experiment));
    }

    /** Publica pendências canônicas para consumo do AI Worker. */
    public List<GeraAnuncioImagemPendingResponse> pending() {
        return experimentService.listPendingCreativeGeneration(10).stream()
                .filter(experiment -> experiment.getCreativeGenerationMode() == CreativeGenerationMode.PIPELINE_ADS)
                .map(this::toPending)
                .toList();
    }

    /** Registra o prompt enviado ao modelo para auditoria da etapa. */
    public void recebePrompt(String stageExecutionId, GeraAnuncioImagemPromptRequest request) {}

    /** Registra a resposta do modelo e a saída estruturada retornada pelo worker. */
    public void recebeResposta(String stageExecutionId, GeraAnuncioImagemRespostaRequest request) {}

    /** Busca o detalhe auditável de uma execução da etapa. */
    public GeraAnuncioImagemDetailResponse detailStageExecution(String stageExecutionId) {
        Long experimentId = extractExperimentId(stageExecutionId);
        if (experimentId == null) {
            return new GeraAnuncioImagemDetailResponse(stageExecutionId, null, "NOT_FOUND", Instant.now(), Map.of());
        }
        Experiment experiment = experimentService.get(experimentId);
        return new GeraAnuncioImagemDetailResponse(stageExecutionId, stageJobId(experiment), experiment.getCreativeGenerationStatus().name(),
                Objects.requireNonNullElse(experiment.getCreativeGenerationRequestedAt(), Instant.now()), context(experiment));
    }

    /** Converte o experimento em resumo auditável da etapa. */
    private GeraAnuncioImagemExecutionSummaryResponse toSummary(Experiment experiment) {
        return new GeraAnuncioImagemExecutionSummaryResponse(stageExecutionId(experiment), experiment.getId(), stageJobId(experiment),
                experiment.getCreativeGenerationStatus().name(), experiment.getCreativeGenerationRequestedAt());
    }

    /** Converte uma solicitação de criativos em unidade de trabalho fechada para o AI Worker. */
    private GeraAnuncioImagemPendingResponse toPending(Experiment experiment) {
        return new GeraAnuncioImagemPendingResponse(stageExecutionId(experiment), experiment.getId(), stageJobId(experiment),
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

    /** Resolve o código/chave recebido no contrato administrativo para o identificador interno do experimento. */
    private Long resolveExperimentId(String experimentKey) {
        if (!StringUtils.hasText(experimentKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "experimentKey required");
        }
        String normalizedExperimentKey = experimentKey.trim();
        if (!normalizedExperimentKey.chars().allMatch(Character::isDigit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "experimentKey must be the numeric experiment id");
        }
        return Long.valueOf(normalizedExperimentKey);
    }

    /** Gera identificador determinístico da execução da etapa a partir do experimento. */
    private String stageExecutionId(Experiment experiment) {
        return "geracaoanuncios-v1-" + STAGE_CODE + "-exp-" + experiment.getId();
    }

    /** Gera identificador de job estável para correlação operacional. */
    private String stageJobId(Experiment experiment) {
        return "exp:" + experiment.getId() + "|pipeline:geracaoanuncios|v:1|stage:" + STAGE_CODE;
    }

    /** Extrai o experimento de um identificador determinístico da etapa. */
    private Long extractExperimentId(String stageExecutionId) {
        String prefix = "geracaoanuncios-v1-" + STAGE_CODE + "-exp-";
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
