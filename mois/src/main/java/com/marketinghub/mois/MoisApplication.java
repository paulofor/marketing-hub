package com.marketinghub.mois;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoisApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoisApplication.class, args);
    }
}
