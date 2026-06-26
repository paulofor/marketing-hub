package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.pipeline.geracaoanuncios.PipelineGeracaoAnuncios;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.detailStageExecution.GeraAnuncioTextoDetailResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.listStageExecutions.GeraAnuncioTextoExecutionSummaryResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.pending.GeraAnuncioTextoPendingResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.recebePrompt.GeraAnuncioTextoPromptRequest;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.recebeRequest.GeraAnuncioTextoRecebeRequestRequest;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.recebeResposta.GeraAnuncioTextoRespostaRequest;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageRepository;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.entity.MoisSalesPage;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.pipeline.geracaoanuncios.PipelineGeracaoAnunciosRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: expor contratos de leitura, escrita e auditoria da etapa Texto do pipeline GeraAnuncio v2. */
@Service
public class GeraAnuncioTextoService {
    private static final Logger log = LoggerFactory.getLogger(GeraAnuncioTextoService.class);
    private static final String STAGE_CODE = "texto";
    private static final String NEXT_STAGE = "imagem";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_AGUARDANDO_MODULO = "AGUARDANDO_MODULO";
    private static final String PIPELINE_VERSION = "v1";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";
    private final ExperimentService experimentService;
    private final OprmNicheCandidateRepository cnaeRepository;
    private final MoisSalesPageRepository salesPageRepository;
    private final PipelineGeracaoAnunciosRepository pipelineRepository;
    private final ObjectMapper objectMapper;

    /** Inicializa o service com os repositórios canônicos de controle e auditoria. */
    public GeraAnuncioTextoService(
            ExperimentService experimentService,
            OprmNicheCandidateRepository cnaeRepository,
            MoisSalesPageRepository salesPageRepository,
            PipelineGeracaoAnunciosRepository pipelineRepository,
            ObjectMapper objectMapper) {
        this.experimentService = experimentService;
        this.cnaeRepository = cnaeRepository;
        this.salesPageRepository = salesPageRepository;
        this.pipelineRepository = pipelineRepository;
        this.objectMapper = objectMapper;
    }

    /** Inicia uma solicitação da etapa Texto usando o código/chave operacional do experimento. */
    public GeraAnuncioTextoExecutionSummaryResponse start(String experimentKey) {
        return start(resolveExperimentId(experimentKey));
    }

    /** Inicia uma solicitação da etapa Texto para o candidato CNAE informado. */
    public GeraAnuncioTextoExecutionSummaryResponse start(Long experimentId) {
        OprmNicheCandidate cnae = cnaeRepository
                .findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CNAE experiment not found"));
        cnae.setGeracaoAnunciosPipelineStatus(STATUS_STARTED);
        cnae.setGeracaoAnunciosCurrentStageCode(STAGE_CODE);
        cnae.setUpdatedAt(Instant.now());
        OprmNicheCandidate saved = cnaeRepository.save(cnae);
        return new GeraAnuncioTextoExecutionSummaryResponse(
                stageExecutionId(saved), saved.getId(), stageJobId(saved), STATUS_STARTED, saved.getUpdatedAt());
    }

    /** Lista execuções da etapa Texto para relatório operacional. */
    public List<GeraAnuncioTextoExecutionSummaryResponse> listStageExecutions(Long experimentId) {
        Experiment experiment = experimentService.get(experimentId);
        if (experiment.getCreativeGenerationMode() != CreativeGenerationMode.PIPELINE_ADS) {
            return List.of();
        }
        return List.of(toSummary(experiment));
    }

    /** Publica até dez pendências iniciadas da etapa Texto para consumo do AI Worker. */
    public List<GeraAnuncioTextoPendingResponse> pending() {
        return cnaeRepository.findByGeracaoAnunciosCurrentStageCodeAndGeracaoAnunciosPipelineStatusOrderByUpdatedAtAsc(
                        STAGE_CODE, STATUS_STARTED, PageRequest.of(0, 10))
                .stream()
                .map(this::toPending)
                .toList();
    }

    /** Recebe e audita o request operacional enviado para a etapa Texto. */
    @Transactional
    public void recebeRequest(String experimentKey, GeraAnuncioTextoRecebeRequestRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload required");
        }
        Instant now = Instant.now();
        MoisSalesPage salesPage = salesPageRepository
                .findById(resolveExperimentId(experimentKey))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sales page not found"));
        salesPage.setStatusPipelineGeracaoAnuncios(STATUS_AGUARDANDO_MODULO);
        salesPage.setDataPipelineGeracaoAnuncios(now);
        salesPageRepository.save(salesPage);

        PipelineGeracaoAnuncios pipeline = new PipelineGeracaoAnuncios();
        pipeline.setIdExterno(experimentKey);
        pipeline.setRequest(serializeRequest(request.request()));
        pipeline.setCodigoEtapa(STAGE_CODE);
        pipeline.setDataHora(now);
        pipeline.setJobId(createJobHash(experimentKey, now));
        pipeline.setPlataforma(request.plataforma());
        pipeline.setPrompt(request.prompt());
        pipeline.setSchema(request.schema());
        pipeline.setVersaoPipeline(PIPELINE_VERSION);
        pipelineRepository.save(pipeline);
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

    /** Converte um candidato CNAE iniciado em unidade de trabalho fechada para o AI Worker. */
    private GeraAnuncioTextoPendingResponse toPending(OprmNicheCandidate cnae) {
        return new GeraAnuncioTextoPendingResponse(stageExecutionId(cnae), cnae.getId(), stageJobId(cnae), cnae.getUpdatedAt(), context(cnae),
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

    /** Serializa o request recebido preservando payload estruturado quando existir. */
    private String serializeRequest(Object request) {
        if (request == null) {
            return null;
        }
        if (request instanceof String requestText) {
            return requestText;
        }
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao serializar request do pipeline geracaoanuncios; etapa={}", STAGE_CODE, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request payload invalid", ex);
        }
    }

    /** Cria um hash único para rastrear o job recebido nesta chamada. */
    private String createJobHash(String experimentKey, Instant now) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((experimentKey + "|" + STAGE_CODE + "|" + now + "|" + UUID.randomUUID())
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            log.error("Falha ao gerar hash jobId do pipeline geracaoanuncios; etapa={}, experimentKey={}", STAGE_CODE, experimentKey, ex);
            throw new IllegalStateException("SHA-256 indisponível para gerar jobId", ex);
        }
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
