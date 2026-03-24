package com.marketinghub.videomanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.marketinghub.videomanagement.config.VideoManagementProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(VideoManagementProperties.class)
public class VideoManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoManagementServiceApplication.class, args);
    }
}
