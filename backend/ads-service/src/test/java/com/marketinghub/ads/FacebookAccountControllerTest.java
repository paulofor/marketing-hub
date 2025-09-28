package com.marketinghub.ads;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.FacebookAccountRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.contains;

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
public class FacebookAccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FacebookAccountRepository repository;

    @Test
    void shouldReturnAccountsWithTokenStatus() throws Exception {
        repository.save(FacebookAccount.builder()
                .name("Account valid")
                .currency("USD")
                .accessToken("token-valid")
                .tokenExpiresAt(LocalDateTime.now().plusDays(30))
                .authorizedUserId("123")
                .authorizedUserName("Marketing Hub")
                .authorizedUserEmail("contato@example.com")
                .build());

        repository.save(FacebookAccount.builder()
                .name("Account expiring")
                .currency("USD")
                .accessToken("token-expiring")
                .tokenExpiresAt(LocalDateTime.now().plusDays(2))
                .build());

        repository.save(FacebookAccount.builder()
                .name("Account missing token")
                .currency("USD")
                .build());

        mockMvc.perform(get("/api/accounts/facebook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.name=='Account valid')].requiresTokenRenewal", contains(false)))
                .andExpect(jsonPath("$[?(@.name=='Account expiring')].requiresTokenRenewal", contains(true)))
                .andExpect(jsonPath("$[?(@.name=='Account missing token')].requiresTokenRenewal", contains(true)))
                .andExpect(jsonPath("$[?(@.name=='Account expiring')].tokenExpired", contains(false)));
    }
}
