package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.jdbc;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupClaimData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupJobData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSignalData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSignalReadData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSourceData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSummaryData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.MarketWarmupSummaryWriteData;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageMarketWarmupGateway.SalesPageWarmupData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Executa as operações JDBC da pesquisa de aquecimento de mercado da Biblioteca MOIS.
 */
@Repository
@RequiredArgsConstructor
public class MoisSalesPageMarketWarmupRepository implements MoisSalesPageMarketWarmupGateway {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Busca os dados mínimos da página consolidada que receberá a pesquisa de aquecimento.
     */
    @Override
    public Optional<SalesPageWarmupData> findSalesPage(long pageId) {
        List<SalesPageWarmupData> rows = jdbcTemplate.query("""
                SELECT sp.id, sp.workspace_id, sp.url_canonical,
                       COALESCE(NULLIF(sp.product_name, ''), NULLIF(cr.product_name, ''), sp.title) AS title,
                       COALESCE(NULLIF(cr.hotmart_producer, ''), NULLIF(cr.producer_name, '')) AS producer_name,
                       sp.current_status, sp.analysis_status,
                       sp.offer_summary, sp.mechanism_summary, sp.promise_summary, sp.proof_summary
                FROM mois_sales_page sp
                LEFT JOIN mois_collected_reference cr ON cr.id = sp.collected_reference_id
                WHERE sp.id = ?
                LIMIT 1
                """, this::mapSalesPage, pageId);
        return rows.stream().findFirst();
    }

    /**
     * Busca o job pendente ou reservado mais recente da página para reaproveitar solicitações duplicadas.
     */
    @Override
    public Optional<MarketWarmupJobData> findActiveJobByPage(long pageId) {
        List<MarketWarmupJobData> rows = jdbcTemplate.query("""
                SELECT id, sales_page_id, workspace_id, status, created_at, error_category, error_message
                FROM mois_sales_page_market_warmup_job
                WHERE sales_page_id = ? AND status IN ('PENDING', 'FETCHING')
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """, this::mapJob, pageId);
        return rows.stream().findFirst();
    }

