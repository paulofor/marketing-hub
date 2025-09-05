package com.marketinghub.facebookadsworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FacebookAdsWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FacebookAdsWorkerApplication.class, args);
    }
}
