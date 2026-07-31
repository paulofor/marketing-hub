package com.marketinghub.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RuntimeBuildInfoControllerTest {

  /** Verifica que a rota usada pelo MCP publica commit e branch do runtime. */
  @Test
  void shouldExposeRuntimeBuildInfoOnActuatorPath() throws Exception {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("BACKEND_BUILD_COMMIT", "1234567890abcdef");
    environment.setProperty("BACKEND_BUILD_BRANCH", "main");
    MockMvc mockMvc =
        MockMvcBuilders.standaloneSetup(
                new RuntimeBuildInfoController(Optional.empty(), Optional.empty(), environment))
            .build();

    mockMvc
        .perform(get("/actuator/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.app.name").value("marketinghub-backend"))
        .andExpect(jsonPath("$.build.version").value("0.0.1-SNAPSHOT"))
        .andExpect(jsonPath("$.git.branch").value("main"))
        .andExpect(jsonPath("$.git['commit.id']").value("1234567890abcdef"))
        .andExpect(jsonPath("$.git['commit.id.abbrev']").value("1234567890ab"));
  }

}
