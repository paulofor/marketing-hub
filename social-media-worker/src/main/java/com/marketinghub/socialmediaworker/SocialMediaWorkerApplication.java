package com.marketinghub.socialmediaworker;

import com.marketinghub.socialmediaworker.config.SocialMediaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Inicializa o executor de integracoes com midias sociais do Marketing Hub.
 */
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(SocialMediaProperties.class)
public class SocialMediaWorkerApplication {

    /**
     * Sobe a aplicacao Spring Boot do worker de midias sociais.
     */
    public static void main(String[] args) {
        SpringApplication.run(SocialMediaWorkerApplication.class, args);
    }
}
