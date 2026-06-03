package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibraryService;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibrarySnapshotService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MoisSalesLibraryController.class)
class MoisSalesLibraryControllerTest {

    @SpringBootApplication
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MoisSalesLibraryService service;

    @MockBean
    private MoisSalesLibrarySnapshotService snapshotService;

    @Test
    void shouldExposeSnapshotCaptureEndpoint() throws Exception {
        when(snapshotService.captureSnapshots(any(MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest.class)))
                .thenReturn(new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureResponse(
                        "workspace-001",
                        3,
                        false,
                        1,
                        1,
                        0,
                        List.of(new MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureItem(
                                10L,
                                20L,
                                "https://example.test/sales",
                                "https://example.test/sales",
                                "https://example.test",
                                "CAPTURED",
                                "abc123",
                                200,
                                512L,
                                1024L,
                                null
                        )),
                        Instant.parse("2026-05-18T22:00:00Z")
                ));

        mockMvc.perform(post("/api/mois/sales-library/snapshots:capture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceId": "workspace-001",
                                  "limit": 3,
                                  "force": false
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.workspaceId").value("workspace-001"))
                .andExpect(jsonPath("$.captured").value(1))
                .andExpect(jsonPath("$.items[0].status").value("CAPTURED"))
                .andExpect(jsonPath("$.items[0].redirectDestinationUrl").value("https://example.test/sales"))
                .andExpect(jsonPath("$.items[0].redirectRootUrl").value("https://example.test"))
                .andExpect(jsonPath("$.items[0].rawHtmlBytes").value(512))
                .andExpect(jsonPath("$.items[0].screenshotBytes").value(1024));
    }

    @Test
    void shouldExposePageSnapshotsEndpoint() throws Exception {
        when(snapshotService.listSnapshots(10L))
                .thenReturn(List.of(new MoisSalesLibraryDtos.SalesLibraryPageSnapshotResponse(
                        20L,
                        10L,
                        "abc123",
                        "CAPTURED",
                        200,
                        "text/html; charset=UTF-8",
                        "https://example.test/sales",
                        "https://example.test",
                        512L,
                        1024L,
                        Instant.parse("2026-05-18T22:00:00Z"),
                        Instant.parse("2026-05-18T22:00:01Z")
                )));

        mockMvc.perform(get("/api/mois/sales-library/pages/10/snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].snapshotId").value(20))
                .andExpect(jsonPath("$[0].status").value("CAPTURED"))
                .andExpect(jsonPath("$[0].rawHtmlBytes").value(512))
                .andExpect(jsonPath("$[0].screenshotBytes").value(1024));
    }
}
