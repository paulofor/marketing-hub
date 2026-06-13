package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisHotmartProductDtos;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
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

    @Test
    void shouldExposeCommercialFieldsRequiredByHotmartCycleTwo() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq("workspace-001")))
                .thenReturn(List.of("job-1"));
        when(jdbcTemplate.query(contains("hotmart_description"), any(org.springframework.jdbc.core.RowMapper.class), eq("workspace-001"), eq("job-1"), eq(1)))
                .thenAnswer(invocation -> {
                    org.springframework.jdbc.core.RowMapper<MoisHotmartProductDtos.HotmartCollectedProductResponse> mapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getString("job_id")).thenReturn("job-1");
                    when(rs.getString("reference_id")).thenReturn("hotmart-abc");
                    when(rs.getString("product_name")).thenReturn("Produto Teste");
                    when(rs.getString("product_url")).thenReturn("https://app.hotmart.com/products/abc");
                    when(rs.getString("hotmart_description")).thenReturn("Descrição comercial");
                    when(rs.getString("producer_name")).thenReturn("Produtor Oficial");
                    when(rs.getString("hotmart_image_url")).thenReturn("https://img.example/a.png");
                    when(rs.getString("hotmart_price")).thenReturn("199.00");
                    when(rs.getString("sales_page_url")).thenReturn("https://example.com/sales");
                    when(rs.getObject("hotmart_temperature")).thenReturn(java.math.BigDecimal.valueOf(88.5));
                    when(rs.getDouble("hotmart_temperature")).thenReturn(88.5);
                    when(rs.getTimestamp("collected_at")).thenReturn(Timestamp.from(Instant.parse("2026-06-13T12:00:00Z")));
                    return List.of(mapper.mapRow(rs, 0));
                });

        MoisHotmartProductService service = new MoisHotmartProductService(jdbcTemplate);
        MoisHotmartProductDtos.HotmartCollectedProductListResponse response = service.listLatestByWorkspace("workspace-001", 1);

        assertEquals("Descrição comercial", response.items().getFirst().description());
        assertEquals("Produtor Oficial", response.items().getFirst().producerName());
        assertEquals(88.5, response.items().getFirst().temperature());
        assertEquals(Instant.parse("2026-06-13T12:00:00Z"), response.items().getFirst().collectedAt());
    }

}
