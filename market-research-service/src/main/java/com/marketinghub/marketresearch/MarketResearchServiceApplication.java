package com.marketinghub.marketresearch;

import com.marketinghub.marketresearch.config.MarketResearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MarketResearchProperties.class)
public class MarketResearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketResearchServiceApplication.class, args);
    }
}
