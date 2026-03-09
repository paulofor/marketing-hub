package com.marketinghub.facebookads.web;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.FacebookAccountRepository;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.ads.FacebookPageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
class FacebookConfigurationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FacebookAccountRepository accountRepository;

    @Autowired
    FacebookPageRepository pageRepository;

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

    @Test
    void shouldExposeRecordedWorkerValidationError() throws Exception {
        accountRepository.save(FacebookAccount.builder()
            .name("Worker Account")
            .currency("BRL")
            .workerEnabled(true)
            .workerLastValidationAt(LocalDateTime.of(2026, 5, 20, 13, 45))
            .workerLastValidationErrorCode("AD_ACCOUNT_ID_MISSING")
            .workerLastValidationErrorDetail("Facebook worker account is missing ad account id")
            .build());

        mockMvc.perform(get("/api/facebook/configuration-status").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.worker.messages[0].code").value("AD_ACCOUNT_ID_MISSING_RECORDED"))
            .andExpect(jsonPath("$.worker.messages[0].message", containsString("ID da conta de anúncios")))
            .andExpect(jsonPath("$.worker.messages[0].message", containsString("20/05/2026 13:45")));
    }
}
