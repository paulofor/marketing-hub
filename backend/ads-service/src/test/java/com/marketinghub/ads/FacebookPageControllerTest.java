package com.marketinghub.ads;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
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
class FacebookPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FacebookAccountRepository accountRepository;

    @Autowired
    FacebookPageRepository pageRepository;

    @Autowired
    ObjectMapper objectMapper;

    FacebookAccount account;

    @BeforeEach
    void setup() {
        pageRepository.deleteAll();
        accountRepository.deleteAll();
        account = accountRepository.save(FacebookAccount.builder()
                .name("Account")
                .currency("BRL")
                .build());
    }

    @Test
    void shouldCreateListAndDeletePages() throws Exception {
        FacebookPageController.UpsertFacebookPageRequest request = new FacebookPageController.UpsertFacebookPageRequest(
                "123456",
                "Página Principal"
        );

        mockMvc.perform(post("/accounts/facebook/" + account.getId() + "/pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value("123456"))
                .andExpect(jsonPath("$.name").value("Página Principal"));

        FacebookPage saved = pageRepository.findAll().getFirst();

        FacebookPageController.UpsertFacebookPageRequest update = new FacebookPageController.UpsertFacebookPageRequest(
                "654321",
                "Página Atualizada"
        );

        mockMvc.perform(put("/accounts/facebook/" + account.getId() + "/pages/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value("654321"))
                .andExpect(jsonPath("$.name").value("Página Atualizada"));

        mockMvc.perform(get("/accounts/facebook/" + account.getId() + "/pages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pageId").value("654321"));

        mockMvc.perform(delete("/accounts/facebook/" + account.getId() + "/pages/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts/facebook/" + account.getId() + "/pages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
