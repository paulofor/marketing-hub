package com.marketinghub.mois.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.dto.MoisCollectionPersistenceDtos;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
}
