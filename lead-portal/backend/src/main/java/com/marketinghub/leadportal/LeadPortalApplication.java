package com.marketinghub.leadportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeadPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeadPortalApplication.class, args);
    }
}
