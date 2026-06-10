package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupClaimData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupJobData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSignalData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSourceData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSummaryWriteData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSummaryData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.SalesPageWarmupData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Valida as regras de serviço da pesquisa de aquecimento de mercado da Biblioteca MOIS.
 */
@ExtendWith(MockitoExtension.class)
class MoisSalesPageMarketWarmupServiceTest {

    @Mock
    private MoisSalesPageMarketWarmupGateway gateway;

    @InjectMocks
    private MoisSalesPageMarketWarmupService service;

    /**
     * Garante que solicitação duplicada reutiliza job pendente em vez de criar outro processamento.
     */
    @Test
    void requestResearchReusesActiveJob() {
        SalesPageWarmupData page = samplePage();
        MarketWarmupJobData job = sampleJob(MoisSalesLibraryDtos.MarketWarmupJobStatus.PENDING);
        given(gateway.findSalesPage(10L)).willReturn(Optional.of(page));
        given(gateway.findActiveJobByPage(10L)).willReturn(Optional.of(job));

        MoisSalesLibraryDtos.MarketWarmupRequestResponse response = service.requestResearch(10L);

        assertThat(response.jobId()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo(MoisSalesLibraryDtos.MarketWarmupJobStatus.PENDING);
        verify(gateway, never()).createPendingJob(page);
    }

    /**
     * Garante que uma página sem job ativo recebe nova pesquisa pendente.
     */
    @Test
    void requestResearchCreatesPendingJobWhenNoActiveJobExists() {
        SalesPageWarmupData page = samplePage();
        MarketWarmupJobData job = sampleJob(MoisSalesLibraryDtos.MarketWarmupJobStatus.PENDING);
        given(gateway.findSalesPage(10L)).willReturn(Optional.of(page));
        given(gateway.findActiveJobByPage(10L)).willReturn(Optional.empty());
        given(gateway.createPendingJob(page)).willReturn(job);

        MoisSalesLibraryDtos.MarketWarmupRequestResponse response = service.requestResearch(10L);

        assertThat(response.pageId()).isEqualTo(10L);
        assertThat(response.jobId()).isEqualTo(99L);
        verify(gateway).createPendingJob(page);
    }

    /**
     * Garante que o worker recebe os dados comerciais necessários ao reservar um job pendente.
     */
    @Test
    void claimJobReturnsCommercialContextWhenPendingJobIsClaimed() {
        SalesPageWarmupData page = samplePage();
        MarketWarmupJobData job = sampleJob(MoisSalesLibraryDtos.MarketWarmupJobStatus.PENDING);
        given(gateway.findNextPendingJob("workspace-001")).willReturn(Optional.of(new MarketWarmupClaimData(job, page)));
        given(gateway.claimPendingJob(99L, "worker-1")).willReturn(true);

        MoisSalesLibraryDtos.MarketWarmupClaimResponse response = service.claimJob(
                new MoisSalesLibraryDtos.MarketWarmupClaimRequest("workspace-001", "worker-1"));

        assertThat(response.claimed()).isTrue();
        assertThat(response.job().pageId()).isEqualTo(10L);
        assertThat(response.job().offerSummary()).isEqualTo("Oferta transforma dor em resultado");
    }

    /**
     * Garante que conclusão grava fontes, sinais, resumo e estado final em uma única orquestração.
     */
    @Test
    void completeJobPersistsSourcesSignalsSummaryAndDoneStatus() {
        MarketWarmupJobData job = sampleJob(MoisSalesLibraryDtos.MarketWarmupJobStatus.FETCHING);
        MoisSalesLibraryDtos.MarketWarmupSourceCompleteItem source = sampleSource();
        MoisSalesLibraryDtos.MarketWarmupSignalCompleteItem signal = new MoisSalesLibraryDtos.MarketWarmupSignalCompleteItem(
                0, MoisSalesLibraryDtos.MarketWarmupSignalType.PAIN_EXPLICIT, BigDecimal.valueOf(8.5), "dor explícita", "alta urgência");
        MoisSalesLibraryDtos.MarketWarmupSummaryCompleteItem summary = sampleSummary();
        given(gateway.findJob(99L)).willReturn(Optional.of(job));
        given(gateway.insertSource(99L, 10L, "workspace-001", sourceData(source))).willReturn(501L);

        service.completeJob(99L, new MoisSalesLibraryDtos.MarketWarmupCompleteRequest(
                List.of(source), List.of(signal), summary, Instant.parse("2026-06-10T10:00:00Z")));

        verify(gateway).deleteJobDetails(99L);
        verify(gateway).insertSignal(99L, 10L, "workspace-001", 501L, signalData(signal));
        verify(gateway).insertSummary(99L, 10L, "workspace-001", summaryData(summary));
        verify(gateway).markJobDone(99L, summaryData(summary), Instant.parse("2026-06-10T10:00:00Z"));
    }

    /**
     * Garante que sinais com índice de fonte inválido não contaminam a conclusão do job.
     */
    @Test
    void completeJobRejectsSignalWithInvalidSourceIndex() {
        MarketWarmupJobData job = sampleJob(MoisSalesLibraryDtos.MarketWarmupJobStatus.FETCHING);
        MoisSalesLibraryDtos.MarketWarmupSourceCompleteItem source = sampleSource();
        MoisSalesLibraryDtos.MarketWarmupSignalCompleteItem signal = new MoisSalesLibraryDtos.MarketWarmupSignalCompleteItem(
                2, MoisSalesLibraryDtos.MarketWarmupSignalType.OBJECTION, BigDecimal.ONE, "objeção", null);
        given(gateway.findJob(99L)).willReturn(Optional.of(job));
        given(gateway.insertSource(99L, 10L, "workspace-001", sourceData(source))).willReturn(501L);

        assertThatThrownBy(() -> service.completeJob(99L, new MoisSalesLibraryDtos.MarketWarmupCompleteRequest(
                List.of(source), List.of(signal), sampleSummary(), Instant.parse("2026-06-10T10:00:00Z"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Índice de fonte inválido");

        verify(gateway, never()).markJobDone(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /**
     * Garante que o resumo lido converte listas em linhas funcionais sem depender de JSON em texto.
     */
    @Test
    void getSummarySplitsFunctionalTextLinesIntoLists() {
        given(gateway.findLatestSummaryByPage(10L)).willReturn(Optional.of(new MarketWarmupSummaryData(
                99L,
                10L,
                BigDecimal.valueOf(82),
                "HOT",
                "CREATORS_HEATED",
                "PRIORITIZE",
                "dor 1\ndor 2",
                "objeção 1",
                "promessa 1\npromessa 2",
                "YouTube\nBlog",
                "Concorrente A",
                "baixo",
                "priorizar experimento",
                "ângulo inicial",
                "DONE",
                null,
                null,
                Instant.parse("2026-06-10T09:00:00Z"),
                Instant.parse("2026-06-10T10:00:00Z"))));

        MoisSalesLibraryDtos.MarketWarmupSummaryResponse response = service.getSummary(10L);

        assertThat(response.mainPains()).containsExactly("dor 1", "dor 2");
        assertThat(response.mainChannels()).containsExactly("YouTube", "Blog");
        assertThat(response.recommendation()).isEqualTo(MoisSalesLibraryDtos.MarketWarmupRecommendation.PRIORITIZE);
    }

    /**
     * Monta uma página consolidada com os campos comerciais necessários ao aquecimento.
     */
    private SalesPageWarmupData samplePage() {
        return new SalesPageWarmupData(
                10L,
                "workspace-001",
                "https://example.test/oferta",
                "Oferta principal",
                "Oferta transforma dor em resultado",
                "Mecanismo plausível",
                "Promessa clara",
                "Prova social");
    }

    /**
     * Monta um job de aquecimento com status controlado pelo cenário de teste.
     */
    private MarketWarmupJobData sampleJob(MoisSalesLibraryDtos.MarketWarmupJobStatus status) {
        return new MarketWarmupJobData(99L, 10L, "workspace-001", status.name(), Instant.parse("2026-06-10T09:00:00Z"), null, null);
    }

    /**
     * Monta os dados desacoplados de persistência esperados para uma fonte pública.
     */
    private MarketWarmupSourceData sourceData(MoisSalesLibraryDtos.MarketWarmupSourceCompleteItem source) {
        return new MarketWarmupSourceData(null, null, null, source.platform().name(), source.sourceType().name(), source.sourceUrl(), source.sourceTitle(),
                source.authorName(), source.publishedAt(), source.lastActivityAt(), source.followersOrSubscribers(), source.viewsCount(), source.likesCount(),
                source.commentsCount(), source.recencyScore(), source.engagementScore(), source.evidenceSummary(), null, null);
    }

    /**
     * Monta os dados desacoplados de persistência esperados para um sinal comercial.
     */
    private MarketWarmupSignalData signalData(MoisSalesLibraryDtos.MarketWarmupSignalCompleteItem signal) {
        return new MarketWarmupSignalData(signal.signalType().name(), signal.signalStrength(), signal.signalText(), signal.businessInterpretation());
    }

    /**
     * Monta os dados desacoplados de persistência esperados para o resumo calculado.
     */
    private MarketWarmupSummaryWriteData summaryData(MoisSalesLibraryDtos.MarketWarmupSummaryCompleteItem summary) {
        return new MarketWarmupSummaryWriteData(summary.scoreTotal(), summary.marketTemperature().name(), summary.ecosystemType().name(), summary.recommendation().name(),
                summary.mainPains(), summary.mainObjections(), summary.mainPromises(), summary.mainChannels(), summary.mainCompetitors(), summary.saturationRisk(),
                summary.opportunityRecommendation(), summary.nextExperimentSuggestion());
    }

    /**
     * Monta uma fonte pública mínima válida para conclusão do aquecimento.
     */
    private MoisSalesLibraryDtos.MarketWarmupSourceCompleteItem sampleSource() {
        return new MoisSalesLibraryDtos.MarketWarmupSourceCompleteItem(
                MoisSalesLibraryDtos.MarketWarmupPlatform.YOUTUBE,
                MoisSalesLibraryDtos.MarketWarmupSourceType.CREATOR_CONTENT,
                "https://youtube.com/watch?v=abc",
                "Vídeo sobre a dor",
                "Creator",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-09T00:00:00Z"),
                10_000L,
                5_000L,
                300L,
                80L,
                BigDecimal.valueOf(9),
                BigDecimal.valueOf(8),
                "Comentários recentes mostram dor explícita");
    }

    /**
     * Monta o resumo final mínimo calculado pelo worker.
     */
    private MoisSalesLibraryDtos.MarketWarmupSummaryCompleteItem sampleSummary() {
        return new MoisSalesLibraryDtos.MarketWarmupSummaryCompleteItem(
                BigDecimal.valueOf(82),
                MoisSalesLibraryDtos.MarketWarmupTemperature.HOT,
                MoisSalesLibraryDtos.MarketWarmupEcosystemType.CREATORS_HEATED,
                MoisSalesLibraryDtos.MarketWarmupRecommendation.PRIORITIZE,
                List.of("dor explícita"),
                List.of("preço"),
                List.of("resultado rápido"),
                List.of("YouTube"),
                List.of("Concorrente A"),
                "baixo",
                "priorizar experimento",
                "testar criativo com dor explícita");
    }
}
