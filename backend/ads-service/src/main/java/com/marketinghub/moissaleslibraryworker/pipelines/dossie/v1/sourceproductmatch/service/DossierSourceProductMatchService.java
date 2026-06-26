package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingJob;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingResponse;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa validação de relação fonte-produto do pipeline de dossiê MOIS v1. */
@Service
public class DossierSourceProductMatchService {
    private final MoisSalesPageRepository salesPageRepository;

    /** Cria o service da etapa com acesso ao repositório canônico da página/produto. */
    public DossierSourceProductMatchService(MoisSalesPageRepository salesPageRepository) {
        this.salesPageRepository = salesPageRepository;
    }

    private static final String STAGE_CODE = "source-product-match";
    private static final String NEXT_STAGE = "warmup-signal-extraction";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    /** Marca a página/produto como iniciado no dossiê e posiciona a etapa atual. */
    public void start(String productKey) {
        long pageId = Long.parseLong(productKey);
        var page = salesPageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("Página/produto MOIS não encontrada: " + productKey));
        page.setDossieProdutoStatus(STATUS_STARTED);
        page.setDossieProdutoCurrentStage(STAGE_CODE);
        page.setDossieProdutoUpdatedAt(Instant.now());
        salesPageRepository.save(page);
    }

    /** Entrega até dez trabalhos iniciados da etapa atual ao executor, ordenados pela data operacional mais antiga. */
    public DossierSourceProductMatchPendingResponse pending(DossierSourceProductMatchPendingRequest request) {
        List<DossierSourceProductMatchPendingJob> jobs = salesPageRepository
                .findTop10ByDossieProdutoStatusAndDossieProdutoCurrentStageOrderByDossieProdutoUpdatedAtAscIdAsc(
                        STATUS_STARTED, STAGE_CODE)
                .stream()
                .map(page -> new DossierSourceProductMatchPendingJob(
                        page.getId(),
                        page.getId(),
                        "mois-sales-page-" + page.getId(),
                        STAGE_CODE,
                        Map.of(
                                "productKey", String.valueOf(page.getId()),
                                "pageId", page.getId(),
                                "stageCode", STAGE_CODE,
                                "status", STATUS_STARTED,
                                "nextStageCode", NEXT_STAGE)))
                .toList();
        return new DossierSourceProductMatchPendingResponse(!jobs.isEmpty(), jobs);
    }
}
