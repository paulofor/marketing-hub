package com.marketinghub.zipper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ImageZipperServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageZipperServiceApplication.class, args);
    }
}
