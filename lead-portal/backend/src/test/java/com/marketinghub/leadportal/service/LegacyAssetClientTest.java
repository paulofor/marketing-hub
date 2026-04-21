package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class LegacyAssetClientTest {

    private RestTemplate restTemplate;
    private LegacyAssetClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new LegacyAssetClient(restTemplate);
    }

    @Test
    void shouldReturnEmptyWhenLegacyAssetIsNotFound() {
        when(restTemplate.exchange(eq("http://legacy.example.com/uploads/missing.png"), eq(HttpMethod.GET), eq(null), eq(byte[].class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        Optional<LegacyAssetClient.DownloadedAsset> result =
                client.fetch("http://legacy.example.com/uploads/missing.png");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyOnOtherClientErrors() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), eq(null), eq(byte[].class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        Optional<LegacyAssetClient.DownloadedAsset> result = client.fetch("http://legacy.example.com/uploads/invalid.png");

        assertThat(result).isEmpty();
    }
}
