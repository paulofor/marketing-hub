package com.marketinghub.facebookadsworker.facebooktargeting.queue;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class TargetingResolutionJobJdbcRepositoryTest {

    /**
     * Garante que candidatos pendentes sem job sejam recolocados automaticamente na fila antes do claim.
     */
    @Test
    void claimPendingJobsRecreatesMissingJobsBeforeClaiming() {
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        TargetingResolutionJobJdbcRepository repository = new TargetingResolutionJobJdbcRepository(jdbcTemplate);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        when(jdbcTemplate.update(sqlCaptor.capture(), anyMap())).thenReturn(2);
        when(jdbcTemplate.queryForList(any(String.class), anyMap(), eq(Long.class))).thenReturn(List.of());

        repository.claimPendingJobs("worker-1", 20);

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).update(any(String.class), anyMap());
        inOrder.verify(jdbcTemplate).queryForList(any(String.class), anyMap(), eq(Long.class));
        String reconciliationSql = sqlCaptor.getValue();
        assertTrue(reconciliationSql.contains("INSERT IGNORE INTO targeting_resolution_job"));
        assertTrue(reconciliationSql.contains("c.status = 'PENDING_FACEBOOK_MATCH'"));
        assertTrue(reconciliationSql.contains("j.id IS NULL"));
    }
}
