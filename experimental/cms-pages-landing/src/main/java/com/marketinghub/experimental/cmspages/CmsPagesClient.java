package com.marketinghub.experimental.cmspages;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CmsPagesClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CmsPagesClient(RestTemplate restTemplate, @Value("${cms.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void createLandingPage(LandingPage page) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LandingPage> request = new HttpEntity<>(page, headers);
        restTemplate.postForEntity(baseUrl + "/pages", request, Void.class);
    }
}
