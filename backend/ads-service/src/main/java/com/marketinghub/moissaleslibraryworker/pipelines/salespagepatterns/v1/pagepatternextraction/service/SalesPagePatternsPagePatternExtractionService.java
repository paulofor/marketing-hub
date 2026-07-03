package com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.pending.SalesPagePatternsPagePatternExtractionPendingJob;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.pending.SalesPagePatternsPagePatternExtractionPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.pending.SalesPagePatternsPagePatternExtractionPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberequest.SalesPagePatternsPagePatternExtractionRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberequest.SalesPagePatternsPagePatternExtractionRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberesponse.SalesPagePatternsPagePatternExtractionRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberesponse.SalesPagePatternsPagePatternExtractionRecebeResponseResponse;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.DossierProductContextGateway;
import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/** Publica pendências e callbacks da extração de padrões do pipeline salespagepatterns.v1. */
@Service
@Slf4j
public class SalesPagePatternsPagePatternExtractionService {
    private static final String PIPELINE_CODE = "salespagepatterns.v1";
    private static final String STAGE_CODE = "page-pattern-extraction";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    private final MoisSalesPageRepository salesPageRepository;
    private final PipelineDossieProdutoRepository pipelineDossieProdutoRepository;
    private final DossierProductContextGateway productContextGateway;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Cria o service com os repositórios canônicos de página e auditoria do pipeline. */
    public SalesPagePatternsPagePatternExtractionService(
            MoisSalesPageRepository salesPageRepository,
            PipelineDossieProdutoRepository pipelineDossieProdutoRepository,
            DossierProductContextGateway productContextGateway,
            JdbcTemplate jdbcTemplate) {
        this.salesPageRepository = salesPageRepository;
        this.pipelineDossieProdutoRepository = pipelineDossieProdutoRepository;
        this.productContextGateway = productContextGateway;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Inicia manualmente a extração de padrões para uma página de venda. */
    public void start(String productKey) {
        long pageId = Long.parseLong(productKey);
        Instant now = Instant.now();
        String jobId = UUID.randomUUID().toString();
        var page = salesPageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Página/produto MOIS não encontrada: " + productKey));
        page.setSalesPagePatternsStatus(STATUS_STARTED);
        page.setSalesPagePatternsCurrentStage(STAGE_CODE);
        page.setSalesPagePatternsUpdatedAt(now);
        salesPageRepository.save(page);
        savePipelineAudit(productKey, jobId, STAGE_CODE, STATUS_STARTED, null, null, null, null, null, null, null, null);
    }

    /** Recebe o request bruto do worker e coloca a etapa em estado de espera por resposta. */
    public SalesPagePatternsPagePatternExtractionRecebeRequestResponse recebeRequest(
            String productKey,
            String jobId,
            SalesPagePatternsPagePatternExtractionRecebeRequestRequest request) {
        long pageId = Long.parseLong(productKey);
        Instant now = Instant.now();
        var page = salesPageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Página/produto MOIS não encontrada: " + productKey));
        page.setSalesPagePatternsStatus(STATUS_WAITING);
        page.setSalesPagePatternsCurrentStage(STAGE_CODE);
        page.setSalesPagePatternsUpdatedAt(now);
        salesPageRepository.save(page);
        savePipelineAudit(productKey, jobId, STAGE_CODE, STATUS_WAITING, request.request(), null, null,
                request.plataforma(), request.prompt(), request.schema(), null, null);
        return new SalesPagePatternsPagePatternExtractionRecebeRequestResponse(jobId, productKey, STAGE_CODE, STATUS_WAITING);
    }

    /** Recebe a resposta do worker, persiste o resultado funcional e fecha a etapa. */
    public SalesPagePatternsPagePatternExtractionRecebeResponseResponse recebeResponse(
            String productKey,
            String jobId,
            SalesPagePatternsPagePatternExtractionRecebeResponseRequest request) {
        long pageId = Long.parseLong(productKey);
        Instant now = Instant.now();
        String status = isBlank(request.descricaoErro()) ? STATUS_COMPLETED : STATUS_FAILED;
        var page = salesPageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Página/produto MOIS não encontrada: " + productKey));
        page.setSalesPagePatternsStatus(status);
        page.setSalesPagePatternsCurrentStage(STAGE_CODE);
        page.setSalesPagePatternsUpdatedAt(now);
        salesPageRepository.save(page);
        savePipelineAudit(productKey, jobId, STAGE_CODE, status, null, request.response(),
                extractOpenAiTextResponse(request.response()), null, null, null, request, request.descricaoErro());
        accumulateCosts(pageId, request.custo());
        return new SalesPagePatternsPagePatternExtractionRecebeResponseResponse(jobId, productKey, STAGE_CODE, status, null);
    }

    /** Entrega páginas iniciadas em salespagepatterns.v1 para o worker externo processar. */
    public SalesPagePatternsPagePatternExtractionPendingResponse pending(
            SalesPagePatternsPagePatternExtractionPendingRequest request) {
        List<SalesPagePatternsPagePatternExtractionPendingJob> jobs = salesPageRepository
                .findTop10BySalesPagePatternsStatusAndSalesPagePatternsCurrentStageOrderBySalesPagePatternsUpdatedAtAscIdAsc(
                        STATUS_STARTED, STAGE_CODE)
                .stream()
                .map(page -> pendingJob(page.getId()))
                .toList();
        return new SalesPagePatternsPagePatternExtractionPendingResponse(!jobs.isEmpty(), jobs);
    }

    /** Monta o contrato pending com contexto rico já coletado e analisado da página. */
    private SalesPagePatternsPagePatternExtractionPendingJob pendingJob(long pageId) {
        String productKey = String.valueOf(pageId);
        String jobId = resolveJobId(productKey);
        Map<String, Object> input = new LinkedHashMap<>(productContextGateway.findContext(pageId)
                .map(DossierProductContextGateway.DossierProductContext::toInputMap)
                .orElseGet(Map::of));
        input.put("jobId", jobId);
        input.put("productKey", productKey);
        input.put("pageId", pageId);
        input.put("pipelineCode", PIPELINE_CODE);
        input.put("stageCode", STAGE_CODE);
        input.put("status", STATUS_STARTED);
        return new SalesPagePatternsPagePatternExtractionPendingJob(
                jobId,
                pageId,
                pageId,
                "mois-sales-page-" + pageId,
                STAGE_CODE,
                input);
    }

    /** Recupera ou cria jobId para pendências antigas não travarem a fila. */
    private String resolveJobId(String productKey) {
        return pipelineDossieProdutoRepository
                .findTopByIdExternoAndCodigoEtapaOrderByDataHoraDescIdDesc(productKey, STAGE_CODE)
                .map(PipelineDossieProduto::getJobId)
                .filter(jobId -> jobId != null && !jobId.isBlank())
                .orElseGet(() -> rebuildJobId(productKey));
    }

    /** Cria auditoria mínima quando o enfileiramento não deixou jobId recuperável. */
    private String rebuildJobId(String productKey) {
        String jobId = UUID.randomUUID().toString();
        savePipelineAudit(productKey, jobId, STAGE_CODE, STATUS_STARTED, null, null, null, null, null, null, null, null);
        return jobId;
    }

    /** Persiste uma linha de auditoria do pipeline mantendo o pipeline_code específico. */
    private void savePipelineAudit(
            String productKey,
            String jobId,
            String stageCode,
            String status,
            String request,
            String response,
            String finalResponse,
            String platform,
            String prompt,
            String schema,
            SalesPagePatternsPagePatternExtractionRecebeResponseRequest responseRequest,
            String errorDescription) {
        PipelineDossieProduto pipeline = new PipelineDossieProduto();
        pipeline.setIdExterno(productKey);
        pipeline.setCodigoEtapa(stageCode);
        pipeline.setStatus(status);
        pipeline.setDataHora(Instant.now());
        pipeline.setJobId(jobId);
        pipeline.setVersaoPipeline("v1");
        pipeline.setPipelineCode(PIPELINE_CODE);
        pipeline.setRequest(request);
        pipeline.setResponse(response);
        pipeline.setRespostaFinal(finalResponse);
        pipeline.setPlataforma(platform);
        pipeline.setPrompt(prompt);
        pipeline.setSchema(schema);
        pipeline.setDescricaoErro(errorDescription);
        if (responseRequest != null) {
            pipeline.setQuantidadeTokenEntrada(toLong(responseRequest.quantidadeTokenEntrada()));
            pipeline.setQuantidadeTokenSaida(toLong(responseRequest.quantidadeTokenSaida()));
            pipeline.setCusto(responseRequest.custo());
            pipeline.setModelo(responseRequest.modelo());
        }
        pipelineDossieProdutoRepository.save(pipeline);
    }

    /** Converte contador de tokens recebido do worker para o tipo persistido na auditoria. */
    private Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }

