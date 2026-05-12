package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisHotmartProductDtos;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoisHotmartProductServiceTest {

    @Test
    void shouldReturnEmptyListWhenThereIsNoJobForWorkspace() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq("workspace-001")))
                .thenReturn(List.of());

        MoisHotmartProductService service = new MoisHotmartProductService(jdbcTemplate);
        MoisHotmartProductDtos.HotmartCollectedProductListResponse response =
                service.listLatestByWorkspace("workspace-001", 24);

        assertEquals("workspace-001", response.workspaceId());
        assertEquals(0, response.items().size());
    }
}
