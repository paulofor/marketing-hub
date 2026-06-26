package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service;

import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.detailStageExecution.GeraAnuncioImagemDetailResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.listStageExecutions.GeraAnuncioImagemExecutionSummaryResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.pending.GeraAnuncioImagemPendingResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebePrompt.GeraAnuncioImagemPromptRequest;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebeResposta.GeraAnuncioImagemRespostaRequest;
import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
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
    private final OprmNicheCandidateRepository cnaeRepository;

    /** Inicializa o service com o repositório canônico de candidatos CNAE. */
    public GeraAnuncioImagemService(ExperimentService experimentService, OprmNicheCandidateRepository cnaeRepository) {
        this.experimentService = experimentService;
        this.cnaeRepository = cnaeRepository;
    }

    /** Inicia uma solicitação da etapa Imagem usando o código/chave operacional do experimento. */
    public GeraAnuncioImagemExecutionSummaryResponse start(String experimentKey) {
        return start(resolveExperimentId(experimentKey));
    }

    /** Inicia uma solicitação da etapa Imagem para o candidato CNAE informado. */
    public GeraAnuncioImagemExecutionSummaryResponse start(Long experimentId) {
        OprmNicheCandidate cnae = cnaeRepository
                .findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CNAE experiment not found"));
        cnae.setGeracaoAnunciosPipelineStatus(STATUS_STARTED);
        cnae.setGeracaoAnunciosCurrentStageCode(STAGE_CODE);
        cnae.setUpdatedAt(Instant.now());
        OprmNicheCandidate saved = cnaeRepository.save(cnae);
        return new GeraAnuncioImagemExecutionSummaryResponse(
                stageExecutionId(saved), saved.getId(), stageJobId(saved), STATUS_STARTED, saved.getUpdatedAt());
    }

    /** Lista execuções da etapa Imagem para relatório operacional. */
    public List<GeraAnuncioImagemExecutionSummaryResponse> listStageExecutions(Long experimentId) {
        Experiment experiment = experimentService.get(experimentId);
        if (experiment.getCreativeGenerationMode() != CreativeGenerationMode.PIPELINE_ADS) {
            return List.of();
        }
        return List.of(toSummary(experiment));
    }

    /** Publica até dez pendências iniciadas da etapa Imagem para consumo do AI Worker. */
    public List<GeraAnuncioImagemPendingResponse> pending() {
        return cnaeRepository.findByGeracaoAnunciosCurrentStageCodeAndGeracaoAnunciosPipelineStatusOrderByUpdatedAtAsc(
                        STAGE_CODE, STATUS_STARTED, PageRequest.of(0, 10))
                .stream()
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

    /** Converte um candidato CNAE iniciado em unidade de trabalho fechada para o AI Worker. */
    private GeraAnuncioImagemPendingResponse toPending(OprmNicheCandidate cnae) {
        return new GeraAnuncioImagemPendingResponse(stageExecutionId(cnae), cnae.getId(), stageJobId(cnae), cnae.getUpdatedAt(), context(cnae),
                previousArtifacts(cnae));
    }

    /** Monta o contexto funcional necessário para geração de anúncio sem consulta adicional. */
    private Map<String, Object> context(Experiment experiment) {
        return Map.of(
                "experimentId", experiment.getId(),
                "adCopy", Objects.toString(experiment.getAdCopy(), ""),
                "adImageBriefing", Objects.toString(experiment.getAdImageBriefing(), ""),
                "creativeGenerationMode", experiment.getCreativeGenerationMode().name());
    }

    /** Monta o contexto funcional do candidato CNAE para geração de anúncio sem consulta adicional. */
    private Map<String, Object> context(OprmNicheCandidate cnae) {
        return Map.of(
                "experimentId", cnae.getId(),
                "cnaeCode", Objects.toString(cnae.getCnaeCode(), ""),
                "cnaeDescription", Objects.toString(cnae.getCnaeDescription(), ""),
                "candidateNicheName", Objects.toString(cnae.getCandidateNicheName(), ""),
                "persona", Objects.toString(cnae.getPersona(), ""),
                "painHypothesis", Objects.toString(cnae.getPainHypothesis(), ""),
                "desiredOutcome", Objects.toString(cnae.getDesiredOutcome(), ""),
                "mechanismHypothesis", Objects.toString(cnae.getMechanismHypothesis(), ""),
                "offerIdea", Objects.toString(cnae.getOfferIdea(), ""),
                "stageCode", Objects.toString(cnae.getGeracaoAnunciosCurrentStageCode(), ""));
    }

    /** Monta artefatos anteriores já persistidos no experimento. */
    private Map<String, Object> previousArtifacts(Experiment experiment) {
        return Map.of(
                "adCopy", Objects.toString(experiment.getAdCopy(), ""),
                "adImageBriefing", Objects.toString(experiment.getAdImageBriefing(), ""));
    }

    /** Monta artefatos anteriores já persistidos no candidato CNAE. */
    private Map<String, Object> previousArtifacts(OprmNicheCandidate cnae) {
        return Map.of(
                "proofDirection", Objects.toString(cnae.getProofDirection(), ""),
                "marketVolumeSignals", Objects.toString(cnae.getMarketVolumeSignals(), ""),
                "sourceArtifacts", Objects.toString(cnae.getSourceArtifacts(), ""));
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

    /** Gera identificador determinístico da execução da etapa a partir do candidato CNAE. */
    private String stageExecutionId(OprmNicheCandidate cnae) {
        return "geracaoanuncios-v1-" + STAGE_CODE + "-cnae-" + cnae.getId();
    }

    /** Gera identificador de job estável para correlação operacional do candidato CNAE. */
    private String stageJobId(OprmNicheCandidate cnae) {
        return "cnae:" + cnae.getId() + "|pipeline:geracaoanuncios|v:1|stage:" + STAGE_CODE;
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
