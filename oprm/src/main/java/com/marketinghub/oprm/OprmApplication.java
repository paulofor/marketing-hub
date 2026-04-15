package com.marketinghub.oprm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OprmApplication {

    public static void main(String[] args) {
        SpringApplication.run(OprmApplication.class, args);
    }
}
