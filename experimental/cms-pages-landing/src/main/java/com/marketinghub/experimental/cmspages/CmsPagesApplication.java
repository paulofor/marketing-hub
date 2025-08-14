package com.marketinghub.experimental.cmspages;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class CmsPagesApplication implements CommandLineRunner {

    private final CmsPagesClient client;

    public CmsPagesApplication(CmsPagesClient client) {
        this.client = client;
    }

    public static void main(String[] args) {
        SpringApplication.run(CmsPagesApplication.class, args);
    }

    @Override
    public void run(String... args) {
        LandingPage page = new LandingPage("Sample Landing Page", "<h1>Hello!</h1>");
        client.createLandingPage(page);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
