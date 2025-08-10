package com.marketinghub.funnel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FunnelController.class)
class FunnelControllerTest {

    @SpringBootApplication
    static class TestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FunnelService funnelService;

    @Test
    void getFunnelDoesNotReturnNestedFunnel() throws Exception {
        UUID id = UUID.randomUUID();
        SalesFunnel funnel = SalesFunnel.builder().id(id).name("test").build();
        FunnelStep step = FunnelStep.builder().id(UUID.randomUUID()).funnel(funnel).note("obs").build();
        funnel.setSteps(List.of(step));
        when(funnelService.get(id)).thenReturn(funnel);

        mockMvc.perform(get("/api/funnels/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[0].funnel").doesNotExist())
                .andExpect(jsonPath("$.steps[0].note").value("obs"));
    }
}

