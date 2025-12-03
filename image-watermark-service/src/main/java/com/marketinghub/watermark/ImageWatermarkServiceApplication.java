package com.marketinghub.watermark;

import com.marketinghub.watermark.config.StorageProperties;
import com.marketinghub.watermark.config.WatermarkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({StorageProperties.class, WatermarkProperties.class})
public class ImageWatermarkServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageWatermarkServiceApplication.class, args);
    }
}
