package com.marketinghub.ads;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import com.marketinghub.ads.FacebookAccountRepository;
import com.marketinghub.ads.InstagramAccountRepository;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;

@Configuration("adsDataSeeder")
public class DataSeeder {

    @Bean
    @Transactional
    CommandLineRunner seed(FacebookAccountRepository fbRepo,
                          InstagramAccountRepository igRepo,
                          ExperimentRepository expRepo,
                          MarketNicheRepository nicheRepo) {
        return args -> {
            if (fbRepo.count() == 0) {
                fbRepo.save(FacebookAccount.builder()
                        .name("Account A")
                        .currency("USD")
                        .build());
                fbRepo.save(FacebookAccount.builder()
                        .name("Account B")
                        .currency("EUR")
                        .build());
                fbRepo.save(FacebookAccount.builder()
                        .name("Account C")
                        .currency("GBP")
                        .build());
            }
            if (igRepo.count() == 0) {
                igRepo.save(InstagramAccount.builder()
                        .name("Insta A")
                        .currency("USD")
                        .avatarUrl("https://example.com/a.png")
                        .build());
                igRepo.save(InstagramAccount.builder()
                        .name("Insta B")
                        .currency("EUR")
                        .avatarUrl("https://example.com/b.png")
                        .build());
                igRepo.save(InstagramAccount.builder()
                        .name("Insta C")
                        .currency("GBP")
                        .avatarUrl("https://example.com/c.png")
                        .build());
            }
            if (expRepo.count() == 0) {
                MarketNiche niche = nicheRepo.findAll().stream().findFirst()
                        .orElseGet(() -> nicheRepo.save(MarketNiche.builder().name("Default Niche").build()));
                expRepo.save(Experiment.builder()
                        .niche(niche)
                        .name("Default Experiment")
                        .hypothesis("Default hypothesis")
                        .kpiTarget(java.math.BigDecimal.valueOf(10))
                        .status(ExperimentStatus.PLANNED)
                        .platform(ExperimentPlatform.FACEBOOK)
                        .build());
            }
        };
    }
}
