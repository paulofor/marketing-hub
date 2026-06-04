package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesPageDualWriteGateway;
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
 * Gerencia snapshots HTML das páginas de venda canônicas da biblioteca MOIS.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoisSalesLibrarySnapshotService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;
    private static final String ARTIFACT_RAW_HTML = "RAW_HTML";
    private static final String ARTIFACT_SCREENSHOT_PNG = "SCREENSHOT_PNG";
    private static final Duration FAILED_RETRY_COOLDOWN = Duration.ofHours(24);
    private static final int MAX_FAILED_ATTEMPTS_WITHOUT_FORCE = 3;

    private final JdbcTemplate jdbcTemplate;
    private final MoisSalesPageDualWriteGateway dualWriteGateway;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Captura snapshots diretamente pelo backend para acionamentos manuais ou legados.
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
     * Lista os snapshots já persistidos para uma página da biblioteca.
     */
    public List<MoisSalesLibraryDtos.SalesLibraryPageSnapshotResponse> listSnapshots(long pageId) {
        return jdbcTemplate.query("""
                SELECT s.id, s.url_ingest_id, s.snapshot_hash, s.status, s.http_status, s.content_type,
                       s.redirect_destination_url, s.redirect_root_url,
                       COALESCE(raw.size_bytes, 0) AS raw_html_bytes,
                       COALESCE(png.size_bytes, 0) AS screenshot_bytes,
                       s.captured_at, s.updated_at
                FROM mois_sales_library_page_snapshot s
                LEFT JOIN mois_sales_library_snapshot_artifact raw
                  ON raw.snapshot_id = s.id AND raw.artifact_type = 'RAW_HTML'
                LEFT JOIN mois_sales_library_snapshot_artifact png
                  ON png.snapshot_id = s.id AND png.artifact_type = 'SCREENSHOT_PNG'
                WHERE s.url_ingest_id = ?
                ORDER BY s.captured_at DESC, s.id DESC
                LIMIT 20
                """, (rs, rowNum) -> new MoisSalesLibraryDtos.SalesLibraryPageSnapshotResponse(
                rs.getLong("id"),
                rs.getLong("url_ingest_id"),
                rs.getString("snapshot_hash"),
                rs.getString("status"),
                (Integer) rs.getObject("http_status"),
                rs.getString("content_type"),
                rs.getString("redirect_destination_url"),
                rs.getString("redirect_root_url"),
                rs.getLong("raw_html_bytes"),
                rs.getLong("screenshot_bytes"),
                toInstant(rs.getTimestamp("captured_at")),
                toInstant(rs.getTimestamp("updated_at"))
        ), pageId);
    }


    /**
     * Reserva a próxima URL normalizada da biblioteca para a etapa worker de obtenção de HTML bruto.
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
        Long snapshotId = insertFetchingSnapshot(page);
        dualWriteGateway.syncSnapshot(snapshotId);
        return new MoisSalesLibraryDtos.HtmlCaptureClaimResponse(
                true,
                new MoisSalesLibraryDtos.HtmlCaptureJobResponse(snapshotId, page.pageId(), page.urlCanonical(), page.title()));
    }

    /**
     * Persiste o HTML bruto capturado pelo worker como artefato RAW_HTML versionado.
     */
    @Transactional
    public MoisSalesLibraryDtos.HtmlCapturePersistResponse completeHtmlCapture(
            long snapshotId,
            MoisSalesLibraryDtos.HtmlCaptureCompleteRequest request
    ) {
        String rawHtml = request.rawHtml() == null ? "" : request.rawHtml();
        byte[] rawHtmlBytes = rawHtml.getBytes(StandardCharsets.UTF_8);
        String hash = request.sha256() == null || request.sha256().isBlank() ? sha256(rawHtml) : request.sha256();
        Long duplicateSnapshotId = findDuplicateSnapshot(snapshotId, hash);
        if (duplicateSnapshotId != null) {
            jdbcTemplate.update("""
                    UPDATE mois_sales_library_page_snapshot
                    SET status = 'DUPLICATE', http_status = ?, content_type = ?, redirect_destination_url = ?, redirect_root_url = ?, error_message = ?, captured_at = ?, updated_at = UTC_TIMESTAMP()
                    WHERE id = ?
                    """, request.httpStatus(), truncate(request.contentType(), 255), truncate(request.redirectDestinationUrl(), 1024),
                    truncate(request.redirectRootUrl(), 1024), "HTML idêntico ao snapshot " + duplicateSnapshotId,
                    Timestamp.from(request.capturedAt() == null ? Instant.now() : request.capturedAt()), snapshotId);
            dualWriteGateway.syncSnapshot(snapshotId);
            return new MoisSalesLibraryDtos.HtmlCapturePersistResponse(snapshotId, "DUPLICATE");
        }
        jdbcTemplate.update("""
                UPDATE mois_sales_library_page_snapshot
                SET snapshot_hash = ?, status = 'CAPTURED', http_status = ?, content_type = ?, redirect_destination_url = ?, redirect_root_url = ?, error_message = NULL, captured_at = ?, updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, hash, request.httpStatus(), truncate(request.contentType(), 255), truncate(request.redirectDestinationUrl(), 1024),
                truncate(request.redirectRootUrl(), 1024), Timestamp.from(request.capturedAt() == null ? Instant.now() : request.capturedAt()), snapshotId);
        insertTextArtifact(snapshotId, ARTIFACT_RAW_HTML, request.contentType() == null ? "text/html" : request.contentType(), rawHtml, rawHtmlBytes.length);
        dualWriteGateway.syncSnapshot(snapshotId);
        return new MoisSalesLibraryDtos.HtmlCapturePersistResponse(snapshotId, "CAPTURED");
    }

    /**
     * Registra falha da etapa worker de obtenção de HTML bruto preservando contexto de auditoria.
     */
    @Transactional
    public MoisSalesLibraryDtos.HtmlCapturePersistResponse failHtmlCapture(
            long snapshotId,
            MoisSalesLibraryDtos.HtmlCaptureFailRequest request
    ) {
        String message = request.errorMessage() == null || request.errorMessage().isBlank()
                ? request.errorCategory()
                : request.errorMessage();
        jdbcTemplate.update("""
                UPDATE mois_sales_library_page_snapshot
                SET status = 'FAILED', http_status = ?, redirect_destination_url = ?, redirect_root_url = ?, error_message = ?, captured_at = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP()
                WHERE id = ?
                """, request.httpStatus(), truncate(request.redirectDestinationUrl(), 1024), truncate(request.redirectRootUrl(), 1024),
                truncate(message, 1000), snapshotId);
        dualWriteGateway.syncSnapshot(snapshotId);
        return new MoisSalesLibraryDtos.HtmlCapturePersistResponse(snapshotId, "FAILED");
    }

    /**
     * Seleciona páginas da biblioteca elegíveis para captura de HTML bruto, evitando reprocessar falhas recentes ou recorrentes.
     */
    private List<PageToCapture> findPagesToCapture(String workspaceId, int limit, boolean force) {
        String forceClause = force ? "" : """
                AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_library_page_snapshot s
                    WHERE s.url_ingest_id = i.id AND s.status IN ('CAPTURED', 'FETCHING')
                )
                AND NOT EXISTS (
                    SELECT 1 FROM mois_sales_library_page_snapshot recent_failure
                    WHERE recent_failure.url_ingest_id = i.id
                      AND recent_failure.status = 'FAILED'
                      AND recent_failure.captured_at >= ?
                )
                AND (
                    SELECT COUNT(1)
                    FROM mois_sales_library_page_snapshot repeated_failure
                    WHERE repeated_failure.url_ingest_id = i.id
                      AND repeated_failure.status = 'FAILED'
                ) < ?
                """;
        String query = """
                SELECT i.id, i.url_canonical, i.title
                FROM mois_sales_library_url_ingest i
                WHERE i.workspace_id = ?
                """ + forceClause + """
                ORDER BY i.updated_at DESC, i.id DESC
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

    /** Converte uma linha JDBC da URL ingerida na estrutura interna de captura. */
    private PageToCapture mapPageToCapture(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new PageToCapture(rs.getLong("id"), rs.getString("url_canonical"), rs.getString("title"));
    }

    /** Executa uma captura isolada convertendo qualquer exceção em snapshot FAILED auditável. */
    private MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem captureOneSafely(PageToCapture page) {
        try {
            return captureOne(page);
        } catch (Exception ex) {
            String errorMessage = categorizeExceptionFailure(ex);
            log.warn("Falha ao capturar snapshot bruto da sales page MOIS. pageId={}, url={}, categoria={}",
                    page.pageId(), page.urlCanonical(), errorMessage, ex);
            Long snapshotId = persistFailedSnapshot(page, errorMessage, null, null, null, null);
            syncSnapshotWhenPresent(snapshotId);
            return new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem(
                    page.pageId(), snapshotId, page.urlCanonical(), null, null, "FAILED", null, null, 0L, 0L, errorMessage);
        }
    }

    /** Busca a URL canônica, tenta fallback pela raiz do redirecionamento e persiste os artefatos quando a captura é útil. */
    private MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem captureOne(PageToCapture page) throws Exception {
        CaptureResponse primary = fetchUrl(page.urlCanonical());
        String redirectDestinationUrl = primary.finalUrl();
        String redirectRootUrl = rootUrl(redirectDestinationUrl);
        CaptureResponse effective = primary;
        if (!primary.isCapturable() && shouldTryRedirectRoot(page.urlCanonical(), redirectDestinationUrl, redirectRootUrl)) {
            log.info("MOIS sales-library snapshot tentando raiz do redirecionamento. pageId={}, urlCanonical={}, redirectDestinationUrl={}, redirectRootUrl={}",
                    page.pageId(), page.urlCanonical(), redirectDestinationUrl, redirectRootUrl);
            effective = fetchUrl(redirectRootUrl);
        }

        log.info("MOIS sales-library snapshot payload bruto recebido. pageId={}, url={}, redirectDestinationUrl={}, redirectRootUrl={}, effectiveUrl={}, httpStatus={}, contentType={}, htmlPreview={}",
                page.pageId(), page.urlCanonical(), redirectDestinationUrl, redirectRootUrl, effective.finalUrl(), effective.statusCode(),
                effective.contentType(), truncate(effective.rawHtml(), 4000));

        if (!effective.isCapturable()) {
            String errorMessage = categorizeHttpFailure(effective.statusCode(), effective.rawHtml());
            Long snapshotId = persistFailedSnapshot(page, errorMessage, effective.statusCode(), effective.contentType(), redirectDestinationUrl, redirectRootUrl);
            syncSnapshotWhenPresent(snapshotId);
            return new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem(
                    page.pageId(), snapshotId, page.urlCanonical(), redirectDestinationUrl, redirectRootUrl, "FAILED", null, effective.statusCode(), 0L, 0L,
                    errorMessage);
        }

        String rawHtml = effective.rawHtml();
        String hash = sha256(rawHtml);
        Long existingSnapshotId = findExistingSnapshot(page.pageId(), hash);
        if (existingSnapshotId != null) {
            return new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem(
                    page.pageId(), existingSnapshotId, page.urlCanonical(), redirectDestinationUrl, redirectRootUrl, "DUPLICATE", hash,
                    effective.statusCode(), rawHtml.getBytes(StandardCharsets.UTF_8).length, 0L, null);
        }

        long snapshotId = insertSnapshot(page, hash, effective.statusCode(), effective.contentType(), redirectDestinationUrl, redirectRootUrl);
        byte[] rawHtmlBytes = rawHtml.getBytes(StandardCharsets.UTF_8);
        insertTextArtifact(snapshotId, ARTIFACT_RAW_HTML, "text/html; charset=UTF-8", rawHtml, rawHtmlBytes.length);
        byte[] screenshot = renderBasicScreenshot(rawHtml, effective.finalUrl());
        insertBinaryArtifact(snapshotId, ARTIFACT_SCREENSHOT_PNG, "image/png", screenshot);
        dualWriteGateway.syncSnapshot(snapshotId);
        return new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem(
                page.pageId(), snapshotId, page.urlCanonical(), redirectDestinationUrl, redirectRootUrl, "CAPTURED", hash,
                effective.statusCode(), rawHtmlBytes.length, screenshot.length, null);
    }

    /** Sincroniza um snapshot no modelo consolidado apenas quando a falha gerou identificador válido. */
    private void syncSnapshotWhenPresent(Long snapshotId) {
        if (snapshotId != null) {
            dualWriteGateway.syncSnapshot(snapshotId);
        }
    }

    /** Persiste uma falha de captura preservando URLs de redirecionamento, categoria, HTTP e tipo de conteúdo quando disponíveis. */
    private Long persistFailedSnapshot(PageToCapture page, String errorMessage, Integer httpStatus, String contentType, String redirectDestinationUrl, String redirectRootUrl) {
        try {
            return insertFailedSnapshot(page, truncate(errorMessage, 1000), httpStatus, contentType, redirectDestinationUrl, redirectRootUrl);
        } catch (Exception persistEx) {
            log.warn("Falha ao persistir snapshot FAILED da sales page MOIS. pageId={}", page.pageId(), persistEx);
            return null;
        }
    }

    /** Insere os metadados principais de um snapshot capturado com sucesso, incluindo URLs de redirecionamento. */
    private long insertSnapshot(PageToCapture page, String hash, int httpStatus, String contentType, String redirectDestinationUrl, String redirectRootUrl) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO mois_sales_library_page_snapshot
                    (url_ingest_id, snapshot_hash, status, http_status, content_type, redirect_destination_url, redirect_root_url, captured_at, created_at, updated_at)
                    VALUES (?, ?, 'CAPTURED', ?, ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_TIMESTAMP())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, page.pageId());
            ps.setString(2, hash);
            ps.setInt(3, httpStatus);
            ps.setString(4, truncate(contentType, 255));
            ps.setString(5, truncate(redirectDestinationUrl, 1024));
            ps.setString(6, truncate(redirectRootUrl, 1024));
            return ps;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    /** Insere um snapshot FAILED com URLs de redirecionamento e metadados suficientes para auditoria. */
    private long insertFailedSnapshot(PageToCapture page, String errorMessage, Integer httpStatus, String contentType, String redirectDestinationUrl, String redirectRootUrl) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO mois_sales_library_page_snapshot
                    (url_ingest_id, status, http_status, content_type, redirect_destination_url, redirect_root_url, error_message, captured_at, created_at, updated_at)
                    VALUES (?, 'FAILED', ?, ?, ?, ?, ?, UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_TIMESTAMP())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, page.pageId());
            if (httpStatus == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, httpStatus);
            }
            ps.setString(3, truncate(contentType, 255));
            ps.setString(4, truncate(redirectDestinationUrl, 1024));
            ps.setString(5, truncate(redirectRootUrl, 1024));
            ps.setString(6, errorMessage);
            return ps;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    /** Cria o registro de snapshot reservado para evitar reprocessamento concorrente. */
    private Long insertFetchingSnapshot(PageToCapture page) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO mois_sales_library_page_snapshot
                    (url_ingest_id, status, captured_at, created_at, updated_at)
                    VALUES (?, 'FETCHING', UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_TIMESTAMP())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, page.pageId());
            return ps;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    /** Localiza snapshot anterior da mesma página com hash idêntico ao capturado. */
    private Long findDuplicateSnapshot(long snapshotId, String hash) {
        List<Long> rows = jdbcTemplate.query("""
                SELECT existing.id
                FROM mois_sales_library_page_snapshot current
                JOIN mois_sales_library_page_snapshot existing
                  ON existing.url_ingest_id = current.url_ingest_id
                 AND existing.snapshot_hash = ?
                 AND existing.id <> current.id
                WHERE current.id = ?
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("id"), hash, snapshotId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Localiza snapshot já persistido para a mesma página e mesmo hash antes de criar duplicidade. */
    private Long findExistingSnapshot(long pageId, String hash) {
        List<Long> rows = jdbcTemplate.query("""
                SELECT id FROM mois_sales_library_page_snapshot
                WHERE url_ingest_id = ? AND snapshot_hash = ?
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("id"), pageId, hash);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Extrai a chave gerada pelo banco após um INSERT com GeneratedKeyHolder. */
    private long generatedId(KeyHolder keyHolder) {
        if (keyHolder.getKeys() != null && keyHolder.getKeys().get("id") instanceof Number id) {
            return id.longValue();
        }
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    /** Persiste um artefato textual associado ao snapshot. */
    private void insertTextArtifact(long snapshotId, String artifactType, String contentType, String contentText, long sizeBytes) {
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_snapshot_artifact
                (snapshot_id, artifact_type, content_type, storage_kind, content_text, size_bytes, created_at)
                VALUES (?, ?, ?, 'DATABASE_TEXT', ?, ?, UTC_TIMESTAMP())
                """, snapshotId, artifactType, contentType, contentText, sizeBytes);
    }

    /** Persiste um artefato binário associado ao snapshot. */
    private void insertBinaryArtifact(long snapshotId, String artifactType, String contentType, byte[] contentBlob) {
        jdbcTemplate.update("""
                INSERT INTO mois_sales_library_snapshot_artifact
                (snapshot_id, artifact_type, content_type, storage_kind, content_blob, size_bytes, created_at)
                VALUES (?, ?, ?, 'DATABASE_BLOB', ?, ?, UTC_TIMESTAMP())
                """, snapshotId, artifactType, contentType, contentBlob, contentBlob.length);
    }

    /** Renderiza uma prévia PNG básica do HTML para auditoria visual da captura. */
    private byte[] renderBasicScreenshot(String rawHtml, String url) throws Exception {
        int width = 1280;
        int height = 1800;
        JEditorPane pane = new JEditorPane("text/html", rawHtml);
        pane.setEditable(false);
        pane.setSize(new Dimension(width, height));
        Dimension preferred = pane.getPreferredSize();
        int renderHeight = Math.max(900, Math.min(2400, preferred.height + 80));
        pane.setSize(new Dimension(width, renderHeight));
        BufferedImage image = new BufferedImage(width, renderHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, renderHeight);
        pane.paint(graphics);
        graphics.setColor(new Color(245, 245, 245));
        graphics.fillRect(0, 0, width, 32);
        graphics.setColor(Color.DARK_GRAY);
        graphics.drawString("Marketing Hub snapshot: " + url, 12, 21);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    /** Normaliza o limite de captura para manter a execução curta e previsível. */
    private int normalizeLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }


    /** Executa GET com redirecionamentos e encapsula HTML, URL final, HTTP status e content type. */
    private CaptureResponse fetchUrl(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "MarketingHub-MOIS-SalesLibrarySnapshot/1.0")
                .GET()
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
            log.warn("Falha ao calcular hash de snapshot HTML MOIS. operacao=sha256, erroClasse={}, erro={}",
                    ex.getClass().getName(), ex.getMessage(), ex);
            throw new IllegalStateException("Falha ao calcular hash de snapshot HTML", ex);
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

    private record CaptureResponse(String rawHtml, String finalUrl, int statusCode, String contentType) {

        /** Informa se a resposta capturada possui corpo aproveitável para análise. */
        private boolean isCapturable() {
            return statusCode >= 200 && statusCode < 400 && rawHtml != null && !rawHtml.isBlank();
        }
    }
}
