package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingJob;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.receberequest.DossierInvestigationAnchorBuilderRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.receberequest.DossierInvestigationAnchorBuilderRecebeRequestResponse;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa geração de âncoras de investigação do pipeline de dossiê MOIS v1. */
@Service
public class DossierInvestigationAnchorBuilderService {
    private final MoisSalesPageRepository salesPageRepository;
    private final PipelineDossieProdutoRepository pipelineDossieProdutoRepository;

    /** Cria o service da etapa com acesso aos repositórios canônicos da página/produto e da auditoria do pipeline. */
    public DossierInvestigationAnchorBuilderService(
            MoisSalesPageRepository salesPageRepository,
            PipelineDossieProdutoRepository pipelineDossieProdutoRepository) {
        this.salesPageRepository = salesPageRepository;
        this.pipelineDossieProdutoRepository = pipelineDossieProdutoRepository;
    }

    private static final String STAGE_CODE = "investigation-anchor-builder";
    private static final String NEXT_STAGE = "warmup-resource-discovery";
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
        pipeline.setDataHora(now);
        pipeline.setJobId(jobId);
        pipeline.setVersaoPipeline("v1");
        pipelineDossieProdutoRepository.save(pipeline);
    }


    /** Recebe o request operacional da etapa, coloca a página em espera do módulo e audita a entrada do pipeline. */
    public DossierInvestigationAnchorBuilderRecebeRequestResponse recebeRequest(String productKey, String jobId, DossierInvestigationAnchorBuilderRecebeRequestRequest request) {
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
        pipeline.setDataHora(now);
        pipeline.setJobId(jobId);
        pipeline.setPlataforma(request.plataforma());
        pipeline.setPrompt(request.prompt());
        pipeline.setSchema(request.schema());
        pipeline.setVersaoPipeline("v1");
        pipelineDossieProdutoRepository.save(pipeline);

        return new DossierInvestigationAnchorBuilderRecebeRequestResponse(jobId, productKey, STAGE_CODE, STATUS_WAITING);
    }

    /** Entrega até dez trabalhos iniciados da etapa atual ao executor, ordenados pela data operacional mais antiga. */
    public DossierInvestigationAnchorBuilderPendingResponse pending(DossierInvestigationAnchorBuilderPendingRequest request) {
        List<DossierInvestigationAnchorBuilderPendingJob> jobs = salesPageRepository
                .findTop10ByDossieProdutoStatusAndDossieProdutoCurrentStageOrderByDossieProdutoUpdatedAtAscIdAsc(
                        STATUS_STARTED, STAGE_CODE)
                .stream()
                .map(page -> {
                    String jobId = resolveJobId(String.valueOf(page.getId()));
                    return new DossierInvestigationAnchorBuilderPendingJob(
                        jobId,
                        page.getId(),
                        page.getId(),
                        "mois-sales-page-" + page.getId(),
                        STAGE_CODE,
                        Map.of(
                                "jobId", jobId,
                                "productKey", String.valueOf(page.getId()),
                                "pageId", page.getId(),
                                "stageCode", STAGE_CODE,
                                "status", STATUS_STARTED,
                                "nextStageCode", NEXT_STAGE));
                })
                .toList();
        return new DossierInvestigationAnchorBuilderPendingResponse(!jobs.isEmpty(), jobs);
    }
    /** Recupera o jobId UUID criado no start para compor o contrato pending da etapa. */
    private String resolveJobId(String productKey) {
        return pipelineDossieProdutoRepository
                .findTopByIdExternoAndCodigoEtapaOrderByDataHoraDescIdDesc(productKey, STAGE_CODE)
                .map(PipelineDossieProduto::getJobId)
                .orElseThrow(() -> new IllegalStateException(
                        "JobId do dossiê MOIS não encontrado para produto " + productKey + " na etapa " + STAGE_CODE));
    }
}
