package com.marketinghub.course.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for Kajabi AI API.
 */
@Component
public class KajabiClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public KajabiClient(RestTemplate restTemplate,
                        @Value("${kajabi.base-url:https://api.kajabi.com}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> proposeCoursePlan(Map<String, Object> request) {
        String url = baseUrl + "/ai/course-plan";
        return restTemplate.postForObject(url, request, Map.class);
    }
}
