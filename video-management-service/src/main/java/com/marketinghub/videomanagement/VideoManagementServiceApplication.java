package com.marketinghub.videomanagement;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VideoManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoManagementServiceApplication.class, args);
    }
}
