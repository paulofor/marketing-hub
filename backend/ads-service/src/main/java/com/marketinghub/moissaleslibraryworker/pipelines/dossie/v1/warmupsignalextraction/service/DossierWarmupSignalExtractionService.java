package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingJob;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingResponse;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa extração de sinais de aquecimento do pipeline de dossiê MOIS v1. */
@Service
public class DossierWarmupSignalExtractionService {
    private final MoisSalesPageRepository salesPageRepository;

    /** Cria o service da etapa com acesso ao repositório canônico da página/produto. */
    public DossierWarmupSignalExtractionService(MoisSalesPageRepository salesPageRepository) {
        this.salesPageRepository = salesPageRepository;
    }

    private static final String STAGE_CODE = "warmup-signal-extraction";
    private static final String NEXT_STAGE = "warmup-map-builder";
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
    public DossierWarmupSignalExtractionPendingResponse pending(DossierWarmupSignalExtractionPendingRequest request) {
        List<DossierWarmupSignalExtractionPendingJob> jobs = salesPageRepository
                .findTop10ByDossieProdutoStatusAndDossieProdutoCurrentStageOrderByDossieProdutoUpdatedAtAscIdAsc(
                        STATUS_STARTED, STAGE_CODE)
                .stream()
                .map(page -> new DossierWarmupSignalExtractionPendingJob(
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
        return new DossierWarmupSignalExtractionPendingResponse(!jobs.isEmpty(), jobs);
    }
}
