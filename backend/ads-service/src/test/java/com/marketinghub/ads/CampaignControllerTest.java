package com.marketinghub.ads;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CampaignControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FacebookAccountRepository fbRepo;

    @Autowired
    InstagramAccountRepository igRepo;

    @Test
    void shouldJoinFacebookAndInstagramCampaigns() throws Exception {
        FacebookAccount fb = fbRepo.save(FacebookAccount.builder()
                .name("FB Account")
                .currency("USD")
                .build());

        InstagramAccount ig = igRepo.save(InstagramAccount.builder()
                .name("IG Account")
                .handle("@ig.account")
                .code("IG-001")
                .build());

        mockMvc.perform(post("/api/accounts/" + fb.getId() + "/campaigns?platform=facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"FB Campaign\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        mockMvc.perform(post("/api/accounts/" + ig.getId() + "/campaigns?platform=instagram")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"IG Campaign\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        mockMvc.perform(get("/api/accounts/" + fb.getId() + "/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
