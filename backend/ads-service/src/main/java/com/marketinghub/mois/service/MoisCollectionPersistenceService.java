package com.marketinghub.mois.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.dto.MoisCollectionPersistenceDtos;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoisCollectionPersistenceService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MoisCollectionPersistenceDtos.CollectionJobStateResponse upsertJobState(
            String jobId,
            MoisCollectionPersistenceDtos.CollectionJobStateResponse state
    ) {
        String workspaceId = state.job() != null ? state.job().workspaceId() : null;
        String status = state.job() != null ? state.job().status() : null;

        jdbcTemplate.update(
                """
                        INSERT INTO mois_collection_job_state (
                          job_id,
                          workspace_id,
                          status,
                          payload_json,
                          updated_at
                        ) VALUES (?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                          workspace_id = VALUES(workspace_id),
                          status = VALUES(status),
                          payload_json = VALUES(payload_json),
                          updated_at = VALUES(updated_at)
                        """,
                jobId,
                workspaceId,
                status,
                writeState(state),
                Timestamp.from(Instant.now())
        );
        persistCollectedReferences(jobId, workspaceId, state.references());

        return state;
    }

    public Optional<MoisCollectionPersistenceDtos.CollectionJobStateResponse> getJobState(String jobId) {
        List<MoisCollectionPersistenceDtos.CollectionJobStateResponse> items = jdbcTemplate.query(
                "SELECT payload_json FROM mois_collection_job_state WHERE job_id = ?",
                (rs, rowNum) -> readState(rs.getString("payload_json")),
                jobId
        );
        return items.stream().findFirst();
    }

    public MoisCollectionPersistenceDtos.CollectionJobStateListResponse listJobStates(String workspaceId, String status) {
        StringBuilder sql = new StringBuilder("SELECT payload_json FROM mois_collection_job_state WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (workspaceId != null && !workspaceId.isBlank()) {
            sql.append(" AND workspace_id = ?");
            params.add(workspaceId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY updated_at DESC");

        List<MoisCollectionPersistenceDtos.CollectionJobStateResponse> items = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> readState(rs.getString("payload_json")),
                params.toArray()
        );

        return new MoisCollectionPersistenceDtos.CollectionJobStateListResponse(new ArrayList<>(items));
    }

    private String writeState(MoisCollectionPersistenceDtos.CollectionJobStateResponse state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar estado de coleta MOIS", ex);
        }
    }

    private MoisCollectionPersistenceDtos.CollectionJobStateResponse readState(String payload) {
        try {
            return objectMapper.readValue(payload, MoisCollectionPersistenceDtos.CollectionJobStateResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao desserializar estado de coleta MOIS", ex);
        }
    }

    private void persistCollectedReferences(
            String jobId,
            String workspaceId,
            List<com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse> references
    ) {
        jdbcTemplate.update("DELETE FROM mois_collected_reference WHERE job_id = ?", jobId);
        if (references == null || references.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO mois_collected_reference (
                          job_id, workspace_id, reference_id, source, title, url, niche, status, favorite,
                          imported_reference_id, success_score, success_signal, confidence_level, ranking_position,
                          engagement_relative, recurrence_score, evidence_score, hotmart_description,
                          hotmart_producer, hotmart_image_url, hotmart_highlight, product_name, product_url, producer_name, sales_page_url, collected_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                references,
                references.size(),
                (ps, item) -> {
                    ps.setString(1, jobId);
                    ps.setString(2, workspaceId);
                    ps.setString(3, item.referenceId());
                    ps.setString(4, item.source());
                    ps.setString(5, item.title());
                    ps.setString(6, item.url());
                    ps.setString(7, item.niche());
                    ps.setString(8, item.status());
                    ps.setBoolean(9, item.favorite());
                    ps.setString(10, item.importedReferenceId());
                    ps.setInt(11, item.successScore());
                    ps.setString(12, item.successSignal());
                    ps.setString(13, item.confidenceLevel());
                    ps.setInt(14, item.rankingPosition());
                    ps.setDouble(15, item.engagementRelative());
                    ps.setDouble(16, item.recurrenceScore());
                    ps.setDouble(17, item.evidenceScore());
                    ps.setString(18, metadataValue(item, "hotmartDescription"));
                    ps.setString(19, metadataValue(item, "hotmartProducer"));
                    ps.setString(20, metadataValue(item, "hotmartImageUrl"));
                    ps.setString(21, metadataValue(item, "hotmartHighlight"));
                    ps.setString(22, coalesceNotBlank(item.title(), metadataValue(item, "productName"), metadataValue(item, "hotmartProductName")));
                    ps.setString(23, coalesceNotBlank(item.url(), metadataValue(item, "productUrl")));
                    ps.setString(24, coalesceNotBlank(metadataValue(item, "hotmartProducer"), metadataValue(item, "producerName"), metadataValue(item, "producer")));
                    ps.setString(25, coalesceNotBlank(metadataValue(item, "salesPageUrl"), metadataValue(item, "checkoutUrl"), item.url()));
                    ps.setTimestamp(26, item.collectedAt() == null ? null : Timestamp.from(item.collectedAt()));
                    ps.setTimestamp(27, Timestamp.from(Instant.now()));
                }
        );
    }

    private String coalesceNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String metadataValue(com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse item, String key) {
        if (item.rawMetadata() == null) {
            return null;
        }
        return item.rawMetadata().get(key);
    }

    public MoisCollectionPersistenceDtos.SourceHighlightListResponse summarizeBySource(String workspaceId, String status) {
        List<MoisCollectionPersistenceDtos.CollectionJobStateResponse> states = listJobStates(workspaceId, status).items();
        Map<String, List<com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse>> bySource = new HashMap<>();
        for (MoisCollectionPersistenceDtos.CollectionJobStateResponse state : states) {
            if (state.references() == null) {
                continue;
            }
            for (com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse reference : state.references()) {
                bySource.computeIfAbsent(reference.source() == null ? "UNKNOWN" : reference.source(), key -> new ArrayList<>())
                        .add(reference);
            }
        }
        List<MoisCollectionPersistenceDtos.SourceHighlightResponse> items = bySource.entrySet().stream()
                .map(entry -> toSourceHighlight(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(MoisCollectionPersistenceDtos.SourceHighlightResponse::averageSuccessScore).reversed())
                .toList();
        return new MoisCollectionPersistenceDtos.SourceHighlightListResponse(items);
    }

    private MoisCollectionPersistenceDtos.SourceHighlightResponse toSourceHighlight(
            String source,
            List<com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse> references
    ) {
        int total = references.size();
        int favorites = (int) references.stream().filter(com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse::favorite).count();
        double avgSuccess = references.stream().mapToInt(com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse::successScore).average().orElse(0);
        double avgEngagement = references.stream().mapToDouble(com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse::engagementRelative).average().orElse(0);
        double avgRecurrence = references.stream().mapToDouble(com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse::recurrenceScore).average().orElse(0);
        double avgEvidence = references.stream().mapToDouble(com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse::evidenceScore).average().orElse(0);
        String topSignal = references.stream()
                .filter(r -> r.successSignal() != null && !r.successSignal().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        com.marketinghub.mois.dto.MoisWorkspaceDtos.CollectedReferenceResponse::successSignal,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        return new MoisCollectionPersistenceDtos.SourceHighlightResponse(
                source, total, avgSuccess, avgEngagement, avgRecurrence, avgEvidence, favorites, topSignal
        );
    }
}
