package com.marketinghub.facebookads;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FacebookAdsClientApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FacebookAdsClientApplication.class);
    private final FacebookAdsClient client;

    public FacebookAdsClientApplication(FacebookAdsClient client) {
        this.client = client;
    }

    public static void main(String[] args) {
        SpringApplication.run(FacebookAdsClientApplication.class, args);
    }

    @Override
    public void run(String... args) {
        JsonNode accounts = client.getAdAccounts();
        log.info("Fetched accounts: {}", accounts);
    }
}
