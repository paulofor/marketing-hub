package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.shared.DossierOpenAiTextResponseExtractor;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.shared.DossierPendingInputSupport;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingJob;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.receberequest.DossierSourceProductMatchRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.receberequest.DossierSourceProductMatchRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.receberesponse.DossierSourceProductMatchRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.receberesponse.DossierSourceProductMatchRecebeResponseResponse;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.PipelineDossieProdutoRepository;
import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa validação de relação fonte-produto do pipeline de dossiê MOIS v1. */
@Service
public class DossierSourceProductMatchService {
    private final MoisSalesPageRepository salesPageRepository;
    private final PipelineDossieProdutoRepository pipelineDossieProdutoRepository;

    /** Cria o service da etapa com acesso aos repositórios canônicos da página/produto e da auditoria do pipeline. */
    public DossierSourceProductMatchService(
            MoisSalesPageRepository salesPageRepository,
            PipelineDossieProdutoRepository pipelineDossieProdutoRepository) {
        this.salesPageRepository = salesPageRepository;
        this.pipelineDossieProdutoRepository = pipelineDossieProdutoRepository;
    }

    private static final String STAGE_CODE = "source-product-match";
    private static final String NEXT_STAGE = "warmup-signal-extraction";
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
        pipeline.setPipelineCode("warmupecosystem.v1");
        pipelineDossieProdutoRepository.save(pipeline);
    }


    /** Recebe o request operacional da etapa, coloca a página em espera do módulo e audita a entrada do pipeline. */
    public DossierSourceProductMatchRecebeRequestResponse recebeRequest(String productKey, String jobId, DossierSourceProductMatchRecebeRequestRequest request) {
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
        pipeline.setPipelineCode("warmupecosystem.v1");
        pipelineDossieProdutoRepository.save(pipeline);

        return new DossierSourceProductMatchRecebeRequestResponse(jobId, productKey, STAGE_CODE, STATUS_WAITING);
    }


    /** Recebe a resposta operacional da etapa, avança sucesso para a próxima etapa e audita a saída do pipeline. */
    public DossierSourceProductMatchRecebeResponseResponse recebeResponse(String productKey, String jobId, DossierSourceProductMatchRecebeResponseRequest request) {
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
        pipeline.setPipelineCode("warmupecosystem.v1");
        pipelineDossieProdutoRepository.save(pipeline);

        return new DossierSourceProductMatchRecebeResponseResponse(jobId, productKey, STAGE_CODE, status, nextStageCode);
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
    public DossierSourceProductMatchPendingResponse pending(DossierSourceProductMatchPendingRequest request) {
        List<DossierSourceProductMatchPendingJob> jobs = salesPageRepository
                .findTop10ByDossieProdutoStatusAndDossieProdutoCurrentStageOrderByDossieProdutoUpdatedAtAscIdAsc(
                        STATUS_STARTED, STAGE_CODE)
                .stream()
                .map(page -> {
                    String jobId = resolveJobId(String.valueOf(page.getId()));
                    return new DossierSourceProductMatchPendingJob(
                        jobId,
                        page.getId(),
                        page.getId(),
                        "mois-sales-page-" + page.getId(),
                        STAGE_CODE,
                        DossierPendingInputSupport.inputFor(
                                page.getId(), jobId, STAGE_CODE, NEXT_STAGE, pipelineDossieProdutoRepository));
                })
                .toList();
        return new DossierSourceProductMatchPendingResponse(!jobs.isEmpty(), jobs);
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
        pipeline.setPipelineCode("warmupecosystem.v1");
        pipelineDossieProdutoRepository.save(pipeline);
        return jobId;
    }
}
