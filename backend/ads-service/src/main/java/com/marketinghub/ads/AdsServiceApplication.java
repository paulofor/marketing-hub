package com.marketinghub.ads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.marketinghub")
@EnableJpaRepositories(basePackages = "com.marketinghub")
@EntityScan(basePackages = "com.marketinghub")
@EnableAsync
@EnableScheduling
public class AdsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdsServiceApplication.class, args);
    }
}
