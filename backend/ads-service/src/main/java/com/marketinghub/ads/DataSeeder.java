package com.marketinghub.ads;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marketinghub.ads.FacebookAccountRepository;
import com.marketinghub.ads.InstagramAccountRepository;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.InstagramAccount;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(FacebookAccountRepository fbRepo, InstagramAccountRepository igRepo) {
        return args -> {
            if (fbRepo.count() == 0) {
                fbRepo.save(new FacebookAccount(1L, "Account A", "USD"));
                fbRepo.save(new FacebookAccount(2L, "Account B", "EUR"));
                fbRepo.save(new FacebookAccount(3L, "Account C", "GBP"));
            }
            if (igRepo.count() == 0) {
                igRepo.save(new InstagramAccount(1L, "Insta A", "USD"));
                igRepo.save(new InstagramAccount(2L, "Insta B", "EUR"));
                igRepo.save(new InstagramAccount(3L, "Insta C", "GBP"));
            }
        };
    }
}
