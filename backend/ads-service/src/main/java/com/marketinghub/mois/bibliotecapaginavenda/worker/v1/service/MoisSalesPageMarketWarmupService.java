package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupClaimData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupJobData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSignalData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSignalReadData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSourceData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSummaryData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSummaryWriteData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.SalesPageWarmupData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra a solicitação, reserva e persistência da pesquisa de aquecimento de mercado da Biblioteca MOIS.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoisSalesPageMarketWarmupService {

    private static final int DEFAULT_STALE_MINUTES = 120;

    private final MoisSalesPageMarketWarmupGateway gateway;
    private final MoisSalesPageMarketWarmupScoreEngine scoreEngine = new MoisSalesPageMarketWarmupScoreEngine();

    /**
     * Solicita uma pesquisa de aquecimento para a página, reutilizando jobs pendentes ou em execução.
     */
    @Transactional
    public MoisSalesLibraryDtos.MarketWarmupRequestResponse requestResearch(long pageId) {
        try {
            SalesPageWarmupData page = gateway.findSalesPage(pageId)
                    .orElseThrow(() -> new IllegalArgumentException("Página MOIS não encontrada para aquecimento: " + pageId));
            MarketWarmupJobData job = gateway.findActiveJobByPage(pageId).orElseGet(() -> gateway.createPendingJob(page));
            log.info("Pesquisa de aquecimento MOIS solicitada. modulo=MOIS, operacao=requestMarketWarmup, pageId={}, jobId={}, status={}",
                    pageId, job.jobId(), mapJobStatus(job.status()));
            return new MoisSalesLibraryDtos.MarketWarmupRequestResponse(pageId, job.jobId(), mapJobStatus(job.status()), job.createdAt());
        } catch (RuntimeException ex) {
            log.error("Falha ao solicitar pesquisa de aquecimento MOIS. modulo=MOIS, operacao=requestMarketWarmup, pageId={}, erroClasse={}, erro={}",
                    pageId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Lista o resumo mais recente de aquecimento de uma página de venda.
     */
    public MoisSalesLibraryDtos.MarketWarmupSummaryResponse getSummary(long pageId) {
        try {
            MarketWarmupSummaryData summary = gateway.findLatestSummaryByPage(pageId)
                    .orElseThrow(() -> new IllegalArgumentException("Pesquisa de aquecimento não encontrada para página: " + pageId));
            return mapSummary(summary);
        } catch (RuntimeException ex) {
            log.error("Falha ao consultar resumo de aquecimento MOIS. modulo=MOIS, operacao=getMarketWarmupSummary, pageId={}, erroClasse={}, erro={}",
                    pageId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Lista as fontes rastreáveis do job de aquecimento mais recente da página.
     */
    public MoisSalesLibraryDtos.MarketWarmupSourceListResponse listSources(long pageId) {
        try {
            MarketWarmupJobData job = gateway.findLatestJobByPage(pageId)
                    .orElseThrow(() -> new IllegalArgumentException("Pesquisa de aquecimento não encontrada para página: " + pageId));
            return new MoisSalesLibraryDtos.MarketWarmupSourceListResponse(pageId, job.jobId(), gateway.listSources(job.jobId()).stream().map(this::mapSource).toList());
        } catch (RuntimeException ex) {
            log.error("Falha ao listar fontes de aquecimento MOIS. modulo=MOIS, operacao=listMarketWarmupSources, pageId={}, erroClasse={}, erro={}",
                    pageId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Lista os sinais comerciais do job de aquecimento mais recente da página.
     */
    public MoisSalesLibraryDtos.MarketWarmupSignalListResponse listSignals(long pageId) {
        try {
            MarketWarmupJobData job = gateway.findLatestJobByPage(pageId)
                    .orElseThrow(() -> new IllegalArgumentException("Pesquisa de aquecimento não encontrada para página: " + pageId));
            return new MoisSalesLibraryDtos.MarketWarmupSignalListResponse(pageId, job.jobId(), gateway.listSignals(job.jobId()).stream().map(this::mapSignal).toList());
        } catch (RuntimeException ex) {
            log.error("Falha ao listar sinais de aquecimento MOIS. modulo=MOIS, operacao=listMarketWarmupSignals, pageId={}, erroClasse={}, erro={}",
                    pageId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Reserva o próximo job pendente para processamento pelo worker de aquecimento.
     */
    @Transactional
    public MoisSalesLibraryDtos.MarketWarmupClaimResponse claimJob(MoisSalesLibraryDtos.MarketWarmupClaimRequest request) {
        try {
            MarketWarmupClaimData pending = gateway.findNextPendingJob(request.workspaceId())
                    .orElse(null);
            if (pending == null) {
                log.info("Nenhum job de aquecimento MOIS pendente para reserva. modulo=MOIS, operacao=claimMarketWarmupJob, workspaceId={}, workerId={}",
                        request.workspaceId(), request.workerId());
                return new MoisSalesLibraryDtos.MarketWarmupClaimResponse(false, null);
            }
            boolean claimed = gateway.claimPendingJob(pending.job().jobId(), request.workerId());
            if (!claimed) {
                log.warn("Job de aquecimento MOIS não foi reservado por concorrência operacional. modulo=MOIS, operacao=claimMarketWarmupJob, workspaceId={}, workerId={}, jobId={}",
                        request.workspaceId(), request.workerId(), pending.job().jobId());
                return new MoisSalesLibraryDtos.MarketWarmupClaimResponse(false, null);
            }
            SalesPageWarmupData page = pending.page();
            log.info("Job de aquecimento MOIS reservado. modulo=MOIS, operacao=claimMarketWarmupJob, workspaceId={}, workerId={}, pageId={}, jobId={}",
                    request.workspaceId(), request.workerId(), page.pageId(), pending.job().jobId());
            return new MoisSalesLibraryDtos.MarketWarmupClaimResponse(true, new MoisSalesLibraryDtos.MarketWarmupClaimedJob(
                    pending.job().jobId(), page.pageId(), page.workspaceId(), page.urlCanonical(), page.title(), page.offerSummary(),
                    page.mechanismSummary(), page.promiseSummary(), page.proofSummary()));
        } catch (RuntimeException ex) {
            log.error("Falha ao reservar job de aquecimento MOIS. modulo=MOIS, operacao=claimMarketWarmupJob, workspaceId={}, workerId={}, erroClasse={}, erro={}",
                    request.workspaceId(), request.workerId(), ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Conclui o job com fontes, sinais e resumo final enviados pelo worker.
     */
    @Transactional
    public void completeJob(long jobId, MoisSalesLibraryDtos.MarketWarmupCompleteRequest request) {
        try {
            MarketWarmupJobData job = gateway.findJob(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job de aquecimento não encontrado: " + jobId));
            if (mapJobStatus(job.status()) == MoisSalesLibraryDtos.MarketWarmupJobStatus.DONE) {
                throw new IllegalStateException("Job de aquecimento já concluído: " + jobId);
            }
            log.info("Iniciando conclusão do job de aquecimento MOIS. modulo=MOIS, operacao=completeMarketWarmupJob, workspaceId={}, pageId={}, jobId={}, fontesRecebidas={}, sinaisRecebidos={}",
                    job.workspaceId(), job.pageId(), jobId, request.sources().size(), request.signals().size());
            gateway.deleteJobDetails(jobId);
            List<Long> sourceIds = persistSources(job, request.sources());
            persistSignals(job, sourceIds, request.signals());
            MarketWarmupSummaryWriteData summary = mapSummaryWrite(scoreEngine.calculate(request));
            gateway.insertSummary(jobId, job.pageId(), job.workspaceId(), summary);
            gateway.markJobDone(jobId, summary, request.finishedAt());
            log.info("Job de aquecimento MOIS concluído. modulo=MOIS, operacao=completeMarketWarmupJob, workspaceId={}, pageId={}, jobId={}, fontes={}, sinais={}, score={}",
                    job.workspaceId(), job.pageId(), jobId, request.sources().size(), request.signals().size(), summary.scoreTotal());
        } catch (RuntimeException ex) {
            log.error("Falha ao concluir job de aquecimento MOIS. modulo=MOIS, operacao=completeMarketWarmupJob, jobId={}, erroClasse={}, erro={}",
                    jobId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Registra falha operacional em um job de aquecimento pendente ou em execução.
     */
    @Transactional
    public void failJob(long jobId, MoisSalesLibraryDtos.MarketWarmupFailRequest request) {
        try {
            boolean failed = gateway.markJobFailed(jobId, request.errorCategory(), request.errorMessage());
            if (!failed) {
                throw new IllegalStateException("Job de aquecimento não encontrado ou não pode falhar: " + jobId);
            }
            log.info("Job de aquecimento MOIS marcado como falho. modulo=MOIS, operacao=failMarketWarmupJob, jobId={}, errorCategory={}, errorMessage={}",
                    jobId, request.errorCategory(), request.errorMessage());
        } catch (RuntimeException ex) {
            log.error("Falha ao registrar erro de job de aquecimento MOIS. modulo=MOIS, operacao=failMarketWarmupJob, jobId={}, erroClasse={}, erro={}",
                    jobId, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Refileira jobs antigos presos em FETCHING para permitir nova execução sem intervenção direta no banco.
     */
    @Transactional
    public MoisSalesLibraryDtos.MarketWarmupReprocessStaleResponse reprocessStaleJobs(
            MoisSalesLibraryDtos.MarketWarmupReprocessStaleRequest request
    ) {
        int staleMinutes = request.staleMinutes() == null ? DEFAULT_STALE_MINUTES : request.staleMinutes();
        try {
            long requeued = gateway.requeueStaleFetchingJobs(request.workspaceId(), staleMinutes);
            log.info("Jobs de aquecimento MOIS presos refileirados. modulo=MOIS, operacao=reprocessStaleMarketWarmupJobs, workspaceId={}, staleMinutes={}, requeuedJobs={}",
                    request.workspaceId(), staleMinutes, requeued);
            return new MoisSalesLibraryDtos.MarketWarmupReprocessStaleResponse(request.workspaceId(), staleMinutes, requeued, Instant.now());
        } catch (RuntimeException ex) {
            log.error("Falha ao reprocessar jobs de aquecimento MOIS presos. modulo=MOIS, operacao=reprocessStaleMarketWarmupJobs, workspaceId={}, staleMinutes={}, erroClasse={}, erro={}",
                    request.workspaceId(), staleMinutes, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Persiste as fontes enviadas pelo worker mantendo a posição de entrada para vincular sinais.
     */
    private List<Long> persistSources(MarketWarmupJobData job, List<MoisSalesLibraryDtos.MarketWarmupSourceCompleteItem> sources) {
        List<Long> sourceIds = new ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            MoisSalesLibraryDtos.MarketWarmupSourceCompleteItem source = sources.get(index);
            long sourceId = gateway.insertSource(job.jobId(), job.pageId(), job.workspaceId(), mapSourceWrite(source));
            sourceIds.add(sourceId);
            log.info("Fonte pública de aquecimento MOIS persistida. modulo=MOIS, operacao=completeMarketWarmupJob, workspaceId={}, pageId={}, jobId={}, sourceIndex={}, sourceId={}, platform={}, sourceType={}, sourceUrl={}",
                    job.workspaceId(), job.pageId(), job.jobId(), index, sourceId, source.platform(), source.sourceType(), source.sourceUrl());
        }
        return sourceIds;
    }

    /**
     * Persiste os sinais enviados pelo worker validando o índice da fonte declarada.
     */
    private void persistSignals(MarketWarmupJobData job, List<Long> sourceIds, List<MoisSalesLibraryDtos.MarketWarmupSignalCompleteItem> signals) {
        for (MoisSalesLibraryDtos.MarketWarmupSignalCompleteItem signal : signals) {
            if (signal.sourceIndex() < 0 || signal.sourceIndex() >= sourceIds.size()) {
                throw new IllegalArgumentException("Índice de fonte inválido para sinal de aquecimento: " + signal.sourceIndex());
            }
            gateway.insertSignal(job.jobId(), job.pageId(), job.workspaceId(), sourceIds.get(signal.sourceIndex()), mapSignalWrite(signal));
        }
    }

    /**
     * Converte o resumo persistido em contrato de leitura da API.
     */
    private MoisSalesLibraryDtos.MarketWarmupSummaryResponse mapSummary(MarketWarmupSummaryData summary) {
        return new MoisSalesLibraryDtos.MarketWarmupSummaryResponse(
                summary.jobId(), summary.pageId(), summary.scoreTotal(), mapTemperature(summary.marketTemperature()), mapEcosystem(summary.ecosystemType()),
                mapRecommendation(summary.recommendation()), splitLines(summary.mainPains()), splitLines(summary.mainObjections()), splitLines(summary.mainPromises()),
                splitLines(summary.mainChannels()), splitLines(summary.mainCompetitors()), summary.saturationRisk(), summary.opportunityRecommendation(), summary.nextExperimentSuggestion(),
                mapJobStatus(summary.status()), summary.errorCategory(), summary.errorMessage(), summary.createdAt(), summary.updatedAt());
    }

    /**
     * Converte uma fonte persistida em contrato de leitura da API.
     */
    private MoisSalesLibraryDtos.MarketWarmupSourceResponse mapSource(MarketWarmupSourceData source) {
        return new MoisSalesLibraryDtos.MarketWarmupSourceResponse(
                source.sourceId(), source.jobId(), source.pageId(), mapPlatform(source.platform()), mapSourceType(source.sourceType()), source.sourceUrl(),
                source.sourceTitle(), source.authorName(), source.publishedAt(), source.lastActivityAt(), source.followersOrSubscribers(), source.viewsCount(),
                source.likesCount(), source.commentsCount(), source.recencyScore(), source.engagementScore(), source.evidenceSummary(), source.createdAt(), source.updatedAt());
    }

    /**
     * Converte um sinal persistido em contrato de leitura da API.
     */
    private MoisSalesLibraryDtos.MarketWarmupSignalResponse mapSignal(MarketWarmupSignalReadData signal) {
        return new MoisSalesLibraryDtos.MarketWarmupSignalResponse(
                signal.signalId(), signal.jobId(), signal.sourceId(), signal.pageId(), mapSignalType(signal.signalType()), signal.signalStrength(),
                signal.signalText(), signal.businessInterpretation(), signal.createdAt());
    }

    /**
     * Converte uma fonte recebida da API em dados desacoplados de persistência.
     */
    private MarketWarmupSourceData mapSourceWrite(MoisSalesLibraryDtos.MarketWarmupSourceCompleteItem source) {
        return new MarketWarmupSourceData(null, null, null, source.platform().name(), source.sourceType().name(), source.sourceUrl(), source.sourceTitle(),
                source.authorName(), source.publishedAt(), source.lastActivityAt(), source.followersOrSubscribers(), source.viewsCount(), source.likesCount(),
                source.commentsCount(), source.recencyScore(), source.engagementScore(), source.evidenceSummary(), null, null);
    }

    /**
     * Converte um sinal recebido da API em dados desacoplados de persistência.
     */
    private MarketWarmupSignalData mapSignalWrite(MoisSalesLibraryDtos.MarketWarmupSignalCompleteItem signal) {
        return new MarketWarmupSignalData(signal.signalType().name(), signal.signalStrength(), signal.signalText(), signal.businessInterpretation());
    }

    /**
     * Converte o resumo recebido da API em dados desacoplados de persistência.
     */
    private MarketWarmupSummaryWriteData mapSummaryWrite(MoisSalesLibraryDtos.MarketWarmupSummaryCompleteItem summary) {
        return new MarketWarmupSummaryWriteData(summary.scoreTotal(), summary.marketTemperature().name(), summary.ecosystemType().name(), summary.recommendation().name(),
                summary.mainPains(), summary.mainObjections(), summary.mainPromises(), summary.mainChannels(), summary.mainCompetitors(), summary.saturationRisk(),
                summary.opportunityRecommendation(), summary.nextExperimentSuggestion());
    }

    /**
     * Converte status textual da persistência para o enum público do módulo.
     */
    private MoisSalesLibraryDtos.MarketWarmupJobStatus mapJobStatus(String status) {
        return status == null ? null : MoisSalesLibraryDtos.MarketWarmupJobStatus.valueOf(status);
    }

    /**
     * Converte temperatura textual da persistência para o enum público do módulo.
     */
    private MoisSalesLibraryDtos.MarketWarmupTemperature mapTemperature(String temperature) {
        return temperature == null ? null : MoisSalesLibraryDtos.MarketWarmupTemperature.valueOf(temperature);
    }

    /**
     * Converte ecossistema textual da persistência para o enum público do módulo.
     */
    private MoisSalesLibraryDtos.MarketWarmupEcosystemType mapEcosystem(String ecosystem) {
        return ecosystem == null ? null : MoisSalesLibraryDtos.MarketWarmupEcosystemType.valueOf(ecosystem);
    }

    /**
     * Converte recomendação textual da persistência para o enum público do módulo.
     */
    private MoisSalesLibraryDtos.MarketWarmupRecommendation mapRecommendation(String recommendation) {
        return recommendation == null ? null : MoisSalesLibraryDtos.MarketWarmupRecommendation.valueOf(recommendation);
    }

    /**
     * Converte plataforma textual da persistência para o enum público do módulo.
     */
    private MoisSalesLibraryDtos.MarketWarmupPlatform mapPlatform(String platform) {
        return platform == null ? null : MoisSalesLibraryDtos.MarketWarmupPlatform.valueOf(platform);
    }

    /**
     * Converte tipo de fonte textual da persistência para o enum público do módulo.
     */
    private MoisSalesLibraryDtos.MarketWarmupSourceType mapSourceType(String sourceType) {
        return sourceType == null ? null : MoisSalesLibraryDtos.MarketWarmupSourceType.valueOf(sourceType);
    }

    /**
     * Converte tipo de sinal textual da persistência para o enum público do módulo.
     */
    private MoisSalesLibraryDtos.MarketWarmupSignalType mapSignalType(String signalType) {
        return signalType == null ? null : MoisSalesLibraryDtos.MarketWarmupSignalType.valueOf(signalType);
    }

    /**
     * Converte texto funcional separado por linhas em lista sem interpretar JSON serializado.
     */
    private List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }
}
