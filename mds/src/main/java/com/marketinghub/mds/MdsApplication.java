package com.marketinghub.mds;

import com.marketinghub.mds.config.MdsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(MdsProperties.class)
public class MdsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MdsApplication.class, args);
    }
}