    /** Acumula custo OpenAI no total individual da página e no total da biblioteca. */
    private void accumulateCosts(long pageId, BigDecimal costUsd) {
        if (costUsd == null || costUsd.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE mois_sales_page
                   SET total_model_cost_usd = COALESCE(total_model_cost_usd, 0) + ?,
                       updated_at = UTC_TIMESTAMP()
                 WHERE id = ?
                """, costUsd, pageId);
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_cost_total (workspace_id, total_cost_usd, updated_at)
                SELECT workspace_id, ?, UTC_TIMESTAMP(6)
                FROM mois_sales_page
                WHERE id = ?
                ON DUPLICATE KEY UPDATE
                    total_cost_usd = total_cost_usd + VALUES(total_cost_usd),
                    updated_at = VALUES(updated_at)
                """, costUsd, pageId);
    }

    /** Verifica se o texto está vazio para decidir sucesso ou falha. */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Extrai o texto funcional de respostas OpenAI sem depender do pipeline de aquecimento. */
    private String extractOpenAiTextResponse(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode outputText = root.path("output_text");
            if (!outputText.isMissingNode() && !outputText.asText().isBlank()) {
                return outputText.asText();
            }
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao extrair output_text de salespagepatterns.v1; mantendo response bruto. stageCode={}, responseLength={}",
                    STAGE_CODE, response.length(), ex);
            return response;
        }
        return response;
    }
}
