package com.marketinghub.vitrines.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.marketinghub.vitrines.app")
public class VitrinesApplication {

  public static void main(String[] args) {
    SpringApplication.run(VitrinesApplication.class, args);
  }
}
