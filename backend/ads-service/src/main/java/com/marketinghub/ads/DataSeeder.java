package com.marketinghub.ads;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marketinghub.ads.FacebookAccountRepository;
import com.marketinghub.ads.InstagramAccountRepository;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.repository.ExperimentRepository;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(FacebookAccountRepository fbRepo, InstagramAccountRepository igRepo,
                          ExperimentRepository expRepo) {
        return args -> {
            if (fbRepo.count() == 0) {
                fbRepo.save(new FacebookAccount(1L, "Account A", "USD"));
                fbRepo.save(new FacebookAccount(2L, "Account B", "EUR"));
                fbRepo.save(new FacebookAccount(3L, "Account C", "GBP"));
            }
            if (igRepo.count() == 0) {
                igRepo.save(new InstagramAccount(1L, "Insta A", "USD", "https://example.com/a.png"));
                igRepo.save(new InstagramAccount(2L, "Insta B", "EUR", "https://example.com/b.png"));
                igRepo.save(new InstagramAccount(3L, "Insta C", "GBP", "https://example.com/c.png"));
            }
            if (expRepo.count() == 0) {
                expRepo.save(Experiment.builder()
                        .hypothesis("Default hypothesis")
                        .kpiTarget(java.math.BigDecimal.valueOf(10))
                        .status(ExperimentStatus.PLANNED)
                        .build());
            }
        };
    }
}
