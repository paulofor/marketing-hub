package com.marketinghub.moisclickbank.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionResponse;
import com.marketinghub.moisclickbank.service.ClickbankCollectorService;
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
class ClickbankCollectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClickbankCollectorService collectorService;

    @Test
    void healthShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/mois-clickbank/health")).andExpect(status().isOk());
    }

    @Test
    void collectShouldReturnCollectionResponse() throws Exception {
        when(collectorService.collect(any())).thenReturn(new ClickbankCollectionResponse(
                "COLLECTION_EXECUTED",
                "Coleta executada com Playwright em modo headless=true.",
                List.of()
        ));

        mockMvc.perform(post("/api/v1/mois-clickbank/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "clickbank-market",
                                  "maxProducts": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTION_EXECUTED"));
    }
}
