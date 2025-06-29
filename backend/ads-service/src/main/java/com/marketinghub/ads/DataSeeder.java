package com.marketinghub.ads;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(FacebookAccountRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new FacebookAccount(1L, "Account A", "USD"));
                repo.save(new FacebookAccount(2L, "Account B", "EUR"));
                repo.save(new FacebookAccount(3L, "Account C", "GBP"));
            }
        };
    }
}
