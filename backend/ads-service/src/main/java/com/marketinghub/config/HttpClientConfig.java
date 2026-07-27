package com.marketinghub.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for {@link HttpClient}. */
@Configuration
public class HttpClientConfig {
  @Bean
  public HttpClient httpClient() {
    return HttpClient.newHttpClient();
  }
}
