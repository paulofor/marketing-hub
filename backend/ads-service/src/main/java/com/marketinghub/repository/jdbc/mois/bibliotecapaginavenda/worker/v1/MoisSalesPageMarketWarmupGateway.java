package com.marketinghub.repository.jdbc.mois.bibliotecapaginavenda.worker.v1;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Define o contrato de persistência da pesquisa de aquecimento de mercado da Biblioteca MOIS.
 */
public interface MoisSalesPageMarketWarmupGateway {

    /**
     * Busca os dados mínimos da página consolidada que receberá a pesquisa de aquecimento.
     */
    Optional<SalesPageWarmupData> findSalesPage(long pageId);

    /**
     * Busca o job pendente ou em execução mais recente da página para evitar duplicidade operacional.
     */
    Optional<MarketWarmupJobData> findActiveJobByPage(long pageId);

    /**
     * Cria um job pendente de aquecimento para uma página elegível.
     */
    MarketWarmupJobData createPendingJob(SalesPageWarmupData page);

    /**
     * Busca o job mais recente da página, independentemente do status.
     */
    Optional<MarketWarmupJobData> findLatestJobByPage(long pageId);

    /**
     * Busca o próximo job pendente no workspace para reserva pelo worker.
     */
    Optional<MarketWarmupClaimData> findNextPendingJob(String workspaceId);

    /**
     * Marca um job pendente como reservado pelo worker informado.
     */
    boolean claimPendingJob(long jobId, String workerId);

    /**
     * Busca um job pelo identificador operacional.
     */
    Optional<MarketWarmupJobData> findJob(long jobId);

    /**
     * Remove fontes, sinais e resumo anteriores do job antes da gravação final idempotente.
     */
    void deleteJobDetails(long jobId);

    /**
     * Insere uma fonte pública rastreável coletada pelo worker.
     */
    long insertSource(long jobId, long pageId, String workspaceId, MoisSalesLibraryDtos.MarketWarmupSourceCompleteItem source);

    /**
     * Insere um sinal comercial vinculado à fonte pública correspondente.
     */
    void insertSignal(long jobId, long pageId, String workspaceId, long sourceId, MoisSalesLibraryDtos.MarketWarmupSignalCompleteItem signal);

    /**
     * Insere o resumo final calculado para o job de aquecimento.
     */
    void insertSummary(long jobId, long pageId, String workspaceId, MoisSalesLibraryDtos.MarketWarmupSummaryCompleteItem summary);

    /**
     * Marca o job como concluído e replica os principais classificadores comerciais para busca rápida.
     */
    void markJobDone(long jobId, MoisSalesLibraryDtos.MarketWarmupSummaryCompleteItem summary, Instant finishedAt);

    /**
     * Marca o job como falho com categoria e mensagem operacional.
     */
    boolean markJobFailed(long jobId, String errorCategory, String errorMessage);

    /**
     * Busca o resumo mais recente da página com os dados do job associado.
     */
    Optional<MarketWarmupSummaryData> findLatestSummaryByPage(long pageId);

    /**
     * Lista fontes públicas de um job de aquecimento.
     */
    List<MoisSalesLibraryDtos.MarketWarmupSourceResponse> listSources(long jobId);

    /**
     * Lista sinais comerciais extraídos de um job de aquecimento.
     */
    List<MoisSalesLibraryDtos.MarketWarmupSignalResponse> listSignals(long jobId);

    /**
     * Representa os dados comerciais da página necessários para iniciar a etapa de aquecimento.
     */
    record SalesPageWarmupData(
            long pageId,
            String workspaceId,
            String urlCanonical,
            String title,
            String offerSummary,
            String mechanismSummary,
            String promiseSummary,
            String proofSummary
    ) {
    }

    /**
     * Representa o estado operacional de um job de aquecimento persistido.
     */
    record MarketWarmupJobData(
            long jobId,
            long pageId,
            String workspaceId,
            MoisSalesLibraryDtos.MarketWarmupJobStatus status,
            Instant createdAt,
            String errorCategory,
            String errorMessage
    ) {
    }

    /**
     * Representa o job pendente enriquecido com a página de venda para reserva interna do worker.
     */
    record MarketWarmupClaimData(
            MarketWarmupJobData job,
            SalesPageWarmupData page
    ) {
    }

    /**
     * Representa o resumo final da pesquisa junto com o estado do job que o produziu.
     */
    record MarketWarmupSummaryData(
            long jobId,
            long pageId,
            java.math.BigDecimal scoreTotal,
            MoisSalesLibraryDtos.MarketWarmupTemperature marketTemperature,
            MoisSalesLibraryDtos.MarketWarmupEcosystemType ecosystemType,
            MoisSalesLibraryDtos.MarketWarmupRecommendation recommendation,
            String mainPains,
            String mainObjections,
            String mainPromises,
            String mainChannels,
            String mainCompetitors,
            String saturationRisk,
            String opportunityRecommendation,
            String nextExperimentSuggestion,
            MoisSalesLibraryDtos.MarketWarmupJobStatus status,
            String errorCategory,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
