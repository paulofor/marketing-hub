package com.marketinghub.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "com.marketinghub",
        exclude = LiquibaseAutoConfiguration.class
)
@EntityScan("com.marketinghub")
public class AiWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiWorkerApplication.class, args);
    }
}
