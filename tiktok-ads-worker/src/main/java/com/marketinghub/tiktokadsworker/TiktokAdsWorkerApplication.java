package com.marketinghub.tiktokadsworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Inicializa o módulo independente de integração com TikTok Ads. */
@SpringBootApplication
public class TiktokAdsWorkerApplication {

    /** Sobe a aplicação Spring Boot do worker TikTok Ads. */
    public static void main(String[] args) {
        SpringApplication.run(TiktokAdsWorkerApplication.class, args);
    }
}