    /**
     * Cria um job pendente de aquecimento para uma página elegível.
     */
    @Override
    public MarketWarmupJobData createPendingJob(SalesPageWarmupData page) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO mois_sales_page_market_warmup_job
                    (sales_page_id, workspace_id, status, attempt, created_at, updated_at)
                    VALUES (?, ?, 'PENDING', 1, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, page.pageId());
            statement.setString(2, page.workspaceId());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        long jobId = key == null ? 0L : key.longValue();
        return findJob(jobId).orElseGet(() -> new MarketWarmupJobData(
                jobId, page.pageId(), page.workspaceId(), "PENDING", Instant.now(), null, null));
    }

    /**
     * Busca o job mais recente da página, independentemente do status.
     */
    @Override
    public Optional<MarketWarmupJobData> findLatestJobByPage(long pageId) {
        List<MarketWarmupJobData> rows = jdbcTemplate.query("""
                SELECT id, sales_page_id, workspace_id, status, created_at, error_category, error_message
                FROM mois_sales_page_market_warmup_job
                WHERE sales_page_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """, this::mapJob, pageId);
        return rows.stream().findFirst();
    }

    /**
     * Busca o próximo job pendente no workspace para reserva pelo worker.
     */
    @Override
    public Optional<MarketWarmupClaimData> findNextPendingJob(String workspaceId) {
        List<MarketWarmupClaimData> rows = jdbcTemplate.query("""
                SELECT j.id AS job_id, j.sales_page_id, j.workspace_id, j.status, j.created_at, j.error_category, j.error_message,
                       sp.url_canonical,
                       COALESCE(NULLIF(sp.product_name, ''), NULLIF(cr.product_name, ''), sp.title) AS title,
                       COALESCE(NULLIF(cr.hotmart_producer, ''), NULLIF(cr.producer_name, '')) AS producer_name,
                       sp.current_status, sp.analysis_status,
                       sp.offer_summary, sp.mechanism_summary, sp.promise_summary, sp.proof_summary
                FROM mois_sales_page_market_warmup_job j
                JOIN mois_sales_page sp ON sp.id = j.sales_page_id
                LEFT JOIN mois_collected_reference cr ON cr.id = sp.collected_reference_id
                WHERE j.workspace_id = ? AND j.status = 'PENDING'
                  AND COALESCE(sp.analysis_status, sp.current_status) IN ('DONE', 'ANALYZED')
                ORDER BY j.created_at ASC, j.id ASC
                LIMIT 1
                """, this::mapClaim, workspaceId);
        return rows.stream().findFirst();
    }

    /**
     * Marca um job pendente como reservado pelo worker informado.
     */
    @Override
    public boolean claimPendingJob(long jobId, String workerId) {
        int updated = jdbcTemplate.update("""
                UPDATE mois_sales_page_market_warmup_job
                SET status = 'FETCHING', claimed_by = ?, started_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP()
                WHERE id = ? AND status = 'PENDING'
                """, workerId, jobId);
        return updated > 0;
    }

    /**
     * Busca um job pelo identificador operacional.
     */
    @Override
    public Optional<MarketWarmupJobData> findJob(long jobId) {
        List<MarketWarmupJobData> rows = jdbcTemplate.query("""
                SELECT id, sales_page_id, workspace_id, status, created_at, error_category, error_message
                FROM mois_sales_page_market_warmup_job
                WHERE id = ?
                LIMIT 1
                """, this::mapJob, jobId);
        return rows.stream().findFirst();
    }

    /**
     * Remove fontes, sinais e resumo anteriores do job antes da gravação final idempotente.
     */
    @Override
    public void deleteJobDetails(long jobId) {
        jdbcTemplate.update("DELETE FROM mois_sales_page_market_warmup_signal WHERE job_id = ?", jobId);
        jdbcTemplate.update("DELETE FROM mois_sales_page_market_warmup_source WHERE job_id = ?", jobId);
        jdbcTemplate.update("DELETE FROM mois_sales_page_market_warmup_summary WHERE job_id = ?", jobId);
    }

    /**
     * Insere uma fonte pública rastreável coletada pelo worker.
     */
    @Override
    public long insertSource(long jobId, long pageId, String workspaceId, MarketWarmupSourceData source) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO mois_sales_page_market_warmup_source
                    (job_id, sales_page_id, workspace_id, platform, source_type, source_url, source_title, author_name,
                     published_at, last_activity_at, followers_or_subscribers, views_count, likes_count, comments_count,
                     recency_score, engagement_score, evidence_summary, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, jobId);
            statement.setLong(2, pageId);
            statement.setString(3, workspaceId);
            statement.setString(4, source.platform());
            statement.setString(5, source.sourceType());
            statement.setString(6, source.sourceUrl());
            statement.setString(7, source.sourceTitle());
            statement.setString(8, source.authorName());
            statement.setTimestamp(9, toTimestamp(source.publishedAt()));
            statement.setTimestamp(10, toTimestamp(source.lastActivityAt()));
            statement.setObject(11, source.followersOrSubscribers());
            statement.setObject(12, source.viewsCount());
            statement.setObject(13, source.likesCount());
            statement.setObject(14, source.commentsCount());
            statement.setBigDecimal(15, source.recencyScore());
            statement.setBigDecimal(16, source.engagementScore());
            statement.setString(17, source.evidenceSummary());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    /**
     * Insere um sinal comercial vinculado à fonte pública correspondente.
     */
    @Override
    public void insertSignal(long jobId, long pageId, String workspaceId, long sourceId, MarketWarmupSignalData signal) {
        jdbcTemplate.update("""
                INSERT INTO mois_sales_page_market_warmup_signal
                (job_id, source_id, sales_page_id, workspace_id, signal_type, signal_strength, signal_text, business_interpretation, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP())
                """, jobId, sourceId, pageId, workspaceId, signal.signalType(), signal.signalStrength(), signal.signalText(), signal.businessInterpretation());
    }

    /**
     * Insere o resumo final calculado para o job de aquecimento.
     */
    @Override
    public void insertSummary(long jobId, long pageId, String workspaceId, MarketWarmupSummaryWriteData summary) {
        jdbcTemplate.update("""
                INSERT INTO mois_sales_page_market_warmup_summary
                (job_id, sales_page_id, workspace_id, score_total, market_temperature, ecosystem_type, main_pains,
                 main_objections, main_promises, main_channels, main_competitors, saturation_risk,
                 opportunity_recommendation, next_experiment_suggestion, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP())
                """, jobId, pageId, workspaceId, summary.scoreTotal(), summary.marketTemperature(), summary.ecosystemType(),
                joinLines(summary.mainPains()), joinLines(summary.mainObjections()), joinLines(summary.mainPromises()), joinLines(summary.mainChannels()),
                joinLines(summary.mainCompetitors()), summary.saturationRisk(), summary.opportunityRecommendation(), summary.nextExperimentSuggestion());
    }

    /**
     * Marca o job como concluído e replica os principais classificadores comerciais para busca rápida.
     */
    @Override
    public void markJobDone(long jobId, MarketWarmupSummaryWriteData summary, Instant finishedAt) {
        jdbcTemplate.update("""
                UPDATE mois_sales_page_market_warmup_job
                SET status = 'DONE', score_total = ?, market_temperature = ?, ecosystem_type = ?, recommendation = ?,
                    finished_at = ?, error_category = NULL, error_message = NULL, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, summary.scoreTotal(), summary.marketTemperature(), summary.ecosystemType(), summary.recommendation(),
                toTimestamp(finishedAt == null ? Instant.now() : finishedAt), jobId);
    }

    /**
     * Marca o job como falho com categoria e mensagem operacional.
     */
    @Override
    public boolean markJobFailed(long jobId, String errorCategory, String errorMessage) {
        int updated = jdbcTemplate.update("""
                UPDATE mois_sales_page_market_warmup_job
                SET status = 'FAILED', error_category = ?, error_message = ?, finished_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP()
                WHERE id = ? AND status IN ('PENDING', 'FETCHING')
                """, errorCategory, errorMessage, jobId);
        return updated > 0;
    }

    /**
     * Recoloca jobs presos em FETCHING antigo na fila pendente para reprocessamento operacional.
     */
    @Override
    public long requeueStaleFetchingJobs(String workspaceId, int staleMinutes) {
        int updated = jdbcTemplate.update("""
                UPDATE mois_sales_page_market_warmup_job
                SET status = 'PENDING', attempt = attempt + 1, claimed_by = NULL, started_at = NULL, finished_at = NULL,
                    error_category = 'STALE_FETCHING_REQUEUED',
                    error_message = 'Job FETCHING antigo recolocado na fila pela ação operacional da Fase 10',
                    updated_at = UTC_TIMESTAMP()
                WHERE workspace_id = ?
                  AND status = 'FETCHING'
                  AND COALESCE(started_at, updated_at, created_at) < DATE_SUB(UTC_TIMESTAMP(), INTERVAL ? MINUTE)
                """, workspaceId, staleMinutes);
        return updated;
    }

    /**
     * Busca o resumo mais recente da página com os dados do job associado.
     */
    @Override
    public Optional<MarketWarmupSummaryData> findLatestSummaryByPage(long pageId) {
        List<MarketWarmupSummaryData> rows = jdbcTemplate.query("""
                SELECT j.id AS job_id, j.sales_page_id, COALESCE(s.score_total, j.score_total) AS score_total,
                       COALESCE(s.market_temperature, j.market_temperature) AS market_temperature,
                       COALESCE(s.ecosystem_type, j.ecosystem_type) AS ecosystem_type,
                       j.recommendation, s.main_pains, s.main_objections, s.main_promises, s.main_channels, s.main_competitors,
                       s.saturation_risk, s.opportunity_recommendation, s.next_experiment_suggestion,
                       j.status, j.error_category, j.error_message, COALESCE(s.created_at, j.created_at) AS created_at,
                       COALESCE(s.updated_at, j.updated_at) AS updated_at
                FROM mois_sales_page_market_warmup_job j
                LEFT JOIN mois_sales_page_market_warmup_summary s ON s.job_id = j.id
                WHERE j.sales_page_id = ?
                ORDER BY j.created_at DESC, j.id DESC
                LIMIT 1
                """, this::mapSummary, pageId);
        return rows.stream().findFirst();
    }

    /**
     * Lista fontes públicas de um job de aquecimento.
     */
    @Override
    public List<MarketWarmupSourceData> listSources(long jobId) {
        return jdbcTemplate.query("""
                SELECT id, job_id, sales_page_id, platform, source_type, source_url, source_title, author_name,
                       published_at, last_activity_at, followers_or_subscribers, views_count, likes_count, comments_count,
                       recency_score, engagement_score, evidence_summary, created_at, updated_at
                FROM mois_sales_page_market_warmup_source
                WHERE job_id = ?
                ORDER BY last_activity_at DESC, created_at DESC, id DESC
                """, this::mapSource, jobId);
    }

    /**
     * Lista sinais comerciais extraídos de um job de aquecimento.
     */
    @Override
    public List<MarketWarmupSignalReadData> listSignals(long jobId) {
        return jdbcTemplate.query("""
                SELECT id, job_id, source_id, sales_page_id, signal_type, signal_strength, signal_text, business_interpretation, created_at
                FROM mois_sales_page_market_warmup_signal
                WHERE job_id = ?
                ORDER BY signal_strength DESC, created_at DESC, id DESC
                """, this::mapSignal, jobId);
    }

    /**
     * Converte uma linha de página consolidada em dados de entrada do aquecimento.
     */
    private SalesPageWarmupData mapSalesPage(ResultSet rs, int rowNum) throws SQLException {
        return new SalesPageWarmupData(rs.getLong("id"), rs.getString("workspace_id"), rs.getString("url_canonical"), rs.getString("title"),
                rs.getString("producer_name"), rs.getString("current_status"), rs.getString("analysis_status"),
                rs.getString("offer_summary"), rs.getString("mechanism_summary"), rs.getString("promise_summary"), rs.getString("proof_summary"));
    }

    /**
     * Converte uma linha de job em estado operacional do aquecimento.
     */
    private MarketWarmupJobData mapJob(ResultSet rs, int rowNum) throws SQLException {
        return new MarketWarmupJobData(rs.getLong("id"), rs.getLong("sales_page_id"), rs.getString("workspace_id"),
                rs.getString("status"), toInstant(rs.getTimestamp("created_at")),
                rs.getString("error_category"), rs.getString("error_message"));
    }

    /**
     * Converte uma linha pendente em payload de claim do worker.
     */
    private MarketWarmupClaimData mapClaim(ResultSet rs, int rowNum) throws SQLException {
        MarketWarmupJobData job = new MarketWarmupJobData(rs.getLong("job_id"), rs.getLong("sales_page_id"), rs.getString("workspace_id"),
                rs.getString("status"), toInstant(rs.getTimestamp("created_at")),
                rs.getString("error_category"), rs.getString("error_message"));
        SalesPageWarmupData page = new SalesPageWarmupData(rs.getLong("sales_page_id"), rs.getString("workspace_id"), rs.getString("url_canonical"),
                rs.getString("title"), rs.getString("producer_name"), rs.getString("current_status"), rs.getString("analysis_status"),
                rs.getString("offer_summary"), rs.getString("mechanism_summary"), rs.getString("promise_summary"), rs.getString("proof_summary"));
        return new MarketWarmupClaimData(job, page);
    }

    /**
     * Converte uma linha de resumo em resposta interna de persistência.
     */
    private MarketWarmupSummaryData mapSummary(ResultSet rs, int rowNum) throws SQLException {
        String temperature = rs.getString("market_temperature");
        String ecosystem = rs.getString("ecosystem_type");
        String recommendation = rs.getString("recommendation");
        return new MarketWarmupSummaryData(
                rs.getLong("job_id"), rs.getLong("sales_page_id"), rs.getBigDecimal("score_total"),
                temperature == null ? null : temperature,
                ecosystem == null ? null : ecosystem,
                recommendation == null ? null : recommendation,
                rs.getString("main_pains"), rs.getString("main_objections"), rs.getString("main_promises"), rs.getString("main_channels"),
                rs.getString("main_competitors"), rs.getString("saturation_risk"), rs.getString("opportunity_recommendation"),
                rs.getString("next_experiment_suggestion"), rs.getString("status"),
                rs.getString("error_category"), rs.getString("error_message"), toInstant(rs.getTimestamp("created_at")), toInstant(rs.getTimestamp("updated_at")));
    }

    /**
     * Converte uma linha de fonte em contrato de leitura para revisão humana.
     */
    private MarketWarmupSourceData mapSource(ResultSet rs, int rowNum) throws SQLException {
        return new MarketWarmupSourceData(
                rs.getLong("id"), rs.getLong("job_id"), rs.getLong("sales_page_id"),
                rs.getString("platform"),
                rs.getString("source_type"),
                rs.getString("source_url"), rs.getString("source_title"), rs.getString("author_name"),
                toInstant(rs.getTimestamp("published_at")), toInstant(rs.getTimestamp("last_activity_at")),
                rs.getObject("followers_or_subscribers", Long.class), rs.getObject("views_count", Long.class),
                rs.getObject("likes_count", Long.class), rs.getObject("comments_count", Long.class),
                rs.getBigDecimal("recency_score"), rs.getBigDecimal("engagement_score"), rs.getString("evidence_summary"),
                toInstant(rs.getTimestamp("created_at")), toInstant(rs.getTimestamp("updated_at")));
    }

    /**
     * Converte uma linha de sinal em contrato de leitura para explicação do score.
     */
    private MarketWarmupSignalReadData mapSignal(ResultSet rs, int rowNum) throws SQLException {
        return new MarketWarmupSignalReadData(
                rs.getLong("id"), rs.getLong("job_id"), rs.getLong("source_id"), rs.getLong("sales_page_id"),
                rs.getString("signal_type"), rs.getBigDecimal("signal_strength"),
                rs.getString("signal_text"), rs.getString("business_interpretation"), toInstant(rs.getTimestamp("created_at")));
    }

    /**
     * Converte listas funcionais em linhas de texto simples sem serializar JSON dentro de campos textuais.
     */
    private String joinLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).reduce((left, right) -> left + "\n" + right).orElse(null);
    }

    /**
     * Converte timestamp JDBC em Instant preservando nulo.
     */
    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    /**
     * Converte Instant em timestamp JDBC preservando nulo.
     */
    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
