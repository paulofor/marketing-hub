package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class MoisSalesLibraryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MoisSalesLibraryService service;

    @Test
    void shouldCreatePendingJobWhenUrlIsNew() {
        MoisSalesLibraryDtos.SalesLibraryIngestRequest request = new MoisSalesLibraryDtos.SalesLibraryIngestRequest(
                "10",
                "hotmart",
                List.of(new MoisSalesLibraryDtos.SalesLibraryUrlItem("https://example.com/pagina", "Title", Instant.parse("2026-05-19T00:00:00Z")))
        );

        given(jdbcTemplate.update(contains("INSERT INTO mois_sales_library_url_ingest"), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(1);
        given(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any())).willReturn(99L);

        service.ingestUrls(request);

        verify(jdbcTemplate).update(contains("INSERT INTO mois_sales_library_processing_job"), eq(99L), eq("PENDING"));
    }

    @Test
    void shouldNotCreatePendingJobWhenUrlAlreadyExists() {
        MoisSalesLibraryDtos.SalesLibraryIngestRequest request = new MoisSalesLibraryDtos.SalesLibraryIngestRequest(
                "10",
                "hotmart",
                List.of(new MoisSalesLibraryDtos.SalesLibraryUrlItem("https://example.com/pagina", "Title", Instant.parse("2026-05-19T00:00:00Z")))
        );

        given(jdbcTemplate.update(contains("INSERT INTO mois_sales_library_url_ingest"), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(2);

        service.ingestUrls(request);

        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class), any());
        verify(jdbcTemplate, never()).update(contains("INSERT INTO mois_sales_library_processing_job"), any(), any());
    }
}
