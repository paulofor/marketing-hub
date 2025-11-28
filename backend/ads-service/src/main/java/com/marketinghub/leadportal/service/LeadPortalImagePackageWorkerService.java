package com.marketinghub.leadportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImagePackageDto;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImageResultRequest;
import com.marketinghub.leadportal.dto.LeadPortalWorkerImageResultRequest.GeneratedImageRequest;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.media.repository.AssetRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Regras de negócio voltadas à comunicação com o Worker AI para pacotes de imagem do Lead Portal.
 */
@Service
public class LeadPortalImagePackageWorkerService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalImagePackageWorkerService.class);

    private static final RowMapper<LeadPortalWorkerImagePackageDto> RECENT_PACKAGE_MAPPER = (rs, rowNum) ->
            new LeadPortalWorkerImagePackageDto(
                    rs.getLong("id"),
                    parseUuid(rs.getString("submission_id")),
                    rs.getString("stored_file_name"),
                    getInteger(rs, "planned_outputs"),
                    getInteger(rs, "free_images"),
                    rs.getString("model"),
                    rs.getString("prompt"),
                    rs.getString("treatment"));

    private final JdbcTemplate jdbcTemplate;
    private final AssetRepository assetRepository;
    private final ObjectMapper objectMapper;

    public LeadPortalImagePackageWorkerService(
            JdbcTemplate jdbcTemplate,
            AssetRepository assetRepository,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetRepository = assetRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Lista pacotes aguardando processamento pelo worker.
     */
    public List<LeadPortalWorkerImagePackageDto> listRecentPackages() {
        String sql = """
                SELECT
                    pack.id,
                    pack.submission_id,
                    sub.stored_file_name,
                    pack.planned_outputs,
                    pack.free_images,
                    pack.model,
                    pack.prompt,
                    NULL AS treatment
                FROM flow_submission_image_package pack
                LEFT JOIN flow_submissions sub ON sub.id = pack.submission_id
                WHERE pack.status IN ('RECENT', 'RECEIVED')
                ORDER BY pack.created_at DESC
                """;
        return jdbcTemplate.query(sql, RECENT_PACKAGE_MAPPER);
    }

    /**
     * Marca um pacote como em processamento.
     */
    @Transactional
    public void markProcessing(long packageId) {
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET status = ?, failure_reason = NULL, updated_at = ? "
                        + "WHERE id = ? AND status IN (?, ?)",
                FlowSubmissionImagePackageStatus.PROCESSING.name(),
                Timestamp.from(Instant.now()),
                packageId,
                FlowSubmissionImagePackageStatus.RECEIVED.name(),
                FlowSubmissionImagePackageStatus.RECENT.name());
        if (updated == 0) {
            FlowSubmissionImagePackageStatus current = findStatus(packageId)
                    .orElseThrow(() -> notFound(packageId));
            throw conflict("Pacote %d não está apto a entrar em processamento (status atual: %s)"
                    .formatted(packageId, current));
        }
    }

    /**
     * Marca um pacote como falho, registrando o motivo informado pelo worker.
     */
    @Transactional
    public void markFailed(long packageId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason não pode ser vazio");
        }
        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET status = ?, failure_reason = ?, updated_at = ? "
                        + "WHERE id = ? AND status IN (?, ?, ?)",
                FlowSubmissionImagePackageStatus.FAILED.name(),
                reason.trim(),
                Timestamp.from(Instant.now()),
                packageId,
                FlowSubmissionImagePackageStatus.PROCESSING.name(),
                FlowSubmissionImagePackageStatus.RECEIVED.name(),
                FlowSubmissionImagePackageStatus.RECENT.name());
        if (updated == 0) {
            FlowSubmissionImagePackageStatus current = findStatus(packageId)
                    .orElseThrow(() -> notFound(packageId));
            throw conflict("Pacote %d não pode ser marcado como FAILED a partir do status %s"
                    .formatted(packageId, current));
        }
    }

    /**
     * Conclui o processamento de um pacote, persistindo as imagens geradas e metadados.
     */
    @Transactional
    public void submitResults(long packageId, LeadPortalWorkerImageResultRequest request) {
        PackageSnapshot snapshot = findPackage(packageId)
                .orElseThrow(() -> notFound(packageId));
        if (snapshot.status() != FlowSubmissionImagePackageStatus.PROCESSING) {
            throw conflict("Pacote %d precisa estar em PROCESSING para receber resultados (status atual: %s)"
                    .formatted(packageId, snapshot.status()));
        }
        if (request.images().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É necessário informar ao menos uma imagem gerada");
        }

        // Limpa resultados anteriores caso existam
        jdbcTemplate.update("DELETE FROM flow_submission_image_item WHERE package_id = ?", packageId);

        List<Asset> assets = saveAssets(request, snapshot);
        insertImageItems(packageId, assets, snapshot.freeImages());

        String finalModel = resolveModelFromAssets(request.model(), snapshot.model(), assets);
        String finalPrompt = resolvePrompt(request.prompt(), snapshot.prompt());

        int updated = jdbcTemplate.update(
                "UPDATE flow_submission_image_package SET status = ?, model = ?, prompt = ?, failure_reason = NULL, updated_at = ? "
                        + "WHERE id = ? AND status = ?",
                FlowSubmissionImagePackageStatus.COMPLETED.name(),
                finalModel,
                finalPrompt,
                Timestamp.from(Instant.now()),
                packageId,
                FlowSubmissionImagePackageStatus.PROCESSING.name());
        if (updated == 0) {
            throw conflict("Não foi possível concluir o pacote %d porque seu status mudou durante o processamento".formatted(packageId));
        }
    }

    private List<Asset> saveAssets(LeadPortalWorkerImageResultRequest request, PackageSnapshot snapshot) {
        List<Asset> assets = new ArrayList<>(request.images().size());
        for (GeneratedImageRequest image : request.images()) {
            String url = image.publicUrl().trim();
            String storedName = image.storedFileName().trim();
            String prompt = resolvePrompt(StringUtils.hasText(image.prompt()) ? image.prompt() : request.prompt(), snapshot.prompt());
            String model = resolveModelValue(image.model(), request.model(), null);
            Asset asset = Asset.builder()
                    .type(AssetType.IMAGE)
                    .provider(resolveProvider(image.source()))
                    .status(AssetStatus.READY)
                    .url(url)
                    .externalId(storedName)
                    .model(model)
                    .prompt(prompt)
                    .payload(buildPayload(image))
                    .build();
            assets.add(asset);
        }
        return assetRepository.saveAll(assets);
    }

    private void insertImageItems(long packageId, List<Asset> assets, Integer freeImages) {
        int freeSlots = freeImages != null && freeImages > 0 ? freeImages : 0;
        String sql = "INSERT INTO flow_submission_image_item (package_id, asset_id, access_type, position_index, created_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        Timestamp now = Timestamp.from(Instant.now());
        for (int index = 0; index < assets.size(); index++) {
            Asset asset = assets.get(index);
            String accessType = index < freeSlots ? "FREE" : "PREMIUM";
            jdbcTemplate.update(sql, packageId, asset.getId(), accessType, index, now);
        }
    }

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    private static UUID parseUuid(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            log.warn("Valor de submission_id '{}' não pôde ser convertido para UUID", raw);
            return null;
        }
    }

    private Optional<FlowSubmissionImagePackageStatus> findStatus(long packageId) {
        List<FlowSubmissionImagePackageStatus> statuses = jdbcTemplate.query(
                "SELECT status FROM flow_submission_image_package WHERE id = ?",
                (rs, rowNum) -> parseStatus(rs.getString("status")),
                packageId);
        return statuses.stream().findFirst();
    }

    private Optional<PackageSnapshot> findPackage(long packageId) {
        String sql = "SELECT id, status, free_images, model, prompt FROM flow_submission_image_package WHERE id = ?";
        List<PackageSnapshot> items = jdbcTemplate.query(sql, (rs, rowNum) -> new PackageSnapshot(
                rs.getLong("id"),
                parseStatus(rs.getString("status")),
                getInteger(rs, "free_images"),
                rs.getString("model"),
                rs.getString("prompt")), packageId);
        return items.stream().findFirst();
    }

    private static FlowSubmissionImagePackageStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
        try {
            return FlowSubmissionImagePackageStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            log.warn("Status '{}' desconhecido para flow_submission_image_package", status);
            return FlowSubmissionImagePackageStatus.RECEIVED;
        }
    }

    private MediaProvider resolveProvider(String source) {
        if (!StringUtils.hasText(source)) {
            return MediaProvider.OPENAI;
        }
        String normalized = source.trim().toUpperCase(Locale.ROOT);
        try {
            return MediaProvider.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Provider '{}' desconhecido, usando OPENAI como padrão", source);
            }
            return MediaProvider.OPENAI;
        }
    }

    private String resolvePrompt(String providedPrompt, String fallbackPrompt) {
        if (StringUtils.hasText(providedPrompt)) {
            return providedPrompt.trim();
        }
        if (StringUtils.hasText(fallbackPrompt)) {
            return fallbackPrompt.trim();
        }
        return null;
    }

    private String resolveModelFromAssets(String requestModel, String snapshotModel, List<Asset> assets) {
        if (StringUtils.hasText(requestModel)) {
            return requestModel.trim();
        }
        if (assets != null && !assets.isEmpty()) {
            List<String> models = new ArrayList<>();
            for (Asset asset : assets) {
                if (asset == null) {
                    continue;
                }
                String model = asset.getModel();
                if (!StringUtils.hasText(model)) {
                    continue;
                }
                String normalized = model.trim();
                if (!models.contains(normalized)) {
                    models.add(normalized);
                }
            }
            if (models.size() == 1) {
                return models.get(0);
            }
            if (models.size() > 1) {
                log.warn("Imagens geradas retornaram múltiplos modelos: {}", models);
                return models.get(0);
            }
        }
        if (StringUtils.hasText(snapshotModel)) {
            return snapshotModel.trim();
        }
        return null;
    }

    private String resolveModelValue(String primary, String secondary, String tertiary) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        if (StringUtils.hasText(secondary)) {
            return secondary.trim();
        }
        if (StringUtils.hasText(tertiary)) {
            return tertiary.trim();
        }
        return null;
    }

    private String buildPayload(GeneratedImageRequest image) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stored_file_name", image.storedFileName());
        payload.put("public_url", image.publicUrl());
        String model = resolveModelValue(image.model(), null, null);
        if (StringUtils.hasText(model)) {
            payload.put("model", model);
        }
        if (StringUtils.hasText(image.prompt())) {
            payload.put("prompt", image.prompt().trim());
        }
        Map<String, Object> metadata = buildGenerationMetadata(image);
        if (!metadata.isEmpty()) {
            payload.put("metadata", metadata);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível serializar o payload da imagem gerada", ex);
        }
    }

    private Map<String, Object> buildGenerationMetadata(GeneratedImageRequest image) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (StringUtils.hasText(image.source())) {
            metadata.put("source", image.source().trim());
        }
        return metadata;
    }

    private ResponseStatusException notFound(long packageId) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Pacote %d não encontrado".formatted(packageId));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record PackageSnapshot(
            long id,
            FlowSubmissionImagePackageStatus status,
            Integer freeImages,
            String model,
            String prompt) {}
}
