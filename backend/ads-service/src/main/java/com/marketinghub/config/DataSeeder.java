package com.marketinghub.config;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Arrays;

@Configuration("nicheDataSeeder")
public class DataSeeder {
    @Bean
    CommandLineRunner seedData(MarketNicheRepository nicheRepo, ExperimentRepository expRepo) {
        return args -> {
            if (nicheRepo.count() > 0) return;
            MarketNiche horta = nicheRepo.save(MarketNiche.builder().name("Horta Urbana").build());
            MarketNiche pets = nicheRepo.save(MarketNiche.builder().name("Pet Lovers").build());

            expRepo.saveAll(Arrays.asList(
                    Experiment.builder()
                            .niche(horta)
                            .name("Teste A")
                            .hypothesis("Hipotese A")
                            .kpiTarget(new BigDecimal("10"))
                            .status(ExperimentStatus.PLANNED)
                            .platform(ExperimentPlatform.FACEBOOK)
                            .build(),
                    Experiment.builder()
                            .niche(horta)
                            .name("Teste B")
                            .hypothesis("Hipotese B")
                            .kpiTarget(new BigDecimal("15"))
                            .status(ExperimentStatus.RUNNING)
                            .platform(ExperimentPlatform.FACEBOOK)
                            .build(),
                    Experiment.builder()
                            .niche(pets)
                            .name("Teste C")
                            .hypothesis("Hipotese C")
                            .kpiTarget(new BigDecimal("20"))
                            .status(ExperimentStatus.PAUSED)
                            .platform(ExperimentPlatform.FACEBOOK)
                            .build()
            ));
        };
    }
}
