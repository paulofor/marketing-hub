package com.marketinghub.mois.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.dto.MoisCollectionPersistenceDtos;
import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class MoisCollectionPersistenceServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private MoisCollectionPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new MoisCollectionPersistenceService(jdbcTemplate, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void upsertJobStatePersistsPayloadInDatabase() {
        MoisCollectionPersistenceDtos.CollectionJobStateResponse state = sampleState("workspace-001", "COMPLETED");

        MoisCollectionPersistenceDtos.CollectionJobStateResponse response = service.upsertJobState("job-1", state);

        assertThat(response).isEqualTo(state);
        verify(jdbcTemplate).update(anyString(), eq("job-1"), eq("workspace-001"), eq("COMPLETED"), anyString(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getJobStateReadsSerializedPayloadFromDatabase() throws Exception {
        MoisCollectionPersistenceDtos.CollectionJobStateResponse state = sampleState("workspace-001", "QUEUED");
        String payload = new ObjectMapper().findAndRegisterModules().writeValueAsString(state);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("job-1"))).thenAnswer(invocation -> {
            RowMapper<MoisCollectionPersistenceDtos.CollectionJobStateResponse> mapper = invocation.getArgument(1);
            java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
            when(rs.getString("payload_json")).thenReturn(payload);
            return List.of(mapper.mapRow(rs, 0));
        });

        assertThat(service.getJobState("job-1")).contains(state);
    }

    @SuppressWarnings("unchecked")
    @Test
    void listJobStatesAppliesWorkspaceAndStatusFilters() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        service.listJobStates("workspace-001", "FAILED");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        assertThat(sqlCaptor.getValue()).contains("workspace_id = ?");
        assertThat(sqlCaptor.getValue()).contains("status = ?");
    }

    private MoisCollectionPersistenceDtos.CollectionJobStateResponse sampleState(String workspaceId, String status) {
        MoisWorkspaceDtos.CollectionJobResponse job = new MoisWorkspaceDtos.CollectionJobResponse(
                "job-1",
                workspaceId,
                "marketing-digital",
                "ofertas-quentes",
                status,
                "LAST_7_DAYS",
                25,
                80,
                List.of("HOTMART"),
                Instant.parse("2026-04-28T00:00:00Z")
        );
        return new MoisCollectionPersistenceDtos.CollectionJobStateResponse(job, List.of(), java.util.Map.of(), null, List.of());
    }
}
