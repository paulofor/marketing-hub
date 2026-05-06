package com.marketinghub.moishotmart.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HotmartCollectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/mois-hotmart/health"))
                .andExpect(status().isOk());
    }

    @Test
    void collectShouldReturnBootstrapResponse() throws Exception {
        mockMvc.perform(post("/api/v1/mois-hotmart/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "hotmart-market",
                                  "maxProducts": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_AUTOMATION"));
    }
}
