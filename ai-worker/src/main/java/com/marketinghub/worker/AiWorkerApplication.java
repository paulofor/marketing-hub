package com.marketinghub.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan({"com.marketinghub.worker", "com.marketinghub.ads"})
@EnableScheduling
public class AiWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiWorkerApplication.class, args);
    }
}
