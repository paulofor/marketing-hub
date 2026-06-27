package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.pipeline.geracaoanuncios.PipelineGeracaoAnuncios;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.detailStageExecution.GeraAnuncioImagemDetailResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.listStageExecutions.GeraAnuncioImagemExecutionSummaryResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.pending.GeraAnuncioImagemPendingResponse;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebePrompt.GeraAnuncioImagemPromptRequest;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebeRequest.GeraAnuncioImagemRecebeRequestRequest;
import com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebeResposta.GeraAnuncioImagemRespostaRequest;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageRepository;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.entity.MoisSalesPage;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.pipeline.geracaoanuncios.PipelineGeracaoAnunciosRepository;
import java.time.Instant;
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

/** Responsabilidade: expor contratos de leitura, escrita e auditoria da etapa Imagem do pipeline GeraAnuncio v2. */
@Service
public class GeraAnuncioImagemService {
    private static final Logger log = LoggerFactory.getLogger(GeraAnuncioImagemService.class);
    private static final String STAGE_CODE = "imagem";
    private static final String NEXT_STAGE = "fim";
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
    public GeraAnuncioImagemService(
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

    /** Inicia uma solicitação da etapa Imagem usando o código/chave operacional do experimento. */
    public GeraAnuncioImagemExecutionSummaryResponse start(String experimentKey) {
        return start(resolveExperimentId(experimentKey));
    }

    /** Inicia uma solicitação da etapa Imagem para o candidato CNAE informado e cria o jobId UUID da execução. */
    @Transactional
    public GeraAnuncioImagemExecutionSummaryResponse start(Long experimentId) {
        OprmNicheCandidate cnae = cnaeRepository
                .findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CNAE experiment not found"));
        cnae.setGeracaoAnunciosPipelineStatus(STATUS_STARTED);
        cnae.setGeracaoAnunciosCurrentStageCode(STAGE_CODE);
        Instant now = Instant.now();
        cnae.setUpdatedAt(now);
        OprmNicheCandidate saved = cnaeRepository.save(cnae);
        String jobId = UUID.randomUUID().toString();
        registrarInicioPipeline(saved, jobId, now);
        return new GeraAnuncioImagemExecutionSummaryResponse(
                stageExecutionId(saved), saved.getId(), jobId, STATUS_STARTED, saved.getUpdatedAt());
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

    /** Recebe e audita o request operacional enviado para a etapa Imagem. */
    @Transactional
    public void recebeRequest(String experimentKey, String jobId, GeraAnuncioImagemRecebeRequestRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload required");
        }
        if (!StringUtils.hasText(jobId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jobId required");
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
        pipeline.setJobId(jobId.trim());
        pipeline.setPlataforma(request.plataforma());
        pipeline.setPrompt(request.prompt());
        pipeline.setSchema(request.schema());
        pipeline.setVersaoPipeline(PIPELINE_VERSION);
        pipelineRepository.save(pipeline);
    }

    /** Registra o prompt enviado ao modelo para auditoria da etapa. */
    public void recebePrompt(String stageExecutionId, GeraAnuncioImagemPromptRequest request) {}

    /** Registra a resposta do modelo e a saída estruturada retornada pelo worker. */
    public void recebeResposta(String stageExecutionId, GeraAnuncioImagemRespostaRequest request) {}

    /** Recebe o callback final da etapa, atualiza o experimento e audita a resposta do AI Worker. */
    @Transactional
    public String recebeResponse(String experimentKey, String jobId, GeraAnuncioImagemRespostaRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload required");
        }
        if (!StringUtils.hasText(jobId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jobId required");
        }
        Instant now = Instant.now();
        String descricaoErro = resolveDescricaoErro(request);
        MoisSalesPage salesPage = salesPageRepository
                .findById(resolveExperimentId(experimentKey))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sales page not found"));
        salesPage.setStatusPipelineGeracaoAnuncios(StringUtils.hasText(descricaoErro) ? STATUS_FAILED : STATUS_COMPLETED);
        salesPage.setDataPipelineGeracaoAnuncios(now);
        salesPageRepository.save(salesPage);

        PipelineGeracaoAnuncios pipeline = new PipelineGeracaoAnuncios();
        pipeline.setIdExterno(experimentKey);
        pipeline.setResponse(serializeRequest(resolveResponsePayload(request)));
        pipeline.setCodigoEtapa(STAGE_CODE);
        pipeline.setDataHora(now);
        pipeline.setVersaoPipeline(PIPELINE_VERSION);
        pipeline.setJobId(jobId.trim());
        pipeline.setQuantidadeTokenEntrada(request.quantidadeTokenEntrada());
        pipeline.setQuantidadeTokenSaida(request.quantidadeTokenSaida());
        pipeline.setCusto(request.custo());
        pipeline.setModelo(request.modelo());
        pipeline.setDescricaoErro(descricaoErro);
        pipelineRepository.save(pipeline);

        return StringUtils.hasText(descricaoErro) ? null : resolveNextStageCode();
    }

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
        return new GeraAnuncioImagemPendingResponse(stageExecutionId(cnae), cnae.getId(), resolveJobId(cnae), cnae.getUpdatedAt(), context(cnae),
                previousArtifacts(cnae));
    }

    /** Registra a criação da execução com o jobId UUID gerado pelo endpoint start. */
    private void registrarInicioPipeline(OprmNicheCandidate cnae, String jobId, Instant now) {
        PipelineGeracaoAnuncios pipeline = new PipelineGeracaoAnuncios();
        pipeline.setIdExterno(String.valueOf(cnae.getId()));
        pipeline.setCodigoEtapa(STAGE_CODE);
        pipeline.setDataHora(now);
        pipeline.setJobId(jobId);
        pipeline.setVersaoPipeline(PIPELINE_VERSION);
        pipelineRepository.save(pipeline);
    }

    /** Resolve o jobId UUID criado no start para envio junto com a pendência ao AI Worker. */
    private String resolveJobId(OprmNicheCandidate cnae) {
        return pipelineRepository.findTopByIdExternoAndCodigoEtapaOrderByDataHoraDesc(String.valueOf(cnae.getId()), STAGE_CODE)
                .map(PipelineGeracaoAnuncios::getJobId)
                .orElseGet(() -> stageJobId(cnae));
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

    /** Resolve a descrição de erro recebida pelo worker para decidir sucesso ou falha da etapa. */
    private String resolveDescricaoErro(GeraAnuncioImagemRespostaRequest request) {
        if (StringUtils.hasText(request.descricaoErro())) {
            return request.descricaoErro().trim();
        }
        return StringUtils.hasText(request.error()) ? request.error().trim() : null;
    }

    /** Resolve o payload de resposta que deve ser auditado no histórico do pipeline. */
    private Object resolveResponsePayload(GeraAnuncioImagemRespostaRequest request) {
        if (request.response() != null) {
            return request.response();
        }
        if (request.responsePayload() != null) {
            return request.responsePayload();
        }
        if (request.structuredOutput() != null) {
            return request.structuredOutput();
        }
        return request;
    }

    /** Retorna a próxima etapa funcional ou nulo quando a etapa atual finaliza o pipeline. */
    private String resolveNextStageCode() {
        return "fim".equals(NEXT_STAGE) ? null : NEXT_STAGE;
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
