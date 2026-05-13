package com.marketinghub.moisclickbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoisClickbankCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoisClickbankCollectorApplication.class, args);
    }
}
