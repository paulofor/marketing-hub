package com.marketinghub.creative.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.WebConfig;
import com.marketinghub.creative.mapper.CreativeMapper;
import com.marketinghub.creative.service.CreativeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CreativeController.class)
@Import(WebConfig.class)
class CreativeControllerCorsTest {

    private static final String REQUEST_ORIGIN = "http://app.local";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreativeService creativeService;

    @MockBean
    private CreativeMapper creativeMapper;

    @Test
    void uploadImageRespondsWithCorsHeaders() throws Exception {
        when(creativeService.uploadImage(any(), any(), any())).thenReturn("/uploads/mock.png");

        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", "dummy".getBytes());

        mockMvc.perform(multipart("/api/assets")
                        .file(file)
                        .param("prompt", "simple-form-test")
                        .header(HttpHeaders.ORIGIN, REQUEST_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, REQUEST_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
    }

    @Test
    void preflightRequestIncludesCorsMetadata() throws Exception {
        mockMvc.perform(options("/api/assets")
                        .header(HttpHeaders.ORIGIN, REQUEST_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, REQUEST_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")));
    }
}
