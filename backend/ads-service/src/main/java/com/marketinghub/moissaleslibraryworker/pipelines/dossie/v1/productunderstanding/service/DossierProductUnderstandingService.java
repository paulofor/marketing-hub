package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingResponse;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa entendimento do produto do pipeline de dossiê MOIS v1. */
@Service
public class DossierProductUnderstandingService {
    private final MoisSalesPageRepository salesPageRepository;

    /** Cria o service da etapa com acesso ao repositório canônico da página/produto. */
    public DossierProductUnderstandingService(MoisSalesPageRepository salesPageRepository) {
        this.salesPageRepository = salesPageRepository;
    }

    private static final String STAGE_CODE = "product-understanding";
    private static final String NEXT_STAGE = "investigation-anchor-builder";
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

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierProductUnderstandingPendingResponse pending(DossierProductUnderstandingPendingRequest request) {
        return new DossierProductUnderstandingPendingResponse(false, List.of());
    }
}
