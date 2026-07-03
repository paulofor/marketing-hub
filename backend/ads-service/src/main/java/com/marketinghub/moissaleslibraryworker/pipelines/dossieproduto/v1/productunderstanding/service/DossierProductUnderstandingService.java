package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.shared.DossierOpenAiTextResponseExtractor;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingJob;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberequest.DossierProductUnderstandingRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberequest.DossierProductUnderstandingRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberesponse.DossierProductUnderstandingRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberesponse.DossierProductUnderstandingRecebeResponseResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.shared.service.PipelinePromptSchemaTemplatePayload;
import com.marketinghub.repository.jpa.aiprompt.AiPromptSchemaTemplateRepository;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.DossierProductContextGateway;
import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa entendimento do produto do pipeline de dossiê MOIS v1. */
@Service
public class DossierProductUnderstandingService {
    private static final String PIPELINE_CODE = "warmupecosystem.v1";
    private final MoisSalesPageRepository salesPageRepository;
    private final PipelineDossieProdutoRepository pipelineDossieProdutoRepository;
    private final DossierProductContextGateway productContextGateway;
    private final AiPromptSchemaTemplateRepository templateRepository;
    private final JdbcTemplate jdbcTemplate;

    /** Cria o service da etapa com acesso aos repositórios canônicos da página/produto e da auditoria do pipeline. */
    public DossierProductUnderstandingService(
            MoisSalesPageRepository salesPageRepository,
            PipelineDossieProdutoRepository pipelineDossieProdutoRepository,
            DossierProductContextGateway productContextGateway,
            AiPromptSchemaTemplateRepository templateRepository,
            JdbcTemplate jdbcTemplate) {
        this.salesPageRepository = salesPageRepository;
        this.pipelineDossieProdutoRepository = pipelineDossieProdutoRepository;
        this.productContextGateway = productContextGateway;
        this.templateRepository = templateRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String STAGE_CODE = "product-understanding";
    private static final String NEXT_STAGE = "investigation-anchor-builder";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    /** Marca a página/produto como iniciado no dossiê, cria o jobId UUID e posiciona a etapa atual. */
    public void start(String productKey) {
        long pageId = Long.parseLong(productKey);
        Instant now = Instant.now();
        String jobId = UUID.randomUUID().toString();
        var page = salesPageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Página/produto MOIS não encontrada: " + productKey));
        page.setDossieProdutoStatus(STATUS_STARTED);
        page.setDossieProdutoCurrentStage(STAGE_CODE);
        page.setDossieProdutoUpdatedAt(now);
        salesPageRepository.save(page);

        PipelineDossieProduto pipeline = new PipelineDossieProduto();
        pipeline.setIdExterno(productKey);
        pipeline.setCodigoEtapa(STAGE_CODE);
        pipeline.setStatus(STATUS_STARTED);
        pipeline.setDataHora(now);
        pipeline.setJobId(jobId);
        pipeline.setVersaoPipeline("v1");
        pipeline.setPipelineCode(PIPELINE_CODE);
        pipelineDossieProdutoRepository.save(pipeline);
    }


    /** Recebe o request operacional da etapa, coloca a página em espera do módulo e audita a entrada do pipeline. */
    public DossierProductUnderstandingRecebeRequestResponse recebeRequest(String productKey, String jobId, DossierProductUnderstandingRecebeRequestRequest request) {
        long pageId = Long.parseLong(productKey);
        Instant now = Instant.now();
        var page = salesPageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Página/produto MOIS não encontrada: " + productKey));
        page.setDossieProdutoStatus(STATUS_WAITING);
        page.setDossieProdutoCurrentStage(STAGE_CODE);
        page.setDossieProdutoUpdatedAt(now);
        salesPageRepository.save(page);

        PipelineDossieProduto pipeline = new PipelineDossieProduto();
        pipeline.setIdExterno(productKey);
        pipeline.setRequest(request.request());
        pipeline.setCodigoEtapa(STAGE_CODE);
        pipeline.setStatus(STATUS_WAITING);
        pipeline.setDataHora(now);
        pipeline.setJobId(jobId);
        pipeline.setPlataforma(request.plataforma());
        pipeline.setPrompt(request.prompt());
        pipeline.setSchema(request.schema());
        pipeline.setVersaoPipeline("v1");
        pipeline.setPipelineCode(PIPELINE_CODE);
        pipeline.setPromptTemplateKey(request.promptTemplateKey());
        pipeline.setPromptTemplateVersion(request.promptTemplateVersion());
        pipeline.setSchemaName(request.schemaName());
        pipelineDossieProdutoRepository.save(pipeline);

        return new DossierProductUnderstandingRecebeRequestResponse(jobId, productKey, STAGE_CODE, STATUS_WAITING);
    }


    /** Recebe a resposta operacional da etapa, avança sucesso para a próxima etapa e audita a saída do pipeline. */
    public DossierProductUnderstandingRecebeResponseResponse recebeResponse(String productKey, String jobId, DossierProductUnderstandingRecebeResponseRequest request) {
        long pageId = Long.parseLong(productKey);
        Instant now = Instant.now();
        String status = isBlank(request.descricaoErro()) ? STATUS_COMPLETED : STATUS_FAILED;
        var page = salesPageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Página/produto MOIS não encontrada: " + productKey));
        String nextStageCode = STATUS_COMPLETED.equals(status) ? normalizeNextStage(NEXT_STAGE) : null;
        if (nextStageCode == null) {
            page.setDossieProdutoStatus(status);
            page.setDossieProdutoCurrentStage(STAGE_CODE);
        } else {
            page.setDossieProdutoStatus(STATUS_STARTED);
            page.setDossieProdutoCurrentStage(nextStageCode);
        }
        page.setDossieProdutoUpdatedAt(now);
        salesPageRepository.save(page);

        PipelineDossieProduto pipeline = new PipelineDossieProduto();
        pipeline.setIdExterno(productKey);
        pipeline.setResponse(request.response());
        pipeline.setRespostaFinal(DossierOpenAiTextResponseExtractor.extract(request.response()));
        pipeline.setCodigoEtapa(STAGE_CODE);
        pipeline.setStatus(status);
        pipeline.setDataHora(now);
        pipeline.setJobId(jobId);
        pipeline.setQuantidadeTokenEntrada(request.quantidadeTokenEntrada());
        pipeline.setQuantidadeTokenSaida(request.quantidadeTokenSaida());
        pipeline.setCusto(request.custo());
        pipeline.setModelo(request.modelo());
        pipeline.setDescricaoErro(request.descricaoErro());
        pipeline.setVersaoPipeline("v1");
        pipeline.setPipelineCode(PIPELINE_CODE);
        pipeline.setPromptTemplateKey(request.promptTemplateKey());
        pipeline.setPromptTemplateVersion(request.promptTemplateVersion());
        pipeline.setSchemaName(request.schemaName());
        pipelineDossieProdutoRepository.save(pipeline);
        accumulateCosts(pageId, request.custo());

        return new DossierProductUnderstandingRecebeResponseResponse(jobId, productKey, STAGE_CODE, status, nextStageCode);
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

    /** Normaliza a próxima etapa para nulo quando a etapa atual encerra o pipeline. */
    private String normalizeNextStage(String nextStage) {
        return isBlank(nextStage) ? null : nextStage;
    }

    /** Verifica se o texto informado está vazio para decidir sucesso ou falha da etapa. */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Entrega até dez trabalhos iniciados da etapa atual ao executor, ordenados pela data operacional mais antiga. */
    public DossierProductUnderstandingPendingResponse pending(DossierProductUnderstandingPendingRequest request) {
        List<DossierProductUnderstandingPendingJob> jobs = salesPageRepository
                .findTop10ByDossieProdutoStatusAndDossieProdutoCurrentStageOrderByDossieProdutoUpdatedAtAscIdAsc(
                        STATUS_STARTED, STAGE_CODE)
                .stream()
                .map(page -> {
                    String jobId = resolveJobId(String.valueOf(page.getId()));
                    Map<String, Object> input = new java.util.LinkedHashMap<>(productContextGateway.findContext(page.getId())
                            .map(DossierProductContextGateway.DossierProductContext::toInputMap)
                            .orElseGet(Map::of));
                    input.put("jobId", jobId);
                    input.put("productKey", String.valueOf(page.getId()));
                    input.put("pageId", page.getId());
                    input.put("stageCode", STAGE_CODE);
                    input.put("status", STATUS_STARTED);
                    input.put("nextStageCode", NEXT_STAGE);
                    return new DossierProductUnderstandingPendingJob(
                        jobId,
                        page.getId(),
                        page.getId(),
                        "mois-sales-page-" + page.getId(),
                        STAGE_CODE,
                        input,
                        loadPromptSchemaTemplate());
                })
                .toList();
        return new DossierProductUnderstandingPendingResponse(!jobs.isEmpty(), jobs);
    }

    /** Carrega o prompt/schema ativo da etapa para o worker depender apenas do contrato do backend. */
    private PipelinePromptSchemaTemplatePayload loadPromptSchemaTemplate() {
        return templateRepository.findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(PIPELINE_CODE, STAGE_CODE)
                .map(PipelinePromptSchemaTemplatePayload::from)
                .orElseThrow(() -> new IllegalStateException(
                        "Template ativo não encontrado para " + PIPELINE_CODE + "/" + STAGE_CODE));
    }
    /** Recupera ou recompõe o jobId UUID para impedir que pendências antigas travem a fila da etapa. */
    private String resolveJobId(String productKey) {
        return pipelineDossieProdutoRepository
                .findTopByIdExternoAndCodigoEtapaOrderByDataHoraDescIdDesc(productKey, STAGE_CODE)
                .map(PipelineDossieProduto::getJobId)
                .filter(jobId -> jobId != null && !jobId.isBlank())
                .orElseGet(() -> rebuildJobId(productKey));
    }

    /** Cria auditoria mínima com jobId novo quando existe pendência legada sem registro rastreável. */
    private String rebuildJobId(String productKey) {
        String jobId = UUID.randomUUID().toString();
        PipelineDossieProduto pipeline = new PipelineDossieProduto();
        pipeline.setIdExterno(productKey);
        pipeline.setCodigoEtapa(STAGE_CODE);
        pipeline.setStatus(STATUS_STARTED);
        pipeline.setDataHora(Instant.now());
        pipeline.setJobId(jobId);
        pipeline.setVersaoPipeline("v1");
        pipeline.setPipelineCode(PIPELINE_CODE);
        pipelineDossieProdutoRepository.save(pipeline);
        return jobId;
    }
}
