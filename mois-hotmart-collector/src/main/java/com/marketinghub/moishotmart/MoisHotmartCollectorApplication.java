package com.marketinghub.moishotmart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoisHotmartCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoisHotmartCollectorApplication.class, args);
    }
}
