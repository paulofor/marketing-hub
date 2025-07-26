package com.marketinghub.ads;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import com.marketinghub.test.MySqlContainerBase;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.IntStream;

import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.FacebookAccountRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FacebookAccountControllerTest extends MySqlContainerBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FacebookAccountRepository repository;

    @Test
    void shouldReturnThreeAccounts() throws Exception {
        IntStream.rangeClosed(1, 3)
                .forEach(i -> repository.save(FacebookAccount.builder()
                        .name("Account " + i)
                        .currency("USD")
                        .build()));

        mockMvc.perform(get("/accounts/facebook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
