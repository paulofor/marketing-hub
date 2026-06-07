package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gerencia capturas HTML das páginas de venda canônicas da biblioteca MOIS usando o modelo operacional consolidado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoisSalesLibrarySnapshotService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;
    private static final Duration FAILED_RETRY_COOLDOWN = Duration.ofHours(24);
    private static final int MAX_FAILED_ATTEMPTS_WITHOUT_FORCE = 3;
    private static final Duration CAPTURE_REQUEST_TIMEOUT = Duration.ofMinutes(5);

    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CAPTURE_REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Captura páginas diretamente pelo backend para acionamentos manuais sobre o modelo operacional novo.
     */
    public MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureResponse captureSnapshots(
            MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest request
    ) {
        int limit = normalizeLimit(request.limit());
        boolean force = Boolean.TRUE.equals(request.force());
        List<PageToCapture> pages = findPagesToCapture(request.workspaceId(), limit, force);
        List<MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem> items = pages.stream()
                .map(this::captureOneSafely)
                .toList();
        int captured = (int) items.stream().filter(item -> "CAPTURED".equals(item.status()) || "DUPLICATE".equals(item.status())).count();
        int failed = (int) items.stream().filter(item -> "FAILED".equals(item.status())).count();
        return new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureResponse(
                request.workspaceId(), limit, force, items.size(), captured, failed, items, Instant.now());
    }

    /**
     * Lista as capturas registradas para uma página no histórico consolidado de execuções.
     */
    public List<MoisSalesLibraryDtos.SalesLibraryPageSnapshotResponse> listSnapshots(long pageId) {
        return jdbcTemplate.query("""
                SELECT id, sales_page_id, raw_html_sha256, status, http_status, content_type,
                       final_url, redirect_root_url, raw_html_bytes, screenshot_bytes, finished_at, updated_at
                FROM mois_sales_page_job_execution
                WHERE sales_page_id = ? AND stage = 'CAPTURE'
                ORDER BY COALESCE(finished_at, updated_at) DESC, id DESC
                LIMIT 20
                """, (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryPageSnapshotResponse(
                rs.getLong("id"),
                rs.getLong("sales_page_id"),
                rs.getString("raw_html_sha256"),
                rs.getString("status"),
                (Integer) rs.getObject("http_status"),
                rs.getString("content_type"),
                rs.getString("final_url"),
                rs.getString("redirect_root_url"),
                rs.getLong("raw_html_bytes"),
                rs.getLong("screenshot_bytes"),
                toInstant(rs.getTimestamp("finished_at")),
                toInstant(rs.getTimestamp("updated_at"))
        ), pageId);
    }

    /**
     * Reserva a próxima página consolidada para a etapa worker de obtenção de HTML bruto.
     */
    @Transactional
    public MoisSalesLibraryDtos.HtmlCaptureClaimResponse claimHtmlCapture(
            MoisSalesLibraryDtos.HtmlCaptureClaimRequest request
    ) {
        int limit = normalizeLimit(request.limit());
        boolean force = Boolean.TRUE.equals(request.force());
        List<PageToCapture> pages = findPagesToCapture(request.workspaceId(), limit, force);
        if (pages.isEmpty()) {
            return new MoisSalesLibraryDtos.HtmlCaptureClaimResponse(false, null);
        }
        PageToCapture page = pages.get(0);
        String claimedBy = UUID.randomUUID().toString();
        long executionId = insertFetchingCaptureExecution(page, claimedBy);
        updatePageForCaptureClaim(page.pageId(), executionId);
        log.info("MOIS sales-library captura reservada no modelo operacional novo. modulo=MOIS, operacao=claimHtmlCapture, workspaceId={}, pageId={}, executionId={}",
                request.workspaceId(), page.pageId(), executionId);
        return new MoisSalesLibraryDtos.HtmlCaptureClaimResponse(
                true,
                new MoisSalesLibraryDtos.HtmlCaptureJobResponse(executionId, page.pageId(), page.urlCanonical(), page.title()));
    }

    /**
     * Persiste o HTML bruto capturado pelo worker diretamente na execução operacional consolidada.
     */
    @Transactional
    public MoisSalesLibraryDtos.HtmlCapturePersistResponse completeHtmlCapture(
            long executionId,
            MoisSalesLibraryDtos.HtmlCaptureCompleteRequest request
    ) {
        CaptureExecution execution = findCaptureExecution(executionId);
        String rawHtml = request.rawHtml() == null ? "" : request.rawHtml();
        byte[] rawHtmlBytes = rawHtml.getBytes(StandardCharsets.UTF_8);
        String hash = request.sha256() == null || request.sha256().isBlank() ? sha256(rawHtml) : request.sha256();
        Instant capturedAt = request.capturedAt() == null ? Instant.now() : request.capturedAt();
        Long duplicateExecutionId = findDuplicateCaptureExecution(executionId, execution.pageId(), hash);
        if (duplicateExecutionId != null) {
            String duplicateMessage = "HTML idêntico à execução " + duplicateExecutionId;
            jdbcTemplate.update("""
                    UPDATE mois_sales_page_job_execution
                    SET status = 'DUPLICATE', http_status = ?, content_type = ?, final_url = ?, redirect_root_url = ?,
                        raw_html_sha256 = ?, raw_html_bytes = ?, error_category = NULL, error_message = ?, finished_at = ?, updated_at = UTC_TIMESTAMP()
                    WHERE id = ?
                    """, request.httpStatus(), truncate(request.contentType(), 255), truncate(resolveFinalUrl(request), 1024),
                    truncate(request.redirectRootUrl(), 1024), hash, rawHtmlBytes.length, duplicateMessage, Timestamp.from(capturedAt), executionId);
            updatePageForCaptureCompletion(execution.pageId(), executionId, "DUPLICATE", request, hash, rawHtmlBytes.length, null, capturedAt);
            return new MoisSalesLibraryDtos.HtmlCapturePersistResponse(executionId, "DUPLICATE");
        }
        byte[] screenshot = renderBasicScreenshot(rawHtml, resolveFinalUrl(request));
        jdbcTemplate.update("""
                UPDATE mois_sales_page_job_execution
                SET status = 'CAPTURED', final_url = ?, redirect_root_url = ?, http_status = ?, content_type = ?, raw_html = ?,
                    raw_html_sha256 = ?, raw_html_bytes = ?, screenshot_blob = ?, screenshot_bytes = ?, error_category = NULL,
                    error_message = NULL, finished_at = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, truncate(resolveFinalUrl(request), 1024), truncate(request.redirectRootUrl(), 1024), request.httpStatus(),
                truncate(request.contentType(), 255), rawHtml, hash, rawHtmlBytes.length, screenshot, screenshot.length,
                Timestamp.from(capturedAt), executionId);
        updatePageForCaptureCompletion(execution.pageId(), executionId, "CAPTURED", request, hash, rawHtmlBytes.length, null, capturedAt);
        log.info("MOIS sales-library captura concluída no modelo operacional novo. modulo=MOIS, operacao=completeHtmlCapture, pageId={}, executionId={}, status=CAPTURED, bytes={}",
                execution.pageId(), executionId, rawHtmlBytes.length);
        return new MoisSalesLibraryDtos.HtmlCapturePersistResponse(executionId, "CAPTURED");
    }

    /**
     * Registra falha da etapa worker de obtenção de HTML bruto no histórico operacional consolidado.
     */
    @Transactional
    public MoisSalesLibraryDtos.HtmlCapturePersistResponse failHtmlCapture(
            long executionId,
            MoisSalesLibraryDtos.HtmlCaptureFailRequest request
    ) {
        CaptureExecution execution = findCaptureExecution(executionId);
        String message = request.errorMessage() == null || request.errorMessage().isBlank()
                ? request.errorCategory()
                : request.errorMessage();
        jdbcTemplate.update("""
                UPDATE mois_sales_page_job_execution
                SET status = 'FAILED', http_status = ?, final_url = ?, redirect_root_url = ?, error_category = ?, error_message = ?,
                    finished_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, request.httpStatus(), truncate(request.redirectDestinationUrl(), 1024), truncate(request.redirectRootUrl(), 1024),
                truncate(request.errorCategory(), 120), truncate(message, 1000), executionId);
        jdbcTemplate.update("""
                UPDATE mois_sales_page
                SET current_stage = 'CAPTURE', current_status = 'FAILED', capture_status = 'FAILED', http_status = ?,
                    url_final = ?, redirect_root_url = ?, last_error_category = ?, last_error_message = ?, last_job_execution_id = ?,
                    updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, request.httpStatus(), truncate(request.redirectDestinationUrl(), 1024), truncate(request.redirectRootUrl(), 1024),
                truncate(request.errorCategory(), 120), truncate(message, 1000), executionId, execution.pageId());
        log.info("MOIS sales-library captura falhou no modelo operacional novo. modulo=MOIS, operacao=failHtmlCapture, pageId={}, executionId={}, errorCategory={}",
                execution.pageId(), executionId, request.errorCategory());
        return new MoisSalesLibraryDtos.HtmlCapturePersistResponse(executionId, "FAILED");
    }

    /**
     * Seleciona páginas sem HTML útil, usando html_bytes = 0 como critério para processamento da etapa 1.
     */
    private List<PageToCapture> findPagesToCapture(String workspaceId, int limit, boolean force) {
        String forceClause = force ? "" : """
                AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_page_job_execution e
                    WHERE e.sales_page_id = sp.id AND e.stage = 'CAPTURE' AND e.status = 'FETCHING'
                )
                AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_page_job_execution recent_failure
                    WHERE recent_failure.sales_page_id = sp.id
                      AND recent_failure.stage = 'CAPTURE'
                      AND recent_failure.status = 'FAILED'
                      AND recent_failure.finished_at >= ?
                )
                AND (
                    SELECT COUNT(1)
                    FROM mois_sales_page_job_execution repeated_failure
                    WHERE repeated_failure.sales_page_id = sp.id
                      AND repeated_failure.stage = 'CAPTURE'
                      AND repeated_failure.status = 'FAILED'
                ) < ?
                """;
        String query = """
                SELECT sp.id, sp.url_canonical, sp.title
                FROM mois_sales_page sp
                WHERE sp.workspace_id = ?
                  AND sp.current_status NOT IN ('FETCHING', 'CAPTURING', 'DISCARDED')
                  AND COALESCE(sp.html_bytes, 0) = 0
                """ + forceClause + """
                ORDER BY sp.updated_at DESC, sp.id DESC
                LIMIT ?
                """;
        if (force) {
            return jdbcTemplate.query(query, this::mapPageToCapture, workspaceId, limit);
        }
        Timestamp retryCutoff = Timestamp.from(Instant.now().minus(FAILED_RETRY_COOLDOWN));
        return jdbcTemplate.query(
                query,
                this::mapPageToCapture,
                workspaceId,
                retryCutoff,
                MAX_FAILED_ATTEMPTS_WITHOUT_FORCE,
                limit);
    }

    /** Converte uma linha JDBC da página consolidada na estrutura interna de captura. */
    private PageToCapture mapPageToCapture(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new PageToCapture(rs.getLong("id"), rs.getString("url_canonical"), rs.getString("title"));
    }

    /** Executa uma captura isolada convertendo qualquer exceção em execução FAILED auditável. */
    private MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem captureOneSafely(PageToCapture page) {
        long executionId = insertFetchingCaptureExecution(page, "backend-capture");
        updatePageForCaptureClaim(page.pageId(), executionId);
        try {
            return captureOne(page, executionId);
        } catch (Exception ex) {
            String errorMessage = categorizeExceptionFailure(ex);
            log.warn("Falha ao capturar HTML bruto da sales page MOIS. pageId={}, executionId={}, url={}, categoria={}",
                    page.pageId(), executionId, page.urlCanonical(), errorMessage, ex);
            failCaptureExecution(page, executionId, errorMessage, null, null, null, null);
            return new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem(
                    page.pageId(), executionId, page.urlCanonical(), null, null, "FAILED", null, null, 0L, 0L, errorMessage);
        }
    }

    /** Busca a URL canônica, tenta fallback pela raiz do redirecionamento e persiste a execução quando a captura é útil. */
    private MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem captureOne(PageToCapture page, long executionId) throws Exception {
        CaptureResponse primary = fetchUrl(page.urlCanonical());
        String redirectDestinationUrl = primary.finalUrl();
        String redirectRootUrl = rootUrl(redirectDestinationUrl);
        CaptureResponse effective = primary;
        if (!primary.isCapturable() && shouldTryRedirectRoot(page.urlCanonical(), redirectDestinationUrl, redirectRootUrl)) {
            log.info("MOIS sales-library captura tentando raiz do redirecionamento. pageId={}, executionId={}, urlCanonical={}, redirectDestinationUrl={}, redirectRootUrl={}",
                    page.pageId(), executionId, page.urlCanonical(), redirectDestinationUrl, redirectRootUrl);
            effective = fetchUrl(redirectRootUrl);
        }

        log.info("MOIS sales-library captura payload bruto recebido. pageId={}, executionId={}, url={}, redirectDestinationUrl={}, redirectRootUrl={}, effectiveUrl={}, httpStatus={}, contentType={}, htmlPreview={}",
                page.pageId(), executionId, page.urlCanonical(), redirectDestinationUrl, redirectRootUrl, effective.finalUrl(), effective.statusCode(),
                effective.contentType(), truncate(effective.rawHtml(), 4000));

        if (!effective.isCapturable()) {
            String errorMessage = categorizeHttpFailure(effective.statusCode(), effective.rawHtml());
            failCaptureExecution(page, executionId, errorMessage, effective.statusCode(), effective.contentType(), redirectDestinationUrl, redirectRootUrl);
            return new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem(
                    page.pageId(), executionId, page.urlCanonical(), redirectDestinationUrl, redirectRootUrl, "FAILED", null, effective.statusCode(), 0L, 0L,
                    errorMessage);
        }

        String rawHtml = effective.rawHtml();
        String hash = sha256(rawHtml);
        byte[] rawHtmlBytes = rawHtml.getBytes(StandardCharsets.UTF_8);
        Long existingExecutionId = findExistingCaptureExecution(page.pageId(), hash);
        if (existingExecutionId != null) {
            markCaptureDuplicate(page, executionId, existingExecutionId, hash, rawHtmlBytes.length, effective.statusCode(), effective.contentType(), redirectDestinationUrl, redirectRootUrl);
            return new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem(
                    page.pageId(), executionId, page.urlCanonical(), redirectDestinationUrl, redirectRootUrl, "DUPLICATE", hash,
                    effective.statusCode(), rawHtmlBytes.length, 0L, null);
        }

        byte[] screenshot = renderBasicScreenshot(rawHtml, effective.finalUrl());
        jdbcTemplate.update("""
                UPDATE mois_sales_page_job_execution
                SET status = 'CAPTURED', final_url = ?, redirect_root_url = ?, http_status = ?, content_type = ?, raw_html = ?,
                    raw_html_sha256 = ?, raw_html_bytes = ?, screenshot_blob = ?, screenshot_bytes = ?, error_category = NULL,
                    error_message = NULL, finished_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, truncate(redirectDestinationUrl, 1024), truncate(redirectRootUrl, 1024), effective.statusCode(),
                truncate(effective.contentType(), 255), rawHtml, hash, rawHtmlBytes.length, screenshot, screenshot.length, executionId);
        updatePageCaptureState(page.pageId(), executionId, "CAPTURED", effective.statusCode(), effective.contentType(), redirectDestinationUrl,
                redirectRootUrl, hash, rawHtmlBytes.length, null, null, Instant.now());
        return new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem(
                page.pageId(), executionId, page.urlCanonical(), redirectDestinationUrl, redirectRootUrl, "CAPTURED", hash,
                effective.statusCode(), rawHtmlBytes.length, screenshot.length, null);
    }

    /** Registra a execução de captura com status FETCHING e retorna sua chave. */
    private long insertFetchingCaptureExecution(PageToCapture page, String claimedBy) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO mois_sales_page_job_execution
                    (sales_page_id, workspace_id, job_type, stage, status, attempt, claimed_by, input_url, started_at, created_at, updated_at)
                    SELECT id, workspace_id, 'HTML_CAPTURE', 'CAPTURE', 'FETCHING', 1, ?, url_canonical, UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_TIMESTAMP()
                    FROM mois_sales_page
                    WHERE id = ?
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, claimedBy);
            ps.setLong(2, page.pageId());
            return ps;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    /** Atualiza o estado atual da página quando uma captura é reservada. */
    private void updatePageForCaptureClaim(long pageId, long executionId) {
        jdbcTemplate.update("""
                UPDATE mois_sales_page
                SET current_stage = 'CAPTURE', current_status = 'FETCHING', capture_status = 'FETCHING',
                    last_error_category = NULL, last_error_message = NULL, last_job_execution_id = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, executionId, pageId);
    }

    /** Localiza a execução de captura ainda manipulável pelo endpoint do worker. */
    private CaptureExecution findCaptureExecution(long executionId) {
        List<CaptureExecution> rows = jdbcTemplate.query("""
                SELECT id, sales_page_id
                FROM mois_sales_page_job_execution
                WHERE id = ? AND stage = 'CAPTURE' AND status IN ('FETCHING', 'PENDING')
                LIMIT 1
                """, (rs, rowNum) -> new CaptureExecution(rs.getLong("id"), rs.getLong("sales_page_id")), executionId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Capture execution not found: " + executionId);
        }
        return rows.get(0);
    }

    /** Atualiza a execução e a página para falha em capturas backend/manuais. */
    private void failCaptureExecution(PageToCapture page, long executionId, String errorMessage, Integer httpStatus, String contentType, String finalUrl, String redirectRootUrl) {
        jdbcTemplate.update("""
                UPDATE mois_sales_page_job_execution
                SET status = 'FAILED', http_status = ?, content_type = ?, final_url = ?, redirect_root_url = ?, error_category = 'CAPTURE_FAILED',
                    error_message = ?, finished_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, httpStatus, truncate(contentType, 255), truncate(finalUrl, 1024), truncate(redirectRootUrl, 1024), truncate(errorMessage, 1000), executionId);
        updatePageCaptureState(page.pageId(), executionId, "FAILED", httpStatus, contentType, finalUrl, redirectRootUrl, null, 0L,
                "CAPTURE_FAILED", errorMessage, Instant.now());
    }

    /** Marca uma execução como duplicada e atualiza o estado atual da página. */
    private void markCaptureDuplicate(PageToCapture page, long executionId, long existingExecutionId, String hash, long rawHtmlBytes, int httpStatus, String contentType, String finalUrl, String redirectRootUrl) {
        String duplicateMessage = "HTML idêntico à execução " + existingExecutionId;
        jdbcTemplate.update("""
                UPDATE mois_sales_page_job_execution
                SET status = 'DUPLICATE', http_status = ?, content_type = ?, final_url = ?, redirect_root_url = ?,
                    raw_html_sha256 = ?, raw_html_bytes = ?, error_category = NULL, error_message = ?, finished_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, httpStatus, truncate(contentType, 255), truncate(finalUrl, 1024), truncate(redirectRootUrl, 1024),
                hash, rawHtmlBytes, duplicateMessage, executionId);
        updatePageCaptureState(page.pageId(), executionId, "DUPLICATE", httpStatus, contentType, finalUrl, redirectRootUrl, hash, rawHtmlBytes,
                null, duplicateMessage, Instant.now());
    }

    /** Atualiza a página para refletir a conclusão da captura feita pelo endpoint worker. */
    private void updatePageForCaptureCompletion(long pageId, long executionId, String status, MoisSalesLibraryDtos.HtmlCaptureCompleteRequest request,
                                                String hash, long rawHtmlBytes, String errorMessage, Instant capturedAt) {
        updatePageCaptureState(pageId, executionId, status, request.httpStatus(), request.contentType(), resolveFinalUrl(request), request.redirectRootUrl(),
                hash, rawHtmlBytes, null, errorMessage, capturedAt);
    }

    /** Atualiza os campos consolidados de captura na página operacional. */
    private void updatePageCaptureState(long pageId, long executionId, String status, Integer httpStatus, String contentType, String finalUrl,
                                        String redirectRootUrl, String hash, long rawHtmlBytes, String errorCategory, String errorMessage, Instant capturedAt) {
        jdbcTemplate.update("""
                UPDATE mois_sales_page
                SET current_stage = 'CAPTURE', current_status = ?, capture_status = ?, http_status = ?, content_type = ?, url_final = ?,
                    redirect_root_url = ?, html_sha256 = COALESCE(?, html_sha256), html_bytes = ?, last_error_category = ?, last_error_message = ?,
                    last_job_execution_id = ?, last_captured_at = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, status, status, httpStatus, truncate(contentType, 255), truncate(finalUrl, 1024), truncate(redirectRootUrl, 1024),
                hash, rawHtmlBytes, truncate(errorCategory, 120), truncate(errorMessage, 1000), executionId, Timestamp.from(capturedAt), pageId);
    }

    /** Localiza outra captura concluída com o mesmo hash para a mesma página. */
    private Long findDuplicateCaptureExecution(long executionId, long pageId, String hash) {
        List<Long> rows = jdbcTemplate.query("""
                SELECT id
                FROM mois_sales_page_job_execution
                WHERE sales_page_id = ? AND stage = 'CAPTURE' AND raw_html_sha256 = ? AND id <> ? AND status IN ('CAPTURED', 'DUPLICATE')
                ORDER BY id DESC
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("id"), pageId, hash, executionId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Localiza captura existente com hash idêntico antes de armazenar conteúdo duplicado. */
    private Long findExistingCaptureExecution(long pageId, String hash) {
        List<Long> rows = jdbcTemplate.query("""
                SELECT id
                FROM mois_sales_page_job_execution
                WHERE sales_page_id = ? AND stage = 'CAPTURE' AND raw_html_sha256 = ? AND status IN ('CAPTURED', 'DUPLICATE')
                ORDER BY id DESC
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("id"), pageId, hash);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Resolve a URL final preferindo o destino efetivo informado pelo worker. */
    private String resolveFinalUrl(MoisSalesLibraryDtos.HtmlCaptureCompleteRequest request) {
        String finalUrl = request.finalUrl();
        if (finalUrl != null && !finalUrl.isBlank()) {
            return finalUrl;
        }
        return request.redirectDestinationUrl();
    }

    /** Extrai a chave gerada pelo banco após um INSERT com GeneratedKeyHolder. */
    private long generatedId(KeyHolder keyHolder) {
        if (keyHolder.getKeys() != null && keyHolder.getKeys().get("id") instanceof Number id) {
            return id.longValue();
        }
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    /** Renderiza uma prévia PNG básica do HTML para auditoria visual da captura. */
    private byte[] renderBasicScreenshot(String rawHtml, String baseUrl) {
        try {
            JEditorPane editorPane = new JEditorPane("text/html", rawHtml);
            editorPane.setEditable(false);
            editorPane.setSize(new Dimension(1200, 1600));
            BufferedImage image = new BufferedImage(1200, 1600, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            editorPane.printAll(graphics);
            graphics.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception ex) {
            log.warn("Falha ao renderizar screenshot básico da sales page MOIS. baseUrl={}, erroClasse={}, erro={}",
                    baseUrl, ex.getClass().getName(), ex.getMessage(), ex);
            return new byte[0];
        }
    }

    /** Normaliza o limite solicitado para evitar lotes grandes demais no backend. */
    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
    }

    /** Executa GET HTTP com follow redirect para obter o HTML efetivo. */
    private CaptureResponse fetchUrl(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(CAPTURE_REQUEST_TIMEOUT)
                .GET()
                .header("User-Agent", "MarketingHub-MOIS-SalesLibrary/1.0")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String rawHtml = response.body() == null ? "" : response.body();
        String contentType = response.headers().firstValue("content-type").orElse("text/html");
        String finalUrl = response.uri() == null ? url : response.uri().toString();
        return new CaptureResponse(rawHtml, finalUrl, response.statusCode(), contentType);
    }

    /** Decide se vale tentar a raiz do domínio de destino quando a URL redirecionada falha. */
    private boolean shouldTryRedirectRoot(String originalUrl, String redirectDestinationUrl, String redirectRootUrl) {
        return redirectDestinationUrl != null
                && redirectRootUrl != null
                && !redirectDestinationUrl.equals(originalUrl)
                && !redirectRootUrl.equals(redirectDestinationUrl)
                && !redirectRootUrl.equals(originalUrl);
    }

    /** Extrai a raiz scheme://host[:port] da URL final de redirecionamento. */
    private String rootUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + port;
        } catch (IllegalArgumentException ex) {
            log.warn("Falha ao extrair raiz da URL redirecionada MOIS. url={}, erroClasse={}, erro={}",
                    url, ex.getClass().getName(), ex.getMessage(), ex);
            return null;
        }
    }

    /** Classifica falhas HTTP para impedir que URLs indisponíveis sejam tratadas como pendência normal. */
    private String categorizeHttpFailure(int httpStatus, String rawHtml) {
        String preview = truncate(rawHtml == null ? "" : rawHtml.strip(), 200);
        if (httpStatus == 404) {
            return "HTTP_404: página final não encontrada";
        }
        if (httpStatus == 503 && preview.toLowerCase().contains("dns resolution failure")) {
            return "DESTINATION_DNS_FAILURE: destino final indisponível por falha de DNS";
        }
        if (httpStatus >= 300 && httpStatus < 400) {
            return "REDIRECT_WITHOUT_HTML: redirecionamento não entregou HTML capturável";
        }
        if (httpStatus >= 500) {
            return "FINAL_URL_UNAVAILABLE: HTTP " + httpStatus + " sem HTML capturável";
        }
        if (preview.isBlank()) {
            return "EMPTY_HTML: HTTP " + httpStatus + " sem corpo HTML capturável";
        }
        return "HTTP_NOT_CAPTURABLE: HTTP " + httpStatus + " sem HTML capturável";
    }

    /** Classifica exceções de captura para distinguir DNS, timeout, bloqueio e falhas genéricas. */
    private String categorizeExceptionFailure(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        String normalized = message.toLowerCase();
        if (ex instanceof java.net.http.HttpTimeoutException || normalized.contains("timeout")) {
            return "TIMEOUT: captura excedeu o tempo limite";
        }
        if (ex instanceof java.net.UnknownHostException || normalized.contains("dns") || normalized.contains("unknownhost")) {
            return "DESTINATION_DNS_FAILURE: " + message;
        }
        if (normalized.contains("too many redirects") || normalized.contains("redirect")) {
            return "REDIRECT_FAILURE: " + message;
        }
        return "CAPTURE_EXCEPTION: " + message;
    }

    /** Calcula o hash SHA-256 do HTML bruto para versionamento e deduplicação. */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            log.warn("Falha ao calcular hash de captura HTML MOIS. operacao=sha256, erroClasse={}, erro={}",
                    ex.getClass().getName(), ex.getMessage(), ex);
            throw new IllegalStateException("Falha ao calcular hash de captura HTML", ex);
        }
    }

    /** Limita textos persistidos em colunas com tamanho máximo. */
    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /** Converte timestamps JDBC opcionais para Instant. */
    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record PageToCapture(long pageId, String urlCanonical, String title) {
    }

    private record CaptureExecution(long executionId, long pageId) {
    }

    private record CaptureResponse(String rawHtml, String finalUrl, int statusCode, String contentType) {

        /** Informa se a resposta capturada possui corpo aproveitável para análise. */
        private boolean isCapturable() {
            return statusCode >= 200 && statusCode < 400 && rawHtml != null && !rawHtml.isBlank();
        }
    }
}
