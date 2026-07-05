package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.storage.FileStorageService;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: registrar engajamento público de clientes com pacotes de imagem do Lead Portal. */
@Service
public class ImagePackageEngagementService {

    private static final Logger log = LoggerFactory.getLogger(ImagePackageEngagementService.class);
    private static final String SAMPLE_EMAIL_OPENED = "SAMPLE_EMAIL_OPENED";
    private static final String SAMPLE_IMAGES_VIEWED = "SAMPLE_IMAGES_VIEWED";

    private final JdbcTemplate jdbcTemplate;
    private final FileStorageService fileStorageService;
    private final FlowSubmissionImagePackageStatusHistoryService statusHistoryService;

    /** Inicializa o serviço com banco, storage público e histórico funcional. */
    public ImagePackageEngagementService(
            JdbcTemplate jdbcTemplate,
            FileStorageService fileStorageService,
            FlowSubmissionImagePackageStatusHistoryService statusHistoryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageService = fileStorageService;
        this.statusHistoryService = statusHistoryService;
    }

    /** Marca abertura do e-mail do pacote quando o token de submissão é válido. */
    @Transactional
    public boolean markEmailOpened(long packageId, String submissionToken) {
        Optional<EngagementTarget> target = loadTarget(packageId);
        if (target.isEmpty() || !matchesSubmission(target.get(), submissionToken)) {
            return false;
        }
        if (target.get().emailOpenedAt() != null) {
            return true;
        }
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET email_opened_at = COALESCE(email_opened_at, ?), "
                        + "updated_at = ? WHERE id = ? AND email_opened_at IS NULL",
                timestamp,
                timestamp,
                packageId);
        if (updated > 0) {
            statusHistoryService.recordStatusChange(packageId, SAMPLE_EMAIL_OPENED, null);
            log.debug("Pacote de imagem {} marcado como e-mail aberto no Lead Portal", packageId);
            return true;
        }
        return false;
    }

    /** Marca visualização das imagens e retorna a URL pública do ZIP quando disponível. */
    @Transactional
    public Optional<String> markImagesViewed(long packageId, String submissionToken) {
        Optional<EngagementTarget> target = loadTarget(packageId);
        if (target.isEmpty() || !matchesSubmission(target.get(), submissionToken)) {
            return Optional.empty();
        }
        EngagementTarget engagementTarget = target.get();
        if (!StringUtils.hasText(engagementTarget.zipObjectKey())) {
            return Optional.empty();
        }
        Optional<String> downloadUrl = fileStorageService.resolvePublicUrl(engagementTarget.zipObjectKey());
        if (downloadUrl.isEmpty()) {
            return Optional.empty();
        }
        if (engagementTarget.emailOpenedAt() != null && engagementTarget.imagesViewedAt() != null) {
            return downloadUrl;
        }

        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET images_viewed_at = COALESCE(images_viewed_at, ?), "
                        + "email_opened_at = COALESCE(email_opened_at, ?), updated_at = ? "
                        + "WHERE id = ? AND (images_viewed_at IS NULL OR email_opened_at IS NULL)",
                timestamp,
                timestamp,
                timestamp,
                packageId);
        if (updated > 0) {
            if (engagementTarget.emailOpenedAt() == null) {
                statusHistoryService.recordStatusChange(packageId, SAMPLE_EMAIL_OPENED, null);
            }
            if (engagementTarget.imagesViewedAt() == null) {
                statusHistoryService.recordStatusChange(packageId, SAMPLE_IMAGES_VIEWED, null);
            }
            log.debug("Pacote de imagem {} marcado como imagens visualizadas no Lead Portal", packageId);
        }
        return downloadUrl;
    }

    /** Carrega os dados mínimos do pacote para validar token e resolver o arquivo público. */
    private Optional<EngagementTarget> loadTarget(long packageId) {
        try {
            return jdbcTemplate.query(
                            "SELECT submission_id, zip_object_key, email_opened_at, images_viewed_at "
                                    + "FROM flow_submission_image_package WHERE id = ?",
                            (rs, rowNum) -> mapTarget(rs),
                            packageId)
                    .stream()
                    .findFirst();
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** Converte uma linha SQL no alvo de engajamento usado pelo serviço. */
    private EngagementTarget mapTarget(ResultSet rs) throws SQLException {
        String submissionId = readSubmissionId(rs.getObject("submission_id"));
        String zipObjectKey = rs.getString("zip_object_key");
        Instant emailOpenedAt = toInstant(rs.getTimestamp("email_opened_at"));
        Instant imagesViewedAt = toInstant(rs.getTimestamp("images_viewed_at"));
        return new EngagementTarget(submissionId, zipObjectKey, emailOpenedAt, imagesViewedAt);
    }

    /** Confere o token recebido contra a submissão do pacote quando ambos estão presentes. */
    private boolean matchesSubmission(EngagementTarget target, String submissionToken) {
        if (!StringUtils.hasText(submissionToken) || !StringUtils.hasText(target.submissionId())) {
            return true;
        }
        boolean matches = submissionToken.trim().equalsIgnoreCase(target.submissionId());
        if (!matches) {
            log.debug("Token de submissão {} não confere com o pacote esperado {}", submissionToken, target.submissionId());
        }
        return matches;
    }

    /** Normaliza o identificador da submissão vindo do banco como binário ou texto. */
    private String readSubmissionId(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof byte[] bytes) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                return new UUID(buffer.getLong(), buffer.getLong()).toString();
            } catch (Exception ex) {
                log.warn("Falha ao converter submission_id binário para UUID no Lead Portal", ex);
                return null;
            }
        }
        String value = raw.toString();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException ex) {
            return value.trim();
        }
    }

    /** Converte timestamp SQL para Instant preservando nulo. */
    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    /** Dados mínimos necessários para registrar engajamento de um pacote. */
    private record EngagementTarget(
            String submissionId, String zipObjectKey, Instant emailOpenedAt, Instant imagesViewedAt) {
    }
}
