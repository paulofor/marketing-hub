package com.marketinghub.microservice.service;

import com.marketinghub.microservice.dto.DiscoveredMicroserviceDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicroserviceDiscoveryServiceTest {

    @Test
    void shouldDiscoverServicesFromComposeFile() throws IOException {
        Path composeFile = Files.createTempFile("compose", ".yml");
        Files.writeString(composeFile, """
                version: '3.9'
                services:
                  api-service:
                    image: sample/api:latest
                    ports:
                      - "8081:8080"
                  worker:
                    image: sample/worker:latest
                    ports:
                      - "9090"
                """);

        MicroserviceDiscoveryService service = new MicroserviceDiscoveryService(composeFile.toString(), "/healthz");

        List<DiscoveredMicroserviceDto> discovered = service.discoverFromCompose();

        assertEquals(2, discovered.size());
        DiscoveredMicroserviceDto apiService = discovered.stream()
                .filter(dto -> dto.serviceName().equals("api-service"))
                .findFirst()
                .orElseThrow();

        assertEquals("http://localhost:8081", apiService.baseUrl());
        assertEquals("/healthz", apiService.healthCheckPath());
        assertEquals(8081, apiService.hostPort());
        assertEquals(8080, apiService.containerPort());

        DiscoveredMicroserviceDto worker = discovered.stream()
                .filter(dto -> dto.serviceName().equals("worker"))
                .findFirst()
                .orElseThrow();

        assertEquals("http://worker:9090", worker.baseUrl());
        assertEquals("/healthz", worker.healthCheckPath());
        assertEquals(9090, worker.hostPort());
        assertEquals(9090, worker.containerPort());

        Files.deleteIfExists(composeFile);
    }

    @Test
    void shouldReturnEmptyListWhenFileDoesNotExist() {
        MicroserviceDiscoveryService service = new MicroserviceDiscoveryService("non-existent-compose.yml", "/health");

        List<DiscoveredMicroserviceDto> discovered = service.discoverFromCompose();

        assertTrue(discovered.isEmpty());
    }
}
