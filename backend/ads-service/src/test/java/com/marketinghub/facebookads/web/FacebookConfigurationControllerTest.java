package com.marketinghub.facebookads.web;

import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.FacebookAccountRepository;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.ads.FacebookPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class FacebookConfigurationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FacebookAccountRepository accountRepository;

    @Autowired
    FacebookPageRepository pageRepository;

    @BeforeEach
    void clean() {
        pageRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void shouldReportConfigurationStatus() throws Exception {
        mockMvc.perform(get("/api/facebook/configuration-status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConfiguredPages").value(false));

        FacebookAccount account = accountRepository.save(FacebookAccount.builder()
                .name("Account")
                .currency("USD")
                .build());

        pageRepository.save(FacebookPage.builder()
                .account(account)
                .pageId("123")
                .name("Main Page")
                .build());

        mockMvc.perform(get("/api/facebook/configuration-status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConfiguredPages").value(true));
    }
}
