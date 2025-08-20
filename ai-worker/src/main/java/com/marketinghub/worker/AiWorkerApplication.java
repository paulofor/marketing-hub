package com.marketinghub.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        scanBasePackages = {
                "com.marketinghub.worker",
                "com.marketinghub.niche",
                "com.marketinghub.hypothesis",
                "com.marketinghub.creative"
        })
@EntityScan({
        "com.marketinghub.worker",
        "com.marketinghub.ads",
        "com.marketinghub.niche",
        "com.marketinghub.hypothesis",
        "com.marketinghub.creative"
})
@EnableScheduling
public class AiWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiWorkerApplication.class, args);
    }
}
