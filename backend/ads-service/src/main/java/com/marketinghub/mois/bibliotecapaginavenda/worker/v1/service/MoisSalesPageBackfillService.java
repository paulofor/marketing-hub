package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageBackfillGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra o backfill inicial idempotente do modelo consolidado de páginas de venda MOIS.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoisSalesPageBackfillService implements ApplicationRunner {

    private final MoisSalesPageBackfillGateway gateway;

    @Value("${mois.sales-page.backfill.enabled:false}")
    private boolean enabled;

    /**
     * Executa o backfill na inicialização para consolidar o estado legado nas duas novas tabelas operacionais.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Backfill MOIS sales page ignorado por configuração. modulo=MOIS, operacao=salesPageBackfill, enabled={}", enabled);
            return;
        }
        try {
            if (!gateway.hasBackfillTables()) {
                log.info("Backfill MOIS sales page ignorado porque o schema necessário ainda não existe. modulo=MOIS, operacao=salesPageBackfill");
                return;
            }
            BackfillCounters counters = executeBackfill();
            log.info(
                    "Backfill MOIS sales page concluído. modulo=MOIS, operacao=salesPageBackfill, paginasLegado={}, paginasAntes={}, paginasDepois={}, execucoesAntes={}, execucoesDepois={}, linhasSalesPageAfetadas={}, processingJobsInseridos={}, analysesInseridas={}, snapshotsInseridos={}, collectedCapturesInseridas={}, ponteirosAtualizados={}",
                    counters.legacyPages(),
                    counters.salesPagesBefore(),
                    counters.salesPagesAfter(),
                    counters.jobExecutionsBefore(),
                    counters.jobExecutionsAfter(),
                    counters.salesPageRowsAffected(),
                    counters.processingJobsInserted(),
                    counters.analysesInserted(),
                    counters.snapshotsInserted(),
                    counters.collectedCapturesInserted(),
                    counters.lastJobPointersUpdated());
        } catch (RuntimeException ex) {
            log.error("Falha no backfill MOIS sales page. modulo=MOIS, operacao=salesPageBackfill, enabled={}, erroClasse={}, erro={}",
                    enabled, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Executa as etapas idempotentes do backfill dentro de uma transação única.
     */
    @Transactional
    public BackfillCounters executeBackfill() {
        long legacyPages = gateway.countLegacyUrlIngests();
        long salesPagesBefore = gateway.countSalesPages();
        long jobExecutionsBefore = gateway.countJobExecutions();

        int salesPageRowsAffected = gateway.backfillSalesPages();
        int processingJobsInserted = gateway.backfillLatestProcessingJobs();
        int analysesInserted = gateway.backfillLatestAnalyses();
        int snapshotsInserted = gateway.backfillLatestSnapshots();
        int collectedCapturesInserted = gateway.backfillLatestCollectedReferenceHtmlCaptures();
        int lastJobPointersUpdated = gateway.updateLastJobExecutionPointers();

        long salesPagesAfter = gateway.countSalesPages();
        long jobExecutionsAfter = gateway.countJobExecutions();
        return new BackfillCounters(
                legacyPages,
                salesPagesBefore,
                salesPagesAfter,
                jobExecutionsBefore,
                jobExecutionsAfter,
                salesPageRowsAffected,
                processingJobsInserted,
                analysesInserted,
                snapshotsInserted,
                collectedCapturesInserted,
                lastJobPointersUpdated);
    }

    /**
     * Resume os contadores operacionais produzidos pela migração idempotente.
     */
    public record BackfillCounters(
            long legacyPages,
            long salesPagesBefore,
            long salesPagesAfter,
            long jobExecutionsBefore,
            long jobExecutionsAfter,
            int salesPageRowsAffected,
            int processingJobsInserted,
            int analysesInserted,
            int snapshotsInserted,
            int collectedCapturesInserted,
            int lastJobPointersUpdated) {
    }
}
