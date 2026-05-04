package com.marketinghub.mois.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.dto.MoisCollectionPersistenceDtos;
import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
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
    @DisplayName("upsert persiste estado consolidado e sincroniza referências relacionais")
    void upsertJobStatePersistsPayloadInDatabase() {
        MoisCollectionPersistenceDtos.CollectionJobStateResponse state = sampleState("workspace-001", "COMPLETED");

        MoisCollectionPersistenceDtos.CollectionJobStateResponse response = service.upsertJobState("job-1", state);

        assertThat(response).isEqualTo(state);
        verify(jdbcTemplate).update(anyString(), eq("job-1"), eq("workspace-001"), eq("COMPLETED"), anyString(), any());
        verify(jdbcTemplate).update("DELETE FROM mois_collected_reference WHERE job_id = ?", "job-1");
        verify(jdbcTemplate, never()).batchUpdate(anyString(), eq(List.of()), eq(0), any(ParameterizedPreparedStatementSetter.class));
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

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("sumário por fonte agrega referências e ordena por score médio")
    void summarizeBySourceAggregatesReferences() throws Exception {
        MoisWorkspaceDtos.CollectedReferenceResponse hotmartRef1 = new MoisWorkspaceDtos.CollectedReferenceResponse(
                "ref-1", "job-1", "HOTMART", "Oferta A", "https://x", "niche", "COLLECTED",
                true, null, 90, "Alto ROI", "HIGH", 1, 0.8, 0.7, 0.6, Instant.parse("2026-05-04T01:00:00Z"), java.util.Map.of());
        MoisWorkspaceDtos.CollectedReferenceResponse hotmartRef2 = new MoisWorkspaceDtos.CollectedReferenceResponse(
                "ref-2", "job-1", "HOTMART", "Oferta B", "https://y", "niche", "COLLECTED",
                false, null, 80, "Alto ROI", "MEDIUM", 2, 0.6, 0.5, 0.4, Instant.parse("2026-05-04T01:10:00Z"), java.util.Map.of());
        MoisWorkspaceDtos.CollectedReferenceResponse metaRef = new MoisWorkspaceDtos.CollectedReferenceResponse(
                "ref-3", "job-2", "META", "Oferta C", "https://z", "niche", "COLLECTED",
                false, null, 60, "CTR forte", "MEDIUM", 1, 0.4, 0.3, 0.2, Instant.parse("2026-05-04T01:20:00Z"), java.util.Map.of());
        MoisCollectionPersistenceDtos.CollectionJobStateResponse state = new MoisCollectionPersistenceDtos.CollectionJobStateResponse(
                sampleState("workspace-001", "COMPLETED").job(),
                List.of(hotmartRef1, hotmartRef2, metaRef),
                java.util.Map.of(),
                null,
                List.of()
        );
        String payload = new ObjectMapper().findAndRegisterModules().writeValueAsString(state);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenAnswer(invocation -> {
            RowMapper<MoisCollectionPersistenceDtos.CollectionJobStateResponse> mapper = invocation.getArgument(1);
            java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
            when(rs.getString("payload_json")).thenReturn(payload);
            return List.of(mapper.mapRow(rs, 0));
        });

        MoisCollectionPersistenceDtos.SourceHighlightListResponse response = service.summarizeBySource("workspace-001", "COMPLETED");

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).source()).isEqualTo("HOTMART");
        assertThat(response.items().get(0).totalReferences()).isEqualTo(2);
        assertThat(response.items().get(0).topSuccessSignal()).isEqualTo("Alto ROI");
        assertThat(response.items().get(0).favorites()).isEqualTo(1);
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
