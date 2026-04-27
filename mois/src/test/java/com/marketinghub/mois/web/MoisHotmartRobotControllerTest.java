package com.marketinghub.mois.web;

import com.marketinghub.mois.dto.MoisAutomationDtos;
import com.marketinghub.mois.service.MoisHotmartRobotService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MoisHotmartRobotController.class)
class MoisHotmartRobotControllerTest {

    @SpringBootApplication
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MoisHotmartRobotService service;

    @Test
    void shouldTriggerManualExecution() throws Exception {
        when(service.triggerManualRun())
                .thenReturn(new MoisAutomationDtos.HotmartRobotRunResponse(
                        "run-001",
                        "SUCCESS",
                        "MANUAL",
                        "workspace-001",
                        "marketing-digital",
                        "ofertas-quentes",
                        "job-001",
                        80,
                        25,
                        Instant.parse("2026-04-27T10:00:00Z"),
                        null
                ));

        mockMvc.perform(post("/api/v1/mois/automation/hotmart/run"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.collectionJobId").value("job-001"));
    }

    @Test
    void shouldListRuns() throws Exception {
        when(service.listRuns(eq(10)))
                .thenReturn(new MoisAutomationDtos.HotmartRobotRunListResponse(List.of(
                        new MoisAutomationDtos.HotmartRobotRunResponse(
                                "run-001",
                                "FAILED",
                                "SCHEDULER",
                                "workspace-001",
                                "marketing-digital",
                                "ofertas-quentes",
                                null,
                                80,
                                25,
                                Instant.parse("2026-04-27T03:10:00Z"),
                                "collection rollout is disabled for workspace"
                        )
                )));

        mockMvc.perform(get("/api/v1/mois/automation/hotmart/runs").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("FAILED"));
    }
}
