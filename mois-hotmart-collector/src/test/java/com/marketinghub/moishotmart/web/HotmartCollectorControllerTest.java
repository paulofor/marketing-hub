package com.marketinghub.moishotmart.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionResponse;
import com.marketinghub.moishotmart.service.HotmartCollectorService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HotmartCollectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HotmartCollectorService collectorService;

    @Test
    void healthShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/mois-hotmart/health")).andExpect(status().isOk());
    }

    @Test
    void collectShouldReturnCollectionResponse() throws Exception {
        when(collectorService.collect(any())).thenReturn(new HotmartCollectionResponse(
                "COLLECTION_EXECUTED",
                "Coleta executada com Playwright em modo headless=true.",
                List.of()
        ));

        mockMvc.perform(post("/api/v1/mois-hotmart/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "hotmart-market",
                                  "maxProducts": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTION_EXECUTED"));
    }
}
